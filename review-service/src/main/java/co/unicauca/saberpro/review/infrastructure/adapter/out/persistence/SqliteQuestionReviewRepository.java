package co.unicauca.saberpro.review.infrastructure.adapter.out.persistence;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException;
import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.QuestionSubmission;
import co.unicauca.saberpro.review.domain.model.ReviewRound;
import co.unicauca.saberpro.review.domain.model.ReviewStatus;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptador de salida: guarda el agregado QuestionReview en SQLite, la base
 * de datos PROPIA del review-service (en Docker vive en un volumen).
 *
 * Tablas (una por parte del agregado; el agregado se lee y se escribe entero):
 * <pre>
 *   review_case      (questionId PK, authorLogin, version)       raiz del agregado
 *   review_round     (questionId, roundNumber) PK, version de la pregunta, estado
 *   round_assignment (questionId, roundNumber, reviewerLogin) PK
 *   round_evaluation (id PK, UNIQUE(questionId, roundNumber))   un veredicto por ronda
 * </pre>
 *
 * Bloqueo optimista: la columna version de review_case se incrementa en cada
 * guardado con "UPDATE ... WHERE version = ?". Si otra operacion guardo
 * antes, no se actualiza ninguna fila, se deshace la transaccion y se lanza
 * ConcurrentReviewUpdateException (409). Cada guardado es una transaccion.
 *
 * Metodos synchronized: se comparte una sola conexion JDBC entre hilos.
 */
public class SqliteQuestionReviewRepository implements QuestionReviewRepository, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqliteQuestionReviewRepository.class);

    private final String url;
    private Connection conn;

    public SqliteQuestionReviewRepository(String url) {
        this.url = url;
        initDatabase();
    }

    private Connection connect() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(url);
            try (Statement pragma = conn.createStatement()) {
                pragma.execute("PRAGMA busy_timeout = 5000;");
                pragma.execute("PRAGMA journal_mode = WAL;");
                pragma.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return conn;
    }

    private synchronized void initDatabase() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS review_case ("
                + " questionId TEXT PRIMARY KEY,"
                + " authorLogin TEXT,"
                + " version INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS review_round ("
                + " questionId TEXT NOT NULL REFERENCES review_case(questionId),"
                + " roundNumber INTEGER NOT NULL,"
                + " questionVersion INTEGER NOT NULL,"
                + " directQuestion TEXT,"
                + " submittedAt TEXT NOT NULL,"
                + " status TEXT NOT NULL,"
                + " PRIMARY KEY (questionId, roundNumber))",
            "CREATE INDEX IF NOT EXISTS idx_review_round_status ON review_round (status)",
            "CREATE TABLE IF NOT EXISTS round_assignment ("
                + " questionId TEXT NOT NULL,"
                + " roundNumber INTEGER NOT NULL,"
                + " reviewerLogin TEXT NOT NULL,"
                + " assignedBy TEXT,"
                + " assignedAt TEXT NOT NULL,"
                + " PRIMARY KEY (questionId, roundNumber, reviewerLogin),"
                + " FOREIGN KEY (questionId, roundNumber) REFERENCES review_round(questionId, roundNumber))",
            "CREATE INDEX IF NOT EXISTS idx_round_assignment_reviewer ON round_assignment (reviewerLogin)",
            "CREATE TABLE IF NOT EXISTS round_evaluation ("
                + " id TEXT PRIMARY KEY,"
                + " questionId TEXT NOT NULL,"
                + " roundNumber INTEGER NOT NULL,"
                + " reviewerLogin TEXT NOT NULL,"
                + " verdict TEXT NOT NULL,"
                + " observations TEXT,"
                + " evaluatedAt TEXT NOT NULL,"
                + " UNIQUE (questionId, roundNumber),"
                + " FOREIGN KEY (questionId, roundNumber) REFERENCES review_round(questionId, roundNumber))"
        };
        try (Statement stmt = connect().createStatement()) {
            for (String sql : ddl) {
                stmt.execute(sql);
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible crear las tablas del review-service (URL: {})", url, ex);
        }
    }

    // ------------------------------------------------------------ escritura

    @Override
    public synchronized void save(QuestionReview review) {
        try {
            Connection c = connect();
            c.setAutoCommit(false);
            try {
                saveRoot(c, review);
                for (ReviewRound round : review.rounds()) {
                    saveRound(c, review.questionId(), round);
                }
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw storageError("guardar la revision de la pregunta " + review.questionId(), ex);
        }
    }

    private void saveRoot(Connection c, QuestionReview review) throws SQLException {
        try (PreparedStatement update = c.prepareStatement(
                "UPDATE review_case SET authorLogin = ?, version = version + 1 WHERE questionId = ? AND version = ?")) {
            update.setString(1, review.authorLogin());
            update.setString(2, review.questionId());
            update.setLong(3, review.version());
            if (update.executeUpdate() == 1) {
                return;
            }
        }
        if (review.version() != 0) {
            throw new ConcurrentReviewUpdateException(review.questionId());
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO review_case (questionId, authorLogin, version) VALUES (?, ?, 1)")) {
            insert.setString(1, review.questionId());
            insert.setString(2, review.authorLogin());
            insert.executeUpdate();
        } catch (SQLException ex) {
            if (isConstraintViolation(ex)) {
                // Otra operacion creo la misma revision al mismo tiempo.
                throw new ConcurrentReviewUpdateException(review.questionId());
            }
            throw ex;
        }
    }

    private void saveRound(Connection c, String questionId, ReviewRound round) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO review_round (questionId, roundNumber, questionVersion, directQuestion, submittedAt, status)"
                        + " VALUES (?, ?, ?, ?, ?, ?)"
                        + " ON CONFLICT (questionId, roundNumber) DO UPDATE SET status = excluded.status")) {
            p.setString(1, questionId);
            p.setInt(2, round.number());
            p.setInt(3, round.questionVersion());
            p.setString(4, round.directQuestion());
            p.setString(5, round.submittedAt().toString());
            p.setString(6, round.status().name());
            p.executeUpdate();
        }
        // Asignaciones y evaluaciones solo se agregan, nunca se modifican.
        for (ReviewerAssignment a : round.assignments()) {
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT OR IGNORE INTO round_assignment (questionId, roundNumber, reviewerLogin, assignedBy, assignedAt)"
                            + " VALUES (?, ?, ?, ?, ?)")) {
                p.setString(1, questionId);
                p.setInt(2, round.number());
                p.setString(3, a.reviewerLogin());
                p.setString(4, a.assignedBy());
                p.setString(5, a.assignedAt().toString());
                p.executeUpdate();
            }
        }
        if (round.evaluation().isPresent()) {
            Evaluation e = round.evaluation().get();
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT OR IGNORE INTO round_evaluation (id, questionId, roundNumber, reviewerLogin, verdict,"
                            + " observations, evaluatedAt) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                p.setString(1, e.id());
                p.setString(2, questionId);
                p.setInt(3, round.number());
                p.setString(4, e.reviewerLogin());
                p.setString(5, e.verdict().name());
                p.setString(6, e.observations());
                p.setString(7, e.evaluatedAt().toString());
                p.executeUpdate();
            }
        }
    }

    // ------------------------------------------------------------ lectura

    @Override
    public synchronized Optional<QuestionReview> findById(String questionId) {
        try {
            Connection c = connect();
            String author;
            long version;
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT authorLogin, version FROM review_case WHERE questionId = ?")) {
                p.setString(1, questionId);
                try (ResultSet rs = p.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    author = rs.getString("authorLogin");
                    version = rs.getLong("version");
                }
            }
            Map<Integer, List<ReviewerAssignment>> assignments = loadAssignments(c, questionId);
            Map<Integer, Evaluation> evaluations = loadEvaluations(c, questionId);

            List<ReviewRound> rounds = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement(
                    "SELECT roundNumber, questionVersion, directQuestion, submittedAt, status FROM review_round"
                            + " WHERE questionId = ? ORDER BY roundNumber")) {
                p.setString(1, questionId);
                try (ResultSet rs = p.executeQuery()) {
                    while (rs.next()) {
                        int number = rs.getInt("roundNumber");
                        QuestionSubmission submission = new QuestionSubmission(questionId,
                                rs.getInt("questionVersion"), author, rs.getString("directQuestion"),
                                Instant.parse(rs.getString("submittedAt")));
                        rounds.add(ReviewRound.restore(number, submission, ReviewStatus.valueOf(rs.getString("status")),
                                assignments.getOrDefault(number, List.of()), evaluations.get(number)));
                    }
                }
            }
            return Optional.of(QuestionReview.restore(questionId, author, rounds, version));
        } catch (SQLException ex) {
            throw storageError("consultar la revision de la pregunta " + questionId, ex);
        }
    }

    private Map<Integer, List<ReviewerAssignment>> loadAssignments(Connection c, String questionId)
            throws SQLException {
        Map<Integer, List<ReviewerAssignment>> result = new HashMap<>();
        try (PreparedStatement p = c.prepareStatement(
                "SELECT roundNumber, reviewerLogin, assignedBy, assignedAt FROM round_assignment"
                        + " WHERE questionId = ? ORDER BY assignedAt, reviewerLogin")) {
            p.setString(1, questionId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    result.computeIfAbsent(rs.getInt("roundNumber"), k -> new ArrayList<>())
                            .add(new ReviewerAssignment(rs.getString("reviewerLogin"), rs.getString("assignedBy"),
                                    Instant.parse(rs.getString("assignedAt"))));
                }
            }
        }
        return result;
    }

    private Map<Integer, Evaluation> loadEvaluations(Connection c, String questionId) throws SQLException {
        Map<Integer, Evaluation> result = new HashMap<>();
        try (PreparedStatement p = c.prepareStatement(
                "SELECT id, roundNumber, reviewerLogin, verdict, observations, evaluatedAt FROM round_evaluation"
                        + " WHERE questionId = ?")) {
            p.setString(1, questionId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getInt("roundNumber"), new Evaluation(rs.getString("id"),
                            rs.getString("reviewerLogin"), EvaluationVerdict.valueOf(rs.getString("verdict")),
                            rs.getString("observations"), Instant.parse(rs.getString("evaluatedAt"))));
                }
            }
        }
        return result;
    }

    @Override
    public synchronized List<QuestionReview> findAwaitingDecision() {
        return findAllWhere("SELECT r.questionId FROM review_round r"
                + " WHERE r.roundNumber = (SELECT MAX(x.roundNumber) FROM review_round x WHERE x.questionId = r.questionId)"
                + " AND r.status IN ('" + ReviewStatus.PENDIENTE_ASIGNACION.name() + "', '"
                + ReviewStatus.EN_REVISION.name() + "')", null);
    }

    @Override
    public synchronized List<QuestionReview> findByReviewer(String reviewerLogin) {
        return findAllWhere("SELECT DISTINCT questionId FROM round_assignment WHERE reviewerLogin = ?", reviewerLogin);
    }

    private List<QuestionReview> findAllWhere(String idsQuery, String parameter) {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement p = connect().prepareStatement(idsQuery)) {
            if (parameter != null) {
                p.setString(1, parameter);
            }
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString(1));
                }
            }
        } catch (SQLException ex) {
            throw storageError("consultar las revisiones", ex);
        }
        List<QuestionReview> result = new ArrayList<>();
        for (String id : ids) {
            findById(id).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public synchronized void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.warn("No fue posible cerrar la conexion SQLite", ex);
        }
    }

    private static boolean isConstraintViolation(SQLException ex) {
        return ex.getMessage() != null && (ex.getMessage().contains("UNIQUE") || ex.getMessage().contains("PRIMARY KEY")
                || ex.getMessage().contains("constraint"));
    }

    private IllegalStateException storageError(String action, SQLException ex) {
        LOGGER.error("No fue posible {}", action, ex);
        return new IllegalStateException("Error de base de datos al " + action, ex);
    }
}

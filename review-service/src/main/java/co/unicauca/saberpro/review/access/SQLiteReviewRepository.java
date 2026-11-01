package co.unicauca.saberpro.review.access;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;
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
import java.util.List;
import java.util.Optional;

/**
 * Implementacion de {@link IReviewRepository} con SQLite: base de datos
 * PROPIA del review-service (PSP-33), para que las preguntas pendientes, las
 * asignaciones y las evaluaciones sobrevivan a un reinicio. Las fechas se
 * guardan como texto ISO-8601.
 *
 * La tabla ReviewAssignment tiene como llave (questionId, reviewerLogin): un
 * revisor no puede quedar asignado dos veces a la misma pregunta.
 *
 * Metodos synchronized: se comparte una sola conexion JDBC entre los hilos de Tomcat.
 */
public class SQLiteReviewRepository implements IReviewRepository, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteReviewRepository.class);

    private final String url;
    private Connection conn;

    public SQLiteReviewRepository(String url) {
        this.url = url;
        initDatabase();
    }

    private Connection connect() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(url);
            try (Statement pragma = conn.createStatement()) {
                pragma.execute("PRAGMA busy_timeout = 5000;");
                pragma.execute("PRAGMA journal_mode = WAL;");
            }
        }
        return conn;
    }

    private synchronized void initDatabase() {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS PendingQuestion (\n"
                + "  questionId TEXT PRIMARY KEY,\n"
                + "  authorLogin TEXT,\n"
                + "  directQuestion TEXT,\n"
                + "  submittedAt TEXT\n"
                + ");",
            "CREATE TABLE IF NOT EXISTS ReviewAssignment (\n"
                + "  questionId TEXT NOT NULL,\n"
                + "  reviewerLogin TEXT NOT NULL,\n"
                + "  assignedBy TEXT,\n"
                + "  assignedAt TEXT NOT NULL,\n"
                + "  PRIMARY KEY (questionId, reviewerLogin)\n"
                + ");",
            "CREATE INDEX IF NOT EXISTS idx_assignment_reviewer ON ReviewAssignment (reviewerLogin);",
            "CREATE TABLE IF NOT EXISTS Evaluation (\n"
                + "  id TEXT PRIMARY KEY,\n"
                + "  questionId TEXT NOT NULL,\n"
                + "  reviewerLogin TEXT NOT NULL,\n"
                + "  verdict TEXT NOT NULL,\n"
                + "  observations TEXT,\n"
                + "  evaluatedAt TEXT\n"
                + ");",
            "CREATE INDEX IF NOT EXISTS idx_evaluation_question ON Evaluation (questionId);"
        };
        try (Statement stmt = connect().createStatement()) {
            for (String sql : ddl) {
                stmt.execute(sql);
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible crear las tablas del review-service (URL: {})", url, ex);
        }
    }

    // ---- Preguntas pendientes ----

    @Override
    public synchronized void savePendingQuestion(PendingQuestion q) {
        String sql = "INSERT OR REPLACE INTO PendingQuestion (questionId, authorLogin, directQuestion, submittedAt) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, q.questionId());
            p.setString(2, q.authorLogin());
            p.setString(3, q.directQuestion());
            p.setString(4, toText(q.submittedAt()));
            p.executeUpdate();
        } catch (SQLException ex) {
            throw storageError("guardar la pregunta pendiente " + q.questionId(), ex);
        }
    }

    @Override
    public synchronized Optional<PendingQuestion> findPendingQuestion(String questionId) {
        String sql = "SELECT questionId, authorLogin, directQuestion, submittedAt FROM PendingQuestion "
                + "WHERE questionId = ?";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, questionId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? Optional.of(mapPending(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw storageError("buscar la pregunta pendiente " + questionId, ex);
        }
    }

    @Override
    public synchronized List<PendingQuestion> findPendingQuestions() {
        String sql = "SELECT questionId, authorLogin, directQuestion, submittedAt FROM PendingQuestion";
        List<PendingQuestion> result = new ArrayList<>();
        try (Statement s = connect().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapPending(rs));
            }
        } catch (SQLException ex) {
            throw storageError("listar las preguntas pendientes", ex);
        }
        return result;
    }

    // ---- Asignaciones (HU04) ----

    @Override
    public synchronized void saveAssignment(ReviewAssignment a) {
        String sql = "INSERT INTO ReviewAssignment (questionId, reviewerLogin, assignedBy, assignedAt) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, a.getQuestionId());
            p.setString(2, a.getReviewerLogin());
            p.setString(3, a.getAssignedBy());
            p.setString(4, toText(a.getAssignedAt()));
            p.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("UNIQUE")) {
                throw new BusinessException("El revisor " + a.getReviewerLogin()
                        + " ya esta asignado a la pregunta " + a.getQuestionId() + ".");
            }
            throw storageError("guardar la asignacion de " + a.getReviewerLogin(), ex);
        }
    }

    @Override
    public synchronized List<ReviewAssignment> findAssignmentsByQuestion(String questionId) {
        return findAssignments("questionId", questionId);
    }

    @Override
    public synchronized List<ReviewAssignment> findAssignmentsByReviewer(String reviewerLogin) {
        return findAssignments("reviewerLogin", reviewerLogin);
    }

    private List<ReviewAssignment> findAssignments(String column, String value) {
        String sql = "SELECT questionId, reviewerLogin, assignedBy, assignedAt FROM ReviewAssignment WHERE "
                + column + " = ?";
        List<ReviewAssignment> result = new ArrayList<>();
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, value);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    result.add(new ReviewAssignment(rs.getString("questionId"), rs.getString("reviewerLogin"),
                            rs.getString("assignedBy"), toInstant(rs.getString("assignedAt"))));
                }
            }
        } catch (SQLException ex) {
            throw storageError("consultar las asignaciones por " + column + " = " + value, ex);
        }
        return result;
    }

    // ---- Evaluaciones (HU05) ----

    @Override
    public synchronized void saveEvaluation(Evaluation e) {
        String sql = "INSERT INTO Evaluation (id, questionId, reviewerLogin, verdict, observations, evaluatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, e.getId());
            p.setString(2, e.getQuestionId());
            p.setString(3, e.getReviewerLogin());
            p.setString(4, e.getVerdict().name());
            p.setString(5, e.getObservations());
            p.setString(6, toText(e.getEvaluatedAt()));
            p.executeUpdate();
        } catch (SQLException ex) {
            throw storageError("guardar la evaluacion de la pregunta " + e.getQuestionId(), ex);
        }
    }

    @Override
    public synchronized List<Evaluation> findEvaluationsByQuestion(String questionId) {
        String sql = "SELECT id, questionId, reviewerLogin, verdict, observations, evaluatedAt FROM Evaluation "
                + "WHERE questionId = ? ORDER BY evaluatedAt";
        List<Evaluation> result = new ArrayList<>();
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, questionId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    result.add(new Evaluation(rs.getString("id"), rs.getString("questionId"),
                            rs.getString("reviewerLogin"), EvaluationVerdict.valueOf(rs.getString("verdict")),
                            rs.getString("observations"), toInstant(rs.getString("evaluatedAt"))));
                }
            }
        } catch (SQLException ex) {
            throw storageError("consultar las evaluaciones de la pregunta " + questionId, ex);
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

    private static PendingQuestion mapPending(ResultSet rs) throws SQLException {
        return new PendingQuestion(rs.getString("questionId"), rs.getString("authorLogin"),
                rs.getString("directQuestion"), toInstant(rs.getString("submittedAt")));
    }

    private static String toText(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    private static Instant toInstant(String text) {
        return text != null ? Instant.parse(text) : null;
    }

    private IllegalStateException storageError(String action, SQLException ex) {
        LOGGER.error("No fue posible {}", action, ex);
        return new IllegalStateException("Error de base de datos al " + action, ex);
    }
}

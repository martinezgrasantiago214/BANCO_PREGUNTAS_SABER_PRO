package co.unicauca.saberpro.question.access;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.domain.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion de {@link IQuestionRepository} con SQLite: base de datos
 * PROPIA del question-service. Los distractores se guardan como texto
 * delimitado por "||". Los revisores asignados ya no estan aqui: pertenecen
 * al review-service.
 *
 * Metodos synchronized: se comparte una sola conexion JDBC entre los hilos de Tomcat.
 */
public class SQLiteQuestionRepository implements IQuestionRepository, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteQuestionRepository.class);
    private static final String DELIM = "\\|\\|";
    private static final String JOIN = "||";
    private static final String COLUMNS = "id, type, context, directQuestion, distractors, correctAnswer, "
            + "justification, bibliography, competence, topic, subtopic, difficultyLevel, status, "
            + "authorLogin, createdAt";

    private final String url;
    private Connection conn;

    public SQLiteQuestionRepository(String url) {
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
        String sql = "CREATE TABLE IF NOT EXISTS Question (\n"
                + "  id TEXT PRIMARY KEY,\n"
                + "  type TEXT NOT NULL,\n"
                + "  context TEXT,\n"
                + "  directQuestion TEXT,\n"
                + "  distractors TEXT,\n"
                + "  correctAnswer TEXT,\n"
                + "  justification TEXT,\n"
                + "  bibliography TEXT,\n"
                + "  competence TEXT,\n"
                + "  topic TEXT,\n"
                + "  subtopic TEXT,\n"
                + "  difficultyLevel TEXT,\n"
                + "  status TEXT NOT NULL,\n"
                + "  authorLogin TEXT,\n"
                + "  createdAt TEXT\n"
                + ");";
        try (Statement stmt = connect().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ex) {
            LOGGER.error("No fue posible crear la tabla Question (URL: {})", url, ex);
        }
    }

    @Override
    public synchronized boolean save(Question q) {
        String sql = "INSERT INTO Question (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            p.setString(1, q.getId());
            bindContent(p, q, 2);
            return p.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible guardar la pregunta {}", q.getId(), ex);
            return false;
        }
    }

    @Override
    public synchronized boolean update(Question q) {
        String sql = "UPDATE Question SET type=?, context=?, directQuestion=?, distractors=?, correctAnswer=?, "
                + "justification=?, bibliography=?, competence=?, topic=?, subtopic=?, difficultyLevel=?, "
                + "status=?, authorLogin=?, createdAt=? WHERE id=?";
        try (PreparedStatement p = connect().prepareStatement(sql)) {
            bindContent(p, q, 1);
            p.setString(15, q.getId());
            return p.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible actualizar la pregunta {}", q.getId(), ex);
            return false;
        }
    }

    @Override
    public synchronized Optional<Question> findById(String id) {
        try (PreparedStatement p = connect().prepareStatement("SELECT " + COLUMNS + " FROM Question WHERE id = ?")) {
            p.setString(1, id);
            try (ResultSet rs = p.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible buscar la pregunta {}", id, ex);
        }
        return Optional.empty();
    }

    /** Filtra en SQL (autor, estado, tema, palabra clave) y pagina con LIMIT/OFFSET. */
    @Override
    public synchronized PagedResult<Question> search(QuestionFilterCriteria criteria) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (criteria.getAuthorLogin() != null && !criteria.getAuthorLogin().isBlank()) {
            where.append(" AND authorLogin = ?");
            params.add(criteria.getAuthorLogin());
        }
        if (criteria.getStatus() != null) {
            where.append(" AND status = ?");
            params.add(criteria.getStatus().name());
        }
        if (criteria.getTopic() != null && !criteria.getTopic().isBlank()) {
            where.append(" AND topic LIKE ?");
            params.add("%" + criteria.getTopic() + "%");
        }
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            where.append(" AND (directQuestion LIKE ? OR context LIKE ?)");
            params.add("%" + criteria.getKeyword() + "%");
            params.add("%" + criteria.getKeyword() + "%");
        }

        int pageSize = criteria.getPageSize();
        int page = criteria.getPage();
        List<Question> items = new ArrayList<>();
        long total = 0;
        try {
            try (PreparedStatement count = connect().prepareStatement("SELECT COUNT(*) FROM Question" + where)) {
                bindParams(count, params);
                try (ResultSet rs = count.executeQuery()) {
                    total = rs.next() ? rs.getLong(1) : 0;
                }
            }
            try (PreparedStatement select = connect().prepareStatement("SELECT " + COLUMNS + " FROM Question"
                    + where + " ORDER BY createdAt DESC LIMIT ? OFFSET ?")) {
                bindParams(select, params);
                select.setInt(params.size() + 1, pageSize);
                select.setInt(params.size() + 2, (page - 1) * pageSize);
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible buscar preguntas (autor={}, estado={})",
                    criteria.getAuthorLogin(), criteria.getStatus(), ex);
        }
        return new PagedResult<>(items, page, pageSize, total);
    }

    private void bindParams(PreparedStatement stmt, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            stmt.setObject(i + 1, params.get(i));
        }
    }

    /** Asigna las columnas desde "type" hasta "createdAt" empezando en la posicion indicada. */
    private void bindContent(PreparedStatement p, Question q, int start) throws SQLException {
        p.setString(start, q.getType());
        p.setString(start + 1, q.getContext());
        p.setString(start + 2, q.getDirectQuestion());
        p.setString(start + 3, String.join(JOIN, q.getDistractors()));
        p.setString(start + 4, q.getCorrectAnswer());
        p.setString(start + 5, q.getJustification());
        p.setString(start + 6, q.getBibliography());
        p.setString(start + 7, q.getCompetence());
        p.setString(start + 8, q.getTopic());
        p.setString(start + 9, q.getSubtopic());
        p.setString(start + 10, q.getDifficultyLevel() != null ? q.getDifficultyLevel().name() : null);
        p.setString(start + 11, q.getStatus().name());
        p.setString(start + 12, q.getAuthorLogin());
        p.setString(start + 13, q.getCreatedAt().toString());
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        String difficulty = rs.getString("difficultyLevel");
        String createdAt = rs.getString("createdAt");
        return Question.builder()
                .id(rs.getString("id"))
                .type(rs.getString("type"))
                .context(rs.getString("context"))
                .directQuestion(rs.getString("directQuestion"))
                .distractors(splitOrEmpty(rs.getString("distractors")))
                .correctAnswer(rs.getString("correctAnswer"))
                .justification(rs.getString("justification"))
                .bibliography(rs.getString("bibliography"))
                .competence(rs.getString("competence"))
                .topic(rs.getString("topic"))
                .subtopic(rs.getString("subtopic"))
                .difficultyLevel(difficulty != null ? DifficultyLevel.valueOf(difficulty) : null)
                .status(QuestionStatus.valueOf(rs.getString("status")))
                .authorLogin(rs.getString("authorLogin"))
                .createdAt(createdAt != null ? LocalDateTime.parse(createdAt) : null)
                .build();
    }

    private List<String> splitOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(DELIM)));
    }

    /** Cierra la conexion (Spring lo invoca al apagar el servicio). */
    @Override
    public synchronized void close() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.warn("Error cerrando la conexion", ex);
        }
    }
}

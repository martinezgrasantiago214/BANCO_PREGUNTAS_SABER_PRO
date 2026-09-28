package co.unicauca.saberpro.access;

import co.unicauca.saberpro.domain.DifficultyLevel;
import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionState;
import co.unicauca.saberpro.service.PagedResult;
import co.unicauca.saberpro.service.QuestionFilterCriteria;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementacion de {@link IQuestionRepository} usando SQLite. Se guarda
 * una solucion sencilla (una tabla) tal como sugiere el taller de capas:
 * los distractores y los revisores asignados se serializan como texto
 * delimitado por "||".
 *
 * Cualquier error de base de datos se imprime SIEMPRE en la consola
 * (ademas del logger), para que un problema de conexion/driver nunca quede
 * en silencio y se vea como "no aparecen las preguntas" sin explicacion.
 */
public class SQLiteQuestionRepository implements IQuestionRepository {

    private static final Logger LOGGER = Logger.getLogger(SQLiteQuestionRepository.class.getName());
    private static final String DELIM = "\\|\\|";
    private static final String JOIN = "||";

    private final String url;
    private Connection conn;
    private volatile String lastError;

    public SQLiteQuestionRepository(String url) {
        this.url = url;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            reportError("No se encontro el driver JDBC de SQLite (org.sqlite.JDBC) en el classpath. "
                    + "Verifique que la dependencia sqlite-jdbc este empaquetada en el jar/ejecucion.", ex);
        }
        initDatabase();
    }

    @Override
    public String getLastError() {
        return lastError;
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

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS Question (\n"
                + "  id TEXT PRIMARY KEY,\n"
                + "  type TEXT,\n"
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
                + "  state TEXT,\n"
                + "  authorLogin TEXT,\n"
                + "  createdAt TEXT,\n"
                + "  assignedReviewers TEXT\n"
                + ");";
        try {
            Statement stmt = connect().createStatement();
            stmt.execute(sql);
        } catch (Exception ex) {
            reportError("No fue posible crear la tabla Question (URL: " + url + ")", ex);
        }
    }

    @Override
    public boolean save(Question q) {
        String sql = "INSERT INTO Question (id, type, context, directQuestion, distractors, "
                + "correctAnswer, justification, bibliography, competence, topic, subtopic, "
                + "difficultyLevel, state, authorLogin, createdAt, assignedReviewers) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement p = connect().prepareStatement(sql);
            bind(p, q);
            int rows = p.executeUpdate();
            return rows > 0;
        } catch (Exception ex) {
            reportError("No fue posible guardar la pregunta " + q.getId(), ex);
            return false;
        }
    }

    @Override
    public boolean update(Question q) {
        String sql = "UPDATE Question SET type=?, context=?, directQuestion=?, distractors=?, "
                + "correctAnswer=?, justification=?, bibliography=?, competence=?, topic=?, "
                + "subtopic=?, difficultyLevel=?, state=?, authorLogin=?, createdAt=?, "
                + "assignedReviewers=? WHERE id=?";
        try {
            PreparedStatement p = connect().prepareStatement(sql);
            p.setString(1, q.getType());
            p.setString(2, q.getContext());
            p.setString(3, q.getDirectQuestion());
            p.setString(4, String.join(JOIN, q.getDistractors()));
            p.setString(5, q.getCorrectAnswer());
            p.setString(6, q.getJustification());
            p.setString(7, q.getBibliography());
            p.setString(8, q.getCompetence());
            p.setString(9, q.getTopic());
            p.setString(10, q.getSubtopic());
            p.setString(11, q.getDifficultyLevel() != null ? q.getDifficultyLevel().name() : null);
            p.setString(12, q.getState().name());
            p.setString(13, q.getAuthorLogin());
            p.setString(14, q.getCreatedAt().toString());
            p.setString(15, String.join(JOIN, q.getAssignedReviewers()));
            p.setString(16, q.getId());
            int rows = p.executeUpdate();
            return rows > 0;
        } catch (Exception ex) {
            reportError("No fue posible actualizar la pregunta " + q.getId(), ex);
            return false;
        }
    }

    @Override
    public Optional<Question> findById(String id) {
        lastError = null;
        String sql = "SELECT * FROM Question WHERE id = ?";
        try {
            PreparedStatement p = connect().prepareStatement(sql);
            p.setString(1, id);
            ResultSet rs = p.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible buscar la pregunta " + id, ex);
        }
        return Optional.empty();
    }

    @Override
    public List<Question> findAll() {
        lastError = null;
        List<Question> result = new ArrayList<>();
        String sql = "SELECT * FROM Question ORDER BY createdAt DESC";
        try {
            Statement stmt = connect().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible listar las preguntas", ex);
        }
        return result;
    }

    @Override
    public List<Question> findByState(QuestionState state) {
        lastError = null;
        List<Question> result = new ArrayList<>();
        String sql = "SELECT * FROM Question WHERE state = ? ORDER BY createdAt DESC";
        try {
            PreparedStatement p = connect().prepareStatement(sql);
            p.setString(1, state.name());
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible listar las preguntas en estado " + state, ex);
        }
        return result;
    }

    /**
     * Filtra en SQL (autor, estado, tema, palabra clave) y pagina en
     * memoria sobre el resultado ya filtrado.
     */
    @Override
    public PagedResult<Question> search(QuestionFilterCriteria criteria) {
        lastError = null;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (criteria.getAuthorLogin() != null && !criteria.getAuthorLogin().isBlank()) {
            where.append(" AND authorLogin = ?");
            params.add(criteria.getAuthorLogin());
        }
        if (criteria.getState() != null) {
            where.append(" AND state = ?");
            params.add(criteria.getState().name());
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

        List<Question> matched = new ArrayList<>();
        try {
            PreparedStatement stmt = connect().prepareStatement(
                    "SELECT * FROM Question" + where + " ORDER BY createdAt DESC");

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                matched.add(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible buscar preguntas (criterios: autor=" + criteria.getAuthorLogin()
                    + ", estado=" + criteria.getState() + ")", ex);
        }

        int pageSize = Math.max(1, criteria.getPageSize());
        int page = Math.max(1, criteria.getPage());

        int from = Math.min((page - 1) * pageSize, matched.size());
        int to = Math.min(from + pageSize, matched.size());

        return new PagedResult<>(
                matched.subList(from, to),
                page,
                pageSize,
                matched.size()
        );
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        Question q = new Question();

        q.setId(rs.getString("id"));
        q.setType(rs.getString("type"));
        q.setContext(rs.getString("context"));
        q.setDirectQuestion(rs.getString("directQuestion"));
        q.setDistractors(splitOrEmpty(rs.getString("distractors")));
        q.setCorrectAnswer(rs.getString("correctAnswer"));
        q.setJustification(rs.getString("justification"));
        q.setBibliography(rs.getString("bibliography"));
        q.setCompetence(rs.getString("competence"));
        q.setTopic(rs.getString("topic"));
        q.setSubtopic(rs.getString("subtopic"));

        String difficulty = rs.getString("difficultyLevel");
        q.setDifficultyLevel(
                difficulty != null ? DifficultyLevel.valueOf(difficulty) : null
        );

        q.setState(QuestionState.valueOf(rs.getString("state")));
        q.setAuthorLogin(rs.getString("authorLogin"));

        String createdAt = rs.getString("createdAt");
        q.setCreatedAt(
                createdAt != null
                        ? LocalDateTime.parse(createdAt)
                        : LocalDateTime.now()
        );

        q.setAssignedReviewers(
                splitOrEmpty(rs.getString("assignedReviewers"))
        );

        return q;
    }

    private void bind(PreparedStatement p, Question q) throws SQLException {
        p.setString(1, q.getId());
        p.setString(2, q.getType());
        p.setString(3, q.getContext());
        p.setString(4, q.getDirectQuestion());
        p.setString(5, String.join(JOIN, q.getDistractors()));
        p.setString(6, q.getCorrectAnswer());
        p.setString(7, q.getJustification());
        p.setString(8, q.getBibliography());
        p.setString(9, q.getCompetence());
        p.setString(10, q.getTopic());
        p.setString(11, q.getSubtopic());
        p.setString(12, q.getDifficultyLevel() != null
                ? q.getDifficultyLevel().name()
                : null);
        p.setString(13, q.getState().name());
        p.setString(14, q.getAuthorLogin());
        p.setString(15, q.getCreatedAt().toString());
        p.setString(16, String.join(JOIN, q.getAssignedReviewers()));
    }

    private List<String> splitOrEmpty(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(
                Arrays.asList(value.split(DELIM))
        );
    }

    private void reportError(String message, Exception ex) {
        lastError = message + " ("
                + ex.getClass().getSimpleName()
                + ": "
                + ex.getMessage()
                + ")";

        LOGGER.log(Level.SEVERE, message, ex);
        System.err.println("[Question] " + message + " -> " + ex);
        ex.printStackTrace();
    }

    public void disconnect() {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Error cerrando la conexion", ex);
        }
    }
}
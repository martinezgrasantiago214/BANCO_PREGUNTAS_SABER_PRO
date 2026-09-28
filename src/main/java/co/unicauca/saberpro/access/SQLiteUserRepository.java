package co.unicauca.saberpro.access;

import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.User;
import co.unicauca.saberpro.domain.UserStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementacion concreta de {@link IUserRepository} usando SQLite.
 * Adaptada del Taller SOLID de Gestion de Usuarios (arquitectura de acceso
 * a datos reutilizada tal cual, agregando la columna "email" usada por
 * HU04 para notificar a los revisores).
 *
 * Cualquier error de base de datos se imprime SIEMPRE en la consola
 * (ademas del logger), para que un problema de conexion/driver nunca quede
 * en silencio.
 */
public class SQLiteUserRepository implements IUserRepository {

    private static final Logger LOGGER = Logger.getLogger(SQLiteUserRepository.class.getName());

    private final String url;
    private Connection conn;

    public SQLiteUserRepository(String url) {
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

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS User (\n"
                + "  userId INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "  login TEXT NOT NULL UNIQUE,\n"
                + "  fullName TEXT NOT NULL,\n"
                + "  email TEXT,\n"
                + "  role TEXT NOT NULL,\n"
                + "  status TEXT NOT NULL,\n"
                + "  passwordHash TEXT NOT NULL\n"
                + ");";
        try {
            Statement stmt = connect().createStatement();
            stmt.execute(sql);
        } catch (Exception ex) {
            reportError("No fue posible crear la tabla User (URL: " + url + ")", ex);
        }
    }

    @Override
    public boolean save(User newUser) {
        String sql = "INSERT INTO User (login, fullName, email, role, status, passwordHash) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = connect().prepareStatement(sql);
            pstmt.setString(1, newUser.getLogin());
            pstmt.setString(2, newUser.getFullName());
            pstmt.setString(3, newUser.getEmail());
            pstmt.setString(4, newUser.getRole().name());
            pstmt.setString(5, newUser.getStatus().name());
            pstmt.setString(6, newUser.getPasswordHash());
            pstmt.executeUpdate();
            return true;
        } catch (Exception ex) {
            reportError("No fue posible guardar el usuario " + newUser.getLogin(), ex);
            return false;
        }
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE User SET fullName = ?, email = ?, role = ?, status = ?, passwordHash = ? "
                + "WHERE login = ?";
        try {
            PreparedStatement pstmt = connect().prepareStatement(sql);
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getStatus().name());
            pstmt.setString(5, user.getPasswordHash());
            pstmt.setString(6, user.getLogin());
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (Exception ex) {
            reportError("No fue posible actualizar el usuario " + user.getLogin(), ex);
            return false;
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        String sql = "SELECT userId, login, fullName, email, role, status, passwordHash "
                + "FROM User WHERE login = ?";
        try {
            PreparedStatement pstmt = connect().prepareStatement(sql);
            pstmt.setString(1, login);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible buscar el usuario " + login, ex);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    @Override
    public List<User> list() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT userId, login, fullName, email, role, status, passwordHash FROM User";
        try {
            Statement stmt = connect().createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible listar los usuarios", ex);
        }
        return users;
    }

    @Override
    public List<User> listByRole(Role role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT userId, login, fullName, email, role, status, passwordHash "
                + "FROM User WHERE role = ? AND status = 'ACTIVO'";
        try {
            PreparedStatement pstmt = connect().prepareStatement(sql);
            pstmt.setString(1, role.name());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (Exception ex) {
            reportError("No fue posible listar los usuarios por rol " + role, ex);
        }
        return users;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("userId"));
        u.setLogin(rs.getString("login"));
        u.setFullName(rs.getString("fullName"));
        u.setEmail(rs.getString("email"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setStatus(UserStatus.valueOf(rs.getString("status")));
        u.setPasswordHash(rs.getString("passwordHash"));
        return u;
    }

    private void reportError(String message, Exception ex) {
        LOGGER.log(Level.SEVERE, message, ex);
        System.err.println("[User] " + message + " -> " + ex);
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

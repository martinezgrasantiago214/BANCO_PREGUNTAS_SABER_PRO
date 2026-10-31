package co.unicauca.saberpro.user.access;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.user.domain.User;
import co.unicauca.saberpro.user.domain.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion de {@link IUserRepository} con SQLite. Es la base de datos
 * PROPIA del user-service: ningun otro servicio lee esta tabla, la consultan
 * por el API REST.
 *
 * Los metodos son synchronized porque se comparte una sola conexion JDBC y
 * Tomcat atiende peticiones en varios hilos.
 */
public class SQLiteUserRepository implements IUserRepository, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteUserRepository.class);

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

    private synchronized void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS User (\n"
                + "  userId INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + "  login TEXT NOT NULL UNIQUE,\n"
                + "  fullName TEXT NOT NULL,\n"
                + "  email TEXT,\n"
                + "  role TEXT NOT NULL,\n"
                + "  status TEXT NOT NULL,\n"
                + "  passwordHash TEXT NOT NULL\n"
                + ");";
        try (Statement stmt = connect().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ex) {
            LOGGER.error("No fue posible crear la tabla User (URL: {})", url, ex);
        }
    }

    @Override
    public synchronized boolean save(User newUser) {
        String sql = "INSERT INTO User (login, fullName, email, role, status, passwordHash) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, newUser.getLogin());
            pstmt.setString(2, newUser.getFullName());
            pstmt.setString(3, newUser.getEmail());
            pstmt.setString(4, newUser.getRole().name());
            pstmt.setString(5, newUser.getStatus().name());
            pstmt.setString(6, newUser.getPasswordHash());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible guardar el usuario {}", newUser.getLogin(), ex);
            return false;
        }
    }

    @Override
    public synchronized boolean update(User user) {
        String sql = "UPDATE User SET fullName = ?, email = ?, role = ?, status = ?, passwordHash = ? "
                + "WHERE login = ?";
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getStatus().name());
            pstmt.setString(5, user.getPasswordHash());
            pstmt.setString(6, user.getLogin());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            LOGGER.error("No fue posible actualizar el usuario {}", user.getLogin(), ex);
            return false;
        }
    }

    @Override
    public synchronized Optional<User> findByLogin(String login) {
        String sql = "SELECT userId, login, fullName, email, role, status, passwordHash "
                + "FROM User WHERE login = ?";
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, login);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible buscar el usuario {}", login, ex);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByLogin(String login) {
        return findByLogin(login).isPresent();
    }

    @Override
    public synchronized List<User> list() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT userId, login, fullName, email, role, status, passwordHash FROM User";
        try (Statement stmt = connect().createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible listar los usuarios", ex);
        }
        return users;
    }

    @Override
    public synchronized List<User> listByRole(Role role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT userId, login, fullName, email, role, status, passwordHash "
                + "FROM User WHERE role = ? AND status = 'ACTIVO'";
        try (PreparedStatement pstmt = connect().prepareStatement(sql)) {
            pstmt.setString(1, role.name());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("No fue posible listar los usuarios por rol {}", role, ex);
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

package com.sunrisedental.dao;

import com.sunrisedental.config.DatabaseConnection;
import com.sunrisedental.model.User;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL & MongoDB implementation of IUserDAO.
 */
public class UserDAOImpl implements IUserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAOImpl.class.getName());
    private static final Map<String, User> MEMORY_STORE = new ConcurrentHashMap<>();

    private Connection getConnection() {
        try {
            return DatabaseConnection.getInstance().getSqlConnection();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public User findByUsername(String username) {
        if (username == null) return null;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM users WHERE LOWER(username) = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, username.trim().toLowerCase());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rsToUser(rs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding user in MySQL: " + e.getMessage());
        }

        for (User u : MEMORY_STORE.values()) {
            if (u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim())) {
                return u;
            }
        }
        return null;
    }

    @Override
    public User findById(String id) {
        if (id == null) return null;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM users WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, id.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rsToUser(rs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding user by ID: " + e.getMessage());
        }

        return MEMORY_STORE.get(id);
    }

    @Override
    public boolean createUser(User user) {
        if (user == null || user.getUsername() == null) return false;

        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "INSERT INTO users (id, username, password_hash, full_name, role, email, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), full_name = VALUES(full_name), role = VALUES(role), email = VALUES(email)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, user.getId());
                    ps.setString(2, user.getUsername());
                    ps.setString(3, user.getPasswordHash());
                    ps.setString(4, user.getFullName());
                    ps.setString(5, user.getRole());
                    ps.setString(6, user.getEmail());
                    ps.setTimestamp(7, new Timestamp(user.getCreatedAt() != null ? user.getCreatedAt().getTime() : System.currentTimeMillis()));
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving user in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(user.getId(), user);
        return true;
    }

    @Override
    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "SELECT * FROM users ORDER BY created_at DESC";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        list.add(rsToUser(rs));
                    }
                    if (!list.isEmpty()) return list;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding all users in MySQL: " + e.getMessage());
        }
        return new ArrayList<>(MEMORY_STORE.values());
    }

    @Override
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "UPDATE users SET full_name = ?, role = ?, email = ? WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, user.getFullName());
                    ps.setString(2, user.getRole());
                    ps.setString(3, user.getEmail());
                    ps.setString(4, user.getId());
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating user in MySQL: " + e.getMessage());
        }

        MEMORY_STORE.put(user.getId(), user);
        return true;
    }

    @Override
    public boolean deleteUser(String id) {
        if (id == null) return false;

        try (Connection conn = getConnection()) {
            if (conn != null) {
                String sql = "DELETE FROM users WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error deleting user: " + e.getMessage());
        }

        MEMORY_STORE.remove(id);
        return true;
    }

    private User rsToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setEmail(rs.getString("email"));
        Timestamp ts = rs.getTimestamp("created_at");
        u.setCreatedAt(ts != null ? new Date(ts.getTime()) : new Date());
        return u;
    }
}

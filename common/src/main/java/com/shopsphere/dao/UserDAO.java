package com.shopsphere.dao;

import com.shopsphere.model.User;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC DAO for the users table. All SQL uses PreparedStatement. */
public class UserDAO {

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setMobile(rs.getString("mobile"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getBoolean("status"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /** @return generated user_id */
    public int insert(User u) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, mobile, role, status) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getMobile());
            ps.setString(5, u.getRole() == null ? "CUSTOMER" : u.getRole());
            ps.setBoolean(6, true);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public boolean updateProfile(int userId, String name, String mobile) throws SQLException {
        String sql = "UPDATE users SET name = ?, mobile = ? WHERE user_id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, mobile);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updatePassword(int userId, String hash) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setStatus(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE user_id = ? AND role <> 'ADMIN'";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> findCustomers() throws SQLException {
        String sql = "SELECT * FROM users WHERE role = 'CUSTOMER' ORDER BY created_at DESC";
        List<User> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countCustomers() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}

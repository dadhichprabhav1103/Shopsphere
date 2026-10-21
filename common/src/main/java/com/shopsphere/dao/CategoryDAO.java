package com.shopsphere.dao;

import com.shopsphere.model.Category;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getInt("category_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setStatus(rs.getBoolean("status"));
        return c;
    }

    public List<Category> findAll(boolean activeOnly) throws SQLException {
        String sql = "SELECT * FROM categories" + (activeOnly ? " WHERE status = 1" : "") + " ORDER BY name";
        List<Category> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Category findById(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM categories WHERE category_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Category cat) throws SQLException {
        String sql = "INSERT INTO categories (name, description, status) VALUES (?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getDescription());
            ps.setBoolean(3, cat.isStatus());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public boolean update(Category cat) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ?, status = ? WHERE category_id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getDescription());
            ps.setBoolean(3, cat.isStatus());
            ps.setInt(4, cat.getCategoryId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Hard delete; fails with SQLException when products still use the category. */
    public boolean delete(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM categories WHERE category_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}

package com.shopsphere.dao;

import com.shopsphere.model.WishlistItem;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WishlistDAO {

    public List<WishlistItem> findByUser(int userId) throws SQLException {
        String sql = "SELECT w.wishlist_id, w.user_id, w.created_at AS wished_at, " + ProductDAO.COLS + ProductDAO.FROM_JOINS
                   + "JOIN wishlist w ON w.product_id = p.product_id WHERE w.user_id = ? ORDER BY w.wishlist_id DESC";
        List<WishlistItem> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    WishlistItem w = new WishlistItem();
                    w.setWishlistId(rs.getInt("wishlist_id"));
                    w.setUserId(rs.getInt("user_id"));
                    w.setCreatedAt(rs.getTimestamp("wished_at"));
                    w.setProduct(ProductDAO.map(rs));
                    w.setProductId(w.getProduct().getProductId());
                    list.add(w);
                }
            }
        }
        return list;
    }

    public void add(int userId, int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO wishlist (user_id, product_id) VALUES (?,?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public void remove(int userId, int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM wishlist WHERE user_id = ? AND product_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public boolean exists(int userId, int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM wishlist WHERE user_id = ? AND product_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}

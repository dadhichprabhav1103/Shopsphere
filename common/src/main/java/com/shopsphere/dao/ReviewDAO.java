package com.shopsphere.dao;

import com.shopsphere.model.Review;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public List<Review> findByProduct(int productId) throws SQLException {
        String sql = "SELECT r.*, u.name AS user_name FROM reviews r JOIN users u ON u.user_id = r.user_id "
                   + "WHERE r.product_id = ? ORDER BY r.created_at DESC, r.review_id DESC";
        List<Review> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Review r = new Review();
                    r.setReviewId(rs.getInt("review_id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setProductId(rs.getInt("product_id"));
                    r.setRating(rs.getInt("rating"));
                    r.setReviewText(rs.getString("review_text"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    r.setUserName(rs.getString("user_name"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    /** One review per user per product: a second submission updates the first. */
    public void save(Review r) throws SQLException {
        String sql = "INSERT INTO reviews (user_id, product_id, rating, review_text) VALUES (?,?,?,?) "
                   + "ON DUPLICATE KEY UPDATE rating = VALUES(rating), review_text = VALUES(review_text), created_at = CURRENT_TIMESTAMP";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getProductId());
            ps.setInt(3, r.getRating());
            ps.setString(4, r.getReviewText());
            ps.executeUpdate();
        }
    }

    /** True when the user has a non-cancelled order containing the product. */
    public boolean hasPurchased(int userId, int productId) throws SQLException {
        String sql = "SELECT 1 FROM orders o JOIN order_items oi ON oi.order_id = o.order_id "
                   + "WHERE o.user_id = ? AND oi.product_id = ? AND o.order_status <> 'CANCELLED' LIMIT 1";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}

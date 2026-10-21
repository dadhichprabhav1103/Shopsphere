package com.shopsphere.dao;

import com.shopsphere.model.CartItem;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDAO {

    private static final String SELECT_CART =
        "SELECT ct.cart_id, ct.user_id, ct.quantity, " + ProductDAO.COLS + ProductDAO.FROM_JOINS
      + "JOIN cart ct ON ct.product_id = p.product_id WHERE ct.user_id = ? ORDER BY ct.cart_id";

    public List<CartItem> findByUser(int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findByUser(c, userId);
        }
    }

    public List<CartItem> findByUser(Connection c, int userId) throws SQLException {
        List<CartItem> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(SELECT_CART)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartItem ci = new CartItem();
                    ci.setCartId(rs.getInt("cart_id"));
                    ci.setUserId(rs.getInt("user_id"));
                    ci.setQuantity(rs.getInt("quantity"));
                    ci.setProduct(ProductDAO.map(rs));
                    ci.setProductId(ci.getProduct().getProductId());
                    list.add(ci);
                }
            }
        }
        return list;
    }

    /** Adds the product, or increases the quantity when it is already in the cart. */
    public void addOrIncrease(int userId, int productId, int qty) throws SQLException {
        String sql = "INSERT INTO cart (user_id, product_id, quantity) VALUES (?,?,?) "
                   + "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            ps.executeUpdate();
        }
    }

    public int getQuantity(int userId, int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT quantity FROM cart WHERE user_id = ? AND product_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public void setQuantity(int userId, int productId, int qty) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE cart SET quantity = ? WHERE user_id = ? AND product_id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, userId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
    }

    public void remove(int userId, int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM cart WHERE user_id = ? AND product_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public int countItems(int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(SUM(quantity),0) FROM cart WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public void clear(Connection c, int userId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM cart WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }
}

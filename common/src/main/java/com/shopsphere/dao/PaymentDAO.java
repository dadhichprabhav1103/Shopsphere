package com.shopsphere.dao;

import com.shopsphere.model.Payment;

import java.sql.*;

public class PaymentDAO {
    public int insert(Connection c, Payment p) throws SQLException {
        String sql = "INSERT INTO payments (order_id, payment_method, transaction_reference, amount, payment_status) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getOrderId());
            ps.setString(2, p.getPaymentMethod());
            ps.setString(3, p.getTransactionReference());
            ps.setDouble(4, p.getAmount());
            ps.setString(5, p.getPaymentStatus());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public Payment findByOrder(Connection c, int orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM payments WHERE order_id = ? ORDER BY payment_id DESC LIMIT 1")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Payment p = new Payment();
                p.setPaymentId(rs.getInt("payment_id"));
                p.setOrderId(rs.getInt("order_id"));
                p.setPaymentMethod(rs.getString("payment_method"));
                p.setTransactionReference(rs.getString("transaction_reference"));
                p.setAmount(rs.getDouble("amount"));
                p.setPaymentStatus(rs.getString("payment_status"));
                p.setCreatedAt(rs.getTimestamp("created_at"));
                return p;
            }
        }
    }

    public void updateStatus(Connection c, int orderId, String status) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE payments SET payment_status = ? WHERE order_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }
}

package com.shopsphere.dao;

import com.shopsphere.model.Order;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderDAO {
    private final OrderItemDAO itemDAO = new OrderItemDAO();

    private static final String SELECT =
        "SELECT o.*, u.name AS user_name FROM orders o JOIN users u ON u.user_id = o.user_id ";

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setUserId(rs.getInt("user_id"));
        o.setAddressId(rs.getInt("address_id"));
        o.setTotalAmount(rs.getDouble("total_amount"));
        o.setDiscount(rs.getDouble("discount"));
        o.setPaymentMethod(rs.getString("payment_method"));
        o.setPaymentStatus(rs.getString("payment_status"));
        o.setOrderStatus(rs.getString("order_status"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        o.setUserName(rs.getString("user_name"));
        return o;
    }

    /** Inserts the order header inside the caller's transaction. */
    public int insert(Connection c, Order o) throws SQLException {
        String sql = "INSERT INTO orders (user_id, address_id, total_amount, discount, payment_method, payment_status, order_status) "
                   + "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getUserId());
            ps.setInt(2, o.getAddressId());
            ps.setDouble(3, o.getTotalAmount());
            ps.setDouble(4, o.getDiscount());
            ps.setString(5, o.getPaymentMethod());
            ps.setString(6, o.getPaymentStatus());
            ps.setString(7, o.getOrderStatus());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public Order findById(int orderId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            return findById(c, orderId);
        }
    }

    public Order findById(Connection c, int orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE o.order_id = ?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Order o = map(rs);
                o.setItems(itemDAO.findByOrder(c, orderId));
                return o;
            }
        }
    }

    public List<Order> findByUser(int userId) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + "WHERE o.user_id = ? ORDER BY o.order_id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Order o = map(rs);
                    o.setItems(itemDAO.findByOrder(c, o.getOrderId()));
                    list.add(o);
                }
            }
        }
        return list;
    }

    /** Admin listing, optionally filtered by status (null/blank = all). */
    public List<Order> findAll(String status) throws SQLException {
        boolean filter = status != null && !status.isBlank();
        List<Order> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT + (filter ? "WHERE o.order_status = ? " : "") + "ORDER BY o.order_id DESC")) {
            if (filter) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public boolean updateStatus(Connection c, int orderId, String orderStatus, String paymentStatus) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE orders SET order_status = ?, payment_status = ? WHERE order_id = ?")) {
            ps.setString(1, orderStatus);
            ps.setString(2, paymentStatus);
            ps.setInt(3, orderId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Number of orders per status (for the admin dashboard). */
    public Map<String, Integer> countByStatus() throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String s : new String[]{"PLACED", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED"}) m.put(s, 0);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT order_status, COUNT(*) FROM orders GROUP BY order_status");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) m.put(rs.getString(1), rs.getInt(2));
        }
        return m;
    }

    /** Revenue of all non-cancelled orders. */
    public double totalRevenue() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE order_status <> 'CANCELLED'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    public int count() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Top selling products: [name, units sold, revenue]. */
    public List<Object[]> topProducts(int limit) throws SQLException {
        String sql = "SELECT p.name, SUM(oi.quantity) units, SUM(oi.quantity * oi.price) revenue "
                   + "FROM order_items oi JOIN orders o ON o.order_id = oi.order_id AND o.order_status <> 'CANCELLED' "
                   + "JOIN products p ON p.product_id = oi.product_id GROUP BY p.product_id, p.name "
                   + "ORDER BY units DESC LIMIT ?";
        List<Object[]> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getDouble(3)});
            }
        }
        return list;
    }
}

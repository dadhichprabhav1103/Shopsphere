package com.shopsphere.dao;

import com.shopsphere.model.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAO {

    public void insert(Connection c, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, item.getOrderId());
            ps.setInt(2, item.getProductId());
            ps.setInt(3, item.getQuantity());
            ps.setDouble(4, item.getPrice());
            ps.executeUpdate();
        }
    }

    public List<OrderItem> findByOrder(Connection c, int orderId) throws SQLException {
        String sql = "SELECT oi.*, p.name AS product_name FROM order_items oi "
                   + "JOIN products p ON p.product_id = oi.product_id WHERE oi.order_id = ? ORDER BY oi.order_item_id";
        List<OrderItem> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem i = new OrderItem();
                    i.setOrderItemId(rs.getInt("order_item_id"));
                    i.setOrderId(rs.getInt("order_id"));
                    i.setProductId(rs.getInt("product_id"));
                    i.setQuantity(rs.getInt("quantity"));
                    i.setPrice(rs.getDouble("price"));
                    i.setProductName(rs.getString("product_name"));
                    list.add(i);
                }
            }
        }
        return list;
    }
}

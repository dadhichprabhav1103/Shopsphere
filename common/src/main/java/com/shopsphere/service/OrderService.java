package com.shopsphere.service;

import com.shopsphere.dao.OrderDAO;
import com.shopsphere.dao.PaymentDAO;
import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Order;
import com.shopsphere.model.OrderItem;
import com.shopsphere.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderService {
    public static final List<String> STATUSES = List.of("PLACED", "PACKED", "SHIPPED", "DELIVERED", "CANCELLED");

    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public List<Order> forUser(int userId) throws ServiceException {
        try { return orderDAO.findByUser(userId); }
        catch (SQLException e) { throw new ServiceException("Could not load orders.", e); }
    }

    /** A customer can only see their own orders; admins pass userId = -1 to bypass the ownership check. */
    public Order details(int orderId, int userId) throws ServiceException {
        try {
            Order o = orderDAO.findById(orderId);
            if (o == null || (userId != -1 && o.getUserId() != userId)) throw new ServiceException("Order not found.");
            return o;
        } catch (SQLException e) { throw new ServiceException("Could not load order.", e); }
    }

    public List<Order> adminList(String status) throws ServiceException {
        try { return orderDAO.findAll(status); }
        catch (SQLException e) { throw new ServiceException("Could not load orders.", e); }
    }

    public void cancelByCustomer(int orderId, int userId) throws ServiceException {
        Order o = details(orderId, userId);
        if (!"PLACED".equals(o.getOrderStatus())) throw new ServiceException("Only orders that have not been packed can be cancelled.");
        changeStatus(orderId, "CANCELLED");
    }

    /** Admin status change with simple workflow rules. Cancelling returns the stock. */
    public void changeStatus(int orderId, String newStatus) throws ServiceException {
        if (!STATUSES.contains(newStatus)) throw new ServiceException("Invalid order status.");
        Connection c = null;
        try {
            c = DBConnection.getConnection();
            c.setAutoCommit(false);
            Order o = orderDAO.findById(c, orderId);
            if (o == null) throw new ServiceException("Order not found.");
            String cur = o.getOrderStatus();
            if (cur.equals(newStatus)) { c.rollback(); return; }
            if (Set.of("DELIVERED", "CANCELLED").contains(cur)) throw new ServiceException("A " + cur.toLowerCase() + " order cannot be changed.");

            String payment = o.getPaymentStatus();
            if ("CANCELLED".equals(newStatus)) {
                for (OrderItem i : o.getItems()) productDAO.restoreStock(c, i.getProductId(), i.getQuantity());
                payment = "PAID".equals(payment) ? "REFUNDED" : "CANCELLED";
            } else if ("DELIVERED".equals(newStatus)) {
                payment = "PAID";               // cash collected on delivery
            }
            orderDAO.updateStatus(c, orderId, newStatus, payment);
            paymentDAO.updateStatus(c, orderId, payment);
            c.commit();
        } catch (ServiceException e) {
            rollback(c); throw e;
        } catch (SQLException e) {
            rollback(c); throw new ServiceException("Could not update the order.", e);
        } finally {
            if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) { }
        }
    }

    public Map<String, Integer> statusCounts() throws ServiceException {
        try { return orderDAO.countByStatus(); }
        catch (SQLException e) { throw new ServiceException("Could not load summary.", e); }
    }

    public double revenue() throws ServiceException {
        try { return orderDAO.totalRevenue(); }
        catch (SQLException e) { throw new ServiceException("Could not load summary.", e); }
    }

    public int totalOrders() throws ServiceException {
        try { return orderDAO.count(); }
        catch (SQLException e) { throw new ServiceException("Could not load summary.", e); }
    }

    public List<Object[]> topProducts() throws ServiceException {
        try { return orderDAO.topProducts(5); }
        catch (SQLException e) { throw new ServiceException("Could not load summary.", e); }
    }

    private static void rollback(Connection c) {
        if (c != null) try { c.rollback(); } catch (SQLException ignored) { }
    }
}

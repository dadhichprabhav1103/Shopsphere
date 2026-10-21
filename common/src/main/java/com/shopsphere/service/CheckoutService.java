package com.shopsphere.service;

import com.shopsphere.dao.*;
import com.shopsphere.model.*;
import com.shopsphere.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Checkout use-case. Everything that changes data (order, items, stock, payment,
 * coupon usage, cart clearing) runs in ONE JDBC transaction. Totals are always
 * recalculated here from database prices - never from browser input.
 */
public class CheckoutService {
    public static final Set<String> PAYMENT_METHODS = Set.of("COD", "UPI", "CARD");

    private final CartDAO cartDAO = new CartDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final AddressDAO addressDAO = new AddressDAO();

    /** Validates a coupon for this user and subtotal and returns it (throws a readable error otherwise). */
    public Coupon validateCoupon(int userId, String code, double subtotal) throws ServiceException {
        try {
            Coupon cp = couponDAO.findByCode(code == null ? "" : code.trim().toUpperCase());
            if (cp == null) throw new ServiceException("Invalid coupon code.");
            if (!cp.isStatus()) throw new ServiceException("This coupon is not active.");
            if (cp.getExpiryDate() != null && cp.getExpiryDate().toLocalDate().isBefore(java.time.LocalDate.now()))
                throw new ServiceException("This coupon has expired.");
            if (subtotal < cp.getMinimumOrder())
                throw new ServiceException("Minimum order of Rs. " + (int) cp.getMinimumOrder() + " required for this coupon.");
            if (couponDAO.hasUserUsed(cp.getCouponId(), userId)) throw new ServiceException("You have already used this coupon.");
            return cp;
        } catch (SQLException e) { throw new ServiceException("Could not verify coupon.", e); }
    }

    public Order placeOrder(int userId, int addressId, String couponCode, String paymentMethod) throws ServiceException {
        if (paymentMethod == null || !PAYMENT_METHODS.contains(paymentMethod)) throw new ServiceException("Select a valid payment method.");
        try {
            if (addressDAO.findByIdForUser(addressId, userId) == null) throw new ServiceException("Select a delivery address.");
        } catch (SQLException e) { throw new ServiceException("Could not verify address.", e); }

        Connection c = null;
        try {
            c = DBConnection.getConnection();
            c.setAutoCommit(false);

            List<CartItem> cart = cartDAO.findByUser(c, userId);
            if (cart.isEmpty()) throw new ServiceException("Your cart is empty.");

            double subtotal = 0;
            for (CartItem ci : cart) {
                Product p = ci.getProduct();
                if (!p.isStatus()) throw new ServiceException(p.getName() + " is no longer available. Remove it from the cart.");
                if (ci.getQuantity() > p.getStock()) throw new ServiceException("Only " + p.getStock() + " units of " + p.getName() + " are left.");
                subtotal += ci.getLineTotal();
            }
            subtotal = Math.round(subtotal * 100) / 100.0;

            Coupon coupon = null;
            double discount = 0;
            if (couponCode != null && !couponCode.isBlank()) {
                coupon = validateCoupon(userId, couponCode, subtotal);
                discount = Math.round(coupon.calculateDiscount(subtotal) * 100) / 100.0;
            }
            double total = Math.round((subtotal - discount) * 100) / 100.0;

            // Mock payment gateway: online methods succeed immediately, COD is collected on delivery.
            boolean online = !"COD".equals(paymentMethod);
            String paymentStatus = online ? "PAID" : "PENDING";

            Order order = new Order();
            order.setUserId(userId);
            order.setAddressId(addressId);
            order.setTotalAmount(total);
            order.setDiscount(discount);
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(paymentStatus);
            order.setOrderStatus("PLACED");
            order.setOrderId(orderDAO.insert(c, order));

            List<OrderItem> items = new ArrayList<>();
            for (CartItem ci : cart) {
                if (!productDAO.decrementStock(c, ci.getProductId(), ci.getQuantity()))
                    throw new ServiceException("Sorry, " + ci.getProduct().getName() + " just went out of stock.");
                OrderItem oi = new OrderItem();
                oi.setOrderId(order.getOrderId());
                oi.setProductId(ci.getProductId());
                oi.setQuantity(ci.getQuantity());
                oi.setPrice(ci.getProduct().getFinalPrice());
                oi.setProductName(ci.getProduct().getName());
                orderItemDAO.insert(c, oi);
                items.add(oi);
            }
            order.setItems(items);

            Payment pay = new Payment();
            pay.setOrderId(order.getOrderId());
            pay.setPaymentMethod(paymentMethod);
            pay.setAmount(total);
            pay.setPaymentStatus(paymentStatus);
            pay.setTransactionReference(online ? "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase() : "COD-" + order.getOrderId());
            paymentDAO.insert(c, pay);

            if (coupon != null) couponDAO.recordUsage(c, coupon.getCouponId(), userId, order.getOrderId());
            cartDAO.clear(c, userId);

            c.commit();
            return order;
        } catch (ServiceException e) {
            rollback(c);
            throw e;
        } catch (SQLException e) {
            rollback(c);
            throw new ServiceException("Could not place the order. Nothing was charged. Please try again.", e);
        } finally {
            close(c);
        }
    }

    private static void rollback(Connection c) {
        if (c != null) try { c.rollback(); } catch (SQLException ignored) { }
    }

    private static void close(Connection c) {
        if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) { }
    }
}

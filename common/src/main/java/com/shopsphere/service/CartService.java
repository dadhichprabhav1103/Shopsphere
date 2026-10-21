package com.shopsphere.service;

import com.shopsphere.dao.CartDAO;
import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Product;

import java.sql.SQLException;

public class CartService {
    private static final int MAX_PER_ITEM = 10;
    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public CartSummary getCart(int userId) throws ServiceException {
        try { return new CartSummary(cartDAO.findByUser(userId)); }
        catch (SQLException e) { throw new ServiceException("Could not load your cart.", e); }
    }

    public int count(int userId) throws ServiceException {
        try { return cartDAO.countItems(userId); }
        catch (SQLException e) { throw new ServiceException("Could not load your cart.", e); }
    }

    public void add(int userId, int productId, int qty) throws ServiceException {
        if (qty < 1) throw new ServiceException("Quantity must be at least 1.");
        try {
            Product p = productDAO.findById(productId);
            if (p == null || !p.isStatus()) throw new ServiceException("Product is not available.");
            int wanted = cartDAO.getQuantity(userId, productId) + qty;
            if (p.getStock() < 1) throw new ServiceException(p.getName() + " is out of stock.");
            if (wanted > p.getStock()) throw new ServiceException("Only " + p.getStock() + " units of " + p.getName() + " are available.");
            if (wanted > MAX_PER_ITEM) throw new ServiceException("You can buy at most " + MAX_PER_ITEM + " units of one product.");
            cartDAO.addOrIncrease(userId, productId, qty);
        } catch (SQLException e) { throw new ServiceException("Could not add to cart.", e); }
    }

    /** Sets an absolute quantity; 0 removes the line. */
    public void update(int userId, int productId, int qty) throws ServiceException {
        try {
            if (qty <= 0) { cartDAO.remove(userId, productId); return; }
            Product p = productDAO.findById(productId);
            if (p == null || !p.isStatus()) { cartDAO.remove(userId, productId); throw new ServiceException("Product is no longer available."); }
            if (qty > p.getStock()) throw new ServiceException("Only " + p.getStock() + " units of " + p.getName() + " are available.");
            if (qty > MAX_PER_ITEM) throw new ServiceException("You can buy at most " + MAX_PER_ITEM + " units of one product.");
            cartDAO.setQuantity(userId, productId, qty);
        } catch (SQLException e) { throw new ServiceException("Could not update cart.", e); }
    }

    public void remove(int userId, int productId) throws ServiceException {
        try { cartDAO.remove(userId, productId); }
        catch (SQLException e) { throw new ServiceException("Could not update cart.", e); }
    }
}

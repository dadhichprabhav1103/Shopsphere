package com.shopsphere.service;

import com.shopsphere.dao.WishlistDAO;
import com.shopsphere.model.WishlistItem;

import java.sql.SQLException;
import java.util.List;

public class WishlistService {
    private final WishlistDAO dao = new WishlistDAO();

    public List<WishlistItem> list(int userId) throws ServiceException {
        try { return dao.findByUser(userId); }
        catch (SQLException e) { throw new ServiceException("Could not load wishlist.", e); }
    }

    public void add(int userId, int productId) throws ServiceException {
        try { dao.add(userId, productId); }
        catch (SQLException e) { throw new ServiceException("Could not update wishlist.", e); }
    }

    public void remove(int userId, int productId) throws ServiceException {
        try { dao.remove(userId, productId); }
        catch (SQLException e) { throw new ServiceException("Could not update wishlist.", e); }
    }
}

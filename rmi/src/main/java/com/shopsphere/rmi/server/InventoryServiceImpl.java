package com.shopsphere.rmi.server;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Product;
import com.shopsphere.rmi.remote.InventoryService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Remote implementation. Extending UnicastRemoteObject exports the object so clients can call it. */
public class InventoryServiceImpl extends UnicastRemoteObject implements InventoryService {
    private static final long serialVersionUID = 1L;
    private final transient ProductDAO dao = new ProductDAO();

    public InventoryServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public Product getProduct(int id) throws RemoteException {
        try {
            return dao.findById(id);
        } catch (SQLException e) {
            throw new RemoteException("Database error while reading product " + id, e);
        }
    }

    @Override
    public int getStock(int productId) throws RemoteException {
        try {
            return dao.getStock(productId);
        } catch (SQLException e) {
            throw new RemoteException("Database error while reading stock", e);
        }
    }

    @Override
    public boolean updateStock(int productId, int quantity) throws RemoteException {
        if (quantity < 0) throw new RemoteException("Stock cannot be negative");
        try {
            return dao.updateStock(productId, quantity);
        } catch (SQLException e) {
            throw new RemoteException("Database error while updating stock", e);
        }
    }

    @Override
    public List<Product> getLowStockProducts(int threshold) throws RemoteException {
        try {
            return new ArrayList<>(dao.findLowStock(threshold));
        } catch (SQLException e) {
            throw new RemoteException("Database error while reading stock", e);
        }
    }
}

package com.shopsphere.rmi.remote;

import com.shopsphere.model.Product;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI remote interface (the contract shared by client and server).
 * Every method must declare RemoteException. Parameters and return values travel by
 * serialization, so Product implements Serializable.
 */
public interface InventoryService extends Remote {
    String SERVICE_NAME = "InventoryService";

    Product getProduct(int id) throws RemoteException;

    int getStock(int productId) throws RemoteException;

    boolean updateStock(int productId, int quantity) throws RemoteException;

    List<Product> getLowStockProducts(int threshold) throws RemoteException;
}

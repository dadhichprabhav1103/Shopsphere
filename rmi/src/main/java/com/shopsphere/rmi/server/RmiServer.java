package com.shopsphere.rmi.server;

import com.shopsphere.rmi.remote.InventoryService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Starts an RMI registry inside this JVM (port 1099) and binds the remote inventory service.
 *
 *   Client --lookup("InventoryService")--> RMI Registry --> stub --> InventoryServiceImpl --> JDBC --> MySQL
 *
 * Run:  java -cp ... com.shopsphere.rmi.server.RmiServer [port]
 */
public class RmiServer {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Registry.REGISTRY_PORT;
        Registry registry = LocateRegistry.createRegistry(port);                // the RMI registry
        InventoryServiceImpl service = new InventoryServiceImpl();             // exported remote object
        registry.rebind(InventoryService.SERVICE_NAME, service);               // name -> stub
        System.out.println("[RmiServer] '" + InventoryService.SERVICE_NAME + "' bound in registry on port " + port);
        System.out.println("[RmiServer] waiting for clients (Ctrl+C to stop)");
        Thread.currentThread().join();                                         // keep the JVM alive
    }
}

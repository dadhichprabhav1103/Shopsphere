package com.shopsphere.rmi.client;

import com.shopsphere.model.Product;
import com.shopsphere.rmi.remote.InventoryService;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

/**
 * RMI client: finds the service in the registry and calls it like a local object.
 *   java -cp ... com.shopsphere.rmi.client.RmiClient [host] [port]
 */
public class RmiClient {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : Registry.REGISTRY_PORT;

        // Way 1: java.rmi.Naming (URL style name) - the "Naming" syllabus topic
        InventoryService service = (InventoryService) Naming.lookup("rmi://" + host + ":" + port + "/" + InventoryService.SERVICE_NAME);

        // Way 2 (equivalent): ask the registry object directly
        Registry registry = LocateRegistry.getRegistry(host, port);
        System.out.println("Names in registry: " + String.join(", ", registry.list()));

        Product p = service.getProduct(1);                                      // remote call, Product returned by serialization
        System.out.println("getProduct(1)  -> " + p);
        System.out.println("getStock(1)    -> " + service.getStock(1));

        int before = service.getStock(13);
        service.updateStock(13, before + 5);
        System.out.println("updateStock(13): " + before + " -> " + service.getStock(13));
        service.updateStock(13, before);                                        // restore

        List<Product> low = service.getLowStockProducts(10);
        System.out.println("low stock (<=10): " + low.size() + " product(s)");
        for (Product lp : low) System.out.println("   " + lp);
    }
}

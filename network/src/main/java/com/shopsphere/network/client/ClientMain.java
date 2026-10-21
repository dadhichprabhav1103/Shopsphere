package com.shopsphere.network.client;

import com.shopsphere.model.Product;

import java.util.List;
import java.util.Scanner;

/**
 * Interactive console client.
 *   java -cp ... com.shopsphere.network.client.ClientMain [host] [port]
 * Type:  GET_PRODUCT 1 | GET_STOCK 1 | SEARCH_PRODUCT java | PING | QUIT
 */
public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5050;
        try (InventoryClient client = new InventoryClient(host, port);
             Scanner sc = new Scanner(System.in)) {
            System.out.println("Connected to " + host + ":" + port + ". Commands: GET_PRODUCT id | GET_STOCK id | SEARCH_PRODUCT text | PING | QUIT");
            while (true) {
                System.out.print("> ");
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                Object reply = client.raw(line);
                if (reply instanceof Product p) {
                    System.out.printf("Product #%d  %s  (%s)  price=%.2f  discount=%.0f%%  stock=%d%n",
                            p.getProductId(), p.getName(), p.getBrand(), p.getPrice(), p.getDiscount(), p.getStock());
                } else if (reply instanceof List<?> list) {
                    System.out.println(list.size() + " product(s):");
                    for (Object o : list) System.out.println("  " + o);
                } else {
                    System.out.println(reply);
                }
                if (line.equalsIgnoreCase("QUIT")) break;
            }
        }
    }
}

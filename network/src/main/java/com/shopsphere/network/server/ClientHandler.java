package com.shopsphere.network.server;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Product;
import com.shopsphere.network.model.Protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Serves one connected client. Commands arrive as Strings; replies are serialized objects. */
class ClientHandler implements Runnable {
    private static final int MAX_COMMAND_LENGTH = 200;
    private final Socket socket;
    private final ProductDAO dao = new ProductDAO();

    ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        // ObjectOutputStream must be created (and flushed) before ObjectInputStream to avoid a handshake deadlock.
        try (Socket s = socket;
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.flush();
            s.setSoTimeout(60_000);                       // drop idle clients after 60 s
            while (true) {
                Object request = in.readObject();
                if (!(request instanceof String command) || command.length() > MAX_COMMAND_LENGTH) {
                    out.writeObject("ERROR invalid request");
                    out.flush();
                    continue;
                }
                Object reply = process(command.trim());
                out.writeObject(reply);
                out.flush();
                out.reset();                              // don't keep references to old replies
                if (Protocol.QUIT.equals(command.trim())) break;
            }
        } catch (EOFException e) {
            // client closed the connection
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ClientHandler] " + e.getMessage());
        }
    }

    Object process(String line) {
        String[] parts = line.split("\\s+", 2);
        String cmd = parts[0].toUpperCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";
        try {
            switch (cmd) {
                case Protocol.PING:
                    return "PONG";
                case Protocol.QUIT:
                    return "BYE";
                case Protocol.GET_PRODUCT: {
                    Product p = dao.findById(Integer.parseInt(arg));
                    return p == null ? "ERROR product not found" : p;
                }
                case Protocol.GET_STOCK: {
                    int stock = dao.getStock(Integer.parseInt(arg));
                    return stock < 0 ? "ERROR product not found" : Integer.valueOf(stock);
                }
                case Protocol.SEARCH_PRODUCT: {
                    List<Product> found = dao.searchByName(arg);
                    return new ArrayList<>(found);
                }
                default:
                    return "ERROR unknown command (use GET_PRODUCT, GET_STOCK, SEARCH_PRODUCT, PING, QUIT)";
            }
        } catch (NumberFormatException e) {
            return "ERROR argument must be a number";
        } catch (SQLException e) {
            return "ERROR database problem";
        }
    }
}

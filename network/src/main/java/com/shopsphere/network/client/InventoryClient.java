package com.shopsphere.network.client;

import com.shopsphere.model.Product;
import com.shopsphere.network.model.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * Client side of the inventory protocol:  Socket -> send command -> receive serialized reply.
 * Deserialization is restricted with an ObjectInputFilter (only ShopSphere model classes and
 * basic JDK types are accepted) - never deserialize untrusted data without a filter.
 */
public class InventoryClient implements Closeable {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public InventoryClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(10_000);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                "maxdepth=10;maxarray=10000;com.shopsphere.model.*;java.util.ArrayList;java.lang.Object;java.lang.String;"
                + "java.lang.Integer;java.lang.Number;java.sql.Timestamp;java.util.Date;!*"));
    }

    private Object send(String command) throws IOException {
        out.writeObject(command);
        out.flush();
        try {
            return in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Unexpected reply type", e);
        }
    }

    public String ping() throws IOException { return (String) send(Protocol.PING); }

    /** @return the product or null when the server says it does not exist */
    public Product getProduct(int id) throws IOException {
        Object r = send(Protocol.GET_PRODUCT + " " + id);
        return r instanceof Product p ? p : null;
    }

    /** @return units in stock, or -1 when the product does not exist */
    public int getStock(int id) throws IOException {
        Object r = send(Protocol.GET_STOCK + " " + id);
        return r instanceof Integer i ? i : -1;
    }

    @SuppressWarnings("unchecked")
    public List<Product> search(String text) throws IOException {
        Object r = send(Protocol.SEARCH_PRODUCT + " " + text);
        return r instanceof List<?> l ? (List<Product>) l : List.of();
    }

    /** Sends any raw command line (used by the interactive console). */
    public Object raw(String command) throws IOException { return send(command); }

    @Override
    public void close() throws IOException {
        try { send(Protocol.QUIT); } catch (IOException ignored) { /* server may already be gone */ }
        socket.close();
    }
}

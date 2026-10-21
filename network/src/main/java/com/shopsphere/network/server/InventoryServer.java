package com.shopsphere.network.server;

import com.shopsphere.network.model.Protocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client/Server demo using java.net: a multi-threaded TCP server.
 *
 *   ServerSocket -> accept() -> Socket -> read command -> query database (JDBC DAO) -> send serialized reply
 *
 * Run:  java -cp ... com.shopsphere.network.server.InventoryServer [port]
 */
public class InventoryServer {
    private final int port;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newFixedThreadPool(8);

    public InventoryServer(int port) { this.port = port; }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("[InventoryServer] listening on port " + serverSocket.getLocalPort());
        while (running) {
            try {
                Socket client = serverSocket.accept();                       // blocks until a client connects
                System.out.println("[InventoryServer] client connected: " + client.getRemoteSocketAddress());
                pool.execute(new ClientHandler(client));                     // one task per client
            } catch (IOException e) {
                if (running) System.err.println("[InventoryServer] accept failed: " + e.getMessage());
            }
        }
    }

    public int getPort() { return serverSocket == null ? port : serverSocket.getLocalPort(); }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null) serverSocket.close();
        pool.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : Protocol.DEFAULT_PORT;
        new InventoryServer(port).start();
    }
}

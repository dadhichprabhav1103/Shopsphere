package com.shopsphere.network.url;

import com.shopsphere.dao.ProductDAO;
import com.shopsphere.model.Product;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Syllabus demo: java.net URL, URLConnection, protocol handlers and content handlers.
 *
 *  1. Parse a URL into its parts.
 *  2. Open an HTTP URLConnection to a tiny local web server and read headers + body.
 *  3. Register a CUSTOM PROTOCOL HANDLER for  shopsphere://product/1  (URLStreamHandler).
 *  4. Use a CONTENT HANDLER (ContentHandlerFactory) so url.getContent() returns a Product-like String.
 *
 * Run:  java -cp ... com.shopsphere.network.url.UrlDemo      (steps 3-4 need the database)
 */
public class UrlDemo {

    public static void main(String[] args) throws Exception {
        // ---------- 1. URL anatomy ----------
        URL u = new URI("https://shop.example.com:8443/shopsphere/products?category=3&sort=price#top").toURL();
        System.out.println("== URL parts ==");
        System.out.println("protocol = " + u.getProtocol());
        System.out.println("host     = " + u.getHost());
        System.out.println("port     = " + u.getPort());
        System.out.println("path     = " + u.getPath());
        System.out.println("query    = " + u.getQuery());
        System.out.println("ref      = " + u.getRef());

        // ---------- 2. HTTP URLConnection against a local server ----------
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/hello", ex -> {
            byte[] body = "Hello from the local ShopSphere demo server".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });
        server.start();
        try {
            URL http = new URI("http://127.0.0.1:" + server.getAddress().getPort() + "/hello").toURL();
            HttpURLConnection con = (HttpURLConnection) http.openConnection();
            con.setRequestMethod("GET");
            System.out.println("\n== HttpURLConnection ==");
            System.out.println("status       = " + con.getResponseCode());
            System.out.println("content-type = " + con.getContentType());
            try (InputStream in = con.getInputStream()) {
                System.out.println("body         = " + new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            server.stop(0);
        }

        // ---------- 3 + 4. Custom protocol handler + content handler ----------
        System.out.println("\n== Custom protocol handler: shopsphere://product/<id> ==");
        URL.setURLStreamHandlerFactory(protocol -> "shopsphere".equals(protocol) ? new ShopSphereHandler() : null);
        URLConnection.setContentHandlerFactory(mime ->
                "text/x-shopsphere-product".equals(mime) ? new ProductContentHandler() : null);

        URL custom = new URI("shopsphere://localhost/product/1").toURL();
        URLConnection con = custom.openConnection();
        System.out.println("content-type = " + con.getContentType());
        System.out.println("raw text     = " + new String(con.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim());
        Object content = custom.getContent();                // ContentHandler turns the stream into an object
        System.out.println("getContent() = " + content + "  (" + content.getClass().getSimpleName() + ")");
    }

    /** Protocol handler: knows how to "connect" to shopsphere: URLs by reading the database. */
    static class ShopSphereHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL url) {
            return new URLConnection(url) {
                private byte[] data;

                @Override public void connect() throws IOException {
                    try {
                        int id = Integer.parseInt(url.getPath().replaceAll("\\D", ""));
                        Product p = new ProductDAO().findById(id);
                        String text = p == null ? "NOT_FOUND" : p.getProductId() + "|" + p.getName() + "|" + p.getFinalPrice() + "|" + p.getStock();
                        data = (text + "\n").getBytes(StandardCharsets.UTF_8);
                        connected = true;
                    } catch (NumberFormatException | SQLException e) {
                        throw new IOException("Cannot resolve " + url, e);
                    }
                }

                @Override public InputStream getInputStream() throws IOException {
                    if (!connected) connect();
                    return new ByteArrayInputStream(data);
                }

                @Override public String getContentType() { return "text/x-shopsphere-product"; }
            };
        }
    }

    /** Content handler: converts the "id|name|price|stock" stream into a readable summary object. */
    static class ProductContentHandler extends ContentHandler {
        @Override
        public Object getContent(URLConnection con) throws IOException {
            try (Scanner sc = new Scanner(con.getInputStream(), StandardCharsets.UTF_8)) {
                String[] f = sc.nextLine().split("\\|");
                if (f.length < 4) return "Product not found";
                return "Product #" + f[0] + " '" + f[1] + "' costs Rs." + f[2] + " (" + f[3] + " in stock)";
            }
        }
    }
}

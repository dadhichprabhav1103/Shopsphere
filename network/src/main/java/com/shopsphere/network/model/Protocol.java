package com.shopsphere.network.model;

/**
 * Wire protocol of the inventory server.
 * The client sends ONE command (a String object) and receives ONE reply object:
 *   GET_PRODUCT <id>      -> Product           (serialized object)   or String "ERROR ..."
 *   GET_STOCK <id>        -> Integer           (units in stock)       or String "ERROR ..."
 *   SEARCH_PRODUCT <text> -> ArrayList<Product>
 *   PING                  -> String "PONG"
 *   QUIT                  -> String "BYE" and the server closes the connection
 */
public final class Protocol {
    public static final int DEFAULT_PORT = 5050;
    public static final String GET_PRODUCT = "GET_PRODUCT";
    public static final String GET_STOCK = "GET_STOCK";
    public static final String SEARCH_PRODUCT = "SEARCH_PRODUCT";
    public static final String PING = "PING";
    public static final String QUIT = "QUIT";

    private Protocol() {}
}

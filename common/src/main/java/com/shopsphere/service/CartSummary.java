package com.shopsphere.service;

import com.shopsphere.model.CartItem;

import java.util.List;

/** Cart contents plus totals, calculated on the server. */
public class CartSummary {
    private final List<CartItem> items;
    private final double subtotal;
    private final int itemCount;

    public CartSummary(List<CartItem> items) {
        this.items = items;
        double s = 0;
        int n = 0;
        for (CartItem i : items) { s += i.getLineTotal(); n += i.getQuantity(); }
        this.subtotal = Math.round(s * 100) / 100.0;
        this.itemCount = n;
    }

    public List<CartItem> getItems() { return items; }
    public double getSubtotal() { return subtotal; }
    public int getItemCount() { return itemCount; }
    public boolean isEmpty() { return items.isEmpty(); }
}

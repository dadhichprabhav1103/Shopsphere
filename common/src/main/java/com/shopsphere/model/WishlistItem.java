package com.shopsphere.model;

import java.io.Serializable;

public class WishlistItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private int wishlistId;
    private int userId;
    private int productId;
    private java.sql.Timestamp createdAt;
    private Product product;

    public WishlistItem() {}

    public int getWishlistId() { return wishlistId; }
    public void setWishlistId(int wishlistId) { this.wishlistId = wishlistId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

}

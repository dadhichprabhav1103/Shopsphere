package com.shopsphere.model;

import java.io.Serializable;

/**
 * Product entity. Implements Serializable so it can be sent over a
 * Socket (ObjectOutputStream) and returned by the RMI InventoryService.
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private int productId;
    private int categoryId;
    private String name;
    private String brand;
    private String description;
    private double price;
    private double discount;      // percentage, 0 - 100
    private int stock;
    private String imageUrl;
    private boolean status;
    private java.sql.Timestamp createdAt;

    // Joined / derived values (filled by DAO queries)
    private String categoryName;
    private double avgRating;
    private int reviewCount;

    public Product() {}

    public Product(int productId, String name, double price, int stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /** Price after applying the percentage discount, rounded to 2 decimals. */
    public double getFinalPrice() {
        return Math.round(price * (100 - discount)) / 100.0;
    }

    public boolean isInStock() { return stock > 0; }
    public boolean isLowStock() { return stock > 0 && stock <= 10; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }
    public java.sql.Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.sql.Timestamp createdAt) { this.createdAt = createdAt; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    @Override
    public String toString() {
        return "Product[" + productId + ", " + name + ", Rs." + getFinalPrice() + ", stock=" + stock + "]";
    }
}

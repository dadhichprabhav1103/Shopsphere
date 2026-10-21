package com.shopsphere.dao;

import com.shopsphere.model.Product;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** JDBC DAO for products (with category name and average rating joined in). */
public class ProductDAO {

    static final String COLS =
        "p.*, c.name AS category_name, COALESCE(r.avg_r, 0) AS avg_rating, COALESCE(r.cnt, 0) AS review_count ";
    static final String FROM_JOINS =
        "FROM products p JOIN categories c ON c.category_id = p.category_id "
      + "LEFT JOIN (SELECT product_id, AVG(rating) AS avg_r, COUNT(*) AS cnt FROM reviews GROUP BY product_id) r "
      + "ON r.product_id = p.product_id ";
    static final String BASE = "SELECT " + COLS + FROM_JOINS;

    /** Whitelisted ORDER BY fragments - user input never goes into SQL text. */
    private static final Map<String, String> SORTS = Map.of(
        "price_asc",  "ORDER BY (p.price * (100 - p.discount)) ASC",
        "price_desc", "ORDER BY (p.price * (100 - p.discount)) DESC",
        "rating",     "ORDER BY avg_rating DESC, review_count DESC",
        "newest",     "ORDER BY p.created_at DESC, p.product_id DESC",
        "name",       "ORDER BY p.name ASC");

    static Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setName(rs.getString("name"));
        p.setBrand(rs.getString("brand"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getDouble("price"));
        p.setDiscount(rs.getDouble("discount"));
        p.setStock(rs.getInt("stock"));
        p.setImageUrl(rs.getString("image_url"));
        p.setStatus(rs.getBoolean("status"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setCategoryName(rs.getString("category_name"));
        p.setAvgRating(Math.round(rs.getDouble("avg_rating") * 10) / 10.0);
        p.setReviewCount(rs.getInt("review_count"));
        return p;
    }

    public Product findById(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.product_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    /**
     * Storefront search / filter / sort. Any argument may be null (= no filter).
     * Only active products in active categories are returned.
     */
    public List<Product> search(String keyword, Integer categoryId, Double minPrice, Double maxPrice,
                                String sort) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE).append("WHERE p.status = 1 AND c.status = 1 ");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append("AND (p.name LIKE ? OR p.brand LIKE ? OR p.description LIKE ?) ");
            String like = "%" + keyword.trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        if (categoryId != null) { sql.append("AND p.category_id = ? "); params.add(categoryId); }
        if (minPrice != null)   { sql.append("AND (p.price * (100 - p.discount) / 100) >= ? "); params.add(minPrice); }
        if (maxPrice != null)   { sql.append("AND (p.price * (100 - p.discount) / 100) <= ? "); params.add(maxPrice); }
        sql.append(SORTS.getOrDefault(sort == null ? "" : sort, SORTS.get("newest")));

        List<Product> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    /** Admin listing: includes inactive products. */
    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "ORDER BY p.product_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Product> findLowStock(int threshold) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(BASE + "WHERE p.stock <= ? ORDER BY p.stock, p.name")) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    /** Simple LIKE search used by the socket server's SEARCH_PRODUCT command. */
    public List<Product> searchByName(String keyword) throws SQLException {
        return search(keyword, null, null, null, "name");
    }

    public List<Product> findFeatured(int limit) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 BASE + "WHERE p.status = 1 AND c.status = 1 ORDER BY p.discount DESC, p.product_id LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public int insert(Product p) throws SQLException {
        String sql = "INSERT INTO products (category_id, name, brand, description, price, discount, stock, image_url, status) "
                   + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public boolean update(Product p) throws SQLException {
        String sql = "UPDATE products SET category_id=?, name=?, brand=?, description=?, price=?, discount=?, "
                   + "stock=?, image_url=?, status=? WHERE product_id=?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            ps.setInt(10, p.getProductId());
            return ps.executeUpdate() > 0;
        }
    }

    private void bind(PreparedStatement ps, Product p) throws SQLException {
        ps.setInt(1, p.getCategoryId());
        ps.setString(2, p.getName());
        ps.setString(3, p.getBrand());
        ps.setString(4, p.getDescription());
        ps.setDouble(5, p.getPrice());
        ps.setDouble(6, p.getDiscount());
        ps.setInt(7, p.getStock());
        ps.setString(8, p.getImageUrl() == null || p.getImageUrl().isBlank() ? "images/placeholder.svg" : p.getImageUrl());
        ps.setBoolean(9, p.isStatus());
    }

    /** Soft delete: keeps order history intact. */
    public boolean deactivate(int id) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET status = 0 WHERE product_id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStock(int productId, int newStock) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = ? WHERE product_id = ?")) {
            ps.setInt(1, newStock);
            ps.setInt(2, productId);
            return ps.executeUpdate() > 0;
        }
    }

    public int getStock(int productId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT stock FROM products WHERE product_id = ?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    /**
     * Atomically reduces stock inside the caller's transaction.
     * The WHERE stock >= ? guard prevents overselling.
     */
    public boolean decrementStock(Connection c, int productId, int qty) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE products SET stock = stock - ? WHERE product_id = ? AND stock >= ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            return ps.executeUpdate() == 1;
        }
    }

    /** Adds stock back (used when an order is cancelled). */
    public void restoreStock(Connection c, int productId, int qty) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = stock + ? WHERE product_id = ?")) {
            ps.setInt(1, qty);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    public int count() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM products WHERE status = 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}

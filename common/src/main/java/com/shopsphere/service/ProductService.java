package com.shopsphere.service;

import com.shopsphere.dao.CategoryDAO;
import com.shopsphere.dao.ProductDAO;
import com.shopsphere.dao.ReviewDAO;
import com.shopsphere.model.Category;
import com.shopsphere.model.Product;
import com.shopsphere.model.Review;

import java.sql.SQLException;
import java.util.List;

/** Catalogue use-cases for both the storefront and the admin area. */
public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    public List<Product> search(String keyword, Integer categoryId, Double min, Double max, String sort) throws ServiceException {
        try { return productDAO.search(keyword, categoryId, min, max, sort); }
        catch (SQLException e) { throw new ServiceException("Could not load products.", e); }
    }

    public List<Product> featured(int limit) throws ServiceException {
        try { return productDAO.findFeatured(limit); }
        catch (SQLException e) { throw new ServiceException("Could not load products.", e); }
    }

    public Product getActive(int id) throws ServiceException {
        try {
            Product p = productDAO.findById(id);
            if (p == null || !p.isStatus()) throw new ServiceException("Product not found.");
            return p;
        } catch (SQLException e) { throw new ServiceException("Could not load product.", e); }
    }

    public List<Review> reviews(int productId) throws ServiceException {
        try { return reviewDAO.findByProduct(productId); }
        catch (SQLException e) { throw new ServiceException("Could not load reviews.", e); }
    }

    public List<Category> categories(boolean activeOnly) throws ServiceException {
        try { return categoryDAO.findAll(activeOnly); }
        catch (SQLException e) { throw new ServiceException("Could not load categories.", e); }
    }

    // ---------------- admin operations ----------------

    public List<Product> adminList() throws ServiceException {
        try { return productDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException("Could not load products.", e); }
    }

    public Product adminGet(int id) throws ServiceException {
        try { return productDAO.findById(id); }
        catch (SQLException e) { throw new ServiceException("Could not load product.", e); }
    }

    public void validate(Product p) throws ServiceException {
        if (p.getName() == null || p.getName().trim().length() < 2) throw new ServiceException("Product name is required.");
        if (p.getCategoryId() <= 0) throw new ServiceException("Select a category.");
        if (p.getPrice() <= 0) throw new ServiceException("Price must be greater than zero.");
        if (p.getDiscount() < 0 || p.getDiscount() > 90) throw new ServiceException("Discount must be between 0 and 90 percent.");
        if (p.getStock() < 0) throw new ServiceException("Stock cannot be negative.");
    }

    public void save(Product p) throws ServiceException {
        validate(p);
        try {
            if (p.getProductId() > 0) productDAO.update(p); else productDAO.insert(p);
        } catch (SQLException e) { throw new ServiceException("Could not save product.", e); }
    }

    public void deactivate(int id) throws ServiceException {
        try { productDAO.deactivate(id); }
        catch (SQLException e) { throw new ServiceException("Could not remove product.", e); }
    }

    public void updateStock(int id, int stock) throws ServiceException {
        if (stock < 0) throw new ServiceException("Stock cannot be negative.");
        try { productDAO.updateStock(id, stock); }
        catch (SQLException e) { throw new ServiceException("Could not update stock.", e); }
    }

    public List<Product> lowStock() throws ServiceException {
        try { return productDAO.findLowStock(10); }
        catch (SQLException e) { throw new ServiceException("Could not load stock.", e); }
    }

    // ---------------- categories ----------------

    public void saveCategory(Category c) throws ServiceException {
        if (c.getName() == null || c.getName().trim().length() < 2) throw new ServiceException("Category name is required.");
        try {
            if (c.getCategoryId() > 0) categoryDAO.update(c); else categoryDAO.insert(c);
        } catch (SQLException e) {
            throw new ServiceException("Could not save category (name may already exist).", e);
        }
    }

    public void deleteCategory(int id) throws ServiceException {
        try { categoryDAO.delete(id); }
        catch (SQLException e) { throw new ServiceException("Category cannot be deleted while products use it. Disable it instead.", e); }
    }
}

package com.shopsphere.service;

import com.shopsphere.dao.ReviewDAO;
import com.shopsphere.model.Review;

import java.sql.SQLException;

public class ReviewService {
    private final ReviewDAO dao = new ReviewDAO();

    public void submit(int userId, int productId, int rating, String text) throws ServiceException {
        if (rating < 1 || rating > 5) throw new ServiceException("Rating must be between 1 and 5.");
        if (text != null && text.length() > 500) throw new ServiceException("Review is too long (max 500 characters).");
        try {
            if (!dao.hasPurchased(userId, productId)) throw new ServiceException("You can review a product after purchasing it.");
            Review r = new Review();
            r.setUserId(userId);
            r.setProductId(productId);
            r.setRating(rating);
            r.setReviewText(text == null ? "" : text.trim());
            dao.save(r);
        } catch (SQLException e) { throw new ServiceException("Could not save review.", e); }
    }

    public boolean canReview(int userId, int productId) throws ServiceException {
        try { return dao.hasPurchased(userId, productId); }
        catch (SQLException e) { throw new ServiceException("Could not check purchase.", e); }
    }
}

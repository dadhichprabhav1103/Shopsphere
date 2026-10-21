package com.shopsphere.service;

import com.shopsphere.dao.UserDAO;
import com.shopsphere.model.User;
import com.shopsphere.util.PasswordUtil;
import com.shopsphere.util.Validator;

import java.sql.SQLException;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();

    public User register(String name, String email, String password, String confirm, String mobile) throws ServiceException {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase();
        mobile = mobile == null ? "" : mobile.trim();
        if (name.length() < 2 || name.length() > 100) throw new ServiceException("Please enter your name (2-100 characters).");
        if (!Validator.isEmail(email)) throw new ServiceException("Please enter a valid email address.");
        if (!Validator.isMobile(mobile)) throw new ServiceException("Mobile number must be 10 digits.");
        if (!Validator.isStrongPassword(password)) throw new ServiceException("Password must be at least 8 characters and contain a letter and a digit.");
        if (!password.equals(confirm)) throw new ServiceException("Passwords do not match.");
        try {
            if (userDAO.findByEmail(email) != null) throw new ServiceException("An account with this email already exists.");
            User u = new User();
            u.setName(name);
            u.setEmail(email);
            u.setMobile(mobile);
            u.setPassword(PasswordUtil.hash(password));
            u.setRole("CUSTOMER");
            u.setUserId(userDAO.insert(u));
            u.setStatus(true);
            return u;
        } catch (SQLException e) {
            throw new ServiceException("Registration failed. Please try again.", e);
        }
    }

    /** @return the authenticated user (password hash removed) or throws with a generic message. */
    public User login(String email, String password) throws ServiceException {
        try {
            User u = Validator.isBlank(email) ? null : userDAO.findByEmail(email.trim().toLowerCase());
            // same message for unknown email and wrong password (no user enumeration)
            if (u == null || !PasswordUtil.verify(password, u.getPassword())) throw new ServiceException("Invalid email or password.");
            if (!u.isStatus()) throw new ServiceException("This account has been blocked. Contact support.");
            u.setPassword(null);
            return u;
        } catch (SQLException e) {
            throw new ServiceException("Login failed. Please try again.", e);
        }
    }

    public User updateProfile(int userId, String name, String mobile) throws ServiceException {
        if (name == null || name.trim().length() < 2) throw new ServiceException("Please enter your name.");
        if (!Validator.isMobile(mobile)) throw new ServiceException("Mobile number must be 10 digits.");
        try {
            userDAO.updateProfile(userId, name.trim(), mobile.trim());
            User u = userDAO.findById(userId);
            u.setPassword(null);
            return u;
        } catch (SQLException e) {
            throw new ServiceException("Could not update profile.", e);
        }
    }

    public void changePassword(int userId, String current, String next, String confirm) throws ServiceException {
        if (!Validator.isStrongPassword(next)) throw new ServiceException("New password must be at least 8 characters with a letter and a digit.");
        if (!next.equals(confirm)) throw new ServiceException("New passwords do not match.");
        try {
            User u = userDAO.findById(userId);
            if (u == null || !PasswordUtil.verify(current, u.getPassword())) throw new ServiceException("Current password is incorrect.");
            userDAO.updatePassword(userId, PasswordUtil.hash(next));
        } catch (SQLException e) {
            throw new ServiceException("Could not change password.", e);
        }
    }
}

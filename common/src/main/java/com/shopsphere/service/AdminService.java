package com.shopsphere.service;

import com.shopsphere.dao.AddressDAO;
import com.shopsphere.dao.CouponDAO;
import com.shopsphere.dao.ProductDAO;
import com.shopsphere.dao.UserDAO;
import com.shopsphere.model.Address;
import com.shopsphere.model.Coupon;
import com.shopsphere.model.User;

import java.sql.SQLException;
import java.util.List;

/** Customer, coupon and address operations used by the admin area and profile page. */
public class AdminService {
    private final UserDAO userDAO = new UserDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public List<User> customers() throws ServiceException {
        try { return userDAO.findCustomers(); }
        catch (SQLException e) { throw new ServiceException("Could not load customers.", e); }
    }

    public void setCustomerActive(int userId, boolean active) throws ServiceException {
        try { userDAO.setStatus(userId, active); }
        catch (SQLException e) { throw new ServiceException("Could not update customer.", e); }
    }

    public int customerCount() throws ServiceException {
        try { return userDAO.countCustomers(); }
        catch (SQLException e) { throw new ServiceException("Could not load summary.", e); }
    }

    public int productCount() throws ServiceException {
        try { return productDAO.count(); }
        catch (SQLException e) { throw new ServiceException("Could not load summary.", e); }
    }

    public List<Coupon> coupons() throws ServiceException {
        try { return couponDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException("Could not load coupons.", e); }
    }

    public void addCoupon(Coupon c) throws ServiceException {
        if (c.getCode() == null || !c.getCode().matches("[A-Za-z0-9]{3,30}")) throw new ServiceException("Coupon code must be 3-30 letters/digits.");
        if (!"PERCENT".equals(c.getDiscountType()) && !"FLAT".equals(c.getDiscountType())) throw new ServiceException("Choose PERCENT or FLAT.");
        if (c.getDiscountValue() <= 0) throw new ServiceException("Discount value must be positive.");
        if ("PERCENT".equals(c.getDiscountType()) && c.getDiscountValue() > 90) throw new ServiceException("Percentage cannot exceed 90.");
        c.setCode(c.getCode().toUpperCase());
        try { couponDAO.insert(c); }
        catch (SQLException e) { throw new ServiceException("Could not save coupon (code may already exist).", e); }
    }

    public void setCouponActive(int couponId, boolean active) throws ServiceException {
        try { couponDAO.setStatus(couponId, active); }
        catch (SQLException e) { throw new ServiceException("Could not update coupon.", e); }
    }

    // ---- addresses (profile + checkout) ----

    public List<Address> addresses(int userId) throws ServiceException {
        try { return addressDAO.findByUser(userId); }
        catch (SQLException e) { throw new ServiceException("Could not load addresses.", e); }
    }

    public void addAddress(Address a) throws ServiceException {
        if (a.getAddressLine() == null || a.getAddressLine().trim().length() < 5) throw new ServiceException("Enter the full address line.");
        if (a.getCity() == null || a.getCity().isBlank() || a.getState() == null || a.getState().isBlank()) throw new ServiceException("City and state are required.");
        if (!com.shopsphere.util.Validator.isPincode(a.getPincode())) throw new ServiceException("Pincode must be 6 digits.");
        try { addressDAO.insert(a); }
        catch (SQLException e) { throw new ServiceException("Could not save address.", e); }
    }

    public void deleteAddress(int addressId, int userId) throws ServiceException {
        try {
            if (!addressDAO.delete(addressId, userId)) throw new ServiceException("This address is used by an order and cannot be deleted.");
        } catch (SQLException e) { throw new ServiceException("Could not delete address.", e); }
    }
}

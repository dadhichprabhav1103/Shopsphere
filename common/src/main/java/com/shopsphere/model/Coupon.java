package com.shopsphere.model;

import java.io.Serializable;

public class Coupon implements Serializable {
    private static final long serialVersionUID = 1L;

    private int couponId;
    private String code;
    private String discountType;
    private double discountValue;
    private double minimumOrder;
    private double maximumDiscount;
    private java.sql.Date expiryDate;
    private boolean status;

    public Coupon() {}

    public int getCouponId() { return couponId; }
    public void setCouponId(int couponId) { this.couponId = couponId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
    public double getMinimumOrder() { return minimumOrder; }
    public void setMinimumOrder(double minimumOrder) { this.minimumOrder = minimumOrder; }
    public double getMaximumDiscount() { return maximumDiscount; }
    public void setMaximumDiscount(double maximumDiscount) { this.maximumDiscount = maximumDiscount; }
    public java.sql.Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(java.sql.Date expiryDate) { this.expiryDate = expiryDate; }
    public boolean isStatus() { return status; }
    public void setStatus(boolean status) { this.status = status; }

    /** Returns true if the coupon can be applied to the given order subtotal. */
    public boolean isApplicable(double subtotal) {
        boolean notExpired = expiryDate == null || !expiryDate.toLocalDate().isBefore(java.time.LocalDate.now());
        return status && notExpired && subtotal >= minimumOrder;
    }

    /** Calculates the discount amount for the given subtotal (server-side rule). */
    public double calculateDiscount(double subtotal) {
        if (!isApplicable(subtotal)) return 0;
        double d = "PERCENT".equalsIgnoreCase(discountType) ? subtotal * discountValue / 100.0 : discountValue;
        if (maximumDiscount > 0) d = Math.min(d, maximumDiscount);
        return Math.min(d, subtotal);
    }

}

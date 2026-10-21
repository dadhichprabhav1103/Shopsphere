
package com.shopsphere.dao;

import com.shopsphere.model.Coupon;
import com.shopsphere.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class CouponDAO {

    public Coupon findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE code = ? LIMIT 1";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCoupon(rs);
                }
            }
        }

        return null;
    }

    public boolean hasUserUsed(int couponId, int userId)
            throws SQLException {
        String sql =
            "SELECT 1 FROM coupon_usage WHERE coupon_id = ? AND user_id = ? LIMIT 1";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, couponId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void recordUsage(Connection con, int couponId,
                            int userId, int orderId)
            throws SQLException {
        String sql =
            "INSERT INTO coupon_usage (coupon_id, user_id, order_id) VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            ps.setInt(3, orderId);
            ps.executeUpdate();
        }
    }

    public List<Coupon> findAll() throws SQLException {
        String sql = "SELECT * FROM coupons ORDER BY id DESC";
        List<Coupon> coupons = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                coupons.add(mapCoupon(rs));
            }
        }

        return coupons;
    }

    public void insert(Coupon coupon) throws SQLException {
        String sql =
            "INSERT INTO coupons (code, discount_type, discount_value, minimum_order, expiry_date, active) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, coupon.getCode());
            ps.setString(2, coupon.getDiscountType());
            ps.setDouble(3, coupon.getDiscountValue());
            ps.setDouble(4, coupon.getMinimumOrder());

            if (coupon.getExpiryDate() == null) {
                ps.setNull(5, Types.DATE);
            } else {
                ps.setDate(5, java.sql.Date.valueOf(
                    coupon.getExpiryDate().toLocalDate()));
            }

            ps.setBoolean(6, coupon.isStatus());
            ps.executeUpdate();
        }
    }

    public void setStatus(int couponId, boolean active)
            throws SQLException {
        String sql = "UPDATE coupons SET active = ? WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, active);
            ps.setInt(2, couponId);
            ps.executeUpdate();
        }
    }

    private Coupon mapCoupon(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();

        coupon.setCouponId(rs.getInt("id"));
        coupon.setCode(rs.getString("code"));
        coupon.setDiscountType(rs.getString("discount_type"));
        coupon.setDiscountValue(rs.getDouble("discount_value"));
        coupon.setMinimumOrder(rs.getDouble("minimum_order"));

        java.sql.Date expiry = rs.getDate("expiry_date");
        if (expiry != null) {
            coupon.setExpiryDate(expiry);
        }

        coupon.setStatus(rs.getBoolean("active"));
        return coupon;
    }
}

package com.shopsphere.dao;

import com.shopsphere.model.Address;
import com.shopsphere.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AddressDAO {

    private Address map(ResultSet rs) throws SQLException {
        Address a = new Address();
        a.setAddressId(rs.getInt("address_id"));
        a.setUserId(rs.getInt("user_id"));
        a.setAddressLine(rs.getString("address_line"));
        a.setCity(rs.getString("city"));
        a.setState(rs.getString("state"));
        a.setPincode(rs.getString("pincode"));
        a.setAddressType(rs.getString("address_type"));
        return a;
    }

    public List<Address> findByUser(int userId) throws SQLException {
        List<Address> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM addresses WHERE user_id = ? ORDER BY address_id")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    /** Returns the address only if it belongs to the given user. */
    public Address findByIdForUser(int addressId, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM addresses WHERE address_id = ? AND user_id = ?")) {
            ps.setInt(1, addressId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(Address a) throws SQLException {
        String sql = "INSERT INTO addresses (user_id, address_line, city, state, pincode, address_type) VALUES (?,?,?,?,?,?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, a.getUserId());
            ps.setString(2, a.getAddressLine());
            ps.setString(3, a.getCity());
            ps.setString(4, a.getState());
            ps.setString(5, a.getPincode());
            ps.setString(6, a.getAddressType() == null ? "HOME" : a.getAddressType());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { return k.next() ? k.getInt(1) : 0; }
        }
    }

    public boolean delete(int addressId, int userId) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM addresses WHERE address_id = ? AND user_id = ?")) {
            ps.setInt(1, addressId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false; // address is referenced by an order
        }
    }
}

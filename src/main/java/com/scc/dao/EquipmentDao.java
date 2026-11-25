
package com.scc.dao;

import com.scc.model.Equipment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipmentDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    public boolean addEquipment(String name, String category,
                                String description, int quantity,
                                int adminId) {

        if (quantity <= 0) return false;

        String sql = """
            INSERT INTO equipment
            (equipment_name,category,description,quantity,
             available_quantity,condition,status)
            VALUES(?,?,?,?,?,'GOOD','AVAILABLE')
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, description);
            ps.setInt(4, quantity);
            ps.setInt(5, quantity);

            if (ps.executeUpdate() > 0) {
                logDao.logAction(adminId, "EQUIPMENT_ADDED",
                        "Added equipment: " + name);
                return true;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    public List<Equipment> getAllEquipment() {

        List<Equipment> list = new ArrayList<>();

        String sql = "SELECT * FROM equipment ORDER BY equipment_id";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapEquipment(rs));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    public List<Equipment> getAvailableEquipment() {

        List<Equipment> list = new ArrayList<>();

        String sql = """
            SELECT * FROM equipment
            WHERE available_quantity > 0 AND status='AVAILABLE'
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) list.add(mapEquipment(rs));

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    public List<Equipment> searchEquipment(String keyword) {

        List<Equipment> list = new ArrayList<>();

        String sql = """
            SELECT * FROM equipment
            WHERE equipment_name LIKE ?
               OR category LIKE ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapEquipment(rs));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return list;
    }

    public boolean updateEquipment(int id, String name,
                                   String category, String description,
                                   int quantity, int adminId) {

        String sql = """
            UPDATE equipment
            SET equipment_name=?, category=?, description=?, quantity=?,
                available_quantity=available_quantity + (? - quantity)
            WHERE equipment_id=?
              AND available_quantity + (? - quantity) >= 0
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, description);
            ps.setInt(4, quantity);
            ps.setInt(5, quantity);
            ps.setInt(6, id);
            ps.setInt(7, quantity);

            if (ps.executeUpdate() > 0) {
                logDao.logAction(adminId, "EQUIPMENT_UPDATED",
                        "Updated equipment ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    public boolean deleteEquipment(int id, int adminId) {

        String sql = """
            UPDATE equipment SET status='INACTIVE'
            WHERE equipment_id=?
              AND available_quantity=quantity
              AND NOT EXISTS (
                SELECT 1 FROM transaction_items
                WHERE equipment_id=?
              )
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setInt(2, id);

            if (ps.executeUpdate() > 0) {
                logDao.logAction(adminId, "EQUIPMENT_ARCHIVED",
                        "Archived equipment ID: " + id);
                return true;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return false;
    }

    private Equipment mapEquipment(ResultSet rs) throws SQLException {

        return new Equipment(
                rs.getInt("equipment_id"),
                rs.getString("equipment_name"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getInt("quantity"),
                rs.getInt("available_quantity"),
                rs.getString("condition"),
                rs.getString("status")
        );
    }
}
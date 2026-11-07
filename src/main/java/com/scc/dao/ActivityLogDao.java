
package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // Insert a new activity log
    public boolean logAction(int userId, String actionType,
                             String description) {

        String sql = """
            INSERT INTO activity_logs
            (user_id, action_type, description, datetime)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Audit log insertion error: "
                    + e.getMessage());
            return false;
        }
    }

    // Retrieve all activity logs
    public List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
            SELECT logs_id, user_id, action_type,
                   description, datetime
            FROM activity_logs
            ORDER BY datetime DESC, logs_id DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                logs.add(mapActivityLog(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving audit logs: "
                    + e.getMessage());
        }

        return logs;
    }

    // Retrieve activity logs by user ID
    public List<ActivityLog> getLogsByUserId(int userId) {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
            SELECT logs_id, user_id, action_type,
                   description, datetime
            FROM activity_logs
            WHERE user_id = ?
            ORDER BY datetime DESC, logs_id DESC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapActivityLog(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error filtering audit logs: "
                    + e.getMessage());
        }

        return logs;
    }

    // Convert ResultSet into ActivityLog
    private ActivityLog mapActivityLog(ResultSet rs)
            throws SQLException {

        return new ActivityLog(
                rs.getInt("logs_id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("datetime")
        );
    }
}

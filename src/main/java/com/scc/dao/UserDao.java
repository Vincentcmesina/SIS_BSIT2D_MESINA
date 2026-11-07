
package com.scc.dao;

import com.scc.model.user;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public UserDao() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            defaultAdminGenerator(conn);
        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public void defaultAdminGenerator(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM user WHERE roles = 'ADMIN'";
        String insertSql = "INSERT INTO user(username, password, role, status) " +
                "VALUES('admin', 'admin123', 'ADMIN', 'APPROVED')";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(insertSql);
                System.out.println("-> Default Admin created (Username: admin | Password: admin123)");
            }

        } catch (SQLException e) {
            System.err.println("Error seeding default admin: " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String password, String role) {
        String sql = "INSERT INTO user(username, password, roles, status) " +
                "VALUES(?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role.toUpperCase());

            pstmt.executeUpdate();

            System.out.println("-> Successful Registration, Waiting for Approval!");
            return true;

        } catch (SQLException e) {
            System.err.println("Failed: " + e.getMessage());
            return false;
        }
    }

    public user login(String username, String password) {
        String sql = "SELECT id, username, password, roles, status " +
                "FROM user WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    String status = rs.getString("status");

                    if ("PENDING".equalsIgnoreCase(status)) {
                        System.out.println("Account is pending, waiting for Approval.");
                        return null;
                    }

                    user loggedInUser = new user(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            status
                    );

                    // Record successful login
                    logActivity(
                            loggedInUser.id(),
                            "LOGIN",
                            "User logged into the system."
                    );

                    return loggedInUser;

                } else {
                    System.out.println("--> Invalid username or password");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }

        return null;
    }

    public List<user> getAllUser() {
        List<user> usersList = new ArrayList<>();

        String sql = "SELECT id, username, password, roles, status FROM user";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usersList.add(new user(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }

        return usersList;
    }

    public List<user> getPendingUser() {
        List<user> pendingList = new ArrayList<>();

        String sql = "SELECT id, username, password, roles, status " +
                "FROM user WHERE status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                pendingList.add(new user(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
        }

        return pendingList;
    }

    public boolean approveUser(int userId) {
        String sql = "UPDATE user SET status = 'APPROVED' WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("-> User ID: " + userId + " approved successfully!");
                return true;
            } else {
                System.out.println("-> ID not found");
            }

        } catch (SQLException e) {
            System.err.println("Error approving user: " + e.getMessage());
        }

        return false;
    }

    public boolean updateUserAndStatus(int userId, String newRoles, String newStatus) {
        String sql = "UPDATE user SET roles = ?, status = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRoles.toUpperCase());
            pstmt.setString(2, newStatus.toUpperCase());
            pstmt.setInt(3, userId);

            int rowsUpdated = pstmt.executeUpdate();

            if (rowsUpdated > 0) {
                System.out.println("-> User ID: " + userId + " updated successfully!");
                return true;
            } else {
                System.out.println("-> User ID not found.");
            }

        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }

        return false;
    }

    // Save an activity to the activity_logs table.
    public void logActivity(int userId, String actionType, String description) {
        String sql = "INSERT INTO activity_logs " +
                "(user_id, action_type, description, datetime) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error saving activity log: " + e.getMessage());
        }
    }

    // Display saved activity logs for the admin.
    // Display saved activity logs for the admin.
    // Display saved activity logs for the admin.
    // Display saved activity logs for the admin.
    public void displayAuditLogs() {
        String sql = "SELECT logs_id, user_id, action_type, description, datetime " +
                "FROM activity_logs ORDER BY logs_id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n======================================================================================================================================");
            System.out.println("                                                    SYSTEM AUDIT LOGS                                                     ");
            System.out.println("======================================================================================================================================");
            System.out.printf("| %-8s | %-8s | %-18s | %-65s | %-19s |\n",
                    "LOG ID", "USER ID", "ACTION", "DESCRIPTION", "DATE/TIME");
            System.out.println("|----------|----------|--------------------|-------------------------------------------------------------------|---------------------|");

            boolean hasLogs = false;

            while (rs.next()) {
                hasLogs = true;

                // Format the description cleanly and replace internal pipes
                String description = rs.getString("description").replace(" | ", " - ");
                // Ensure it fits within the allocated width or pad it manually if needed,
                // but using standard formatting with explicit widths:
                String formattedDesc = String.format("%-65s", description);

                System.out.printf("| %-8d | %-8d | %-18s | %s | %-19s |\n",
                        rs.getInt("logs_id"),
                        rs.getInt("user_id"),
                        rs.getString("action_type"),
                        formattedDesc,
                        rs.getString("datetime"));
            }

            if (!hasLogs) {
                System.out.println("|                                               No activity logs found.                                                  |");
            }

            System.out.println("======================================================================================================================================");

        } catch (SQLException e) {
            System.err.println("Error displaying audit logs: " + e.getMessage());
        }
    }
}
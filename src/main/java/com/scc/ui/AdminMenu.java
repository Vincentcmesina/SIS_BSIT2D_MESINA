
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.user;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner scanner;

    public AdminMenu(UserDao userDao, Scanner scanner) {
        this.userDao = userDao;
        this.scanner = scanner;
    }

    public void display(user admin) {
        boolean loggedIn = true;

        while (loggedIn) {
            System.out.println("\n=================================");
            System.out.println("         ADMIN DASHBOARD");
            System.out.println("=================================");
            System.out.println("1. View All Users");
            System.out.println("2. View Pending User Approvals");
            System.out.println("3. Approve User Registration");
            System.out.println("4. Add New User");
            System.out.println("5. Update User Role / Status");
            System.out.println("6. Delete User Account");
            System.out.println("7. View System Audit Logs");
            System.out.println("8. Logout");
            System.out.print("Choose an option (1-8): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUser();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser(admin);
                case "4" -> System.out.println("Add New User is not implemented yet.");
                case "5" -> updateUser(admin);
                case "6" -> System.out.println("Delete User Account is not implemented yet.");
                case "7" -> userDao.displayAuditLogs();
                case "8" -> {
                    userDao.logActivity(
                            admin.id(),
                            "LOGOUT",
                            "Admin logged out of the system."
                    );
                    System.out.println("-> Admin logged out successfully.");
                    loggedIn = false;
                }
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void displayUserTable(List<user> users, String title) {
        System.out.println("\n==================== " + title + " ====================");

        if (users.isEmpty()) {
            System.out.println("-> No Records Found");
            return;
        }

        System.out.println("+-----+-----------------+------------+------------+");
        System.out.printf("| %-3s | %-15s | %-10s | %-10s |%n",
                "ID", "Username", "Roles", "Status");
        System.out.println("+-----+-----------------+------------+------------+");

        for (user u : users) {
            System.out.printf("| %-3d | %-15s | %-10s | %-10s |%n",
                    u.id(), u.username(), u.roles(), u.status());
        }

        System.out.println("+-----+-----------------+------------+------------+");
    }

    private void viewAllUser() {
        displayUserTable(userDao.getAllUser(), "ALL REGISTERED USERS");
    }

    private void viewPendingUsers() {
        displayUserTable(userDao.getPendingUser(), "PENDING APPROVALS");
    }

    private void approveUser(user admin) {
        List<user> pendingUsers = userDao.getPendingUser();

        displayUserTable(pendingUsers, "PENDING APPROVALS");

        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter User ID to approve (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        boolean exists = pendingUsers.stream().anyMatch(u -> u.id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (userDao.approveUser(userId)) {
            userDao.logActivity(
                    admin.id(),
                    "APPROVE_USER",
                    "Admin approved user account with ID: " + userId
            );
        }
    }

    private void updateUser(user currentAdmin) {
        List<user> users = userDao.getAllUser();

        displayUserTable(users, "SELECT USER TO UPDATE");

        if (users.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter selected User ID to update (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        boolean exists = users.stream().anyMatch(u -> u.id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (userId == currentAdmin.id()) {
            System.out.println("-> [WARNING] Modifying the currently active admin is restricted.");
            return;
        }

        String newRole = readValidRole();

        if (newRole == null) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        String newStatus = readValidStatus();

        if (newStatus == null) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        if (userDao.updateUserAndStatus(userId, newRole, newStatus)) {
            userDao.logActivity(
                    currentAdmin.id(),
                    "UPDATE_USER",
                    "Admin updated user ID: " + userId +
                            " | New roles: " + newRole +
                            " | New status: " + newStatus
            );
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Enter Role (STUDENT/CUSTODIAN/ADMIN) [0 to cancel]: ");

            String role = scanner.nextLine().trim().toUpperCase();

            if (role.equals("0") || role.equals("CANCEL")) {
                return null;
            }

            if (role.equals("STUDENT") ||
                    role.equals("CUSTODIAN") ||
                    role.equals("ADMIN")) {
                return role;
            }

            System.out.println("-> [ERROR] Invalid Role.");
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter Status (APPROVED/PENDING) [0 to cancel]: ");

            String status = scanner.nextLine().trim().toUpperCase();

            if (status.equals("0") || status.equals("CANCEL")) {
                return null;
            }

            if (status.equals("APPROVED") || status.equals("PENDING")) {
                return status;
            }

            System.out.println("-> [ERROR] Invalid status.");
        }
    }

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);

            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("-> [ERROR] Please enter a valid numeric ID.");
            }
        }
    }
}


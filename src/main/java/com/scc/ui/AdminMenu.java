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
                case "4" -> addUser(admin);
                case "5" -> updateUser(admin);
                case "6" -> deleteUser(admin);
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

    private void addUser(user admin) {
        System.out.println("\n==================== ADD NEW USER ====================");
        System.out.println("(Type 0 at the username prompt to cancel)");

        String username;
        while (true) {
            System.out.print("Enter username: ");
            username = scanner.nextLine().trim();

            if (username.equals("0")) {
                System.out.println("-> Operation cancelled.");
                return;
            }
            if (username.isEmpty() || username.contains(" ")) {
                System.out.println("-> [ERROR] Username cannot be empty or contain spaces.");
            } else if (userDao.usernameExists(username)) {
                System.out.println("-> [ERROR] Username already taken.");
            } else {
                break;
            }
        }

        String password;
        while (true) {
            System.out.print("Enter password: ");
            password = scanner.nextLine();

            if (password.isBlank()) {
                System.out.println("-> [ERROR] Password cannot be empty.");
            } else {
                break;
            }
        }

        String role = readValidRole();
        if (role == null) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        String status = readValidStatus();
        if (status == null) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        int newId = userDao.addUser(username, password, role, status);

        if (newId > 0) {
            userDao.logActivity(
                    admin.id(),
                    "ADD_USER",
                    "Admin created user '" + username + "' (ID: " + newId +
                            ") | Roles: " + role + " | Status: " + status
            );
        }
    }

    private void deleteUser(user admin) {
        List<user> users = userDao.getAllUser();

        displayUserTable(users, "SELECT USER TO DELETE");

        if (users.isEmpty()) {
            return;
        }

        int userId = readIntInput("Enter User ID to delete (or 0 to cancel): ");

        if (userId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        user target = users.stream()
                .filter(u -> u.id() == userId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (target.id() == admin.id()) {
            System.out.println("-> [WARNING] You cannot delete the account you are logged in with.");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(target.roles()) && userDao.countUsersByRole("ADMIN") <= 1) {
            System.out.println("-> [WARNING] Cannot delete the last remaining admin.");
            return;
        }

        System.out.println("-> You are about to permanently delete: " + target);
        System.out.println("   This also removes the activity logs belonging to this user.");
        System.out.print("Type YES to confirm: ");

        if (!scanner.nextLine().trim().equals("YES")) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        if (userDao.deleteUser(userId)) {
            userDao.logActivity(
                    admin.id(),
                    "DELETE_USER",
                    "Admin deleted user '" + target.username() + "' (ID: " + userId +
                            ") | Roles: " + target.roles()
            );
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Enter Role (STUDENT/TEACHER/ADMIN) [0 to cancel]: ");

            String role = scanner.nextLine().trim().toUpperCase();

            if (role.equals("0") || role.equals("CANCEL")) {
                return null;
            }

            if (role.equals("STUDENT") ||
                    role.equals("TEACHER") ||
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
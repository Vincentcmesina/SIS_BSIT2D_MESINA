package com.scc.ui;

import com.scc.dao.EquipmentDao;
import com.scc.dao.BorrowTransactionDao;
import com.scc.dao.UserDao;
import com.scc.model.user;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final BorrowTransactionDao transactionDao = new BorrowTransactionDao();
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
            System.out.println("7. Manage Equipment");
            System.out.println("8. View Transactions");
            System.out.println("9. View System Audit Logs");
            System.out.println("10. Logout");
            System.out.print("Choose an option (1-10): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewAllUser();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser(admin);
                case "4" -> addNewUser(admin);
                case "5" -> updateUser(admin);
                case "6" -> deleteUser(admin);
                case "7" -> manageEquipment(admin);
                case "8" -> viewTransactions();
                case "9" -> userDao.displayAuditLogs();
                case "10" -> {
                    userDao.logActivity(
                            admin.user_id(),
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
                "ID", "Username", "Role", "Status");
        System.out.println("+-----+-----------------+------------+------------+");

        for (user u : users) {
            System.out.printf("| %-3d | %-15s | %-10s | %-10s |%n",
                    u.user_id(), u.username(), u.roles(), u.status());
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

        boolean exists = pendingUsers.stream().anyMatch(u -> u.user_id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (userDao.approveUser(userId)) {
            userDao.logActivity(
                    admin.user_id(),
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

        boolean exists = users.stream().anyMatch(u -> u.user_id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        if (userId == currentAdmin.user_id()) {
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
                    currentAdmin.user_id(),
                    "UPDATE_USER",
                    "Admin updated user ID: " + userId +
                            " | New role: " + newRole +
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

    private void addNewUser(user admin) {
        System.out.println("\n========== ADD NEW USER ==========");

        System.out.print("Enter Username: ");
        String username = scanner.nextLine().trim();

        if (username.isEmpty()) {
            System.out.println("-> [ERROR] Username cannot be empty.");
            return;
        }

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        if (password.isEmpty()) {
            System.out.println("-> [ERROR] Password cannot be empty.");
            return;
        }

        String role = readValidRole();

        if (role == null) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        // Register the new user
        if (userDao.registerUser(username, password, role)) {
            userDao.logActivity(
                    admin.user_id(),
                    "ADD_USER",
                    "Admin added a new user: " + username +
                            " | Role: " + role
            );

            System.out.println("-> User registered successfully!");
            System.out.println("-> Account Status: PENDING");
        } else {
            System.out.println("-> [ERROR] Failed to register user.");
        }
    }

    private void deleteUser(user admin) {
        System.out.println("\n========== DELETE USER ACCOUNT ==========");

        List<user> users = userDao.getAllUser();

        displayUserTable(users, "REGISTERED USERS");

        if (users.isEmpty()) {
            return;
        }

        int userId = readIntInput(
                "Enter User ID to delete (or 0 to cancel): "
        );

        if (userId == 0) {
            System.out.println("-> Operation cancelled.");
            return;
        }

        // Prevent admin from deleting their own account
        if (userId == admin.user_id()) {
            System.out.println(
                    "-> [WARNING] You cannot delete your own active account."
            );
            return;
        }

        boolean exists = users.stream()
                .anyMatch(u -> u.user_id() == userId);

        if (!exists) {
            System.out.println("-> [ERROR] User ID not found.");
            return;
        }

        System.out.print(
                "Are you sure you want to delete this account? (YES/NO): "
        );

        String confirmation = scanner.nextLine().trim();

        if (!confirmation.equalsIgnoreCase("YES")) {
            System.out.println("-> Deletion cancelled.");
            return;
        }

        if (userDao.deleteUser(userId)) {
            userDao.logActivity(
                    admin.user_id(),
                    "DELETE_USER",
                    "Admin deleted user account with ID: " + userId
            );

            System.out.println("-> User account deleted successfully!");
        } else {
            System.out.println("-> [ERROR] Failed to delete user account.");
        }
    }

    private void manageEquipment(user admin) {
        boolean running = true;

        while (running) {
            System.out.println("\n========== EQUIPMENT MANAGEMENT ==========");
            System.out.println("1. Add Equipment");
            System.out.println("2. View Equipment");
            System.out.println("3. Search Equipment");
            System.out.println("4. Update Equipment");
            System.out.println("5. Archive Equipment");
            System.out.println("6. Back to Admin Dashboard");
            System.out.print("Choose an option (1-6): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Equipment name: ");
                    String name = scanner.nextLine().trim();

                    System.out.print("Category: ");
                    String category = scanner.nextLine().trim();

                    System.out.print("Description: ");
                    String description = scanner.nextLine().trim();

                    int quantity = readIntInput("Quantity: ");

                    if (equipmentDao.addEquipment(name, category, description, quantity, admin.user_id())) {
                        userDao.logActivity(
                                admin.user_id(),
                                "ADD_EQUIPMENT",
                                "Admin added equipment: " + name + " | Qty: " + quantity
                        );
                        System.out.println("-> Equipment added successfully.");
                    } else {
                        System.out.println("-> [ERROR] Unable to add equipment.");
                    }
                }
                case "2" -> {
                    System.out.println("\n========== ALL EQUIPMENT ==========");
                    var equipmentList = equipmentDao.getAllEquipment();
                    if (equipmentList.isEmpty()) {
                        System.out.println("-> No Equipment Found");
                    } else {
                        equipmentList.forEach(System.out::println);
                    }
                }
                case "3" -> {
                    System.out.print("Search keyword: ");
                    String keyword = scanner.nextLine().trim();

                    System.out.println("\n========== SEARCH RESULTS ==========");
                    var results = equipmentDao.searchEquipment(keyword);
                    if (results.isEmpty()) {
                        System.out.println("-> No Matching Equipment Found");
                    } else {
                        results.forEach(System.out::println);
                    }
                }
                case "4" -> {
                    int id = readIntInput("Equipment ID: ");

                    System.out.print("New name: ");
                    String name = scanner.nextLine().trim();

                    System.out.print("Category: ");
                    String category = scanner.nextLine().trim();

                    System.out.print("Description: ");
                    String description = scanner.nextLine().trim();

                    int quantity = readIntInput("Total quantity: ");

                    if (equipmentDao.updateEquipment(id, name, category, description, quantity, admin.user_id())) {
                        userDao.logActivity(
                                admin.user_id(),
                                "UPDATE_EQUIPMENT",
                                "Admin updated equipment ID: " + id + " | Name: " + name + " | Qty: " + quantity
                        );
                        System.out.println("-> Equipment updated successfully.");
                    } else {
                        System.out.println("-> [ERROR] Update failed.");
                    }
                }
                case "5" -> {
                    int id = readIntInput("Equipment ID to archive: ");

                    if (equipmentDao.deleteEquipment(id, admin.user_id())) {
                        userDao.logActivity(
                                admin.user_id(),
                                "ARCHIVE_EQUIPMENT",
                                "Admin archived equipment ID: " + id
                        );
                        System.out.println("-> Equipment archived successfully.");
                    } else {
                        System.out.println("-> [ERROR] Cannot archive equipment.");
                    }
                }
                case "6" -> running = false;
                default -> System.out.println("-> [ERROR] Invalid Option.");
            }
        }
    }

    private void viewTransactions() {
        System.out.println("\n========== EQUIPMENT TRANSACTIONS ==========");
        var transactions = transactionDao.getAllTransactions();
        if (transactions.isEmpty()) {
            System.out.println("-> No Transactions Found");
        } else {
            transactions.forEach(System.out::println);
        }
    }
}
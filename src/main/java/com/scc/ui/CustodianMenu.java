package com.scc.ui;

import com.scc.dao.BorrowTransactionDao;
import com.scc.model.BorrowTransaction;
import com.scc.model.user;

import java.util.List;
import java.util.Scanner;

public class CustodianMenu {

    private final Scanner scanner;
    private final BorrowTransactionDao transactionDao = new BorrowTransactionDao();

    public CustodianMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void display(user custodian) {

        boolean running = true;

        while (running) {

            System.out.println("\n===== CUSTODIAN MENU =====");
            System.out.println("1. View Pending Requests");
            System.out.println("2. Approve Request");
            System.out.println("3. Reject Request");
            System.out.println("4. View Approved Requests");
            System.out.println("5. Release Equipment");
            System.out.println("6. View Return Requests");
            System.out.println("7. Confirm Equipment Return");
            System.out.println("8. View All Transactions");
            System.out.println("9. Logout");
            System.out.print("Choice: ");

            switch (scanner.nextLine()) {

                case "1" -> showPending();

                case "2" -> {
                    // Show pending list first so custodian knows what to approve
                    if (!showPending()) {
                        break;
                    }
                    System.out.print("\nEnter Transaction ID to APPROVE: ");
                    int id = readInt();

                    if (transactionDao.approveRequest(id, custodian.user_id()))
                        System.out.println("Request approved.");
                    else
                        System.out.println("Approval failed or stock unavailable.");
                }

                case "3" -> {
                    // Show pending list first so custodian knows what to reject
                    if (!showPending()) {
                        break;
                    }
                    System.out.print("\nEnter Transaction ID to REJECT: ");
                    int id = readInt();

                    System.out.print("Reason for rejection: ");
                    String reason = scanner.nextLine();

                    if (transactionDao.rejectRequest(id, custodian.user_id(), reason))
                        System.out.println("Request rejected.");
                    else
                        System.out.println("Rejection failed.");
                }

                case "4" -> showApproved();

                case "5" -> {
                    // Show approved list first so custodian knows what can be released
                    if (!showApproved()) {
                        break;
                    }
                    System.out.print("\nEnter Transaction ID to RELEASE (hand over to student): ");
                    int id = readInt();

                    if (transactionDao.releaseEquipment(id, custodian.user_id()))
                        System.out.println("Equipment released to student.");
                    else
                        System.out.println("Release failed. Make sure the request is already APPROVED.");
                }

                case "6" ->
                        transactionDao.getReturnRequests()
                                .forEach(System.out::println);

                case "7" -> {
                    // Show return requests table first so custodian sees who requested a return
                    if (!showReturnRequestsTable()) {
                        break;
                    }

                    System.out.print("\nTransaction ID: ");
                    int id = readInt();

                    System.out.print("Returned condition (GOOD/DAMAGED/LOST): ");
                    String condition = scanner.nextLine().toUpperCase();

                    if (!condition.equals("GOOD")
                            && !condition.equals("DAMAGED")
                            && !condition.equals("LOST")) {
                        System.out.println("Invalid condition.");
                        break;
                    }

                    if (transactionDao.confirmReturn(
                            id, custodian.user_id(), condition))
                        System.out.println("Return confirmed.");
                    else
                        System.out.println("Return confirmation failed.");
                }

                case "8" ->
                        transactionDao.getAllTransactions()
                                .forEach(System.out::println);

                case "9" -> running = false;

                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // Helper: show pending requests and return true if there are any
    private boolean showPending() {
        var list = transactionDao.getPendingTransactions();
        if (list.isEmpty()) {
            System.out.println("\nNo pending requests found.");
            return false;
        }
        System.out.println("\n----- PENDING REQUESTS -----");
        list.forEach(System.out::println);
        return true;
    }

    // Helper: show approved requests and return true if there are any
    private boolean showApproved() {
        var list = transactionDao.getApprovedTransactions();
        if (list.isEmpty()) {
            System.out.println("\nNo approved requests found.");
            return false;
        }
        System.out.println("\n----- APPROVED REQUESTS (ready for release) -----");
        list.forEach(System.out::println);
        return true;
    }

    // Helper: show return requests in a formatted table before confirming return
    private boolean showReturnRequestsTable() {
        List<BorrowTransaction> list = transactionDao.getReturnRequests();
        if (list.isEmpty()) {
            System.out.println("\nNo return requests found.");
            return false;
        }

        String line = "+----+----------+------------------+----------------------+------------------+-----------------+";
        System.out.println("\n----- RETURN REQUESTS -----");
        System.out.println(line);
        System.out.printf("| %-2s | %-8s | %-16s | %-20s | %-16s | %-15s |%n",
                "ID", "User ID", "Student", "Equipment", "Expected Return", "Status");
        System.out.println(line);

        for (BorrowTransaction tx : list) {
            String item = tx.transactionItem() != null ? tx.transactionItem() : "N/A";
            System.out.printf("| %-2d | %-8d | %-16s | %-20s | %-16s | %-15s |%n",
                    tx.transactionId(),
                    tx.userId(),
                    truncate(tx.username(), 16),
                    truncate(item, 20),
                    truncate(tx.expectedReturnDate(), 16),
                    truncate(tx.status(), 15));
        }

        System.out.println(line);
        return true;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return maxLen > 1 ? value.substring(0, maxLen - 1) + "~" : value.substring(0, maxLen);
    }

    private int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
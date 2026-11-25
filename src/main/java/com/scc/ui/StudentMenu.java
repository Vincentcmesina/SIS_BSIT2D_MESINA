
package com.scc.ui;

import com.scc.dao.EquipmentDao;
import com.scc.dao.BorrowTransactionDao;
import com.scc.model.Equipment;
import com.scc.model.BorrowTransaction;
import com.scc.model.user;

import java.util.List;
import java.util.Scanner;

public class StudentMenu {

    private final Scanner scanner;
    private final EquipmentDao equipmentDao = new EquipmentDao();
    private final BorrowTransactionDao transactionDao = new BorrowTransactionDao();

    public StudentMenu(Scanner scanner) {
        this.scanner = scanner;
    }

    public void display(user student) {

        boolean running = true;

        while (running) {

            System.out.println("\n===== STUDENT MENU =====");
            System.out.println("1. View Available Equipment");
            System.out.println("2. Search Equipment");
            System.out.println("3. Request Borrowing");
            System.out.println("4. View My Transactions");
            System.out.println("5. Request Return");
            System.out.println("6. Logout");
            System.out.print("Choice: ");

            switch (scanner.nextLine()) {

                case "1" -> equipmentDao.getAvailableEquipment()
                        .forEach(System.out::println);

                case "2" -> {
                    System.out.print("Search: ");
                    equipmentDao.searchEquipment(scanner.nextLine())
                            .forEach(System.out::println);
                }

                case "3" -> requestBorrow(student);

                case "4" -> transactionDao.getStudentTransactions(student.user_id())
                        .forEach(System.out::println);

                case "5" -> requestReturn(student);

                case "6" -> running = false;

                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void requestBorrow(user student) {

        List<Equipment> equipment =
                equipmentDao.getAvailableEquipment();

        if (equipment.isEmpty()) {
            System.out.println("No available equipment.");
            return;
        }

        equipment.forEach(System.out::println);

        System.out.print("Equipment ID: ");
        int id = readInt();

        System.out.print("Quantity: ");
        int quantity = readInt();

        System.out.print("Expected return date (YYYY-MM-DD): ");
        String date = scanner.nextLine();

        if (transactionDao.requestBorrow(
                student.user_id(), id, quantity, date)) {
            System.out.println("Borrowing request submitted.");
        } else {
            System.out.println("Request failed.");
        }
    }


    private void requestReturn(user student) {

        List<BorrowTransaction> borrowed =
                transactionDao.getBorrowedTransactions(student.user_id());

        if (borrowed.isEmpty()) {
            System.out.println("\nYou have no borrowed equipment to return.");
            return;
        }

        System.out.println("\n===== MY BORROWED EQUIPMENT =====");

        System.out.printf("%-15s %-35s %-15s%n",
                "Transaction ID", "Equipment", "Status");

        System.out.println(
                "----------------------------------------------------------------");

        for (BorrowTransaction transaction : borrowed) {

            System.out.printf("%-15d %-35s %-15s%n",
                    transaction.transactionId(),
                    transaction.transactionItem(),
                    transaction.status());
        }

        System.out.println(
                "----------------------------------------------------------------");

        System.out.print("\nEnter Transaction ID to request return: ");

        int id = readInt();

        boolean validId = borrowed.stream()
                .anyMatch(t -> t.transactionId() == id);

        if (!validId) {
            System.out.println("Invalid Transaction ID.");
            return;
        }

        if (transactionDao.requestReturn(id, student.user_id())) {
            System.out.println("Return request submitted successfully.");
        } else {
            System.out.println("Return request failed.");
        }
    }
    private int readInt() {
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Invalid number. Please enter a valid integer: ");
            }
        }
    }
}
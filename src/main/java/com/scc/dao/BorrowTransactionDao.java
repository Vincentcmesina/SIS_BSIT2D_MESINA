
package com.scc.dao;

import com.scc.model.BorrowTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BorrowTransactionDao {

    private final ActivityLogDao logDao = new ActivityLogDao();

    // ==================== REQUEST BORROW ====================

    public boolean requestBorrow(int studentId, int equipmentId,
                                 int quantity, String returnDate) {

        if (quantity <= 0 || returnDate == null || returnDate.isBlank()) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {
                String equipmentName;

                String checkSql = """
                    SELECT equipment_name
                    FROM equipment
                    WHERE equipment_id = ?
                    AND available_quantity >= ?
                    AND status = 'AVAILABLE'
                    """;

                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {

                    ps.setInt(1, equipmentId);
                    ps.setInt(2, quantity);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            conn.rollback();
                            System.out.println("Equipment unavailable.");
                            return false;
                        }

                        equipmentName = rs.getString("equipment_name");
                    }
                }

                String itemInfo = equipmentName +
                        " (ID:" + equipmentId + ") x" + quantity;

                String transactionSql = """
                    INSERT INTO transactions
                    (user_id, expected_return_date, status, transaction_item)
                    VALUES (?, ?, 'PENDING', ?)
                    """;

                int transactionId;

                try (PreparedStatement ps =
                             conn.prepareStatement(transactionSql)) {

                    ps.setInt(1, studentId);
                    ps.setString(2, returnDate);
                    ps.setString(3, itemInfo);

                    ps.executeUpdate();
                }

                // Retrieve the newly generated SQLite transaction ID
                try (PreparedStatement ps =
                             conn.prepareStatement(
                                     "SELECT last_insert_rowid()");
                     ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        throw new SQLException(
                                "Could not retrieve transaction ID.");
                    }

                    transactionId = rs.getInt(1);
                }

                conn.commit();

                logDao.logAction(
                        studentId,
                        "BORROW_REQUESTED",
                        "Submitted borrowing request #" +
                                transactionId + " for " + itemInfo
                );

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Request error: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // ==================== GET EQUIPMENT DETAILS ====================

    private int[] getEquipmentDetails(Connection conn, int transactionId)
            throws SQLException {

        String sql = """
            SELECT transaction_item
            FROM transactions
            WHERE transaction_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, transactionId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    String itemInfo = rs.getString("transaction_item");

                    if (itemInfo == null) {
                        return null;
                    }

                    Pattern pattern =
                            Pattern.compile("\\(ID:(\\d+)\\) x(\\d+)");

                    Matcher matcher = pattern.matcher(itemInfo);

                    if (matcher.find()) {

                        int equipmentId =
                                Integer.parseInt(matcher.group(1));

                        int quantity =
                                Integer.parseInt(matcher.group(2));

                        return new int[]{equipmentId, quantity};
                    }
                }
            }
        }

        return null;
    }

    // ==================== APPROVE REQUEST ====================

    public boolean approveRequest(int transactionId, int custodianId) {

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int[] details =
                        getEquipmentDetails(conn, transactionId);

                if (details == null) {
                    conn.rollback();
                    System.out.println("Equipment details not found.");
                    return false;
                }

                int equipmentId = details[0];
                int quantity = details[1];

                String stockSql = """
                    UPDATE equipment
                    SET available_quantity = available_quantity - ?
                    WHERE equipment_id = ?
                    AND available_quantity >= ?
                    AND status = 'AVAILABLE'
                    """;

                try (PreparedStatement ps = conn.prepareStatement(stockSql)) {

                    ps.setInt(1, quantity);
                    ps.setInt(2, equipmentId);
                    ps.setInt(3, quantity);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        System.out.println("Insufficient available stock.");
                        return false;
                    }
                }

                String updateSql = """
                    UPDATE transactions
                    SET status = 'APPROVED', approved_by = ?
                    WHERE transaction_id = ?
                    AND status = 'PENDING'
                    """;

                try (PreparedStatement ps =
                             conn.prepareStatement(updateSql)) {

                    ps.setInt(1, custodianId);
                    ps.setInt(2, transactionId);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                conn.commit();

                logDao.logAction(
                        custodianId,
                        "REQUEST_APPROVED",
                        "Approved transaction #" + transactionId
                );

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Approve error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== REJECT REQUEST ====================

    public boolean rejectRequest(int transactionId, int custodianId,
                                 String reason) {

        String sql = """
            UPDATE transactions
            SET status = 'REJECTED', remarks = ?
            WHERE transaction_id = ?
            AND status = 'PENDING'
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reason);
            ps.setInt(2, transactionId);

            if (ps.executeUpdate() > 0) {

                logDao.logAction(
                        custodianId,
                        "REQUEST_REJECTED",
                        "Rejected transaction #" + transactionId
                );

                return true;
            }

        } catch (SQLException e) {
            System.out.println("Reject error: " + e.getMessage());
        }

        return false;
    }

    // ==================== RELEASE EQUIPMENT ====================

    public boolean releaseEquipment(int transactionId, int custodianId) {

        String sql = """
            UPDATE transactions
            SET status = 'BORROWED',
                borrow_date = CURRENT_TIMESTAMP
            WHERE transaction_id = ?
            AND status = 'APPROVED'
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, transactionId);

            if (ps.executeUpdate() > 0) {

                logDao.logAction(
                        custodianId,
                        "EQUIPMENT_RELEASED",
                        "Released equipment for transaction #" + transactionId
                );

                return true;
            }

        } catch (SQLException e) {
            System.out.println("Release error: " + e.getMessage());
        }

        return false;
    }

    // ==================== REQUEST RETURN ====================

    public boolean requestReturn(int transactionId, int studentId) {

        String sql = """
            UPDATE transactions
            SET status = 'RETURN_REQUESTED'
            WHERE transaction_id = ?
            AND user_id = ?
            AND status = 'BORROWED'
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, transactionId);
            ps.setInt(2, studentId);

            if (ps.executeUpdate() > 0) {

                logDao.logAction(
                        studentId,
                        "RETURN_REQUESTED",
                        "Requested return for transaction #" + transactionId
                );

                return true;
            }

        } catch (SQLException e) {
            System.out.println("Return request error: " + e.getMessage());
        }

        return false;
    }

    // ==================== CONFIRM RETURN ====================

    public boolean confirmReturn(int transactionId, int custodianId,
                                 String condition) {

        if (condition == null ||
                (!condition.equalsIgnoreCase("GOOD")
                        && !condition.equalsIgnoreCase("DAMAGED")
                        && !condition.equalsIgnoreCase("LOST"))) {
            return false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int[] details =
                        getEquipmentDetails(conn, transactionId);

                if (details == null) {
                    conn.rollback();
                    return false;
                }

                int equipmentId = details[0];
                int quantity = details[1];

                String transactionSql = """
                    UPDATE transactions
                    SET status = 'RETURNED',
                        actual_return_date = CURRENT_TIMESTAMP
                    WHERE transaction_id = ?
                    AND status = 'RETURN_REQUESTED'
                    """;

                try (PreparedStatement ps =
                             conn.prepareStatement(transactionSql)) {

                    ps.setInt(1, transactionId);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // Only GOOD equipment goes back to available stock.

                if (condition.equalsIgnoreCase("GOOD")) {

                    String stockSql = """
                        UPDATE equipment
                        SET available_quantity = available_quantity + ?
                        WHERE equipment_id = ?
                        """;

                    try (PreparedStatement ps =
                                 conn.prepareStatement(stockSql)) {

                        ps.setInt(1, quantity);
                        ps.setInt(2, equipmentId);

                        if (ps.executeUpdate() == 0) {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                conn.commit();

                logDao.logAction(
                        custodianId,
                        "EQUIPMENT_RETURNED",
                        "Confirmed return of transaction #" +
                                transactionId +
                                " with condition: " + condition
                );

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            System.out.println("Confirm return error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== QUERY METHODS ====================

    public List<BorrowTransaction> getAllTransactions() {

        return getTransactions("""
            SELECT t.transaction_id, t.user_id, u.username,
                   t.request_date, t.expected_return_date,
                   t.status, t.transaction_item
            FROM transactions t
            JOIN users u ON t.user_id = u.user_id
            ORDER BY t.transaction_id DESC
            """, -1);
    }

    public List<BorrowTransaction> getStudentTransactions(int studentId) {

        return getTransactions("""
            SELECT t.transaction_id, t.user_id, u.username,
                   t.request_date, t.expected_return_date,
                   t.status, t.transaction_item
            FROM transactions t
            JOIN users u ON t.user_id = u.user_id
            WHERE t.user_id = ?
            ORDER BY t.transaction_id DESC
            """, studentId);
    }

    public List<BorrowTransaction> getPendingTransactions() {

        return getTransactions("""
            SELECT t.transaction_id, t.user_id, u.username,
                   t.request_date, t.expected_return_date,
                   t.status, t.transaction_item
            FROM transactions t
            JOIN users u ON t.user_id = u.user_id
            WHERE t.status = 'PENDING'
            ORDER BY t.transaction_id
            """, -1);
    }

    public List<BorrowTransaction> getApprovedTransactions() {

        return getTransactions("""
            SELECT t.transaction_id, t.user_id, u.username,
                   t.request_date, t.expected_return_date,
                   t.status, t.transaction_item
            FROM transactions t
            JOIN users u ON t.user_id = u.user_id
            WHERE t.status = 'APPROVED'
            ORDER BY t.transaction_id
            """, -1);
    }

    public List<BorrowTransaction> getReturnRequests() {

        return getTransactions("""
            SELECT t.transaction_id, t.user_id, u.username,
                   t.request_date, t.expected_return_date,
                   t.status, t.transaction_item
            FROM transactions t
            JOIN users u ON t.user_id = u.user_id
            WHERE t.status = 'RETURN_REQUESTED'
            ORDER BY t.transaction_id
            """, -1);
    }

    // ==================== COMMON QUERY METHOD ====================

    private List<BorrowTransaction> getTransactions(
            String sql, int studentId) {

        List<BorrowTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (studentId > 0) {
                ps.setInt(1, studentId);
            }

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    String requestDate =
                            rs.getString("request_date");

                    String transactionItem =
                            rs.getString("transaction_item");

                    list.add(new BorrowTransaction(
                            rs.getInt("transaction_id"),
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            requestDate != null ? requestDate : "N/A",
                            rs.getString("expected_return_date"),
                            rs.getString("status"),
                            transactionItem != null
                                    ? transactionItem : "N/A"
                    ));
                }
            }

        } catch (SQLException e) {
            System.out.println("Error loading transactions: " +
                    e.getMessage());
            e.printStackTrace();
        }

        return list;
    }


    public List<BorrowTransaction> getBorrowedTransactions(int studentId) {

        return getTransactions("""
        SELECT t.transaction_id, t.user_id, u.username,
               t.request_date, t.expected_return_date,
               t.status, t.transaction_item
        FROM transactions t
        JOIN users u ON t.user_id = u.user_id
        WHERE t.user_id = ?
        AND t.status = 'BORROWED'
        ORDER BY t.transaction_id DESC
        """, studentId);
    }

}
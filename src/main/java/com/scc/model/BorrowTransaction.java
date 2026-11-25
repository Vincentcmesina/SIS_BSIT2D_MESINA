
package com.scc.model;

public record BorrowTransaction(
        int transactionId,
        int userId,
        String username,
        String requestDate,
        String expectedReturnDate,
        String status,
        String transactionItem
) {

    @Override
    public String toString() {

        return String.format(
                "Transaction #%d | Student: %s (ID: %d) | Equipment: %s | Status: %s | Requested: %s | Expected Return: %s",
                transactionId,
                username,
                userId,
                transactionItem != null ? transactionItem : "N/A",
                status,
                requestDate,
                expectedReturnDate
        );
    }
}
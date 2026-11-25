
package com.scc.model;

public record Equipment(
        int equipmentId,
        String equipmentName,
        String category,
        String description,
        int quantity,
        int availableQuantity,
        String condition,
        String status
) {
    @Override
    public String toString() {
        return String.format(
                "[%d] %s | %s | Total: %d | Available: %d | Condition: %s",
                equipmentId, equipmentName, category,
                quantity, availableQuantity, condition
        );
    }
}
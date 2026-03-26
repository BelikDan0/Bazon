package org.example.bazon.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class OrderItem {
    private String id;
    private String productName;
    private int quantity;
    private ItemStatus status;

    public OrderItem(String productName, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.productName = productName;
        this.quantity = quantity;
        this.status = ItemStatus.PENDING;
    }
}

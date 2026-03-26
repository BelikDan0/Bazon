package org.example.bazon.models;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class Order {
    private String id;
    private OrderStatus status;
    private String assignedPickerId; // Кто взял заказ
    private List<OrderItem> items;

    public Order() {
        this.id = UUID.randomUUID().toString();
        this.status = OrderStatus.NEW;
        this.items = new ArrayList<>();
    }
}

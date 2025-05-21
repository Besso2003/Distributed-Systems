package com.example.customerservice.model;

import org.bson.types.ObjectId;
import java.util.List;

public class Order {
    private ObjectId            id;
    private ObjectId            customerId;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getCustomerId() {
        return customerId;
    }

    public void setCustomerId(ObjectId customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    private List<OrderItem>     items;
    private String              status;          // NEW | CONFIRMED | REJECTED
    private double              total;
    /* getters / setters */
}
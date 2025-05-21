package com.example.sellerservice.model;

import org.bson.types.ObjectId;
import java.util.List;

import org.bson.types.ObjectId;

public class Order {
    private ObjectId id;
    private ObjectId sellerId;
    private ObjectId customerId;
    private List<OrderItem> items;
    private String status;
    private String shippingCompany;
    private double total;

    // Getters and setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public ObjectId getSellerId() {
        return sellerId;
    }

    public void setSellerId(ObjectId sellerId) {
        this.sellerId = sellerId;
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

    public String getShippingCompany() {
        return shippingCompany;
    }

    public void setShippingCompany(String shippingCompany) {
        this.shippingCompany = shippingCompany;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Double getTotal()
    {
        return total;
    }

    public static class OrderItem {
        private ObjectId dishId;
        private int qty;
        private double price;

        // Getters and Setters
        public ObjectId getDishId() {
            return dishId;
        }

        public void setDishId(ObjectId dishId) {
            this.dishId = dishId;
        }

        public int getQty() {
            return qty;
        }

        public void setQty(int qty) {
            this.qty = qty;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

    }
}

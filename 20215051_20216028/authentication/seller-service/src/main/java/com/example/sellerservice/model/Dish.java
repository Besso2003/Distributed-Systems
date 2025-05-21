package com.example.sellerservice.model;

import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import javax.swing.text.Document;

public class Dish {
    private ObjectId id;
    private ObjectId sellerId;
    private String name;
    private double price;
    private int quantity;

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
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public double getPrice() {
        return price;
    }
    public void setPrice(double price) {
        this.price = price;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

}

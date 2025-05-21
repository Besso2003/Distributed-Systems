package com.example.customerservice.model;
import org.bson.types.ObjectId;   // ←  add this line

public class OrderItem {
    private ObjectId dishId;

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

    private int      qty;
    private double   price;
    /* getters / setters */

}
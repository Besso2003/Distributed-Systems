package com.example.customerservice.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public final class MongoCustomerConnection {
    private static final MongoClient CLIENT =
            MongoClients.create("mongodb://localhost:27017");
    private static final MongoDatabase DB = CLIENT.getDatabase("customer");

    private MongoCustomerConnection() {}
    public static MongoDatabase db() { return DB; }
}

package com.example.sellerservice.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoSellerConnection {
    private static final MongoClient client =
            MongoClients.create("mongodb://localhost:27017");
    private static final MongoDatabase db =
            client.getDatabase("seller");

    public static MongoDatabase getDatabase() { return db; }
}

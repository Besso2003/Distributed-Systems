package com.example.authentication;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDatabaseConnection {

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    static {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        database = mongoClient.getDatabase("admin");
    }

    public static MongoDatabase getDatabase() {
        return database;
    }
}


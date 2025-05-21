package com.example.authentication;

import org.bson.types.ObjectId;

public class User {

    private ObjectId id;
    private String username;
    private String email;
    private String password;
    private String role;  // Can be SELLER or ADMIN

    // Getters and setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Convert ObjectId to String for easy handling in APIs
    public String getIdAsString() {
        return id != null ? id.toHexString() : null;
    }

    public void setIdFromString(String idString) {
        if (idString != null && !idString.isEmpty()) {
            this.id = new ObjectId(idString);
        }
    }
}

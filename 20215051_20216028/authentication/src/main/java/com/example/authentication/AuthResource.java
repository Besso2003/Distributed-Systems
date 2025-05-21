package com.example.authentication;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Path("/api")
public class AuthResource {

    @jakarta.ejb.EJB
    private AuthService authService;

    // Test endpoint (To confirm the server is working)
    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response testEndpoint() {
        return Response.status(Response.Status.OK).entity("API is working!").build();
    }

    // Register Admin (for internal use only)
    @POST
    @Path("/register-admin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerAdmin(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank() ||
                user.getEmail() == null || user.getEmail().isBlank() ||
                user.getPassword() == null || user.getPassword().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("All fields are required").build();
        }

        user.setRole("ADMIN");
        boolean success = authService.registerAdmin(user);
        if (success) {
            return Response.status(Response.Status.CREATED)
                    .entity("Admin registered").build();
        } else {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username already exists").build();
        }
    }

    @POST
    @Path("/admin-login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginAdmin(Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing credentials").build();
        }

        boolean authenticated = authService.verifyAdminCredentials(username, password);
        if (authenticated) {
            return Response.status(Response.Status.OK).entity(Map.of("message", "Login successful")).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }
    }


    // Create Seller endpoint (Admin creates a seller account)
    @POST
    @Path("/create-seller")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSeller(User user) {

        /* ---- basic validation ---- */
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username is required").build();
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Email is required").build();
        }

        /* role + auto password will be set in AuthService */
        user.setRole("SELLER");

        /* ---- create locally + push to Seller‑service ---- */
        boolean success = authService.createSeller(user);   // method now sets random pwd

        if (success) {
            /* user.getPassword() was filled by AuthService */
            Map<String, String> body = Map.of(
                    "message",  "Seller created and synced",
                    "username", user.getUsername(),
                    "password", user.getPassword());   // auto‑generated
            return Response.status(Response.Status.CREATED).entity(body).build();
        }

        /* duplicate username or remote push failed */
        return Response.status(Response.Status.CONFLICT)
                .entity("Username exists or remote push failed").build();
    }


    // List all Customers (Admin function)
    @GET
    @Path("/list-customers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCustomers() {
        Iterable<Document> customers = authService.listCustomers();
        return Response.status(Response.Status.OK).entity(customers).build();
    }

    // List all Sellers (Admin function)
    @GET
    @Path("/list-sellers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSellers() {
        Iterable<Document> sellers = authService.listSellers();
        List<Map<String, Object>> sellerList = new ArrayList<>();

        for (Document seller : sellers) {
            Map<String, Object> sellerMap = new HashMap<>();
            sellerMap.put("username", seller.getString("username"));
            sellerMap.put("email", seller.getString("email"));
            sellerMap.put("role", seller.getString("role"));
            sellerList.add(sellerMap);
        }

        return Response.status(Response.Status.OK).entity(sellerList).build();
    }
}

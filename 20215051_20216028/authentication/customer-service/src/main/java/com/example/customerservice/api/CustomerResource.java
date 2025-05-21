package com.example.customerservice.api;

import com.example.customerservice.model.Customer;
import com.example.customerservice.model.Order;
import com.example.customerservice.model.OrderItem;
import com.example.customerservice.service.CustomerService;
import com.example.customerservice.service.OrderProcessingEJB;
import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.bson.types.ObjectId;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/customer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @EJB
    private OrderProcessingEJB orderProcessingEJB;

    private final CustomerService svc = new CustomerService();

    /* ---------- health ---------- */
    @GET
    @Path("/test")
    public String test() {
        return "customer API up";
    }

    /* ---------- auth ---------- */
    @POST
    @Path("/register")
    public Response register(Customer c) {
        return svc.register(c)
                ? Response.status(Response.Status.CREATED).build()
                : Response.status(Response.Status.CONFLICT).entity("Username taken").build();
    }

    @POST
    @Path("/login")
    public Response login(Customer cred) {
        Customer c = svc.login(cred.getUsername(), cred.getPassword());
        return c == null ? Response.status(Response.Status.UNAUTHORIZED).build()
                : Response.ok(Map.of("id", c.getId().toHexString(), "username", c.getUsername())).build();
    }

    /* ---------- orders ---------- */
    @GET
    @Path("/orders")
    public List<Order> list(@QueryParam("customerId") String cid) {
        return svc.listOrders(new ObjectId(cid)); // Convert the customerId string to ObjectId
    }

    /* ---------- Place Order ---------- */
    @POST
    @Path("/place-order")
    public Response place(Order order) {
        // Validate order
        if (order.getItems().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Order must contain at least one item").build();
        }

        // Ensure the customer exists (this is for safety)
        Customer customer = svc.getCustomerById(order.getCustomerId());
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Customer not found").build();
        }

        // Save the order to the database
        boolean orderSaved = svc.saveOrder(order);
        if (!orderSaved) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to save the order").build();
        }

        // Process the order via RabbitMQ to the seller service
        System.out.println(order.getTotal());
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++");
        boolean orderProcessed = svc.placeOrder(order);
        if (!orderProcessed) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to send the order to the seller").build();
        }

        // Respond with a successful status
        return Response.status(Response.Status.CREATED).entity("Order placed successfully").build();
    }


    /* ------------------------------------------------------------------
       ADMIN – list **all** customers
       GET  /customer-service-1.0-SNAPSHOT/api/api/customer/all
       ------------------------------------------------------------------ */

    @GET
    @Path("/all")
    public Response allCustomers() {
        List<Customer> list = svc.listAllCustomers();

        // Keep the payload small & flat
        List<Map<String, Object>> payload = list.stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", c.getId().toHexString());
                    m.put("username", c.getUsername());
                    m.put("email", c.getEmail());
                    m.put("balance", c.getBalance());
                    return m;
                })
                .collect(Collectors.toList());

        return Response.ok(payload).build();
    }


    // Get the balance of the customer
    @GET
    @Path("/get-balance")
    public Response getBalance(@QueryParam("customerId") String cid) {
        if (cid == null || cid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Customer ID is required").build();
        }
        try {
            ObjectId customerId = new ObjectId(cid);
            Customer customer = svc.getCustomerById(customerId);
            if (customer == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Customer not found").build();
            }
            // Return only the balance in JSON format
            Map<String, Object> result = Map.of("balance", customer.getBalance());
            return Response.ok(result).build();
        } catch (IllegalArgumentException e) {
            // This exception occurs if the ObjectId string is invalid
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid customer ID format").build();
        }
    }

    // Reduce the balance of the customer
    @POST
    @Path("/reduce-balance")
    public Response reduceBalance(
            @QueryParam("customerId") String cid,
            @QueryParam("amount") Double amount) {
        
        if (cid == null || cid.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity("Customer ID is required")
                         .build();
        }
        
        if (amount == null || amount <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity("Amount must be greater than 0")
                         .build();
        }

        try {
            ObjectId customerId = new ObjectId(cid);
            Customer customer = svc.getCustomerById(customerId);
            
            if (customer == null) {
                return Response.status(Response.Status.NOT_FOUND)
                             .entity("Customer not found")
                             .build();
            }

            if (customer.getBalance() < amount) {
                return Response.status(Response.Status.BAD_REQUEST)
                             .entity("Insufficient balance")
                             .build();
            }

            // Reduce the balance
            customer.setBalance(customer.getBalance() - amount);
            svc.updateCustomer(customer);

            // Return updated balance
            Map<String, Object> result = Map.of(
                "newBalance", customer.getBalance(),
                "amountReduced", amount
            );
            
            return Response.ok(result).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity("Invalid customer ID format")
                         .build();
        }
    }

    @GET
    @Path("/orders/history")
    public Response getOrderHistory(@QueryParam("customerId") String customerIdStr) {
        try {
            ObjectId customerId = new ObjectId(customerIdStr);
            List<Order> orders = svc.listOrders(customerId);

            List<Map<String, Object>> response = orders.stream().map(o -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id", o.getId().toHexString());
                m.put("status", o.getStatus() != null ? o.getStatus() : "PENDING");
                return m;
            }).collect(Collectors.toList());

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid customer ID format").build();
        }
    }
}
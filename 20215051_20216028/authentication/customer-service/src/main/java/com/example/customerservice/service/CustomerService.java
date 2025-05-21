package com.example.customerservice.service;

import com.example.customerservice.config.LogProducer;
import com.example.customerservice.config.MongoCustomerConnection;
import com.example.customerservice.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.rabbitmq.client.*;
import jakarta.ejb.Stateless;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.*;

@Stateless
public class CustomerService {
    private static final String ORDER_QUEUE = "order_queue";

    /* ------------ collections ------------ */
    private final MongoCollection<Document> custCol =
            MongoCustomerConnection.db().getCollection("customers");
    private final MongoCollection<Document> orderCol =
            MongoCustomerConnection.db().getCollection("orders");

    /* ------------ auth ------------ */
    public boolean register(Customer c) {
        // Check if the username already exists
        if (custCol.find(Filters.eq("username", c.getUsername())).first() != null)
            return false;

        // Add a balance field with a value of 500
        c.setBalance(500.0);

        // Insert the customer document with the balance field
        custCol.insertOne(toDoc(c));

        return true;
    }


    public Customer login(String u, String p) {
        Document d = custCol.find(Filters.eq("username", u)).first();
        return (d!=null && p.equals(d.getString("password"))) ? fromDoc(d) : null;
    }

    /* ------------ orders ------------ */
    public List<Order> listOrders(ObjectId customerId) {
        return docsToList(orderCol.find(Filters.eq("customerId", customerId)), Order.class);
    }

    public boolean placeOrder(Order order) {
        double total = 0.0;
        for (OrderItem item : order.getItems()) {
            total += item.getPrice() * item.getQty();  // calculate the total based on price and quantity
        }
        order.setTotal(total);  // Set the total on the order object
        System.out.println(total);
        System.out.println(order);

        sendOrderToSeller(order);

        try {
            LogProducer.sendLog("Order", "Info", "Order " + " placed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
      return true;
    }

    private final static String REQUEST_QUEUE_NAME = "request-queue";
    private final static String EXCHANGE_NAME = "";
    private final static String RESPONSE_QUEUE_NAME = "response-queue";  // The reply queue

    // send order to the seller via RabbitMQ
    private boolean sendOrderToSeller(Order order) {
        // Create a connection factory
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             final Channel channel = connection.createChannel()) {

            // Declare the request queue and response queue
            channel.queueDeclare(REQUEST_QUEUE_NAME, false, false, false, null);
            channel.queueDeclare(RESPONSE_QUEUE_NAME, false, false, false, null);

            // Create a unique correlation ID for the request
            String correlationId = UUID.randomUUID().toString();
            System.out.println("Sending Request with Correlation ID: " + correlationId);

            // Set up the properties with the correlation ID and reply-to queue
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .replyTo(RESPONSE_QUEUE_NAME)  // Specify the response queue here
                    .correlationId(correlationId)  // Set correlation ID
                    .build();

            // Prompt user for payment amount
            java.util.Scanner scanner = new java.util.Scanner(System.in);


            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new ObjectIdModule());

            // order as a message sent to the seller.
            String message = objectMapper.writeValueAsString(order);


            channel.basicPublish(EXCHANGE_NAME, REQUEST_QUEUE_NAME, props, message.getBytes());
            System.out.println("Request sent. Waiting for reply...");

            // Wait for the reply
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                if (delivery.getProperties().getCorrelationId().equals(correlationId)) {
                    String response = new String(delivery.getBody(), "UTF-8");
                    System.out.println("Received reply: " + response);
                    // Extract status from response string
                    String status = response.toLowerCase().contains("failed") ? "REJECTED" : "CONFIRMED";
                    // Update order status in DB
                    order.setStatus(status);
                    orderCol.updateOne(
                            Filters.eq("_id", order.getId()),
                            new Document("$set", new Document("status", status))
                    );
                }
            };

            // Start consuming the reply from the response queue
            channel.basicConsume(RESPONSE_QUEUE_NAME, true, deliverCallback, consumerTag -> {});

            Thread.sleep(100);

        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }



    /* ------------ tiny REST calls to seller‑service ------------ */
    private static final String SELLER_API  = "http://localhost:8180/seller-service-1.0-SNAPSHOT/api/api";

    /* ------------ mapping helpers ------------ */
    private Document toDoc(Customer c){ return new Document()
            .append("username",c.getUsername()).append("password",c.getPassword()).append("email",c.getEmail()).append("balance", c.getBalance()); }

    private Customer fromDoc(Document d) {
        Customer c = new Customer();
        c.setId(d.getObjectId("_id"));
        c.setUsername(d.getString("username"));
        c.setPassword(d.getString("password"));
        c.setEmail(d.getString("email"));

        // Fix: read the balance field from MongoDB document
        Double balance = d.getDouble("balance");
        if (balance == null) {
            balance = 0.0;  // default value if missing
        }
        c.setBalance(balance);

        return c;
    }

    private Document toDoc(Order o){
        return new Document("customerId",o.getCustomerId())
                .append("items", o.getItems().stream().map(it->new Document()
                        .append("dishId",it.getDishId())
                        .append("qty",it.getQty())
                        .append("price",it.getPrice())).collect(Collectors.toList()))
                .append("status",o.getStatus()).append("total",o.getTotal());
    }

    /* generic converter (simple) */
    private <T> List<T> docsToList(Iterable<Document> it, Class<T> c){
        return StreamSupport.stream(it.spliterator(),false)
                .map(d->c==Order.class? (T)docToOrder(d): (T)fromDoc(d))
                .collect(Collectors.toList());
    }
    private Order docToOrder(Document d){
        Order o=new Order(); o.setId(d.getObjectId("_id"));
        o.setCustomerId(d.getObjectId("customerId"));
        o.setStatus(d.getString("status")); o.setTotal(d.getDouble("total"));
        // items skipped for brevity
        return o;
    }


    // Method to save order to database
    public boolean saveOrder(Order order) {
        try {
            orderCol.insertOne(toDoc(order));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Method to get customer by ID
    public Customer getCustomerById(ObjectId customerId) {
        Document doc = custCol.find(Filters.eq("_id", customerId)).first();
        if (doc == null) {
            return null;
        }
        return fromDoc(doc);
    }

    public List<Customer> listAllCustomers() {
        return docsToList(custCol.find(), Customer.class);
    }

    // Method to update customer in the database
    // used to update the balance of the customer
    public boolean updateCustomer(Customer customer) {
        if (customer.getId() == null) return false;
        Document updateDoc = toDoc(customer);
        custCol.updateOne(Filters.eq("_id", customer.getId()), new Document("$set", updateDoc));
        return true;
    }
}

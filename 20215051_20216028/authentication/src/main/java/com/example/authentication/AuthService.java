package com.example.authentication;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.*;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.bson.Document;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.concurrent.TimeoutException;

@Singleton
@Startup
public class AuthService {

    private MongoCollection<Document> userCollection;
    private static boolean consumerRunning = false;

    // Required no-arg constructor for EJB
    public AuthService() {
        MongoDatabase db = MongoDatabaseConnection.getDatabase();
        this.userCollection = db.getCollection("users");
    }

    // Register Admin
    public boolean registerAdmin(User user) {
        if (userCollection.find(new Document("username", user.getUsername())).first() != null)
            return false;

        userCollection.insertOne(new Document()
                .append("username", user.getUsername())
                .append("email", user.getEmail())
                .append("password", user.getPassword())
                .append("role", "ADMIN"));
        return true;
    }

    // Login Admin
    public boolean verifyAdminCredentials(String username, String password) {
        Document admin = userCollection.find(new Document("username", username).append("role", "ADMIN")).first();
        if (admin != null && admin.getString("password").equals(password)) {
            if (!consumerRunning) {
                new Thread(() -> {
                    try {
                        listenToEvents();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                consumerRunning = true;
                System.out.println("✅ RabbitMQ consumer started after seller login.");
            }
            startAdminLogConsumer();
            return true;
        }
        return false;
    }

    // Generate random Password For new Sellers
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    public Iterable<Document> listCustomers() {
        return userCollection.find(new Document("role", "CUSTOMER"));
    }

    public Iterable<Document> listSellers() {
        return userCollection.find(new Document("role", "SELLER"));
    }

    private static final String SELLER_API_URL =
            "http://localhost:8180/seller-service-1.0-SNAPSHOT/api/api/seller/register-from-admin";

    // Create New Seller
    public boolean createSeller(User user) {
        if (userCollection.find(new Document("username", user.getUsername())).first() != null)
            return false;

        user.setPassword(generateRandomPassword());

        userCollection.insertOne(new Document()
                .append("username", user.getUsername())
                .append("email", user.getEmail())
                .append("password", user.getPassword())
                .append("role", "SELLER"));

        return pushToSellerService(user);
    }

    // Send New Seller to The Seller Using API
    private boolean pushToSellerService(User u) {
        try {
            String body = String.format(
                    "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}",
                    u.getUsername(), u.getEmail(), u.getPassword());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(SELLER_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> res = HttpClient.newHttpClient()
                    .send(req, HttpResponse.BodyHandlers.ofString());

            return res.statusCode() / 100 == 2;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static final String PAYMENTS_EXCHANGE  = "payments-exchange";
    private static final String PAYMENT_FAILED_KEY = "PaymentFailed";
    private static final String ADMIN_QUEUE        = "admin-alerts";

    // Bonus
    // Get Failure orders
    public void listenToEvents() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection conn = factory.newConnection();
             Channel ch = conn.createChannel()) {

            ch.exchangeDeclare(PAYMENTS_EXCHANGE, BuiltinExchangeType.DIRECT, true);
            ch.queueDeclare(ADMIN_QUEUE, true, false, false, null);
            ch.queueBind(ADMIN_QUEUE, PAYMENTS_EXCHANGE, PAYMENT_FAILED_KEY);

            System.out.println("AdminAlertService waiting for PaymentFailed events…");

            DeliverCallback cb = (tag, delivery) -> {
                String body = new String(delivery.getBody(), "UTF-8");
                System.out.println("PAYMENT FAILURE ALERT: " + body);
            };

            ch.basicConsume(ADMIN_QUEUE, true, cb, tag -> {});
            while (true) Thread.sleep(1000);

        } catch (IOException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String LOG_EXCHANGE_NAME = "log";
    private static volatile boolean adminConsumerRunning = false;

    // Bonus
    // Logs
    public void startAdminLogConsumer() {
        if (adminConsumerRunning) return;

        new Thread(() -> {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setHost("localhost");

                Connection connection = factory.newConnection();
                Channel channel = connection.createChannel();

                channel.exchangeDeclare(LOG_EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
                String queueName = channel.queueDeclare().getQueue();

                channel.queueBind(queueName, LOG_EXCHANGE_NAME, "Order.*");
                channel.queueBind(queueName, LOG_EXCHANGE_NAME, "*.Error");
                channel.queueBind(queueName, LOG_EXCHANGE_NAME, "*.Warning");

                System.out.println(" [*] Waiting for logs from Order service. To exit press CTRL+C");

                DeliverCallback callback = (consumerTag, delivery) -> {
                    String routingKey = delivery.getEnvelope().getRoutingKey();
                    String message = new String(delivery.getBody(), "UTF-8");
                    System.out.println(" [ORDER LOG] " + routingKey + ": '" + message + "'");
                };

                channel.basicConsume(queueName, true, callback, consumerTag -> {});
                adminConsumerRunning = true;

                while (true) Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

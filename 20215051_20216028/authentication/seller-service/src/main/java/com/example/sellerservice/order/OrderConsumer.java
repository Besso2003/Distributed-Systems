package com.example.sellerservice.order;

import com.example.sellerservice.config.MongoSellerConnection;
import com.example.sellerservice.model.Dish;
import com.example.sellerservice.model.Order;
import com.example.sellerservice.model.Seller;
import com.example.sellerservice.service.SellerService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.bson.Document;
import org.bson.types.ObjectId;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class OrderConsumer {
    private static final String REQUEST_QUEUE_NAME   = "request-queue";
    private static final String EXCHANGE_NAME        = "";

    //  payments exchange constants
    private static final String PAYMENTS_EXCHANGE    = "payments-exchange";
    private static final String PAYMENT_FAILED_KEY   = "PaymentFailed";        // routing key

    private static final String ORDER_QUEUE = "order_queue";
    private static final String CONFIRMATION_QUEUE  = "confirmation_exchange";
    private static final double MINIMUM_CHARGE = 10.0;

    // Mongo collection reference
    private final MongoCollection<Document> dishCol =
            MongoSellerConnection.getDatabase().getCollection("dishes");

    public static double addBalanceFromUrl(double currentBalance, String customerId) {
        double updatedBalance = currentBalance;
        try {
            String urlString = "http://localhost:8280/customer-service-1.0-SNAPSHOT/api/api/customer/get-balance?customerId=" + customerId;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }

            conn.disconnect();

            JSONObject jsonObj = new JSONObject(sb.toString());
            double fetchedBalance = jsonObj.getDouble("balance");
             updatedBalance= fetchedBalance;


        } catch (Exception e) {
            e.printStackTrace();
        }

        return updatedBalance;
    }

    public void startConsumer() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection conn = factory.newConnection();
             Channel ch = conn.createChannel()) {

            ch.queueDeclare(REQUEST_QUEUE_NAME, false, false, false, null);
            ch.exchangeDeclare(PAYMENTS_EXCHANGE, BuiltinExchangeType.DIRECT, true);

            System.out.println("Server is waiting for requests...");

            DeliverCallback cb = (consumerTag, delivery) -> {
                String corrId = delivery.getProperties().getCorrelationId();
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received Request: " + message);

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                Order order = objectMapper.readValue(message, Order.class);

                SellerService sellerService = new SellerService();

                // 1: Validate stock using service call
                boolean stockOk = true;
                for (Order.OrderItem item : order.getItems()) {
                    ObjectId dishId = item.getDishId();
                    int qty = item.getQty();

                    if (!sellerService.dishAvailable(dishId, qty)) {
                        stockOk = false;
                        System.out.println("Dish " + dishId + " is unavailable");
                        break;
                    }
                }

                if (!stockOk) {
                    System.out.println("Order rejected due to insufficient stock.");
                    order.setStatus("REJECTED");
                    sendReply(ch, delivery, corrId, "Stock check failed: insufficient stock for one or more items.");
                    return;
                }

                //  2: Check balance
                // Customer ID
                String customerId = order.getCustomerId().toHexString();
                double balance = getCustomerBalance(customerId);

                System.out.println("Customer Id "+customerId);

                if (balance < MINIMUM_CHARGE) {
                    System.out.println("Order rejected due to insufficient balance.");
                    order.setStatus("REJECTED");
                    publishPaymentFailureEvent(ch, corrId, MINIMUM_CHARGE, "Insufficient funds");
                    sendReply(ch, delivery, corrId, "Payment failed: insufficient balance.");
                    return;
                }

                //  3: Deduct balance
                boolean deducted = reduceCustomerBalance(customerId, MINIMUM_CHARGE);
                if (!deducted) {
                    System.out.println("Balance deduction failed.");
                    order.setStatus("REJECTED");
                    publishPaymentFailureEvent(ch, corrId, MINIMUM_CHARGE, "Failed to deduct balance");
                    sendReply(ch, delivery, corrId, "Payment failed: unable to deduct balance.");
                    return;
                }

                // Decrement Stock
                boolean stockUpdated = true;
                for (Order.OrderItem item : order.getItems()) {
                    ObjectId dishId = item.getDishId();
                    int qty = item.getQty();
                    if (!sellerService.decrementDishStock(dishId, qty)) {
                        System.out.println("Dish Id: " + dishId + " Decrement Stock Failed");
                        stockUpdated = false;
                        break;
                    }
                }
                if (!stockUpdated) {
                    System.out.println("Failed to update stock after order confirmation.");
                    order.setStatus("REJECTED");
                    publishPaymentFailureEvent(ch, corrId, MINIMUM_CHARGE, "Stock update failed");
                    sendReply(ch, delivery, corrId, "Order failed: unable to update stock.");
                    return;
                }

                //  4: Save order
                MongoCollection<Document> orderCol = MongoSellerConnection.getDatabase().getCollection("orders");
                Document orderDoc = new Document()
                        .append("customerId", order.getCustomerId())
                        .append("items", order.getItems().stream().map(it -> new Document()
                                .append("dishId", it.getDishId())
                                .append("qty", it.getQty())
                                .append("price", it.getPrice())).collect(Collectors.toList()))
                        .append("total", order.getTotal())
                        .append("status", "CONFIRMED");

                orderCol.insertOne(orderDoc);
                order.setStatus("CONFIRMED");
                System.out.println("Order confirmed and saved.");

                // Reply to customer
                sendReply(ch, delivery, corrId, "Order CONFIRMED and payment processed.");
            };

            ch.basicConsume(REQUEST_QUEUE_NAME, true, cb, tag -> {});
            System.out.println("Server running…");
            while (true) Thread.sleep(1000);

        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendReply(Channel ch, Delivery delivery, String corrId, String replyMessage) throws IOException {
        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                .correlationId(corrId)
                .build();

        ch.basicPublish(EXCHANGE_NAME,
                delivery.getProperties().getReplyTo(),
                replyProps,
                replyMessage.getBytes());
    }

    private double getCustomerBalance(String customerId) {
        try {
            String urlStr = "http://localhost:8280/customer-service-1.0-SNAPSHOT/api/api/customer/get-balance?customerId=" + customerId;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);

            JSONObject json = new JSONObject(sb.toString());
            return json.getDouble("balance");

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private boolean reduceCustomerBalance(String customerId, double amount) {
        try {
            String urlStr = String.format("http://localhost:8280/customer-service-1.0-SNAPSHOT/api/api/customer/reduce-balance?customerId=%s&amount=%.2f",
                    customerId, amount);
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void publishPaymentFailureEvent(Channel ch, String corrId, double amount, String reason) {
        try {
            String failPayload = String.format(
                    "paymentId=%s | amount=%.2f | reason=%s", corrId, amount, reason);

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)
                    .build();

            ch.basicPublish(PAYMENTS_EXCHANGE, PAYMENT_FAILED_KEY, props, failPayload.getBytes());
            System.out.println("PaymentFailed event published for " + corrId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    private Dish getDish(ObjectId dishId) {
        Document doc = dishCol.find(Filters.eq("_id", dishId)).first();
        if (doc == null) return null;

        Dish dish = new Dish();
        dish.setId(doc.getObjectId("_id"));
        dish.setName(doc.getString("name"));
        dish.setQuantity(doc.getInteger("quantity"));
        dish.setPrice(doc.getDouble("price"));
        return dish;
    }
}
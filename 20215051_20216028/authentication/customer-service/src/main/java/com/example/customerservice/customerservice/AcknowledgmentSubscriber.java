package com.example.customerservice.customerservice;

import com.example.customerservice.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.nio.charset.StandardCharsets;

public class AcknowledgmentSubscriber {

    private static final String CONFIRMATION_QUEUE = "confirmation_queue";

    public static void start() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.queueDeclare(CONFIRMATION_QUEUE, true, false, false, null);

            System.out.println(" [*] Waiting for confirmations in confirmation_queue...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
                Order order = new ObjectMapper().readValue(json, Order.class);
                System.out.println(" [✔] Order for customer " + order.getCustomerId() + " → " + order.getStatus());
            };

            channel.basicConsume(CONFIRMATION_QUEUE, true, deliverCallback, consumerTag -> {});

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

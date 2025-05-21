package com.example.sellerservice.order;

import com.example.sellerservice.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ejb.Stateless;
import com.rabbitmq.client.*;

@Stateless
public class OrderPublisherEJB {

    private static final String EXCHANGE_NAME = "confirmation";

//    public void sendConfirmationToCustomer(Order order) {
//        publishToRabbitMQ(order, "Order confirmed and processed");
//    }
//
//    public void publishCancellationToCustomer(Order order) {
//        publishToRabbitMQ(order, "Order rejected or cancelled");
//    }

//    private void publishToRabbitMQ(Order order, String logMessage) {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//
//        try (Connection connection = factory.newConnection();
//             Channel channel = connection.createChannel()) {
//
//            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true);
//
//            ObjectMapper mapper = new ObjectMapper();
//            String message = mapper.writeValueAsString(order);
//
//            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
//
//            System.out.println(" [x] Sent to confirmation exchange: " + logMessage);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

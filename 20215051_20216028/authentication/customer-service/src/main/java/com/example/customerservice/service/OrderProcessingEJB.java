package com.example.customerservice.service;


import com.example.customerservice.model.Order;
//import com.example.customerservice.model.OrderItem;
import com.rabbitmq.client.*;
import jakarta.ejb.Stateless;
//import jakarta.inject.Inject;
//import org.bson.types.ObjectId;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;


@Stateless
public class OrderProcessingEJB {

    private static final String ORDER_QUEUE = "order_queue";

//    public boolean placeOrder(Order order) {
//        ConnectionFactory factory = new ConnectionFactory();
//        factory.setHost("localhost");
//
//        try (Connection connection = factory.newConnection();
//             Channel channel = connection.createChannel()) {
//
//            channel.queueDeclare(ORDER_QUEUE, true, false, false, null);
//
//            ObjectMapper mapper = new ObjectMapper();
//            String orderJson = mapper.writeValueAsString(order);
//
//            channel.basicPublish("", ORDER_QUEUE, null, orderJson.getBytes());
//            System.out.println("✅ Order sent to order_queue: " + order.getCustomerId());
//            return true;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
}
package com.example.customerservice.config;

import com.rabbitmq.client.*;

public class LogProducer {
    private static final String EXCHANGE_NAME = "log";

    public static void sendLog(String service, String severity, String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            String routingKey = service + "." + severity;
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes("UTF-8"));

            System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");
        }
    }

}


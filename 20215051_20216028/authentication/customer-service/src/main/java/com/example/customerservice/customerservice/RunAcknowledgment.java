package com.example.customerservice.customerservice;


public class RunAcknowledgment {
    public static void main(String[] args) {
        AcknowledgmentSubscriber.start();
        // Keep the app alive
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
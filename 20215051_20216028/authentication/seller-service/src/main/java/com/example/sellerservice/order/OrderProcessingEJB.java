package com.example.sellerservice.order;

import com.example.sellerservice.model.Order;
import com.example.sellerservice.service.SellerService;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class OrderProcessingEJB {

    @Inject
    private SellerService sellerService;

    @Inject
    private OrderProcessingEJB processor;

    @Inject
    private OrderPublisherEJB publisher;

//    public void handleOrder(Order order) {
//        Order processed = processor.processOrder(order);
//
//        // Save order (you've already done that in SellerService)
//        sellerService.saveOrder(processed);
//
//        if ("CONFIRMED".equals(processed.getStatus())) {
//            publisher.sendConfirmationToCustomer(processed);
//        } else {
//            publisher.publishCancellationToCustomer(processed);
//        }
//    }

}


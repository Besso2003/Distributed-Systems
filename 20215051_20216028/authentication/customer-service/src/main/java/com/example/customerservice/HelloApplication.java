package com.example.customerservice;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;
@ApplicationPath("/api")
public class HelloApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new java.util.HashSet<>();
        // Register CORS filter
        classes.add(com.example.customerservice.config.CORSFilter.class);

        classes.add(com.example.customerservice.api.CustomerResource.class);
        return classes;
    }

// http://localhost:8280/customer-service-1.0-SNAPSHOT/customer/index.html
}
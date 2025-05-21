package com.example.sellerservice.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class CORSFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext req,
                       ContainerResponseContext res) throws IOException {

        // Allow your customer portal (8280) to talk to this service
        res.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:8280");

        // let browsers cache the pre‑flight for an hour (optional)
        res.getHeaders().add("Access-Control-Max-Age", "3600");

        // allow the usual headers & methods
        res.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
        res.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        res.getHeaders().add("Access-Control-Allow-Credentials", "true");
    }
}


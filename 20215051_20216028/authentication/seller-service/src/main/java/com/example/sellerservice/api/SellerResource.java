package com.example.sellerservice.api;

import com.example.sellerservice.model.Dish;
import com.example.sellerservice.model.Order;
import com.example.sellerservice.model.Seller;
import com.example.sellerservice.service.SellerService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Path("/api/seller")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SellerResource {

    private final SellerService svc = new SellerService();

    /* ---------- Test If seller Is Up ---------- */
    @GET @Path("/test") @Produces(MediaType.TEXT_PLAIN)
    public String test() { return "seller API is working!"; }

    /* ---------- registration pushed from Admin ---------- */
    @POST @Path("/register-from-admin")
    public Response registerFromAdmin(Seller s){
        return svc.registerSeller(s)
                ? Response.status(Response.Status.CREATED).entity("Seller stored").build()
                : Response.status(Response.Status.CONFLICT).entity("Username exists").build();
    }

    /* ---------- login ---------- */
    @POST @Path("/login")
    public Response login(Seller creds){
        Seller s = svc.login(creds.getUsername(), creds.getPassword());
        if (s == null)
            return Response.status(Response.Status.UNAUTHORIZED).entity("Bad credentials").build();

        /* return a flat JSON object id is a plain String */
        return Response.ok(Map.of(
                "id",       s.getId().toHexString(),
                "username", s.getUsername(),
                "email",    s.getEmail()
        )).build();
    }

    /* ---------- Get dishes For A Seller ---------- */
    @GET @Path("/dishes")
    public Response listDishes(@QueryParam("sellerId") String sid){
        ObjectId sellerId = new ObjectId(sid);
        List<Dish> dishes = svc.listDishes(sellerId);
        List<Map<String, Object>> mapped = dishes.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId().toHexString());
            map.put("name", d.getName());
            map.put("price", d.getPrice());
            map.put("quantity", d.getQuantity());
            return map;
        }).collect(Collectors.toList());

        return Response.ok(mapped).build();
    }

    @POST @Path("/dishes")
    public Response addDish(Dish d){
        svc.addDish(d);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT @Path("/dishes/{id}")
    public Response updateDish(@PathParam("id") String id, Dish d){
        svc.updateDish(new ObjectId(id), d);
        return Response.ok().build();
    }

    /* ---------- Get Past Orders---------- */
    @GET
    @Path("/orders/history")
    public Response pastOrders(@QueryParam("sellerId") String sid){
        ObjectId sellerId = new ObjectId(sid);
        List<Order> orders = svc.listPastOrders(sellerId);
        List<Map<String, Object>> mappedOrders = orders.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId().toHexString());
            m.put("status", o.getStatus());
            m.put("shippingCompany", o.getShippingCompany());
            return m;
        }).collect(Collectors.toList());

        return Response.ok(mappedOrders).build();
    }

    /* ---------- Update dish stock ---------- */
    @PUT @Path("/dishes/{id}/decrement")
    public Response decrementDishStock(@PathParam("id") String id, @QueryParam("qty") int qty) {
        boolean success = svc.decrementDishStock(new ObjectId(id), qty);
        if (success) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Insufficient stock").build();
    }

    @PUT
    @Path("/orders/{id}")
    public Response processOrder(@PathParam("id") String orderId) {
        Order order = svc.getOrderById(new ObjectId(orderId));
        if (order != null && "CONFIRMED".equals(order.getStatus())) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.BAD_REQUEST).entity("Invalid order status").build();
    }

    @POST
    @Path("/process-order")
    public void processOrder(Order order) {
        boolean isStockAvailable = true;
        double total = 0;

        // Check stock availability
        for (Order.OrderItem item : order.getItems()) {
            boolean stockAvailable = svc.decrementDishStock(item.getDishId(), item.getQty());
            if (!stockAvailable) {
                isStockAvailable = false;
                break;
            }
            total += item.getPrice() * item.getQty();
        }

        if (isStockAvailable && total >= 10) {
            order.setStatus("COMPLETED");
            confirmOrderToCustomer(order);
        } else {
            order.setStatus("REJECTED");
            cancelOrderToCustomer(order);
        }
    }

    private void confirmOrderToCustomer(Order order) {
        // Send order confirmation to customer (via RabbitMQ)
        System.out.println("Order confirmed for customer: " + order.getCustomerId());
    }

    private void cancelOrderToCustomer(Order order) {
        // Send order cancellation to customer (via RabbitMQ)
        System.out.println("Order canceled for customer: " + order.getCustomerId());
    }



    /* ---------- Fetch all dishes for all sellers ---------- */
    @GET
    @Path("/dishes/all")
    public Response getAllDishes() {
        // Fetch all dishes from the database (all sellers)
        List<Dish> allDishes = svc.listAllDishes();

        // Map the dish data for response
        List<Map<String, Object>> mappedDishes = allDishes.stream().map(d -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", d.getId().toHexString());
            map.put("name", d.getName());
            map.put("price", d.getPrice());
            map.put("quantity", d.getQuantity());
            map.put("sellerId", d.getSellerId().toHexString());
            return map;
        }).collect(Collectors.toList());

        return Response.ok(mappedDishes).build();
    }


    @GET
    @Path("/dish/available")
    public Response dishAvailable(
            @QueryParam("dishId") String dishIdStr,
            @QueryParam("qty") int qty) {

        ObjectId dishId;
        try {
            dishId = new ObjectId(dishIdStr);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid dishId format").build();
        }

        boolean available = svc.dishAvailable(dishId, qty);

        Map<String, Object> response = Map.of(
                "dishId", dishIdStr,
                "requestedQty", qty,
                "available", available
        );

        return Response.ok(response).build();
    }

    /* ---------- Fetch all Orders for a seller ---------- */
    @GET
    @Path("/orders/all")
    public Response getAllOrdersBySeller(@QueryParam("sellerId") String sellerIdStr) {
        try {
            ObjectId sellerId = new ObjectId(sellerIdStr);
            List<Order> orders = svc.getOrdersBySeller(sellerId);

            List<Map<String, Object>> response = orders.stream().map(order -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", order.getId().toHexString());
                map.put("status", order.getStatus());
                map.put("total", order.getTotal());
                map.put("customerId", order.getCustomerId().toHexString());
                map.put("items", order.getItems().stream().map(item -> Map.of(
                        "dishId", item.getDishId().toHexString(),
                        "qty", item.getQty(),
                        "price", item.getPrice()
                )).collect(Collectors.toList()));
                return map;
            }).collect(Collectors.toList());

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid sellerId format").build();
        }
    }
}
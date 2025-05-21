package com.example.sellerservice.service;

import com.example.sellerservice.config.LogProducer;
import com.example.sellerservice.config.MongoSellerConnection;
import com.example.sellerservice.model.Dish;
import com.example.sellerservice.model.Order;
import com.example.sellerservice.model.Seller;
import com.example.sellerservice.order.OrderConsumer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import jakarta.ejb.Stateless;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Stateless
public class SellerService {

    private final MongoCollection<Document> sellerCol =
            MongoSellerConnection.getDatabase().getCollection("sellers");
    private final MongoCollection<Document> dishCol =
            MongoSellerConnection.getDatabase().getCollection("dishes");
    private final MongoCollection<Document> orderCol =
            MongoSellerConnection.getDatabase().getCollection("orders");

    // Register
    public boolean registerSeller(Seller s) {
        if (sellerCol.find(Filters.eq("username", s.getUsername())).first() != null)
            return false;
        sellerCol.insertOne(toDoc(s));
        return true;
    }

    // queue that is used for getting the orders
    private static final OrderConsumer orderConsumer = new OrderConsumer();
    private static boolean consumerRunning = false;

    // Login then initialize the queue
    public Seller login(String username, String pwd) {
        Document doc = sellerCol.find(Filters.eq("username", username)).first();
        if (doc != null && pwd.equals(doc.getString("password"))) {

            // Start the consumer in a background thread if not already running
            if (!consumerRunning) {
                new Thread(() -> {
                    try {
                        orderConsumer.startConsumer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                consumerRunning = true;
                System.out.println("✅ RabbitMQ consumer started after seller login.");
            }
            return fromDocSeller(doc);
        } else {
            // Log failed login attempt
            try {
                LogProducer.sendLog("Seller", "Error", "Failed login attempt for user: " + username);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    // Dishes
    public List<Dish> listDishes(ObjectId sellerId) {
        return docsToList(dishCol.find(Filters.eq("sellerId", sellerId)), Dish.class);
    }

    public void addDish(Dish d) {
        dishCol.insertOne(toDoc(d));
    }

    public void updateDish(ObjectId dishId, Dish d) {
        d.setId(dishId);
        dishCol.replaceOne(Filters.eq("_id", dishId), toDoc(d));
    }


    // Orders
    public List<Order> listPastOrders(ObjectId sellerId) {
        return docsToList(orderCol.find(Filters.and(
                Filters.eq("sellerId", sellerId),
                Filters.eq("status", "COMPLETED")
        )), Order.class);
    }

    // ---------------- Document Mapping ----------------

    private Document toDoc(Seller s) {
        return new Document()
                .append("username", s.getUsername())
                .append("email", s.getEmail())
                .append("password", s.getPassword());
    }

    private Seller fromDocSeller(Document d) {
        Seller s = new Seller();
        s.setId(d.getObjectId("_id"));
        s.setUsername(d.getString("username"));
        s.setEmail(d.getString("email"));
        s.setPassword(d.getString("password"));
        return s;
    }

    private Document toDoc(Dish d) {
        return new Document()
                .append("sellerId", d.getSellerId())
                .append("name", d.getName())
                .append("price", d.getPrice())
                .append("quantity", d.getQuantity());
    }

    private Dish fromDocDish(Document d) {
        Dish dish = new Dish();
        dish.setId(d.getObjectId("_id"));
        dish.setSellerId(d.getObjectId("sellerId"));
        dish.setName(d.getString("name"));
        dish.setPrice(d.getDouble("price"));
        dish.setQuantity(d.getInteger("quantity"));
        return dish;
    }

    // Placeholder
    private Order fromDocOrder(Document doc) {
        Order order = new Order();
        order.setId(doc.getObjectId("_id"));
        order.setCustomerId(doc.getObjectId("customerId"));
        order.setSellerId(doc.getObjectId("sellerId"));
        order.setStatus(doc.getString("status"));

        List<Document> itemDocs = (List<Document>) doc.get("items");
        List<Order.OrderItem> items = itemDocs.stream().map(itemDoc -> {
            Order.OrderItem item = new Order.OrderItem();
            item.setDishId(itemDoc.getObjectId("dishId"));
            item.setQty(itemDoc.getInteger("qty"));
            item.setPrice(itemDoc.getDouble("price"));
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        order.setTotal(doc.getDouble("total"));

        return order;
    }

    private <T> List<T> docsToList(Iterable<Document> it, Class<T> clazz) {
        return StreamSupport.stream(it.spliterator(), false)
                .map(doc -> mapDoc(doc, clazz))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T> T mapDoc(Document d, Class<T> clazz) {
        if (clazz == Dish.class) return (T) fromDocDish(d);
        if (clazz == Seller.class) return (T) fromDocSeller(d);
        if (clazz == Order.class) return (T) fromDocOrder(d);
        return null;
    }

    // Decrement stock for a dish
    public boolean decrementDishStock(ObjectId dishId, int qty) {
        Dish dish = getDish(dishId);
        if (dish != null && dish.getQuantity() >= qty) {
            int newQty = dish.getQuantity() - qty;
            dishCol.updateOne(Filters.eq("_id", dishId),
                    new Document("$set", new Document("quantity", newQty)));
            return true;
        }
        return false;
    }


    // Get dish by ID
    public Dish getDish(ObjectId id) {
        Document doc = dishCol.find(Filters.eq("_id", id)).first();
        if (doc != null) {
            Dish dish = new Dish();
            dish.setId(doc.getObjectId("_id"));
            dish.setName(doc.getString("name"));
            dish.setPrice(doc.getDouble("price"));
            dish.setQuantity(doc.getInteger("quantity"));
            return dish;
        }
        return null;
    }

    // Check if a dish is available in stock
    public  boolean dishAvailable(ObjectId dishId, int qty) {
        Dish dish = getDish(dishId);
        return dish != null && dish.getQuantity() >= qty;
    }

    // Save order to MongoDB
    public void saveOrder(Order order) {
        orderCol.insertOne(toDoc(order));
    }

    // method to convert Order to Document
    private Document toDoc(Order order) {
        return new Document("customerId", order.getCustomerId())
                .append("sellerId", order.getSellerId())
                .append("items", order.getItems())
                .append("status", order.getStatus())
                .append("total", order.getTotal());
    }

    public Order getOrderById(ObjectId id) {
        Document doc = orderCol.find(Filters.eq("_id", id)).first();
        if (doc != null) {
            return fromDocOrder(doc);
        }
        return null;
    }

    // list all dishes
    public List<Dish> listAllDishes() {
        return docsToList(dishCol.find(), Dish.class);
    }

    // list orders for a seller
    public List<Order> getOrdersBySeller(ObjectId sellerId) {
        //  1: Get all dishes for this seller
        List<ObjectId> sellerDishIds = dishCol.find(Filters.eq("sellerId", sellerId))
                .into(new java.util.ArrayList<>())
                .stream()
                .map(d -> d.getObjectId("_id"))
                .collect(Collectors.toList());

        if (sellerDishIds.isEmpty()) return List.of(); // No dishes => no orders

        //  2: Find all orders that contain any of these dishIds
        List<Order> allOrders = docsToList(orderCol.find(), Order.class);

        //  3: Filter orders that contain any seller's dish
        List<Order> matchedOrders = allOrders.stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(item -> sellerDishIds.contains(item.getDishId())))
                .collect(Collectors.toList());

        return matchedOrders;
    }
}
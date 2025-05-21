# Online Home-Made Dishes Operation Platform

This project implements a microservices-based online platform for home-made dishes operations, designed to demonstrate the use of Enterprise Java Beans (EJBs), RabbitMQ messaging, and microservice architectural principles.

---

## Overview

The platform supports three main user roles — Admins, Dish Seller Representatives, and Customers — providing each with tailored functionalities accessible via a web-based interface. It covers full lifecycle operations such as user management, dish management, order processing, and asynchronous communication via messaging queues.

---

## Features

### Admin

- Create dish seller company representative accounts with auto-generated passwords.
- List customer accounts.
- List dish seller company representative accounts.

### Dish Seller Representative

- Login using credentials provided by the admin.
- View currently offered dishes.
- View previously sold dishes with customer and shipping details.
- Add new dishes with prices and available amounts.
- Update existing dish information.

### Customer

- Register as a new user.
- Login with registered credentials.
- View current and past orders.
- Make new orders consisting of multiple dishes with varied quantities.
- Receive confirmation of order processing.

---

## RabbitMQ Integration

- Order placement triggers a stock availability check through RabbitMQ messaging.
- Orders are confirmed only if there is sufficient stock and the minimum charge condition is met.
- Payment processing is handled asynchronously; failed payments trigger order cancellation and rollback.
- **Bonus Features:**
  - Admins receive notifications on payment failures using RabbitMQ direct exchanges.
  - A centralized logging exchange distributes logs with different severity levels (Info, Warning, Error), with admins notified only on errors via RabbitMQ topic exchanges.

---

## Technical Architecture

- **Microservices Architecture:** At least 3 independent services, each with its own codebase and database, communicating via REST APIs.
- **EJB Usage:** One service uses Enterprise Java Beans with at least two EJB types (Stateless, Stateful, Singleton, or Message Driven).
- **Messaging:** RabbitMQ handles asynchronous order processing, payment verification, and logging.
- **Frontend:** A web-based user interface (technology of your choice) interacts with backend services through REST endpoints.

---

## Technology Stack

- Java EE (EJB, JPA)
- RabbitMQ (message broker)
- RESTful APIs
- Relational databases (e.g., MySQL, PostgreSQL)
- Application server supporting EJB (e.g., WildFly, GlassFish)
- Frontend (HTML/CSS/JavaScript or any preferred framework)

---

## Getting Started

### Prerequisites

- Java JDK 11 or higher
- Maven or Gradle
- RabbitMQ server installed and running locally or remotely
- Application server (WildFly, GlassFish, or similar)
- Database server for each microservice

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/online-home-made-dishes-platform.git
   cd online-home-made-dishes-platform
    Configure RabbitMQ:

        Ensure RabbitMQ is running.

        Create the necessary exchanges, queues, and bindings as defined in the services.

    Configure databases:

        Set up individual databases for each microservice.

        Update database connection settings in each service’s configuration files.

    Build and deploy microservices:

        Navigate to each microservice project folder.

        Use Maven or Gradle to build the project.

        Deploy EJB-enabled services to your Java EE application server.

        Deploy other services as standalone Spring Boot apps or Java services.

    Run the frontend:

        Start the web-based UI.

        Ensure it communicates with microservices’ REST APIs.

Usage

Admin:

  - Log in to manage users and company representatives.

  - Create dish seller accounts and view user lists.

  - Dish Seller Representative:

   - Log in to manage dishes and track orders.

Customer:

  - Register and log in.

  - Browse dishes and place orders.

  - Receive real-time order confirmation.
Notes

- RabbitMQ messaging ensures decoupled, asynchronous order processing and payment verification.

- Minimum order charge is enforced during order confirmation.

- Failed payments trigger automatic order cancellation and rollback.

- Admin notifications on payment failures and critical logs are implemented via RabbitMQ direct and topic exchanges.

- Each microservice maintains its own database, following best practices of microservice isolation.

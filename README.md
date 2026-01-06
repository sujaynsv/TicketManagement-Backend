# Ticket Management System - Backend

A robust, microservices-based backend system for managing customer support tickets. Built with **Spring Boot**, this system handles ticket lifecycle management, SLA tracking, and multi-channel notifications.

---

## 📋 Table of Contents

- [Architecture](#architecture)
- [Microservices Overview](#microservices-overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [API Documentation](#api-documentation)
- [Role-Based Access Control](#role-based-access-control)
- [Testing](#testing)

---

## 🏗️ Architecture

The backend follows a distributed microservices architecture:

```
                                  [API Gateway :8080]
                                          │
        ┌─────────────────────────────────┼─────────────────────────────────┐
        ▼                                 ▼                                 ▼
[Auth Service :8081]            [Ticket Service :8082]          [Assignment Service :8083]
(PostgreSQL)                    (MongoDB + AWS S3)              (PostgreSQL/JPA)
        │                                 │                                 │
        └─────────────────────────────────┼─────────────────────────────────┘
                                          ▼
                               [Notification Service :8084]
                               (Postgres + RabbitMQ)
```

- **Service Discovery**: Netflix Eureka
- **Config Management**: Spring Cloud Config Server
- **Inter-service Communication**: REST (Feign Clients) & RabbitMQ (Async Messaging)
- **Resilience**: Resilience4j (Circuit Breakers)

---

## 🔍 Microservices Overview

| Service                  | Port   | Description                                                          |
| :----------------------- | :----- | :------------------------------------------------------------------- |
| **Service Registry**     | `8761` | Eureka Server for service discovery.                                 |
| **Config Server**        | `8888` | Centralized configuration management.                                |
| **API Gateway**          | `8080` | Entry point, JWT validation, routing, and varying rate limits.       |
| **Auth Service**         | `8081` | User registration, login (JWT), RBAC, and agent management.          |
| **Ticket Service**       | `8082` | Core ticket CRUD, comments, activity logs, and attachments (S3).     |
| **Assignment Service**   | `8083` | assignment logic, agent workload tracking, SLAs, and analytics. |
| **Notification Service** | `8084` | Email/In-app notifications via RabbitMQ consumer.                    |

---

## 🛠️ Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.2.0, Spring Cloud 2023.0.0
- **Databases**:
  - PostgreSQL (Auth, Assignment users/rules)
  - MongoDB (Tickets, Notifications, Logs)
- **Messaging**: RabbitMQ
- **Storage**: AWS S3 (Document attachments)
- **Security**: Spring Security, JWT (jjwt)
- **Build Tool**: Maven

---

## 📌 Prerequisites

Ensure you have the following installed:

1. **Java 17+**
2. **Maven 3.8+**
3. **Docker** (optional, for running dependencies easily)
4. **PostgreSQL** & **MongoDB** running locally or in containers.
5. **RabbitMQ** running on port 5672.

---

## 🚀 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/your-repo/ticket-management-backend.git
cd ticket-management-backend
```

### 2. Configure Environment Variables

You can set these in your IDE or system environment execution context.

| Variable            | Default Value                         | Description               |
| :------------------ | :------------------------------------ | :------------------------ |
| `POSTGRES_USER`     | `postgres`                            | DB User                   |
| `POSTGRES_PASSWORD` | `password`                            | DB Password               |
| `MONGODB_URI`       | `mongodb://localhost:27017/ticket_db` | Mongo Connection          |
| `JWT_SECRET`        | _(Must be secure)_                    | Secret for signing tokens |

### 3. Build the Project

Using the root `pom.xml`:

```bash
mvn clean install -DskipTests
```

### 4. Run Services (Order Matters)

1. **Eureka Server**
2. **Config Server** (if utilizing external configs)
3. **Auth Service** (Required for authentication)
4. **Ticket Service**
5. **Assignment Service**
6. **Notification Service**
7. **API Gateway**

Run any service via Maven:

```bash
cd auth-service
mvn spring-boot:run
```

---

## 📡 API Documentation

### **Authentication**

- `POST /auth/register` - Register a new user
- `POST /auth/login` - Login and receive JWT

### **Tickets**

- `POST /tickets` - Create a ticket (supports multipart/form-data)
- `GET /tickets` - Get all tickets (filtered by permissions)
- `PATCH /tickets/{id}/status` - Update ticket status
- `PATCH /tickets/{id}/priority` - Update ticket priority (Manager only)

### **Assignments & SLAs**

- `GET /assignments/my` - View assigned tickets
- `GET /analytics/overview` - System-wide performance stats

---

## 🔐 Role-Based Access Control

The API Gateway enforces permissions based on the JWT `role` claim:

- **END_USER**: Can only create tickets and view/update their own tickets.
- **SUPPORT_AGENT**: Can view assigned tickets, update status, and add internal comments.
- **SUPPORT_MANAGER**: Full access to tickets, assignments, dashboard analytics, and SLAs.
- **ADMIN**: Global system access.

---

## 🧪 Testing

Run unit and integration tests using Maven:

```bash
mvn test
```

The project uses **JUnit 5**, **Mockito**, and **H2/Embedded Mongo** for testing scenarios.

# Task Manager

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.2-6DB33F)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED)
![JWT](https://img.shields.io/badge/Auth-JWT-orange)

## Live Demo

The application is deployed and available here:

🔗 https://taskmanager-45b5.onrender.com/

The deployed version uses:

- **Backend:** Spring Boot application running on Render
- **Database:** MySQL hosted on Aiven
- **Deployment:** Docker container
- **Frontend:** Served directly by Spring Boot from static resources

A full-stack task management web application built with **Spring Boot** and **Vanilla JavaScript**.

The project was created as a portfolio application to demonstrate practical backend development with modern Spring technologies, REST APIs, authentication, testing, Docker, and clean project architecture.

---

## Features

### Authentication

- User registration and login
- JWT token-based authentication
- BCrypt password hashing
- Duplicate username and email validation
- Single fixed administrator account created automatically at startup

### Task Management

- Create, edit and delete tasks
- Priority levels (Low / Medium / High)
- Status tracking (To Do / In Progress / Done)
- Start date and deadline support
- Recurring tasks (daily, weekly, monthly, yearly)
- Automatic generation of future recurring tasks
- Filtering by status, priority and date
- Sorting and pagination
- Calendar view (week and month)
- Current time indicator in calendar view

### Admin Panel

- Manage all users (except the administrator account)
- Search and sort users
- Manage all tasks
- Edit and delete users together with their tasks
- Scroll back to the edited row with highlight animation

### Frontend

- Vanilla JavaScript (ES Modules)
- No frontend framework
- Responsive layout
- Hungarian / English language support
- Locale-aware date and time formatting
- Modular CSS architecture

### Backend

- RESTful API
- Spring Boot 3.3
- Spring Security + JWT
- Spring Data JPA + Hibernate
- MySQL
- Bean Validation
- Global exception handling with consistent JSON responses
- IDOR protection (server-side ownership verification)
- Idempotent admin account seeder

### Testing

- Unit tests (JUnit 5 + Mockito)
- Controller tests (`@WebMvcTest`)
- Repository tests (`@DataJpaTest`)
- Integration smoke test
- Shared TestFixtures builder

---

# Tech Stack

| Layer | Technology |
|--------|------------|
| Language | Java 21 |
| Backend | Spring Boot 3.3.2 |
| Security | Spring Security + JWT |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| Validation | Jakarta Bean Validation |
| Frontend | HTML5, CSS3, Vanilla JavaScript (ES Modules) |
| Build Tool | Maven Wrapper |
| Containerization | Docker & Docker Compose |
| Testing | JUnit 5, Mockito, MockMvc, H2 |

---

# Requirements

## Local Development

- Java 21
- MySQL 8
- Maven (or use the included Maven Wrapper)

## Docker deployment

- Docker
- Docker Compose

---

# Getting Started

Choose one of the following options.

## Option 1 — Run with Docker (Recommended)

Clone the repository:

```bash
git clone https://github.com/IPetoI/taskmanager.git
cd taskmanager
```

Create a `.env` file from the example:

```bash
cp .env.example .env
```

Edit the values if necessary.

Build and start the containers:

```bash
docker compose up --build
```

The application will be available at

```
http://localhost:8080
```

Docker Compose automatically starts

- the Spring Boot application
- a MySQL database
- the internal Docker network between them

No local MySQL installation is required.

---

## Option 2 — Run Locally

Clone the repository:

```bash
git clone https://github.com/IPetoI/taskmanager.git
cd taskmanager
```

Create the database:

```sql
CREATE DATABASE taskmanager;
```

Create a `.env` file:

```bash
cp .env.example .env
```

Example:

```env
DB_USER=root
DB_PASS=your_password

JWT_SECRET=your_very_secret_key_with_at_least_32_characters
JWT_EXPIRATION=86400000
```

Start the application:

Linux / macOS

```bash
./mvnw spring-boot:run
```

Windows

```powershell
.\mvnw.cmd spring-boot:run
```

Open:

```
http://localhost:8080
```

---

# Configuration

The application uses **Spring Profiles** to separate different environments.

| Profile | Purpose |
|---------|---------|
| `local` | Local development with a locally installed MySQL server |
| `docker` | Running with Docker Compose and a local MySQL container |
| `prod` | Production deployment with external MySQL database |

The active profile is selected using:

```properties
SPRING_PROFILES_ACTIVE
```

If no profile is specified, the application uses the **local** profile by default.

---

# Environment Variables

The application uses environment variables for database credentials and security configuration.

For local development these values can be provided through a `.env` file.

For production deployment, the variables are configured directly in the hosting provider environment settings.

Required variables:

```env
DB_HOST=
DB_PORT=
DB_NAME=
DB_USER=
DB_PASS=

JWT_SECRET=
JWT_EXPIRATION=

SPRING_PROFILES_ACTIVE=prod
```

Sensitive values such as database credentials and JWT secrets are never committed to the repository.

---

# Docker

Running

```bash
docker compose up --build
```

starts two containers:

| Container | Purpose |
|-----------|---------|
| **taskmanager-app** | Spring Boot application |
| **taskmanager-mysql** | MySQL database |

The application connects to MySQL using Docker's internal network.

Because Docker Compose provides internal DNS, the hostname

```
mysql
```

automatically resolves to the MySQL container.

No manual database configuration is required.

---

# Admin Account

At startup, the application automatically creates (or updates) a fixed administrator account.

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin` |
| Email | Configured in `application.properties` |

The administrator account

- cannot be deleted
- cannot be edited through the Admin panel
- is recreated automatically if missing

All newly registered users receive the **USER** role.

---

# API Overview

## Authentication

| Method | Endpoint | Authentication | Description |
|--------|----------|----------------|-------------|
| POST | `/api/auth/register` | Public | Register a new user |
| POST | `/api/auth/login` | Public | Authenticate and receive a JWT |

---

## Tasks

| Method | Endpoint | Authentication | Description |
|--------|----------|----------------|-------------|
| GET | `/api/tasks` | USER | List own tasks |
| POST | `/api/tasks` | USER | Create a task |
| PUT | `/api/tasks/{id}` | USER | Update own task |
| DELETE | `/api/tasks/{id}` | USER | Delete own task |

Filtering, sorting and pagination are available through query parameters.

---

## Administration

| Method | Endpoint | Authentication | Description |
|--------|----------|----------------|-------------|
| GET | `/api/admin/users` | ADMIN | List all users |
| PUT | `/api/admin/users/{id}` | ADMIN | Edit a user |
| DELETE | `/api/admin/users/{id}` | ADMIN | Delete a user and all of their tasks |
| GET | `/api/admin/tasks` | ADMIN | List every task |
| PUT | `/api/admin/tasks/{id}` | ADMIN | Edit any task |
| DELETE | `/api/admin/tasks/{id}` | ADMIN | Delete any task |

---

# Authentication Flow

1. Register a new account.
2. Log in using your username and password.
3. Receive a JWT token.
4. The frontend stores the token.
5. Every protected request includes

```
Authorization: Bearer <JWT>
```

6. Spring Security validates the token before allowing access to protected endpoints.

---

# Running Tests

Run the complete test suite:

```bash
./mvnw test
```

or on Windows

```powershell
.\mvnw.cmd test
```

The project contains multiple test types:

| Test Type | Purpose |
|------------|---------|
| Unit Tests | Verify business logic using Mockito |
| Controller Tests | Test REST endpoints with MockMvc |
| Repository Tests | Verify JPA repositories using an in-memory H2 database |
| Integration Test | Ensure the application context starts successfully |

---

# Project Structure

```
src/
├── main/
│   ├── java/com/ipetoi/taskmanager/
│   │   ├── config/          # Security and app config, admin seeder
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Request and response DTOs
│   │   ├── exception/       # Custom exceptions, global handler
│   │   ├── model/           # JPA entities and enums
│   │   ├── repository/      # Spring Data JPA repositories
│   │   ├── security/        # JWT auth, token provider, security components
│   │   └── service/         # Business logic
│   └── resources/
│       ├── static/
│       │   ├── css/         # Thematic CSS files (imported via style.css)
│       │   └── js/          # ES modules
│       ├── application.properties
│       ├── application-local.properties
│       ├── application-docker.properties
│       └── application-prod.properties
└── test/
    └── java/com/ipetoi/taskmanager/
        ├── controller/      # @WebMvcTest controller tests
        ├── exception/       # Exception handler tests
        ├── repository/      # @DataJpaTest repository tests
        ├── security/        # JWT filter and provider tests
        ├── service/         # Unit tests with Mockito
        ├── TestFixtures.java
        ├── SmokeTest.java
        └── TaskManagerIntegrationTest.java
```

---

# Architecture

The backend follows a layered architecture.

```
Client
    │
    ▼
Controller
    │
    ▼
Service
    │
    ▼
Repository
    │
    ▼
Database
```

Each layer has a single responsibility.

### Controller

Responsible for

- receiving HTTP requests
- request validation
- returning HTTP responses

Business logic is intentionally kept out of controllers.

---

### Service

Contains all business logic.

Examples include

- authentication
- recurring task generation
- ownership verification
- administrator account initialization

Services communicate with repositories but never with HTTP.

---

### Repository

Repositories are implemented using Spring Data JPA.

Responsibilities include

- CRUD operations
- custom JPQL queries
- database access

---

### Security

Authentication is implemented using

- Spring Security
- JWT
- BCrypt

Every protected endpoint validates the incoming JWT before executing business logic.

---

### Frontend

The frontend is intentionally framework-free.

Technologies:

- HTML
- CSS
- Vanilla JavaScript (ES Modules)

JavaScript is organized into reusable modules instead of a single large script.

CSS is split into thematic files for easier maintenance.

---

# Design Decisions

Some implementation choices were made intentionally to keep the project clean and maintainable.

### DTO Pattern

Entities are never exposed directly through the REST API.

Separate request and response DTOs are used to decouple the persistence layer from the API.

---

### Global Exception Handling

All exceptions are handled by a centralized exception handler.

This provides

- consistent JSON responses
- proper HTTP status codes
- cleaner controller code

---

### IDOR Protection

Task ownership is verified on the server side.

Attempting to access another user's task returns **404 Not Found** instead of **403 Forbidden**, preventing resource enumeration.

---

### Admin Seeder

The administrator account is created (or updated) automatically during startup.

The operation is idempotent, meaning it can safely run every time the application starts.

---

# Database

The application uses **MySQL 8** together with **Hibernate**.

Schema generation is handled automatically by Hibernate during development.

```
spring.jpa.hibernate.ddl-auto=update
```

This keeps the schema synchronized with the entity classes while preserving existing data.

For production environments, database migrations (Flyway) are planned.

---

# What I Practised

This project was built to practice designing and implementing a complete full-stack application using modern Java and Spring technologies.

During development I focused on:

- Designing a layered backend architecture (Controller → Service → Repository)
- Building a RESTful API following common Spring Boot practices
- Implementing JWT-based authentication and authorization
- Securing endpoints with Spring Security
- Designing JPA entities and relationships
- Separating API models from persistence models using DTOs
- Applying Bean Validation for request validation
- Preventing IDOR vulnerabilities through server-side ownership verification
- Implementing recurring task generation with enum-based polymorphism
- Creating reusable frontend modules using Vanilla JavaScript (ES Modules)
- Organizing CSS into maintainable thematic files
- Writing unit, controller, repository and integration tests
- Managing multiple environments using Spring Profiles
- Containerizing the application with Docker and Docker Compose
- Building an automated CI pipeline with GitHub Actions
- Deploying a Spring Boot application to Render
- Configuring a managed MySQL database (Aiven)

The project intentionally avoids frontend frameworks to place greater emphasis on backend architecture and business logic.

---

# Planned

Features not yet implemented — will be moved to the features list above once done.

## Planned features

- [ ] Task management improvements (recurring tasks, deleting old tasks, sorting)
- [X] User account improvements (profile management, validation, password policy)
- [ ] UI/UX improvements (dark mode, calendar refinements)

## Planned infrastructure improvements

- [x] Docker + Docker Compose setup
- [x] CI pipeline (GitHub Actions)
- [x] Production deployment
- [ ] CD deployment pipeline
- [ ] Flyway database migrations

---

# License

This project is available for educational and portfolio purposes.

---

# Author

Developed by **József Pető**.

Personal portfolio website: 
https://ipetoi.github.io/

---
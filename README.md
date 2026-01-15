# Blogging Platform - Database Layer

A high-performance relational database-driven blogging platform built with JavaFX and PostgreSQL. This project focuses on efficient data modeling, CRUD operations, and performance optimization using indexing and in-memory caching.

---

## System Architecture

The platform follows a layered architecture to ensure separation of concerns:
- **View Layer**: Provide user interface for interaction
- **Controller Layer**: Manages JavaFX UI interactions.
- **Service Layer**: Implements business logic and orchestration (includes in-memory caching).
- **DAO (Data Access Object)**: Handles database persistence and optimized SQL execution.
- **Model**: Defines core entities (User, Post, Comment, Tag, Review).

### Physical Data Model
Below is the Entity Relationship Diagram (ERD) representing the database structure normalized to Third Normal Form (3NF).

![Database ERD](docs/ERD.png)

---

## Features

- **User Management**: Secure registration and login with Argon2 password hashing.
- **Content Creation**: Full CRUD operations for blog posts.
- **Interactive Feedback**: Add, view, and manage comments and nested replies.
- **Tagging System**: Organize posts with dynamic tag assignment and filtering.
- **Review System**: Star-based ratings (1-5) for curated post feedback.
- **Advanced Search**: Case-insensitive keyword search optimized with GIN indexing.
- **Performance Optimized**: Built-in caching for feed retrieval and user statistics.

---

## Application Flow

When you start the JavaFX application, the flow is as follows:
1. **Landing Page**: The entry point providing an overview and navigation to authentication.
2. **Authentication**:
    - **Login**: Authenticate existing users.
    - **Sign Up**: Register new users with validated inputs.
3. **Main Dashboard (Feed)**: Browse the latest posts, filter by tags, or search.
4. **Post Details**: View full post content, read comments, and leave reviews.
5. **Create/Update Post**: Dedicated interfaces for content management.
6. **User Profile**: View personal statistics and manage account.

---

## Project Structure

```text
.
├── docs/                             # Documentation & SQL scripts
│   ├── database-design.md           # Conceptual, Logical, & Physical models
│   ├── performance-report.md        # Benchmarking & Optimization analysis
│   ├── nosql-justification.md       # Theoretical NoSQL application
│   ├── script.sql                   # Schema creation script
│   ├── feedDB.sql                   # Sample data script
│   ├── ERD.png                      # Database diagram
│   └── stats-performance.png        # Benchmarking results screenshot
├── src/main/java/amalitech/blog/    # Source Code
│   ├── Main.java                    # JavaFX Application Entry Point
│   ├── PerformanceMain.java          # Performance Benchmarking Entry Point
│   ├── ApplicationContext.java      # Application state management
│   ├── controller/                  # JavaFX UI Controllers
│   │   ├── auth/                    # Login & SignUp logic
│   │   └── posts/                   # Post creation & details logic
│   ├── dao/                         # Data Access Objects
│   │   ├── DatabaseConnection.java  # JDBC connection pooling
│   │   └── enums/                   # Column name enums
│   ├── service/                     # Service Layer (Business Logic)
│   ├── model/                       # Entity Definitions
│   ├── dto/                         # Data Transfer Objects
│   └── utils/                       # Validation & Performance tools
├── src/main/resources/              # Assets & Views
│   ├── amalitech/blog/view/         # FXML UI Definitions
│   └── amalitech/blog/css/          # Stylesheets
└── pom.xml                          # Maven Dependencies
```

---

## Tech Stack & Dependencies

### Core Technologies
- **Java**: Version 21
- **UI**: JavaFX 21
- **Database**: PostgreSQL 16+
- **Security**: Argon2 JVM (Password Hashing)
- **Configuration**: Dotenv Java

### Dependencies (Maven)
- `javafx-controls` & `javafx-fxml`: UI components and view management.
- `postgresql`: JDBC driver for database connectivity.
- `lombok`: Boilerplate reduction for models and DTOs.
- `dotenv-java`: Secure environment variable management.
- `argon2-jvm`: Advanced password hashing implementation.
- `slf4j-api` & `logback-classic`: Comprehensive logging framework.
- `jna`: Java Native Access for low-level library support.

---

## Setup & Installations

### 1. Prerequisites
- JDK 21
- PostgreSQL Installed and Running
- Maven 3.8+

### 2. Repository Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/angebhd/amalitech-lab-database-blogging-platform.git
   cd amalitech-lab-database-blogging-platform
   ```
2. Create your environment file:
   ```bash
   cp .env.example .env
   ```
3. Open `.env` and fill in your PostgreSQL credentials:
   ```properties
   DB_URL=jdbc:postgresql://localhost:5432/blogging
   DB_USER=your_username
   DB_PASSWORD=your_password
   ```

### 3. Database Initialization
1. Create a database named `blogging`.
2. Execute the main schema script to create tables and indexes:
   ```bash
   psql -d blogging -f docs/script.sql
   ```
3. Execute the sample data script to populate the database:
   ```bash
   psql -d blogging -f docs/feedDB.sql
   ```

---

## Performance Benchmarking

To measure the impact of optimizations, run the dedicated benchmarking tool:
```bash
mvn -q clean compile exec:java
```
> [!NOTE]
> The `-q` flag silences Maven output for a cleaner view of the performance metrics.

![Performance Comparison](docs/stats-performance.png)

Detailed analysis available in the [Performance Report](docs/performance-report.md).

---

## Documentation Links
- [Database Design & Normalization](docs/database-design.md)
- [Optimization & Benchmarking Report](docs/performance-report.md)
- [NoSQL Theoretical Advantages](docs/nosql-justification.md)

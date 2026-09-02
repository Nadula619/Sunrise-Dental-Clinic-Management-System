# Sunrise Dental Clinic Management System

> **Module:** CIS6003 Advanced Programming (WRIT1)  
> **Institution:** Cardiff Metropolitan University / ICBT Campus  
> **Domain:** Dental Clinic Appointment, Patient Records & Billing Management  

---

## 📋 System Overview

Sunrise Dental Clinic is a private dental care center located in Colombo. This computerized patient and appointment management web application replaces manual paper files and notebooks, completely solving:
- Double bookings & doctor scheduling conflicts
- Lost patient records and treatment history
- Long patient waiting times
- Manual calculation and billing discrepancies

---

## 🛠️ Technology Stack

- **Backend Runtime:** Java 17 / 20 (Plain Java + OOP Core)
- **Web Layer:** Java Servlets (`javax.servlet`) + REST Web Services
- **Database:** MongoDB NoSQL (via `mongodb-driver-sync` official Java Driver)
- **Server:** Embedded Apache Tomcat 9
- **Serialization:** Google Gson
- **Security:** BCrypt salted password encryption (`jBcrypt`)
- **Frontend:** Responsive HTML5, Vanilla CSS3 (Custom Design System), JavaScript (ES6)
- **Testing:** JUnit 5 (Jupiter) + Mockito (33 automated unit & integration tests)
- **CI/CD:** Apache Maven + GitHub Actions Workflow

---

## 🏛️ Architecture & Applied Design Patterns

The application is structured into a clean **3-Tier Distributed Architecture**:

```
Presentation Layer (HTML5, CSS3, JS REST Client, Print Stylesheets)
        ↓ (HTTP / JSON Web Services)
Business Logic / Service Layer (Plain Java Services, Factory & Strategy Patterns)
        ↓
Data Access Layer (DAO Pattern Interfaces & MongoDB Implementations)
        ↓
Persistence Layer (MongoDB Database: sunrise_dental_db)
```

### Design Patterns Used
1. **Singleton Pattern (`DatabaseConnection.java`):** Ensures a single, thread-safe MongoDB client instance throughout the application lifecycle.
2. **Data Access Object (DAO) Pattern (`IAppointmentDAO`, `IBillingDAO`, `IUserDAO`, etc.):** Decouples business logic from persistence logic.
3. **Factory Pattern (`BillingCalculatorFactory.java`):** Dynamically instantiates the correct billing calculation strategy based on discount/patient type.
4. **Strategy Pattern (`BillingStrategy.java`, `StandardBillingStrategy.java`, `SeniorCitizenBillingStrategy.java`, `InsuranceCoveredBillingStrategy.java`, `PromotionalBillingStrategy.java`):** Encapsulates extensible discount calculation algorithms.
5. **Model-View-Controller (MVC) Pattern:** Separates Document Models, Servlet Controllers, and HTML/JSP Views.

---

## 🚀 How to Run the Application

### Prerequisites
- JDK 17 or higher
- Apache Maven 3.8+
- (Optional) MongoDB running on `mongodb://localhost:27017` (The app includes resilient auto-seeding & in-memory caching fallback if MongoDB service is offline).

### Quick Start (One Command)
Run the embedded server directly from the project directory:
```bash
mvn clean compile exec:java
```

Once started, open your web browser and navigate to:
```
http://localhost:8080/
```

### Running Automated Tests
```bash
mvn clean test
```
*(Runs all 33 JUnit 5 unit and integration tests).*

---

## 👥 Default Demo Staff Credentials

| Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full clinic management & oversight |
| **Receptionist** | `receptionist` | `rec123` | Appointment booking & patient billing |
| **Dentist** | `dr.roshan` | `doc123` | Medical practitioner schedule view |

---

## 📡 REST Web Services API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Staff authentication & session creation |
| `POST` | `/api/auth/logout` | Session invalidation / safe exit |
| `GET` | `/api/auth/session` | Get active user profile |
| `GET` | `/api/appointments` | List appointments (supports `?search=`, `?date=`, `?dentist=`) |
| `GET` | `/api/appointments/{id}` | Get specific appointment details |
| `POST` | `/api/appointments` | Register new appointment with conflict check |
| `PUT` | `/api/appointments/{id}` | Update appointment details |
| `POST` | `/api/appointments/{id}/status` | Update status (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) |
| `GET` | `/api/billing` | List all issued invoices |
| `GET` | `/api/billing/{billNo}` | Get specific bill / receipt data |
| `POST` | `/api/billing/calculate` | Dry-run live cost calculation |
| `POST` | `/api/billing` | Generate and save final invoice |
| `GET` | `/api/reports` | Get management KPIs & doctor breakdown |
| `GET` | `/api/treatments` | Get treatment catalog & base prices |
| `GET` | `/api/dentists` | Get clinic dentists & consultation fees |

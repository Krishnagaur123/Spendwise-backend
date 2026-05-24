# Spendwise — Backend

## Overview
A Spring Boot 3 REST API for personal finance management. The service stores users, transactions (income & expense unified), goals, and provides monthly/yearly reports. It uses **stateful session authentication** with secure HttpOnly cookies for assignment compliance.

## Features
- Session‑based login/logout (no JWT)
- Unified `Transaction` entity with `type` enum (INCOME / EXPENSE)
- Goal tracking with real‑time progress calculation
- Monthly & yearly aggregation reports grouped by category
- Default category seeding on registration
- Daily email reminders (scheduled jobs)

## Tech Stack
| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.5.6 |
| Language | Java 21 |
| Security | Spring Security (session) |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL (prod) / H2 (dev) |
| Build | Maven (`./mvnw`) |
| Container | Docker (multi‑stage) |

## Architecture & Design Decisions
- **Unified Transaction model** – `Transaction` replaces separate income/expense entities, simplifying CRUD and reporting. An enum `TransactionType { INCOME, EXPENSE }` distinguishes the flow.
- **Session‑based auth** – Login creates a server‑side `HttpSession`; the session ID is stored in an HttpOnly cookie (`JSESSIONID`). All protected endpoints rely on `SecurityContext` populated from the session. No JWT handling or token storage on the client.
- **Separation of concerns** – Controllers delegate to services, which use repositories. DTOs shield entities from external exposure.
- **Goal progress** – `GoalService` computes percentage completion using aggregated transaction sums.
- **Report service** – `ReportService` runs JPQL `GROUP BY` queries for efficient monthly/yearly summaries.

## Authentication (Session‑Based)
- **Login** – `POST /auth/login` validates credentials, creates a session, and returns user details. The `JSESSIONID` cookie is `HttpOnly`, `Secure` (in production), and has a short lifespan.
- **Logout** – `POST /auth/logout` invalidates the session and clears the cookie.
- **Protected routes** – Spring Security enforces authentication on all `/api/**` endpoints. CSRF protection is disabled for simplicity in assignment context.

## API Overview
| Method | Endpoint | Description |
|--------|----------|-------------|
| **POST** | `/auth/login` | Authenticate user, create session, return profile data |
| **POST** | `/auth/logout` | Invalidate session |
| **POST** | `/auth/register` | Register new user (active immediately) |
| **GET** | `/profile` | Retrieve current user's profile |
| **PUT** | `/profile` | Update profile fields |
| **GET** | `/transactions` | List all transactions (supports `type`, `date`, `category` filters) |
| **POST** | `/transactions` | Create a new transaction (specify `type`) |
| **PUT** | `/transactions/{id}` | Update transaction details |
| **DELETE** | `/transactions/{id}` | Delete a transaction |
| **GET** | `/goals` | List user's savings goals with progress |
| **POST** | `/goals` | Create a new goal |
| **PUT** | `/goals/{id}` | Update goal target or deadline |
| **DELETE** | `/goals/{id}` | Delete a goal |
| **GET** | `/reports/monthly/{year}/{month}` | Income/expense totals per category for a month |
| **GET** | `/reports/yearly/{year}` | Income/expense totals per category for a year |

## Setup
1. **Prerequisites** – Java 21, Maven, MySQL (optional). H2 is used automatically for dev/test.
2. **Clone repo** and navigate to `Spendwise-backend-master`.
3. **Configure environment** – create a `.env` or export variables:
   ```bash
   export DEV_DB_URL=jdbc:h2:mem:testdb
   export JWT_SECRET=ignored   # kept for backward compatibility only
   export SESSION_COOKIE_NAME=JSESSIONID
   export SERVER_PORT=8080
   ```
   (Adjust `DEV_DB_URL` for a MySQL instance.)
4. **Run**:
   ```bash
   ./mvnw spring-boot:run
   ```
   API base: `http://localhost:8080/api`

## Running Locally with Docker
```bash
docker build -t spendwise-backend .
docker run -p 8080:8080 \
  -e DEV_DB_URL=jdbc:h2:mem:testdb \
  spendwise-backend
```

## Testing
```bash
./mvnw test
# JaCoCo report at target/site/jacoco/index.html
```
Integration tests use an in‑memory H2 DB and mock email service.

## Deployment
- **Backend** – Render (or any cloud provider). Example URL: `https://spendwise-backend.onrender.com/api`
- **CI/CD** – GitHub Actions can build the Docker image and push to a registry.





# Bank API

REST banking API focused on **accounts and transactional transfers**  
(Java 21, Spring Boot, PostgreSQL, Docker).

## Features

- Deposit / withdraw / transfer with full transaction log
  - Idempotency-Key on transfers
  - Daily outgoing limit
  - Statement & transaction history
  - Swagger UI, Docker Compose, integration tests

## Run

```bash
docker compose up --build
```
API: http://localhost:8080
Swagger: http://localhost:8080/swagger-ui/index.html
Demo user is created on startup: demo@bank.com (see logs for account ids).
## Stack
Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL · SpringDoc · Docker


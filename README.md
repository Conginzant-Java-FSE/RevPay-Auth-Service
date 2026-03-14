# RevPay — Auth Service

> Handles all authentication and authorization for RevPay. Registration, login, JWT issuance, password management, and admin access — all in one dedicated service.

---

## Overview

The Auth Service is the **identity and access management** layer of RevPay. It manages user registration for both personal and business account types, authenticates users, issues signed JWT tokens, and exposes admin endpoints for user management. All other services trust the JWT headers injected by the API Gateway after this service validates credentials.

| Property | Value |
|---|---|
| **Service Name** | `auth-service` |
| **Internal Port** | `8080` |
| **External Port** | `8081` |
| **Framework** | Spring Boot 3.2.5 + Spring Security + JPA |
| **Database** | MySQL — `auth_db` |
| **Java Version** | 17 |

---

## Architecture Role

```
Client
  │
  ▼
API Gateway :8080
  │  (public routes — no JWT needed)
  ▼
Auth Service :8081
  │
  ├── Validates credentials against auth_db
  ├── Issues JWT (userId, email, accountType, roles)
  ├── Notifies notification-service via Feign on register
  └── Returns token to client
```

After login, the client sends `Authorization: Bearer <token>` on every subsequent request. The gateway validates the token and injects `X-User-Id`, `X-User-Email`, `X-Account-Type` headers — other services never see the raw JWT.

---

## API Endpoints

### Public Endpoints (No Auth Required)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new personal or business account |
| `POST` | `/api/auth/login` | Login and receive JWT token |
| `POST` | `/api/auth/forgot-password/request` | Request password reset OTP |
| `POST` | `/api/auth/forgot-password/verify` | Verify OTP and reset password |

### Protected Endpoints (JWT Required)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/auth/logout` | Logout (token invalidation) |
| `PUT` | `/api/auth/change-password` | Change password with current password verification |
| `GET` | `/api/auth/me` | Get current authenticated user info |

### Admin Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/admin/login` | Admin login |
| `GET` | `/api/admin/users` | List all users |
| `PUT` | `/api/admin/users/{id}/status` | Activate/deactivate user |

---

## JWT Token Structure

Tokens issued by this service contain:
```json
{
  "sub": "user@example.com",
  "userId": 42,
  "accountType": "PERSONAL",
  "roles": ["ROLE_USER"],
  "iat": 1710000000,
  "exp": 1710086400
}
```

Token expiry: **24 hours** (configurable via `jwt.expiration`)

---

## Getting Started

### Run with Docker
```bash
docker build -t revpay-auth .
docker run -p 8081:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/auth_db \
  -e DB_USER=revpay_user \
  -e DB_PASS=revpay_pass \
  -e JWT_SECRET=your-secret-min-32-chars \
  -e EUREKA_HOST=eureka-server \
  revpay-auth
```

### Run Locally
```bash
./mvnw spring-boot:run
```

### Test Registration
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "9876543210",
    "password": "Secret@123",
    "accountType": "PERSONAL",
    "securityQuestion": "Pet name?",
    "securityAnswer": "Buddy"
  }'
```

---

## Configuration

| Environment Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL for `auth_db` |
| `DB_USER` | Database username |
| `DB_PASS` | Database password |
| `JWT_SECRET` | JWT signing secret (min 32 chars) |
| `EUREKA_HOST` | Eureka server hostname |
| `CONFIG_SERVER_HOST` | Config server hostname |

---

## Database Schema (`auth_db`)

Key tables:
- `users` — core user record (email, phone, password hash, account type, status)
- `roles` — role definitions
- `user_roles` — many-to-many user ↔ role mapping

Password storage: **BCrypt** with strength 12.

---

## Security Features

- Passwords hashed with BCrypt
- JWT signed with HMAC-SHA256
- Security questions for password recovery
- Account activation status check on every login
- Spring Security filter chain with stateless session management

---

## Inter-Service Communication

| Called Service | When | Method |
|---|---|---|
| `notification-service` | On successful registration | OpenFeign |

---

## Health & Monitoring

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Service health + DB connectivity |
| `GET /actuator/info` | Service version and metadata |

---

## Key Dependencies

```xml
<dependency>spring-boot-starter-security</dependency>
<dependency>spring-boot-starter-data-jpa</dependency>
<dependency>spring-cloud-starter-netflix-eureka-client</dependency>
<dependency>spring-cloud-starter-openfeign</dependency>
<dependency>jjwt-api 0.11.5</dependency>
<dependency>mysql-connector-j</dependency>
```

---

## Project Structure

```
auth-service/
├── src/main/java/com/revpay/authservice/
│   ├── AuthServiceApplication.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   └── AdminController.java
│   ├── service/
│   │   └── AuthService.java
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtUtil.java
│   │   └── SecurityConfig.java
│   ├── entity/
│   │   └── User.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   └── LoginRequest.java
│   └── feign/
│       └── NotificationServiceClient.java
├── src/main/resources/
│   └── application.properties
├── Dockerfile
└── pom.xml
```

---

## Related Services

- **API Gateway** — routes public auth endpoints without JWT validation
- **Notification Service** — receives registration event via Feign
- All other services trust the JWT claims forwarded by the gateway

See the main [RevPay Microservices](https://github.com/Conginzant-Java-FSE/RevPay-Frontend) repository for full architecture.

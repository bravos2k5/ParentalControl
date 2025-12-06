# Parental Control API

A Spring Boot service for managing device access time and active sessions, designed for parental-control scenarios (e.g., grant screen time, block devices after a delay, manage active sessions via WebSocket).

The service exposes a REST API and WebSocket endpoint backed by Redis. **All HTTP REST endpoints are authenticated via a shared secret token** implemented in `AuthFilter`.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Authentication Model](#authentication-model)
- [Running the Service](#running-the-service)
- [Configuration](#configuration)
- [REST API Reference](#rest-api-reference)
- [WebSocket Protocol](#websocket-protocol)
- [Example Usage](#example-usage)

---

## Architecture Overview

- **Framework**: Spring Boot 4.0 (Spring Web MVC, WebSocket, Spring Security)
- **Language / Runtime**: Java 25
- **Data Store**: Redis (via Spring Data Redis, with `Session` stored as a `@RedisHash`)
- **Authentication**: Custom `OncePerRequestFilter` (`AuthFilter`) that validates a shared secret against a BCrypt hash
- **Packaging / Build**: Gradle with Spring Boot plugin, Dockerfile building a minimal distroless image

---

## Project Structure

```
src/main/java/com/bravos/parentalcontrol/
├── ParentalControlApplication.java     # Main application entry point
├── config/
│   ├── AppConfig.java                  # Application configuration
│   ├── RedisConfig.java                # Redis connection configuration
│   ├── SecurityConfig.java             # Spring Security configuration
│   └── WebSocketConfig.java            # WebSocket configuration
├── controller/
│   ├── AccessController.java           # REST endpoints for access management
│   ├── GlobalExceptionHandler.java     # Global exception handling
│   └── SessionController.java          # REST endpoints for session management
├── dto/
│   ├── request/
│   │   ├── NewSessionRequest.java      # DTO for new session creation
│   │   └── TimeRequest.java            # DTO for time-based requests
│   └── response/
│       └── ApiResponse.java            # Standard API response wrapper
├── entity/
│   └── Session.java                    # Redis-backed session entity
├── repository/
│   └── SessionRepository.java          # Redis repository for sessions
├── security/
│   ├── AuthFilter.java                 # Authentication filter
│   └── BenchmarkFilter.java            # Request timing filter
├── service/
│   ├── AccessService.java              # Business logic for access control
│   └── SessionService.java             # Business logic for session management
├── util/
│   ├── DateTimeHelper.java             # Date/time utilities
│   └── Snowflake.java                  # Unique ID generator
└── websocket/
    ├── WebSocketSessionManager.java    # WebSocket session management
    ├── handler/
    │   └── ControlHandler.java         # WebSocket message handler
    └── interceptor/
        └── ConnectInterceptor.java     # WebSocket handshake interceptor
```

---

## Authentication Model

All REST APIs are authenticated via the `AuthFilter`.

### How it works

1. Every HTTP request passes through `AuthFilter`, **except** URIs starting with `/ws`
2. The filter reads the `Authorization` header (raw secret, no `Bearer` prefix)
3. The token is validated against a BCrypt hash from `PARENTAL_CONTROL_PASSWORD_HASH`
4. If valid, the request proceeds with an authenticated principal

### Configuring the shared secret

1. Choose a strong secret (e.g., `MY_SUPER_SECRET`)
2. Generate a BCrypt hash of this secret
3. Set `PARENTAL_CONTROL_PASSWORD_HASH` environment variable to the hash

```bash
export PARENTAL_CONTROL_PASSWORD_HASH='$2a$10$...'
```

### Client authentication

Send the plain secret in the `Authorization` header:

```http
Authorization: MY_SUPER_SECRET
```

---

## Running the Service

### Prerequisites

- Java 25+
- Redis server
- Gradle (or use the included wrapper)

### Run locally with Gradle

```bash
./gradlew bootRun
```

### Run with Docker

```bash
docker build -t parental-control .
docker run -p 8080:8080 \
  -e REDIS_HOST=localhost \
  -e REDIS_PORT=6379 \
  -e REDIS_PASSWORD=your_redis_password \
  -e PARENTAL_CONTROL_PASSWORD_HASH='$2a$10$...' \
  parental-control
```

### Run with docker-compose

Create a `.env` file:

```env
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
PARENTAL_CONTROL_PASSWORD_HASH=$2a$10$...
ALLOW_ORIGINS=http://localhost:3000
```

Then run:

```bash
docker-compose up -d
```

---

## Configuration

| Environment Variable              | Description                              | Default |
|-----------------------------------|------------------------------------------|---------|
| `REDIS_HOST`                      | Redis server hostname                    | -       |
| `REDIS_PORT`                      | Redis server port                        | 6379    |
| `REDIS_PASSWORD`                  | Redis authentication password            | -       |
| `PARENTAL_CONTROL_PASSWORD_HASH`  | BCrypt hash of the API secret            | -       |
| `ALLOW_ORIGINS`                   | Comma-separated list of allowed origins  | -       |

---

## REST API Reference

All responses use the standard `ApiResponse` format:

```json
{
  "success": true,
  "message": "Operation message",
  "data": "<response_data>"
}
```

### Access Management (`/access`)

#### POST `/access/generate-code`

Generate a time-limited access code for a device.

**Request:**
```json
{
  "deviceId": "device-123",
  "seconds": 3600
}
```

**Response:**
```json
{
  "success": true,
  "message": "Access granted",
  "data": "123456"
}
```

#### POST `/access/grant`

Grant access directly to a device (sends WebSocket message).

**Request:**
```json
{
  "deviceId": "device-123",
  "seconds": 3600
}
```

#### POST `/access/block`

Schedule a device block after specified time.

**Request:**
```json
{
  "deviceId": "device-123",
  "seconds": 300
}
```

#### GET `/access/block-time/{deviceId}`

Get remaining block time for a device.

**Response:**
```json
{
  "success": true,
  "message": "Remaining block time",
  "data": 180
}
```

### Session Management (`/sessions`)

#### GET `/sessions`

List all active sessions.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "session-id",
      "deviceName": "Kid's Tablet",
      "deviceId": "device-123",
      "ipAddress": "192.168.1.100",
      "createdAt": 1733500000000,
      "lastActive": 1733500500000
    }
  ]
}
```

#### DELETE `/sessions/{id}`

Delete a specific session.

#### DELETE `/sessions`

Delete all sessions.

---

## WebSocket Protocol

### Connection

Connect to `/ws/control` with required headers:

```
X-Device-Id: unique-device-identifier
X-Device-Name: Device Display Name
X-Real-IP: client-ip-address
```

### Client Messages

| Message           | Description                          |
|-------------------|--------------------------------------|
| `ping`            | Heartbeat, server responds `pong`    |
| `PASSWORD:<code>` | Submit access code for verification  |
| `BLOCKED`         | Notify server device is now blocked  |

### Server Messages

| Message            | Description                           |
|--------------------|---------------------------------------|
| `pong`             | Response to ping                      |
| `GRANTED:<seconds>`| Access granted for specified seconds  |
| `DENIED`           | Access code verification failed       |
| `BLOCK:<seconds>`  | Device will be blocked after seconds  |
| `UNKNOWN_COMMAND`  | Unrecognized client message           |

---

## Example Usage

### Grant 1 hour of access to a device

```bash
curl -X POST http://localhost:8080/access/grant \
  -H "Authorization: MY_SUPER_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "device-123", "seconds": 3600}'
```

### Generate an access code

```bash
curl -X POST http://localhost:8080/access/generate-code \
  -H "Authorization: MY_SUPER_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "device-123", "seconds": 3600}'
```

### List all active sessions

```bash
curl http://localhost:8080/sessions \
  -H "Authorization: MY_SUPER_SECRET"
```

### Block a device in 5 minutes

```bash
curl -X POST http://localhost:8080/access/block \
  -H "Authorization: MY_SUPER_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "device-123", "seconds": 300}'
```

---

## License

See [LICENSE](LICENSE) file.

# Parental Control API

A small Spring Boot service for managing device access time and active sessions, intended for parental-control style setups (e.g., grant a child one hour of screen time, block a device after a delay, inspect and clear active sessions).

The service exposes a simple REST API backed by Redis. **All HTTP REST endpoints are authenticated via a shared secret token** implemented in `AuthFilter`.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Authentication Model](#authentication-model)
  - [Configuring the shared secret](#configuring-the-shared-secret)
  - [How clients authenticate](#how-clients-authenticate)
  - [Authentication failures](#authentication-failures)
- [Running the Service](#running-the-service)
  - [Prerequisites](#prerequisites)
  - [Run locally with Gradle](#run-locally-with-gradle)
  - [Run with Docker](#run-with-docker)
  - [Run with docker-compose](#run-with-docker-compose)
- [Configuration](#configuration)
- [REST API Reference](#rest-api-reference)
  - [/access – Access management](#access--access-management)
  - [/sessions – Session management](#sessions--session-management)
- [Example curl Usage](#example-curl-usage)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

- **Framework**: Spring Boot (Spring Web MVC, WebSocket, Spring Security)
- **Language / Runtime**: Java (configured toolchain: **Java 25**)
- **Data store**: Redis (via Spring Data Redis, with `Session` stored as a `@RedisHash`).
- **Authentication**: Custom `OncePerRequestFilter` (`AuthFilter`) that checks a shared secret provided in the `Authorization` header against an environment-configured BCrypt hash.
- **Packaging / Build**: Gradle with Spring Boot plugin (`bootJar` enabled, `parental-control.jar`), Dockerfile building a minimal distroless image.

Main components:

- `AccessController` (`/access/...`) – Grant or schedule blocks for devices based on a device ID and a number of seconds.
- `SessionController` (`/sessions/...`) – List and delete active sessions stored in Redis.
- `AuthFilter` – Enforces authentication on all HTTP endpoints **except** those under `/ws` (used for WebSocket traffic).
- `Session` model – Redis-backed representation of a device session.

---

## Authentication Model

All REST APIs are authenticated. The logic is implemented in `com.bravos.parentalcontrol.filter.AuthFilter`.

### How it works

- Every HTTP request passes through `AuthFilter`, **except** URIs starting with `/ws`.
- The filter reads the `Authorization` header.
  - If the header is missing → the request is rejected with `401 Unauthorized`.
  - If the header is present, its full value (after `.trim()`) is treated as the **password token**. There is **no `Bearer` prefix**; it is just the raw secret.
- The filter compares this token against a BCrypt hash from the environment variable `PARENTAL_CONTROL_PASSWORD_HASH` using Spring Security's `PasswordEncoder.matches(token, hashedPassword)`.
  - If `PARENTAL_CONTROL_PASSWORD_HASH` is missing or empty, or the token does not match, the request is rejected with `401 Unauthorized`.
  - If it matches, a synthetic authenticated principal (`admin` with `ROLE_ADMIN`) is placed into the `SecurityContext`, and the request is allowed to proceed.

### Configuring the shared secret

There is **no endpoint** to obtain a token. Instead, you choose a shared secret (password) yourself, hash it, and configure that hash in the environment.

1. Choose a strong secret, e.g.:

   ```text
   MY_SUPER_SECRET
   ```

2. Generate a BCrypt hash of this secret using any tool that uses the same algorithm as Spring Security's `PasswordEncoder` (e.g. a simple Spring Boot runner, online BCrypt generator, or `spring-security` CLI). The result will look like:

   ```text
   $2a$10$VpP.................etc
   ```

3. Set the environment variable **`PARENTAL_CONTROL_PASSWORD_HASH`** to this hash **before starting the app**.

   Example (Linux/macOS shell):

   ```bash
   export PARENTAL_CONTROL_PASSWORD_HASH='$2a$10$VpP.......................'
   ```

4. Start the application (via Gradle, Docker, or docker-compose). The filter will use this hash to validate incoming requests.

### How clients authenticate

Clients must send the **plain secret** in the `Authorization` header on every HTTP request:

```http
Authorization: MY_SUPER_SECRET
```

There is no session, no token issuing endpoint, and no refresh mechanism. This is a simple single shared secret for protecting the API.

> Note: WebSocket endpoints under `/ws` are currently excluded from this filter and may use a different authentication mechanism or none at all.

### Authentication failures

If authentication fails:

- `Authorization` header missing → `401 Unauthorized` with an empty body.
- `Authorization` header present but incorrect, or `PARENTAL_CONTROL_PASSWORD_HASH` not set → `401 Unauthorized` with an empty body.

---

## Running the Service

### Prerequisites

- JDK **25** (as configured in `build.gradle.kts` via `JavaLanguageVersion.of(25)`).
- Redis instance:
  - Local Redis server, **or**
  - The Redis container from `docker-compose.yml`.
- Environment variables:
  - `PARENTAL_CONTROL_PASSWORD_HASH` – required for any authenticated REST access.
  - If using Docker/docker-compose: `REDIS_PORT`, `REDIS_PASSWORD` as described below.

The application binds to port **8080** by default (see `application.properties`).

### Run locally with Gradle

From the project root (`/home/bravos/IdeaProjects/parental-control`):

1. Export your shared secret hash:

   ```bash
   export PARENTAL_CONTROL_PASSWORD_HASH='YOUR_BCRYPT_HASH_HERE'
   ```

2. Ensure Redis is running and reachable using its default configuration (or point the app to your Redis via additional Spring properties if needed).

3. Build the project:

   ```bash
   ./gradlew clean build
   ```

4. Run the application:

   ```bash
   ./gradlew bootRun
   ```

5. Access the API at:

   ```text
   http://localhost:8080
   ```

### Run with Docker

The included `Dockerfile` builds a minimal image using a custom JRE and a distroless base image.

1. Build the image:

   ```bash
   docker build -t parental-control:latest .
   ```

2. Run the container (assuming Redis is reachable at `host.docker.internal:6379` or similar, and you pass any needed Redis properties as env vars/`SPRING_APPLICATION_JSON` if not using docker-compose):

   ```bash
   docker run \
     -e PARENTAL_CONTROL_PASSWORD_HASH='YOUR_BCRYPT_HASH_HERE' \
     -p 8080:8080 \
     parental-control:latest
   ```

3. The API is then reachable at `http://localhost:8080` on the host.

> The `Dockerfile` also defines a `HEALTHCHECK` that runs the jar with a `health` argument. Ensure your runtime supports this or adjust accordingly if you customize the image.

### Run with docker-compose

The `docker-compose.yml` file defines both **Redis** and the **parental-control** service.

- Service: `redis`
  - Image: `redis:8.0.2-alpine3.21`
  - Exposes `6379` mapped from `${REDIS_PORT}`.
  - Requires `REDIS_PASSWORD` and starts with `--requirepass`.

- Service: `parental-control`
  - Image: `bravos/parental-control:latest` (ensure you've built/pushed or use `image` pointing to your local build).
  - Exposes `8080:8080`.
  - Environment variables:
    - `REDIS_HOST=redis`
    - `REDIS_PORT=${REDIS_PORT}`
    - `REDIS_PASSWORD=${REDIS_PASSWORD}`
    - `PARENTAL_CONTROL_PASSWORD_HASH=${PARENTAL_CONTROL_PASSWORD_HASH}`

Example usage:

```bash
export REDIS_PORT=6379
export REDIS_PASSWORD='your_redis_password'
export PARENTAL_CONTROL_PASSWORD_HASH='YOUR_BCRYPT_HASH_HERE'

docker-compose up --build
```

The API will be available at `http://localhost:8080`.

---

## Configuration

Key configuration points:

- `server.port` – defaults to `8080` (see `src/main/resources/application.properties`).
- Redis configuration – by default relies on Spring Boot's standard Redis properties; when running via `docker-compose.yml`, the app uses:
  - `REDIS_HOST=redis`
  - `REDIS_PORT` and `REDIS_PASSWORD` from environment.
- `PARENTAL_CONTROL_PASSWORD_HASH` – **must** be set for successful authentication.

If you need to override any Spring Boot properties, you can use `application.properties`, environment variables, or command-line arguments as usual.

---

## REST API Reference

### Common Request Conventions

- **Base URL**: `http://localhost:8080`
- **Authentication header** (required for all REST endpoints):

  ```http
  Authorization: <your-plain-secret>
  ```

- **Content type**:
  - `POST` endpoints expect `Content-Type: application/json`.
- **Error responses**:
  - `401 Unauthorized` – missing or invalid `Authorization` or misconfigured `PARENTAL_CONTROL_PASSWORD_HASH`.
  - `AccessController` endpoints return plain text for both success and error messages (caught exceptions are converted to `String` without changing HTTP status).

---

### `/access` – Access management

Handled by `AccessController`.

#### Data type: `TimeRequest`

Used by the `/access/grant` and `/access/block` endpoints.

```json
{
  "deviceId": "string",  // required – unique identifier of the device
  "seconds": 3600          // required – number of seconds to grant or delay a block
}
```

#### `POST /access/grant`

Grant access time for a device.

- **URL**: `/access/grant`
- **Method**: `POST`
- **Headers**:
  - `Authorization: <plain-secret>`
  - `Content-Type: application/json`
- **Request body**: `TimeRequest` JSON.
- **Responses**:
  - `200 OK` – body is `text/plain`:
    - Success: return value from `accessService.grantTime(deviceId, seconds)` (implementation-specific text).
    - Error: exception message as plain text (still HTTP 200 in this controller).
  - `401 Unauthorized` – if authentication fails (empty body).

#### `POST /access/block`

Schedule a block for a device after a specified delay.

- **URL**: `/access/block`
- **Method**: `POST`
- **Headers**:
  - `Authorization: <plain-secret>`
  - `Content-Type: application/json`
- **Request body**: `TimeRequest` JSON.
- **Responses**:
  - `200 OK` – body is `text/plain`:
    - On success: `"Device will be blocked after {seconds} seconds."`
    - On error: exception message as plain text.
  - `401 Unauthorized` – if authentication fails.

#### `GET /access/block-time/{deviceId}`

Retrieve remaining block time for a device.

- **URL**: `/access/block-time/{deviceId}`
- **Method**: `GET`
- **Headers**:
  - `Authorization: <plain-secret>`
- **Path variables**:
  - `deviceId` – unique identifier of the device.
- **Responses**:
  - `200 OK` – body is `text/plain`:
    - If blocked: `"Remaining block time for device {deviceId}: {seconds} seconds."`
    - If not blocked: `"Device {deviceId} is not currently blocked."`
  - `401 Unauthorized` – if authentication fails.

---

### `/sessions` – Session management

Handled by `SessionController`.

#### Data type: `Session`

Stored in Redis as `@RedisHash("session")`.

```json
{
  "id": "string",          // unique session identifier
  "deviceName": "string",  // human-readable device name
  "deviceId": "string",    // device identifier (indexed in Redis)
  "ipAddress": "string",   // IP address of the device
  "createdAt": 1710000000000, // epoch millis when session was created
  "lastActive": 1710003600000  // epoch millis when session was last active
}
```

> There is also a `NewSessionRequest` DTO with fields `id`, `deviceId`, `deviceName`, and `ipAddress`, which is not used directly by the REST controllers but may be used elsewhere (e.g., WebSockets).

#### `GET /sessions`

List all active sessions.

- **URL**: `/sessions`
- **Method**: `GET`
- **Headers**:
  - `Authorization: <plain-secret>`
- **Responses**:
  - `200 OK` – body is `application/json` array of `Session` objects.
  - `401 Unauthorized` – if authentication fails.

#### `DELETE /sessions/{id}`

Delete a single session by its ID.

- **URL**: `/sessions/{id}`
- **Method**: `DELETE`
- **Headers**:
  - `Authorization: <plain-secret>`
- **Path variables**:
  - `id` – session ID to delete.
- **Responses**:
  - `204 No Content` (or `200 OK` with empty body, depending on Spring defaults) – on success.
  - `401 Unauthorized` – if authentication fails.
  - Behavior when the session does not exist depends on `sessionService.deleteSession` (likely idempotent and non-erroring).

#### `DELETE /sessions`

Delete **all** sessions.

- **URL**: `/sessions`
- **Method**: `DELETE`
- **Headers**:
  - `Authorization: <plain-secret>`
- **Responses**:
  - `204 No Content` (or `200 OK` with empty body) – on success.
  - `401 Unauthorized` – if authentication fails.

---

## Example curl Usage

Assume:

- Your chosen secret is `MY_SUPER_SECRET`.
- `PARENTAL_CONTROL_PASSWORD_HASH` is configured with the BCrypt hash of `MY_SUPER_SECRET`.
- The service is running on `http://localhost:8080`.

### Grant 1 hour (3600 seconds) of access

```bash
curl -X POST "http://localhost:8080/access/grant" \
  -H "Authorization: MY_SUPER_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"device-123","seconds":3600}'
```

### Block a device after 5 minutes (300 seconds)

```bash
curl -X POST "http://localhost:8080/access/block" \
  -H "Authorization: MY_SUPER_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"device-123","seconds":300}'
```

### Check remaining block time for a device

```bash
curl -X GET "http://localhost:8080/access/block-time/device-123" \
  -H "Authorization: MY_SUPER_SECRET"
```

### List all sessions

```bash
curl -X GET "http://localhost:8080/sessions" \
  -H "Authorization: MY_SUPER_SECRET"
```

### Delete a specific session

```bash
curl -X DELETE "http://localhost:8080/sessions/<SESSION_ID>" \
  -H "Authorization: MY_SUPER_SECRET"
```

Replace `<SESSION_ID>` with the actual session ID.

### Delete all sessions

```bash
curl -X DELETE "http://localhost:8080/sessions" \
  -H "Authorization: MY_SUPER_SECRET"
```

---

## Troubleshooting

- **All requests return 401 Unauthorized**
  - Check that `PARENTAL_CONTROL_PASSWORD_HASH` is set in the environment on the machine/container running the app.
  - Verify that the `Authorization` header value you send is the **plain secret**, not the hash and not prefixed by `Bearer`.
  - Ensure line breaks or quotes are not accidentally included in the secret when exporting it.

- **Redis connection errors**
  - If running locally, ensure a Redis server is running and reachable at the expected host and port.
  - If using docker-compose, verify the `redis` container is healthy and that `REDIS_PORT`/`REDIS_PASSWORD` environment variables are correctly set.

- **Port conflicts**
  - The app listens on `8080` by default. Change `server.port` in `application.properties` or set the `SERVER_PORT` environment variable if needed.

This README should give you enough context to run the service, configure authentication, and integrate with the REST API quickly.


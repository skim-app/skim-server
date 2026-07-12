# Skim server

Kotlin + Spring Boot + PostgreSQL backend for the Skim Android MVP.

## Run

```bash
# Default ports used by the Android emulator configuration.
docker compose up --build --detach

# Use alternate host ports when 5432 or 8080 is already occupied.
SKIM_API_HOST_PORT=8081 SKIM_POSTGRES_HOST_PORT=5433 docker compose up --build --detach
```

- API: `http://localhost:8080/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

The development startup seeds a completed recording with transcript and summary source timestamps, enabling the Android Milestone 2 vertical slice. Audio upload, asynchronous deterministic processing, and byte-range audio delivery support Milestones 3–5. Actual STT/LLM remains out of scope.

## Test

```bash
./gradlew test
./gradlew bootJar
```

## Current endpoints

- `POST`, `GET /v1/recordings`
- `GET /v1/recordings/{recordingId}`
- `POST /v1/recordings/{recordingId}/audio`
- `POST /v1/recordings/{recordingId}/process`
- `GET /v1/recordings/{recordingId}/audio`
- `GET /v1/recordings/{recordingId}/processing-status`
- `GET /v1/recordings/{recordingId}/transcript`
- `GET /v1/recordings/{recordingId}/summary`
- `POST`, `GET`, `PATCH /v1/todos`

# FF-Training Card

Training tool for the Volunteer Fire Department to display vehicle positions and status in real time on an interactive map of Hamburg. Used to train the Emergency Operations Director Program (ELA/EDV) with realistic radio transmissions.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.1, Java 21, Gradle (Kotlin DSL) |
| UI | Thymeleaf, Leaflet + OpenStreetMap tiles |
| Database | PostgreSQL 16, Flyway migrations |
| Sessions/Caching | Redis 7 |
| Authentication | Spring Security 7, form login, roles ADMIN / VIEWER |
| Real-time communication | Server-Sent Events (SSE) + Redis Pub/Sub |

## Features

### Map (`/`)
- Interactive Leaflet map covering Hamburg
- Vehicle markers with color-coded status:
  - 🟢 Free on radio / Free at station
  - 🔴 Dispatch accepted / On scene
  - ⚫ Off duty
- Marker popup shows call sign, vehicle type, status, and last update time
- One-click status change in the popup (all authenticated users)
- Auto-refresh every 10 seconds + SSE real-time updates

### Administration (`/admin/vehicles`, `/admin/users`)
- **Vehicle management**: CRUD for vehicles with coordinate input via map, required fields and duplicate check (call sign)
- **User management**: Create users, change roles, activate/deactivate
- Self-protection: ADMIN cannot delete or deactivate themselves
- Only `ROLE_ADMIN` has access to `/admin/**`; VIEWER receives 403

### Real-time Architecture
- Event-driven via Redis Pub/Sub and SSE (Server-Sent Events)
- Vehicle change → Redis publish → SSE broadcast to all connected maps
- Heartbeat mechanism for connection maintenance (60s timeout)

## Status Codes (ELA System)

| Code | Meaning | Color on map |
|---|---|---|
| 1 | Free on radio | Green |
| 2 | Free at station | Green |
| 3 | Dispatch accepted | Red |
| 4 | On scene | Red |
| 6 | Off duty | Gray |

## Configuration

All parameters configurable via environment variables (defaults for local development):

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ff_trainingskarte` | Database JDBC connection |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `password` | Database password |
| `REDIS_HOST` | `localhost` | Redis host (sessions + Pub/Sub) |
| `REDIS_PORT` | `6379` | Redis port |
| `SERVER_PORT` | `8080` | HTTP servlet port |

## Local Development

```bash
# 1. Start infrastructure layer (Postgres + Redis)
docker compose up -d

# 2. Run app in dev mode with seed data
./gradlew bootRun --args='--spring.profiles.active=dev'
```

App: http://localhost:8080
Admin login: `admin` / `admin`

Automatically seeds one ADMIN user and four Hamburg volunteer fire department vehicles:
- HLF 20/1 (Wandsbek)
- DLK 12/1 (Altona)
- TLF 3/1 (Hamburg-Mitte)
- MTW 1/1 (Harburg)

### Tests

```bash
./gradlew test allTests
```

31 unit and integration tests (`@WebMvcTest` for controllers, self-protection logic, CRUD coverage).

## API Endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/api/vehicles` | Authenticated | Return all vehicles (JSON) |
| POST | `/api/vehicles` | ADMIN | Create new vehicle |
| PUT | `/api/vehicles/{id}` | ADMIN | Update vehicle basic data |
| PUT | `/api/vehicles/{id}/position` | ADMIN | Change position (coordinate validation: Hamburg radius) |
| PUT | `/api/vehicles/{id}/status` | Authenticated | Change status (values 1,2,3,4,6 → triggers SSE broadcast) |
| DELETE | `/api/vehicles/{id}` | ADMIN | Delete vehicle → triggers SSE delete notification |

HTTP request examples in `http/*.http`.

## Docker & Deployment

### Build image

```bash
docker buildx build --platform linux/amd64 -t ghcr.io/slin86/ff-trainingskarte:v0.1.0 --push .
```

Two-stage image: build with Eclipse Temurin JDK 21, runtime only JRE (non-root user `appuser`).

### k3s Deployment (with ArgoCD)

Kubernetes manifests in `deploy/`:

| File | Purpose |
|---|---|
| `ns.yaml` | Namespace `feuerwehr` |
| `configmap.yaml` | Spring profile `prod`, Redis host, SSE configuration |
| `deployment.yaml` | 2 replicas, liveness/readiness via actuator `/actuator/health`, resource limits (256m/512m memory) |
| `service.yaml` | ClusterIP on port 8080 |
| `ingress.yaml` | Traefik ingress for host `feuerwehr.home.lan` |

Secrets (`InfisicalSecret` CRD), DB password, Spring Redis configuration, and CORS allow-origin. **Not in the repository**.

Deploy via ArgoCD:
1. Copy manifests to ArgoCD repo under `apps/feuerwehr/`
2. Create ArgoCD Application resource (`repoURL`, `targetRevision`, `path: apps/feuerwehr`)
3. Sync → pods provisioned, ingress active

Details: [deploy/README.md](deploy/README.md)

## Repository Structure

```
ff-trainingskarte/
├── deploy/               # Kubernetes manifests
├── docker-compose.yml    # Local dev environment (Postgres + Redis)
├── Dockerfile            # Production Docker image
├── http/                 # HTTP request templates (.http)
├── src/main/java/de/ffw/trainingskarte/
│   ├── config/           # SecurityConfig
│   ├── controller/       # Web + Admin + REST API controllers
│   │   └── dto/          # Request records (Vehicle, Position, StatusChange)
│   ├── entity/           # JPA @Entity classes (Vehicle, AppUser)
│   ├── repository/       # Spring Data repositories
│   ├── seeder/           # Dev seed (DataSeeder.java)
│   └── sse/              # SSE registry + Redis Pub/Sub event handling
├── src/main/resources/
│   ├── db/migration/     # Flyway scripts
│   ├── static/js/        # Frontend JavaScript (Leaflet map)
│   ├── templates/        # Thymeleaf templates (map.html, login.html)
│   └── application.yml   # Defaults (all configurable via env overrides)
├── src/test/java/        # Unit + @WebMvcTest tests
```

## Architectural Decisions

- **10s polling as fallback**: The map freezes reliably when SSE connections drop. Polling ensures display even without persistent connections.
- **Redis session store**: Enables horizontal scaling (multi-replica) with shared session storage.
- **No external CSS framework**: Custom minimal CSS for reduced dependencies and fast rendering on tablets/communication devices at the station.
- **Constructor injection throughout**: no Lombok, no `@Autowired` field injection → explicit dependencies, easy testing.

## Conventions

- Constructor injection, no Lombok, no field injection
- UI texts in German, code in English
- No global imports; fully qualified packages where needed
- Only Jakarta namespaces (`jakarta.*`), no `javax.*`
- Every change needs an observable acceptance check (log line, cookie name, HTTP status) — "compiles" proves nothing

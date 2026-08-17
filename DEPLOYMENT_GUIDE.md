# Travel Dabble — Production Deployment & Cloud Guide

This guide details how to deploy the **Travel Dabble Ktor backend** to **Render** (recommended 1-click or manual Docker), **Railway**, **Fly.io**, or any **Docker VPS**, and seamlessly connect the **Travel Dabble Android / KMP App**.

---

## Architecture Overview

```
 ┌─────────────────────────────────────────────────────────┐
 │                   Travel Dabble Mobile App              │
 │  • Local-first SQLite (SQLDelight)                      │
 │  • Dynamic Server URL in Settings                       │
 │  • Offline / Demo Mode & Online Cloud Sync              │
 └────────────────────────────┬────────────────────────────┘
                              │ HTTPS REST / MCP
                              ▼
 ┌─────────────────────────────────────────────────────────┐
 │             Travel Dabble Ktor Backend (Docker)         │
 │  • Netty Engine / Ktor 3.4                              │
 │  • Auth & JWT Tokens (7-day validity)                   │
 │  • Trips, Destinations, Routing & Collaboration API     │
 │  • AI Copilot & MCP Tools (Weather, Advisories, Events) │
 │  • Auto Flyway Migrations & Initial Seed Data           │
 └────────────────────────────┬────────────────────────────┘
                              │ JDBC
                              ▼
 ┌─────────────────────────────────────────────────────────┐
 │                 Managed PostgreSQL Database             │
 │  • Supabase / Neon / Render Postgres / Railway Postgres │
 └─────────────────────────────────────────────────────────┘
```

---

## 1. Deploying to Render.com (Recommended)

Render provides free Docker web services and managed PostgreSQL databases.

### Method A: 1-Click Render Blueprint (Fastest)

1. Fork or push this repository to your GitHub/GitLab account.
2. Log in to [Render Dashboard](https://dashboard.render.com/).
3. Click **New +** → **Blueprint**.
4. Connect your repository. Render will automatically detect [`render.yaml`](render.yaml).
5. Click **Apply**:
   - Render creates a managed PostgreSQL database (`travel-dabble-db`).
   - Render builds the Docker image and launches the Web Service (`travel-dabble-server`).
   - `DATABASE_URL` and `JWT_SECRET` are automatically generated and linked.
6. Once deployed, note your service URL (e.g. `https://travel-dabble-server.onrender.com`).

---

### Method B: Manual Render Setup

If configuring manually without a Blueprint:

1. **Create PostgreSQL Database**:
   - Click **New +** → **PostgreSQL**.
   - Name: `travel-dabble-db`, Database: `traveldabble`, User: `traveldabble`.
   - Click **Create Database** and copy the **Internal Database URL** (or External URL).

2. **Create Web Service**:
   - Click **New +** → **Web Service** → Connect your repository.
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `server/Dockerfile` (or `Dockerfile`)
   - **Health Check Path**: `/health`
   - **Environment Variables**:
     | Variable | Value / Description |
     |---|---|
     | `PORT` | `8080` |
     | `DATABASE_URL` | *Paste your Render Postgres Connection String* |
     | `JWT_SECRET` | *A secure random string (e.g., 32+ characters)* |
     | `OPENROUTER_API_KEY` | *(Optional) Server-wide AI Copilot key* |
     | `DATABASE_MAX_POOL_SIZE` | `10` |
   - Click **Create Web Service**.

---

## 2. Deploying to Other Managed Hosts

### Railway
```bash
railway init
railway add --plugin postgresql
railway up --dockerfile server/Dockerfile
```

### Fly.io
```bash
fly launch --dockerfile server/Dockerfile --no-deploy
fly postgres create
fly postgres attach <postgres-app-name>
fly deploy
```

### Self-Hosted VPS (Docker Compose + Caddy Auto-TLS)
```bash
cp .env.example .env
# Edit .env and set DOMAIN=api.yourdomain.com and DATABASE_PASSWORD
docker compose up -d --build
```

---

## 3. Server Environment Variables Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `PORT` | No | `8080` | HTTP port the Ktor server listens on |
| `DATABASE_URL` | No | In-memory H2 | PostgreSQL connection string (`postgres://...` or `jdbc:postgresql://...`) |
| `JWT_SECRET` | No | Fallback secret | Secret key used to sign and verify HMAC-256 auth tokens |
| `DATABASE_MAX_POOL_SIZE` | No | `10` | Maximum HikariCP connection pool size |
| `OPENROUTER_API_KEY` | No | None | AI key for Travel Copilot (users can also BYOK in app settings) |

---

## 4. Verifying Server Health

Once deployed, test your endpoints via `curl` or browser:

```bash
# 1. Basic Health Check
curl https://your-service.onrender.com/health
# Response: ok

# 2. Server Root
curl https://your-service.onrender.com/
# Response: TravelDabble API v1.0

# 3. AI Service Health
curl https://your-service.onrender.com/api/ai/health
# Response: {"status":"ok","server_key_configured":true,...}

# 4. Explore Destinations (Public)
curl https://your-service.onrender.com/api/destinations
```

---

## 5. Connecting the Travel Dabble Mobile App

The Travel Dabble app is designed with a **flexible local-first and cloud hybrid architecture**. You can connect it to your live deployed backend in two easy ways:

### Option A: In-App UI Configuration (No Recompile Needed)

1. Open the **Travel Dabble** app on your device or emulator.
2. Navigate to **Profile / Settings** → **Account Settings**.
3. Tap **Backend Server Connection**.
4. Enter your deployed server URL (e.g., `https://travel-dabble-server.onrender.com`).
5. Tap **Test** — the app immediately sends a test request to `/health` and displays a green `✓ Connected successfully` indicator.
6. Tap **Save**. All subsequent registrations, logins, trips, routes, and AI queries will use your live cloud server!

### Option B: Build-time Default Configuration

To bake your production URL into the APK / build permanently:
- Update `DEFAULT_BASE_URL` in `shared/src/androidMain/kotlin/com/dabber/traveldabble/data/HttpClientFactory.android.kt`:
  ```kotlin
  actual val DEFAULT_BASE_URL: String = "https://your-service.onrender.com"
  ```
- Build and install the release APK:
  ```bash
  ./gradlew :composeApp:assembleRelease
  ```

---

## 6. Database Migrations & Initial Data

- **Flyway Migrations**: Executed automatically upon application boot.
- **Initial Seed Data**: Pre-loaded with curated Vietnam travel destinations (Hanoi, Ha Long Bay, Hoi An, Da Nang, Ho Chi Minh City, Sapa, Phu Quoc), sample itineraries, and activities.
- **Data Persistence**: With PostgreSQL attached, user registrations, custom trips, notes, and collaboration invites persist reliably across server restarts.

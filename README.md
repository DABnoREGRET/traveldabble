# Travel Dabble 🌴⛵

<div align="center">

![Travel Dabble Logo](assets/logo.svg)

**An intelligent, collaborative, and local-first travel planning platform.**

Built with **Kotlin Multiplatform (KMP)**, **Compose Multiplatform**, and a high-performance **Ktor** backend with **Model Context Protocol (MCP)** server integration.

[![CI](https://github.com/DABnoREGRET/traveldabble/actions/workflows/ci.yml/badge.svg)](https://github.com/DABnoREGRET/traveldabble/actions/workflows/ci.yml)
[![Release APK](https://github.com/DABnoREGRET/traveldabble/actions/workflows/release.yml/badge.svg)](https://github.com/DABnoREGRET/traveldabble/actions/workflows/release.yml)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

</div>

---

## 🌟 Highlights & Features

- **🗺️ Interactive Map & Day-by-Day Itineraries**: Visual route exploration powered by MapLibre GL, with interactive pins, place previews, custom styling for dark/light themes, and day-by-day itinerary tracking.
- **🤖 AI Travel Copilot & MCP Tools**: Integrated AI assistant that can reason over your travel plans, execute local/remote tools via the Model Context Protocol (`/mcp`), check weather forecasts, and compare destinations. Supports Bring-Your-Own-Key (BYOK) with OpenRouter.
- **👥 Group Trip Collaboration**: Generate and manage multi-use invite codes with expiration dates. Invite fellow travelers with fine-grained roles (`owner`, `editor`, `viewer`).
- **💰 Budget & Expense Management**: Set trip budgets across custom categories (accommodation, food, transit, activities) with real-time spend tracking and expense logging.
- **📱 Local-First & Offline-Ready**: Full offline capability backed by SQLDelight local caching, with automatic synchronization to the remote PostgreSQL backend when connected.
- **🎨 Premium Adaptive Design**: Responsive docking navigation, smooth micro-animations, full dark/light theme contrast tuning, and edge-to-edge support.
- **🔔 Notifications & Telemetry**: Privacy-first opt-out telemetry dashboard (`/api/stats`), in-app notification center, and FCM device token registration.

---

## 🏗️ Architecture & Tech Stack

```
traveldabble/
├── composeApp/      # Android application (Compose Multiplatform)
├── shared/          # Kotlin Multiplatform library (UI, ViewModels, Repositories, SQLDelight, Ktor Client)
├── server/          # Ktor Netty backend (PostgreSQL + Exposed, Flyway, JWT Auth, MCP Server)
└── .github/         # CI/CD Workflows (automated testing, APK & AAB release packaging)
```

### Mobile App (`:composeApp` & `:shared`)
- **UI Framework**: Compose Multiplatform 1.8.2 (Material 3)
- **Networking**: Ktor Client 3.1.3 with ContentNegotiation & Kotlinx Serialization
- **Local Persistence**: SQLDelight 2.0.2 with Android SQLite Driver
- **Dependency Injection**: Koin 4.0.4 (`koin-compose`, `koin-compose-viewmodel`)
- **Mapping**: MapLibre GL Android SDK 11.13.5
- **Key-Value Storage**: Multiplatform Settings 1.3.0

### Backend Server (`:server`)
- **Engine**: Ktor 3.1.3 Netty Engine
- **ORM / Database**: JetBrains Exposed 0.56.0 with PostgreSQL 17 / H2 (HikariCP connection pooling)
- **Database Migrations**: Flyway 10.22.0
- **Security & Auth**: JWT Tokens (java-jwt 4.4.0) + BCrypt (at.favre.lib:bcrypt 0.10.2)
- **AI Integration**: OpenRouter API with dynamic tool definitions and streaming execution
- **Model Context Protocol**: MCP 2024-11-05 standard (`/mcp` endpoint for LLM tool discovery and calling)

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17** (Temurin, Eclipse Adoptium, or Oracle)
- **Android Studio** Ladybug (2024.2+) or Android SDK CLI
- **Docker & Docker Compose** (optional, for local PostgreSQL container)

### 1. Clone the Repository
```bash
git clone https://github.com/DABnoREGRET/traveldabble.git
cd traveldabble
```

### 2. Configure Environment
Copy the example environment configuration:
```bash
cp .env.example .env
```

### 3. Run the Backend Server
Start the local PostgreSQL database (or use Docker Compose):
```bash
docker compose up -d postgres
```

Launch the Ktor server:
```bash
./gradlew :server:run
```
- Server API: `http://localhost:8080`
- Health Check: `http://localhost:8080/health`
- MCP Discovery: `http://localhost:8080/mcp`
- Telemetry Stats: `http://localhost:8080/api/stats`

### 4. Run the Android App
```bash
./gradlew :composeApp:installDebug
```
*(Note: From the Android Emulator, the app connects to the host machine at `http://10.0.2.2:8080`. You can also configure a custom server URL in the app's Account Settings.)*

---

## 🧪 Testing

Travel Dabble includes a **116-test suite** covering 100% of server endpoints and mobile client integration flows:

```bash
# Run all project tests
./gradlew test

# Run backend integration tests (88 tests)
./gradlew :server:test

# Run shared mobile client tests with Ktor MockEngine (28 tests)
./gradlew :shared:testDebugUnitTest
```

---

## 🚢 Building & Deployment

### Build Signed Release APK
To build a release APK locally using your release keystore:
```bash
./gradlew :composeApp:assembleRelease
```
The output APK is placed at `composeApp/build/outputs/apk/release/composeApp-release.apk`.

### Deploying Backend with Docker
```bash
docker compose up -d --build
```
Includes Caddy with automatic Let's Encrypt TLS provisioning for production domains.

### Deploying to Render
A [`render.yaml`](render.yaml) blueprint and [`Dockerfile`](Dockerfile) are included in the repository for one-click deployment.

---

## 🤖 CI / CD Pipelines

GitHub Actions workflows are pre-configured:
- **`ci.yml`**: Runs all 116 tests and builds debug APK on every pull request and push to `main`.
- **`release.yml`**: Automatically builds and publishes signed release APKs and Google Play App Bundles (`.aab`) to GitHub Releases when a version tag (`v*.*.*`) is pushed.

---

## 📄 License & Attribution

Licensed under the [Apache License, Version 2.0](LICENSE).

```
Copyright 2026 Travel Dabble Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

# Race Photos Monorepo

This repository contains two modules:
- `backend/`: Spring Boot Java API
- `frontend/`: Angular application (standalone components)

## Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+ and npm 9+

## Backend (Spring Boot)
- Directory: `backend/`
- Run: `mvn spring-boot:run`
- Health endpoint: `GET http://localhost:8080/api/health`

### Database
- Start Postgres: `docker compose up -d postgres`
- Default connection (configured in `backend/src/main/resources/application.yml`):
  - URL: `jdbc:postgresql://localhost:5432/racephotos`
  - Username: `racephotos`
  - Password: `secret`
- Override via env vars when running the backend:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Frontend (Angular)
- Directory: `frontend/`
- Install deps: `npm install`
- Start dev server (proxies `/api` to backend): `npm start`
- App: `http://localhost:4200`

The dev server uses `frontend/proxy.conf.json` to forward API calls to the backend at `http://localhost:8080`.

## Notes
- Backend uses Spring Boot 3.3.x with Web + Validation + Data JPA and PostgreSQL driver.
- Frontend uses Angular 18 with standalone bootstrap and a minimal router setup.
- Adjust package names, groupId, and versions as needed for your organization.

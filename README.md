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
 - Security: HTTP Basic; `/api/health` is public, all other endpoints require auth.
   - Default creds (override via env): `BASIC_AUTH_USER=api`, `BASIC_AUTH_PASSWORD=changeit`
   - Example: `curl -u api:changeit http://localhost:8080/your/secure/endpoint`

### AWS (S3 and Rekognition)
- SDK: AWS SDK v2
- Config properties (see `backend/src/main/resources/application.yml`):
  - `aws.region` (default `us-east-1`)
  - `aws.s3.bucket` (optional)
  - `aws.s3.endpoint` (optional; set when using LocalStack)
  - `aws.s3.path-style-enabled` (bool; set `true` for LocalStack)
  - `aws.rekognition.endpoint` (optional; set when using LocalStack)
- Env var overrides supported: `AWS_REGION`, `AWS_S3_BUCKET`, `AWS_S3_ENDPOINT`, `AWS_S3_PATH_STYLE`, `AWS_REKOGNITION_ENDPOINT`
- Clients auto-configured via Spring beans: `S3Client` and `RekognitionClient` using the default credentials provider chain.

LocalStack example (S3 + Rekognition):
```
AWS_REGION=us-east-1 \
AWS_S3_ENDPOINT=http://localhost:4566 \
AWS_S3_PATH_STYLE=true \
AWS_REKOGNITION_ENDPOINT=http://localhost:4566 \
mvn -f backend/pom.xml spring-boot:run
```

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
- Backend uses Spring Boot 3.3.x with Web, Security, Validation, Data JPA and PostgreSQL driver.
- Frontend uses Angular 18 with standalone bootstrap and a minimal router setup.
- Adjust package names, groupId, and versions as needed for your organization.

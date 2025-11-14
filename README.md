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

### Face Indexing Service
- Service bean: `FaceIndexingService` (backend module).
- Entry point `indexFacesForEvent(eventId, List<objectKeys>)` should be invoked once uploads finish (e.g., by the eventual S3 event handler).
- For each provided S3 object the service makes sure the Rekognition collection exists, runs `IndexFaces`, and stores `faceId → eventId, bucket, photoKey, bounding box, confidence, timestamps` rows in DynamoDB for later lookups.

Key configuration (see `backend/src/main/resources/application.yml`):
- `aws.rekognition.collection-id` (default `race-photos-face-collection`).
- `aws.dynamodb.table` (default `race-photos-face-metadata`).
- `aws.dynamodb.endpoint` (optional; useful for LocalStack).

Manual AWS setup you must perform once per environment:
1. **IAM permissions** for the backend's runtime role/user: `rekognition:*Collection`, `rekognition:IndexFaces`, `s3:GetObject` on the upload bucket, and `dynamodb:PutItem` on the metadata table.
2. **DynamoDB table** with `faceId` (String) as the partition key. Example CLI:
   ```bash
   aws dynamodb create-table \
     --table-name race-photos-face-metadata \
     --attribute-definitions AttributeName=faceId,AttributeType=S \
     --key-schema AttributeName=faceId,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST
   ```
3. (Optional) Point the service to LocalStack by setting `AWS_DYNAMODB_ENDPOINT`, `AWS_REKOGNITION_ENDPOINT`, and reusing the same S3 bucket configuration.

Once an upload-notification trigger is wired, call the service with the event identifier plus the S3 keys that were uploaded to keep Rekognition and DynamoDB synchronized.

### Face Search API
- Endpoint: `POST /api/faces/search`
- Auth: HTTP Basic (same as the rest of the API except health/presign).
- Request body:
  ```json
  {
    "eventId": "race-2024",
    "photoKey": "photos/athlete-1.jpg"
  }
  ```
- Response: returns the probe key plus a list of other S3 keys for that event that contain the same person, based on Rekognition’s `SearchFacesByImage` matches filtered through DynamoDB metadata. Each match includes the faceId, similarity score, Rekognition confidence, and bounding box if available.
- Config knobs (see `application.yml`):
  - `aws.rekognition.search.max-faces` (default 50) – cap on Rekognition matches inspected.
  - `aws.rekognition.search.threshold` (default 90) – Rekognition similarity threshold.
- Behavior: the probe photo itself is excluded from the results; duplicates per photo key are collapsed keeping the highest similarity score.

### Presigned Upload URLs API
- Endpoint: `POST /api/s3/presigned-urls`
- Auth: public (will be secured later)
- Request body: JSON array of object names, e.g.:
  `[
    "photos/1.jpg",
    "photos/2.jpg"
  ]`
- Response: array of `{ name, url }` entries:
  `[
    { "name": "photos/1.jpg", "url": "https://..." },
    { "name": "photos/2.jpg", "url": "https://..." }
  ]`
- Config:
  - Bucket (required): `aws.s3.bucket` (env `AWS_S3_BUCKET`)
  - Presign expiration seconds: `aws.s3.presign.expiration-seconds` (env `AWS_S3_PRESIGN_EXPIRATION_SECONDS`, default `7200`)
  - Region/endpoint as above; for LocalStack use the `e2e` profile or set endpoints explicitly.
- Upload example (browser or CLI): perform a `PUT` to the `url` with the file bytes as body. No extra headers are required.

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

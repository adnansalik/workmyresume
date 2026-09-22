# workmyresume
Resume Analyzer App

Resume management and analysis SaaS. Backend only (Spring Boot 4, Java 21, PostgreSQL).

# Things it should do (MVP)

- [x] User uploads Resume
- [x] Store the resume
- [x] Analyze the resume (LLM, with a local heuristic fallback)
- [x] Results with a score, suggestions, strengths

# Data Model

User:
- id, email, password, isActive, createdAt, role, plan

Resume:
- id, user_id, title, created_at
- has many versions

ResumeVersion:
- id, resume_id, version_number, title, raw_text, created_at
- has files and analyses

ResumeFile (metadata only; bytes stored on disk / external storage):
- id, version_id, original_filename, content_type, size_bytes, storage_key, uploaded_at

ResumeAnalysis:
- id, version_id, job_description, score, strengths(JSON), gaps(JSON), suggestions(JSON), created_at

Subscription:
- id, user_id, plan (FREE/PRO), status (ACTIVE/CANCELED/EXPIRED),
  current_period_start, current_period_end, canceled_at, timestamps

Plan limits:
- FREE: 3 analyses per 30 days
- PRO: unlimited

# Roadmap

- [x] Foundation
- [x] Signup
- [x] Login
- [x] JWT (env-configurable secret)
- [x] Authorization (ROLE_USER / ROLE_ADMIN method security)
- [x] Billing foundation (plans, subscriptions, lifecycle, usage quotas)
- [ ] Payments (Stripe/Razorpay - after product usage model stabilizes)
- [x] Resume domain + upload/storage + text extraction (PDF/DOCX/TXT)
- [x] Resume analysis (LLM with heuristic fallback, job-description matching)
- [x] OAuth (Google/GitHub mapped to internal users)
- [x~] Testing (unit + integration, 49 tests)
- [x~] Production hardening (env JWT secret, CORS, rate limiting, Flyway migrations, actuator, security headers, CI)

# Running

```bash
docker compose up -d          # PostgreSQL on port 5433
cd backend && ./mvnw spring-boot:run   # App on :8080
```

# Tests

```bash
./scripts/run-integration-tests.sh   # isolated Postgres container, dev DB untouched
cd backend && ./mvnw test            # uses the docker-compose dev database
```

# Configuration

| Key | Env override | Default | Purpose |
| --- | --- | --- | --- |
| `app.jwt.secret` | `APP_JWT_SECRET` | dev-only default | JWT signing key (HS256, ≥32 chars) |
| `app.ai.api-key` | `APP_AI_APIKEY` | *(unset)* | Enables the LLM analyzer; blank → heuristic |
| `app.ai.base-url` | `APP_AI_BASE_URL` | `https://api.openai.com/v1` | OpenAI-compatible endpoint |
| `app.ai.model` | `APP_AI_MODEL` | `gpt-4o-mini` | LLM model |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Browser origins allowed |
| `app.rate-limit.*` | `APP_RATE_LIMIT_*` | 300 req / 60 s on `/auth/` | In-memory login brute-force protection |
| `app.upload-dir` | `APP_UPLOAD_DIR` | `uploads` | Local file storage root |
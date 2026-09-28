# 🏥 Nigeria Health & Safety Platform

A multi-module Spring Boot backend that addresses three critical gaps in Nigeria's public health and emergency response infrastructure:

| Module | Port | Purpose |
|---|---|---|
| **Blood Bank Network** | 8081 | Real-time blood stock tracking, donor registry, hospital matching |
| **Road Accident Reporting** | 8082 | Citizen accident reporting, FRSC alerting, hospital blood notification |
| **Drug Authenticity Verification** | 8083 | NAFDAC drug verification, counterfeit reporting, hotspot detection |

---

## 🗂️ Project Structure

```
nigeria-health-safety-platform/
├── pom.xml                          ← Parent Maven POM
├── shared/                          ← JWT, Email, SMS, Exceptions (shared library)
├── blood-bank/                      ← Module 1 — runs on port 8081
├── accident-report/                 ← Module 2 — runs on port 8082
└── drug-verification/               ← Module 3 — runs on port 8083
```

---

## ⚙️ Prerequisites

Before running this project, make sure you have installed:

- **Java 17** or higher → https://adoptium.net
- **Maven 3.8+** → https://maven.apache.org/download.cgi
- **PostgreSQL 14+** → https://www.postgresql.org/download
- **IntelliJ IDEA** (recommended) or VS Code with Java extensions

---

## 🗄️ Database Setup

All three modules share one PostgreSQL database. Run the following once:

```sql
-- Connect to PostgreSQL and create the database
CREATE DATABASE nigeria_health_db;
```

Each module uses **Flyway** to auto-run its own migration scripts on startup.
You do **not** need to run any SQL manually — Flyway handles it.

---

## 🔑 Environment Variables

Create a `.env` file in each module's root, or set these as system environment variables.
**Never commit real credentials to Git.**

```bash
# Database (shared by all modules)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nigeria_health_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# JWT — generate a Base64-encoded 256-bit secret key:
# Run this in terminal: openssl rand -base64 32
JWT_SECRET=your_base64_encoded_secret_here

# Gmail SMTP (use an App Password, not your real Gmail password)
# Setup: Google Account → Security → 2-Step Verification → App Passwords
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password

# Termii SMS API (sign up free at https://termii.com)
TERMII_API_KEY=your_termii_api_key

# Cloudinary (sign up free at https://cloudinary.com)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

---

## 🚀 Running the Project

### Option A — Run each module separately (recommended for development)

Open three separate terminals:

```bash
# Terminal 1 — Blood Bank (port 8081)
cd blood-bank
mvn spring-boot:run

# Terminal 2 — Accident Report (port 8082)
cd accident-report
mvn spring-boot:run

# Terminal 3 — Drug Verification (port 8083)
cd drug-verification
mvn spring-boot:run
```

### Option B — Build all modules from root

```bash
# From the project root
mvn clean install -DskipTests

# Then run each module's JAR
java -jar blood-bank/target/blood-bank-1.0.0.jar
java -jar accident-report/target/accident-report-1.0.0.jar
java -jar drug-verification/target/drug-verification-1.0.0.jar
```

---

## 📖 API Documentation (Swagger UI)

Once each module is running, open in your browser:

| Module | Swagger URL |
|---|---|
| Blood Bank | http://localhost:8081/swagger-ui.html |
| Accident Report | http://localhost:8082/swagger-ui.html |
| Drug Verification | http://localhost:8083/swagger-ui.html |

---

## 🔐 Authentication

All protected endpoints require a JWT Bearer token.

**1. Register an account:**
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "fullName": "Dr. Ngozi Obi",
  "email": "ngozi@hospital.ng",
  "phone": "08012345678",
  "password": "SecurePass1",
  "role": "HOSPITAL_ADMIN",
  "state": "Lagos",
  "lga": "Surulere"
}
```

**2. Check your email for the 6-digit OTP, then verify:**
```http
POST /api/v1/auth/verify-email
{
  "email": "ngozi@hospital.ng",
  "otp": "482910"
}
```

**3. Login to get your JWT:**
```http
POST /api/v1/auth/login
{
  "email": "ngozi@hospital.ng",
  "password": "SecurePass1"
}
```
Response includes `accessToken`. Use it in all subsequent requests:
```http
Authorization: Bearer eyJhbGci...
```

---

## 🌱 Seed Data

The following accounts are pre-loaded (all passwords: `Admin@1234`):

| Email | Role | Purpose |
|---|---|---|
| admin@nigeriahealth.gov.ng | SUPER_ADMIN | Full platform access |
| admin@luth.edu.ng | HOSPITAL_ADMIN | LUTH Lagos hospital |
| admin@unilag.edu.ng | HOSPITAL_ADMIN | National Hospital Abuja |
| admin@unth.edu.ng | HOSPITAL_ADMIN | UNTH Enugu |
| donor@example.com | DONOR | Sample donor (O+) |

### Pre-loaded data includes:
- 3 approved hospitals with full blood stock
- 6 FRSC stations across 5 states
- 10 approved NAFDAC drugs including Coartem, Amoxil, Paracetamol

---

## 🔌 Key API Endpoints Quick Reference

### Blood Bank
```
POST   /api/v1/auth/register              Register any user
POST   /api/v1/auth/login                 Login → get JWT
GET    /api/v1/hospitals                  List all hospitals (public)
GET    /api/v1/blood-stock/search         Find blood by type+state+LGA (public)
POST   /api/v1/hospitals/register         Hospital self-registration
PUT    /api/v1/hospitals/{id}/stock       Update blood stock
POST   /api/v1/blood-requests             Create blood request
GET    /api/v1/blood-requests/match/{id}  Find matching hospitals
PUT    /api/v1/blood-requests/{id}/fulfill Fulfill a request
POST   /api/v1/donors/register            Register as donor
GET    /api/v1/donors/eligibility         Check donation eligibility
POST   /api/v1/donors/book-appointment    Book donation appointment
```

### Accident Report
```
POST   /api/v1/accidents/report           Report accident (public, multipart)
GET    /api/v1/accidents/track/{ref}      Track report by reference (public)
GET    /api/v1/accidents/{id}             Get report by ID
PUT    /api/v1/accidents/{id}/status      Update status (FRSC_OFFICER)
GET    /api/v1/accidents                  All reports in state (FRSC_OFFICER)
```

### Drug Verification
```
GET    /api/v1/drugs/verify/{nafdacNo}    Verify drug (public)
POST   /api/v1/drugs/report-counterfeit   Report counterfeit (public)
POST   /api/v1/drugs                      Register drug (PHARMACY_ADMIN)
PUT    /api/v1/drugs/admin/{id}/approve   Approve drug (SUPER_ADMIN)
GET    /api/v1/drugs/admin/pending        Pending drugs (SUPER_ADMIN)
GET    /api/v1/drugs                      Browse approved drugs
```

---

## ⏰ Scheduled Jobs

| Module | Job | Schedule | Description |
|---|---|---|---|
| Blood Bank | `escalateCriticalBloodRequests` | Every 30 mins | Re-alerts hospitals for unmet CRITICAL requests |
| Blood Bank | `notifyEligibleDonors` | Daily 8am | Emails donors eligible to donate (56-day rule) |
| Blood Bank | `expireOldBloodRequests` | Every hour | Marks 24hr-old open requests as EXPIRED |
| Accident | `escalateUnacknowledgedReports` | Every 10 mins | Re-alerts FRSC for serious/fatal reports unacknowledged 30+ mins |
| Drug | `detectCounterfeitHotspots` | Daily 7am | Detects LGAs with 3+ counterfeit verifications in 7 days |

---

## 🧪 Running Tests

```bash
# Run all tests from project root
mvn test

# Run tests for a specific module
cd blood-bank && mvn test
cd accident-report && mvn test
cd drug-verification && mvn test
```

Test classes:
- `BloodBankServiceImplTest` — donor eligibility, proximity matching, stock alerts
- `AccidentReportServiceImplTest` — FRSC alerts, FATAL escalation, status transitions
- `DrugVerificationServiceImplTest` — all 4 verification outcomes, counterfeit threshold

---

## 🚢 Deployment (Railway — Free Tier)

Railway is the easiest free hosting platform for Spring Boot + PostgreSQL.

**1. Install Railway CLI:**
```bash
npm install -g @railway/cli
railway login
```

**2. Create a new project and PostgreSQL database:**
```bash
railway init
railway add postgresql
```

**3. Set environment variables on Railway dashboard:**
Go to your service → Variables → add all variables from the `.env` section above.

**4. Deploy each module:**
```bash
# From the blood-bank directory
railway up
```

**5. Your API will be live at:**
`https://your-project-name.railway.app/api/v1`

---

## 📈 What to Build Next

Once this platform is running, here are the natural next steps:

1. **API Gateway** — add Spring Cloud Gateway to route all 3 modules through a single entry point (port 8080)
2. **Real Cloudinary Integration** — replace the placeholder photo URLs in the accident controller with actual `CloudinaryService.upload()` calls
3. **WebSocket/SSE** — add real-time accident status updates using Spring WebSocket so reporters see live updates without refreshing
4. **Admin Dashboard** — build a React frontend showing blood stock maps, accident heatmaps, and counterfeit drug hotspot maps by state
5. **NIN Verification** — integrate NIMC's NIN lookup API to verify donor and hospital identities during registration
6. **Mobile App** — build the React Native companion app so citizens can report accidents and verify drugs from their phones

---

## 🤝 Modules Connection Flow

```
ACCIDENT REPORTED
      ↓
accident-report module fires
      ↓
Nearest FRSC station → Email + SMS
      ↓
If casualties > 0 → Notify nearest hospital with blood
      ↓
If blood type specified → blood-bank module creates CRITICAL BloodRequest
      ↓
All hospitals in same state with matching blood → Alerted immediately
      ↓
If drug needed on scene → drug-verification module → GENUINE confirmed
```

---

*Built to serve Nigeria. Every line of code is a step toward saving a life.* 🇳🇬

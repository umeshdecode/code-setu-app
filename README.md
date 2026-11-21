# CargoSetu App - Packers & Movers ERP Management System

CargoSetu is an enterprise-grade ERP management system built for packers and movers. It provides robust capabilities for company assets, branches, customer shifting processes, document uploads, real-time communication, and automated event-driven notifications.

---

## 🚀 Core Features & Architecture

### 1. Decoupled Event-Driven Notification Engine
The application implements a production-grade, asynchronous, transaction-bound Notification Engine to handle communications (Email, SMS, Push Notifications) without impacting core business transaction response times.

* **Publisher-Subscriber Architecture:** Core services (e.g. `StaffService`, `CustomerService`) do not call notification APIs directly. They simply publish immutable Spring Application Events (Java records).
* **Asynchronous Thread Pool:** Handled via custom executor (`notificationTaskExecutor`) using Spring's `@Async` AOP proxy to immediately delegate tasks to background worker threads, releasing Tomcat HTTP threads fast.
* **Transaction Bound Execution:** Using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` ensures that notifications are sent **only** after database transactions successfully commit, preventing race conditions or spamming users on transaction rollbacks.
* **Transient Failure Retries:** Integrated with Spring Retry. If an external API is down, it retries up to 3 times with **exponential backoff** (delay starting at 1s, doubling each time) and gracefully falls back to a `@Recover` method on exhaustion.
* **Channel-Specific Auditing:** Audit histories are written to dedicated tables (`email_notification_audit`, `sms_notification_audit`, `push_notification_audit`) representing status, retry counts, payloads (subject, body), timestamps, and gateway message IDs.
* **Mock Fallback Toggles:** Safe mock settings exist for both Email and SMS to prevent application startup errors or local testing failures if third-party credentials are not configured.

---

## 🛠️ Technology Stack
* **Language/Toolchain:** Java 17, Gradle 8.x
* **Core Framework:** Spring Boot 3.5.9
* **Security & Auth:** Spring Security (Stateless JWT and OAuth2 integration)
* **Database & Migration:** MySQL 8.0, Spring Data JPA, Liquibase
* **Real-time Messaging:** Spring Kafka, WebSockets
* **Document Processing:** Flying Saucer, OpenHTMLtoPDF (HTML to PDF rendering), Thymeleaf
* **External Services:** JavaMailSender (SMTP), Twilio SMS Java SDK

---

## 📁 Package Layout
```
com.cargosetu
├── CargoSetuAppApplication.java         (Application Entrypoint)
├── abstracts/                           (Mapped superclass models)
├── config/                              (Security, JWT, WebSockets configurations)
├── controller/                          (REST API controllers - Auth, Company, Staff, Driver, templates)
├── customer/                            (Customer shifting, room details, and cargo inventory logic)
├── document/                            (Master documents and upload entities)
├── dto/ & mapper/                       (Jackson DTOs and MapStruct mapper interfaces)
├── model/ & repo/                       (JPA entities and repository layer)
├── service/                             (Core business service implementations)
└── notification/                        (Notification Engine)
    ├── config/                          (Async thread pools & Twilio SDK config properties)
    ├── event/                           (NotificationEvent records: Registration, Orders, Staff, Customer)
    ├── infrastructure/provider/         (SmtpEmailProvider and TwilioSmsProvider)
    ├── listener/                        (NotificationEventListener post-commit hooks)
    ├── model/                           (Email, SMS, and Push notification audit entities)
    └── service/                         (Orchestrated dispatcher & audit logging services)
```

---

## ⚙️ Setup & Configuration

Configure your local database and credentials in [application-local.properties](file:///d:/cargo%20project/code-setu-app/src/main/resources/application-local.properties) or [application.properties](file:///d:/cargo%20project/code-setu-app/src/main/resources/application.properties):

```properties
# Server port
server.port=31201

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/cargosetuapp?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false
spring.datasource.username=root
spring.datasource.password=root

# SMTP Email configuration (Google Example)
app.notification.email.mock=false
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-gmail-16-digit-app-password
app.notification.email.from=your-email@gmail.com

# Twilio SMS configuration
app.notification.twilio.mock=false
app.notification.twilio.account-sid=your-twilio-account-sid
app.notification.twilio.auth-token=your-twilio-auth-token
app.notification.twilio.from-number=your-twilio-phone-number
```

---

## 🧪 Testing the API & Notifications

You can test the notification engine flows and transaction bindings using the following REST endpoints:

### 1. Trigger User Registration (Email & SMS)
Simulates a business transaction that commits and launches background notifications:
```bash
curl --location --request POST "http://localhost:31201/api/demo-notifications/register?email=subhp152@gmail.com&fullName=John%20Doe&username=johndoe"
```

### 2. Trigger Transaction Rollback
If `shouldRollback=true` is passed, the transaction throws an exception and rolls back. The system will **not** send any email or SMS:
```bash
curl --location --request POST "http://localhost:31201/api/demo-notifications/register?email=test@example.com&fullName=Rollback%20Test&username=rollback&shouldRollback=true"
```

### 3. Trigger Order Creation (Twilio SMS & Email)
Deliver notifications with automatic formatting (auto-sanitizes leading spaces and guarantees `+` prefix for Twilio):
```bash
curl --location --request POST "http://localhost:31201/api/demo-notifications/order?orderId=ORD-9999&email=customer@example.com&phone=%2B919876543210&name=John&amount=100.00"
```

### 4. Fetch Execution Audits
Verify statuses (`SENT`, `FAILED`, `PENDING`), retry counts, timestamps, and compiled bodies from the database:
* **Email Logs:** `GET http://localhost:31201/api/demo-notifications/email-logs`
* **SMS Logs:** `GET http://localhost:31201/api/demo-notifications/sms-logs`
* **Push Logs:** `GET http://localhost:31201/api/demo-notifications/push-logs`
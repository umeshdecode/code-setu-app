# Notification Engine Module (`com.cargosetu.notification`)

> Cross-cutting, asynchronous, transaction-bound communication layer (Email / SMS / WhatsApp / Push) used by
> the rest of the CargoSetu ERP backend. Core business services never call a mail/SMS API directly — they
> publish a `GenericNotificationEvent` and this module handles templating, auditing, dispatch, and retries.

All claims below are verified by direct reading of the source files listed in each section (paths given as
`src/main/java/com/cargosetu/...` relative to `D:\cargo project\code-setu-app`). Anywhere this document
disagrees with `README.md` or `docs/services-and-known-issues.md`, that is called out explicitly under
**Discrepancies with existing docs** at the end — this module's actual `WhatsAppChannelSender` implementation
does **not** match what those two documents describe.

---

## 1. Overview

The Notification Engine is a decoupled, event-driven engine built on:

- **Spring Application Events** (`ApplicationEventPublisher.publishEvent(...)`) as the decoupling boundary
  between business services and notification dispatch.
- **`@TransactionalEventListener(phase = AFTER_COMMIT)`** so notifications only fire once the originating
  DB transaction has actually committed (no notifications on rollback).
- **`@Async`** on a dedicated thread pool (`notificationTaskExecutor`) so the HTTP/business thread is never
  blocked waiting on SMTP/Twilio/etc.
- A **strategy pattern** of `NotificationChannelSender` implementations, one per `NotificationChannel`
  (`EMAIL`, `SMS`, `WHATSAPP`, `PUSH`), selected at dispatch time via `.supports(channel)`.
- **Spring Retry** (`@Retryable`/`@Recover`) on `EmailChannelSender` and `SmsChannelSender` only (see §7 —
  `WhatsAppChannelSender` has no retry logic at all in the current source).
- **DB-backed audit trail** (`notification_audit` table) and **DB-backed templates**
  (`notification_template` table), seeded on boot by `NotificationDatabaseInitializer`.

Package layout (as found on disk):

```
notification/
├── config/          AsyncThreadPoolConfig, NotificationProperties, TwilioConfig, NotificationDatabaseInitializer
├── controller/       DemoNotificationController, NotificationAdminController
├── event/           NotificationEvent (marker interface), GenericNotificationEvent (record)
├── infrastructure/
│   └── channel/     NotificationChannelSender (interface), EmailChannelSender, SmsChannelSender, WhatsAppChannelSender
├── listener/        GenericNotificationEventListener
├── model/           NotificationChannel, NotificationStatus, NotificationAuditBase, NotificationAudit, NotificationTemplate
├── repository/      NotificationAuditRepository, NotificationTemplateRepository
└── service/         DemoBusinessService, NotificationProcessingService(Impl), TemplateRenderingService(Impl)
```

Note: there is no `PushChannelSender` class anywhere in the codebase. `NotificationChannel.PUSH` is a valid
enum value and has a dedicated audit-log endpoint (`/api/demo-notifications/push-logs`) and DB rows are
possible via `NotificationAdminController`'s `/send-test`, but nothing ever actually delivers a push
notification — see §7.

---

## 2. API Controllers & Endpoints

### 2.1 `NotificationAdminController` (`src/main/java/com/cargosetu/notification/controller/NotificationAdminController.java`)

Base path: `/api/admin/notifications`. Not present in `SecurityConfig`'s `permitAll()` list, so it falls
under `.anyRequest().authenticated()` — a valid JWT is required, but there is **no `@PreAuthorize`/role
check** on any method in this controller (verified: no `@PreAuthorize`, `hasRole`, or `hasAuthority`
annotation anywhere in `notification/controller/`). Any authenticated user can create/update templates and
send arbitrary test notifications.

| Method | Path | Request | Response | Notes |
|---|---|---|---|---|
| GET | `/api/admin/notifications/templates` | — | `List<NotificationTemplate>` | `templateRepository.findAll()` |
| POST | `/api/admin/notifications/templates` | `NotificationTemplate` body | `NotificationTemplate` (saved) | No validation of `templateCode`/`channel` uniqueness at the controller level (relies on DB `UniqueConstraint`) |
| PUT | `/api/admin/notifications/templates/{id}` | `NotificationTemplate` body | `NotificationTemplate` (updated) or 404 | Only mutates `subjectTemplate`, `bodyTemplate`, `isActive` — `templateCode`/`channel` are not updatable via this endpoint |
| GET | `/api/admin/notifications/audits` | — | `List<NotificationAudit>` | `auditRepository.findAll()`, unpaginated |
| POST | `/api/admin/notifications/send-test` | Query params `templateCode`, `recipient`, `channel` (enum) + JSON body `Map<String,Object>` variables | `200 OK` string incl. generated `eventId` | Builds a `GenericNotificationEvent` with a single recipient and publishes it via `ApplicationEventPublisher` — exercises the exact same async pipeline as real business events |

### 2.2 `DemoNotificationController` (`src/main/java/com/cargosetu/notification/controller/DemoNotificationController.java`)

Base path: `/api/demo-notifications`. **This entire controller is fully public/unauthenticated** —
`SecurityConfig.java` line 78 explicitly lists `"/api/demo-notifications/**"` in the `permitAll()`
matcher list (`src/main/java/com/cargosetu/config/SecurityConfig.java`, lines 66–85). No JWT, no API key,
no rate limiting.

| Method | Path | Params | Response | Notes |
|---|---|---|---|---|
| POST | `/api/demo-notifications/register` | `email`, `fullName`, `username`, `shouldRollback` (default `false`) | 200 OK / 400 on simulated rollback | Calls `DemoBusinessService.registerUser(...)`; if `shouldRollback=true` the `@Transactional` method throws after publishing the event, demonstrating `AFTER_COMMIT` semantics — no notification is actually sent because the transaction never commits |
| POST | `/api/demo-notifications/order` | `orderId`, `email`, `phone`, `name`, `amount` (double) | 200 OK | Calls `DemoBusinessService.createOrder(...)`; publishes `BOOKING_CREATED` to EMAIL and/or SMS depending on which of `email`/`phone` is non-blank |
| GET | `/api/demo-notifications/email-logs` | — | `List<NotificationAudit>` filtered to `channel == EMAIL` | In-memory filter over `auditRepository.findAll()` (not a DB-level query) |
| GET | `/api/demo-notifications/sms-logs` | — | `List<NotificationAudit>` filtered to `channel == SMS` | Same in-memory filter pattern |
| GET | `/api/demo-notifications/push-logs` | — | `List<NotificationAudit>` filtered to `channel == PUSH` | Will always be empty in practice since nothing in this module publishes a PUSH-channel event and no sender exists for PUSH |

Because this controller is public and unauthenticated, and its `/email-logs`, `/sms-logs`, `/push-logs`
endpoints return raw `NotificationAudit` rows (recipient email/phone, rendered message bodies, failure
reasons) to anyone with network access, it exposes PII/audit data with no access control — documenting the
fact per instructions, not editorializing further.

---

## 3. Business / Service Logic — full event flow

Step-by-step, verified against source:

1. **A business service publishes an event.** Any Spring bean with an injected `ApplicationEventPublisher`
   calls `eventPublisher.publishEvent(new GenericNotificationEvent(templateCode, recipients, channel,
   variables))`. The 4-arg constructor (`GenericNotificationEvent.java` lines 19–21) auto-generates
   `eventId = UUID.randomUUID()` and `timestamp = Instant.now()`.

2. **`GenericNotificationEventListener.handleGenericNotificationEvent`**
   (`notification/listener/GenericNotificationEventListener.java`) is annotated:
   ```java
   @Async("notificationTaskExecutor")
   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
   ```
   - `AFTER_COMMIT` means the listener only runs if/when the surrounding `@Transactional` method commits
     successfully. If there is no active transaction, `fallbackExecution = true` makes it run anyway
     (immediately, synchronously w.r.t. transaction semantics — still dispatched onto the async executor
     thread).
   - `@Async("notificationTaskExecutor")` moves execution onto the custom thread pool (see §8), returning
     control to the caller immediately.
   - The method body wraps `processingService.processEvent(event)` in a try/catch that only logs — an
     exception here does not propagate anywhere (see §7).

3. **`NotificationProcessingServiceImpl.processEvent`**
   (`notification/service/NotificationProcessingServiceImpl.java`):
   - **Template lookup**: `templateRepository.findByTemplateCodeAndChannelAndIsActiveTrue(event.templateCode(),
     event.channel())`. If no active template matches, logs an error and returns — **no audit row is
     created at all** in this case (silent drop, no failure record).
   - **Rendering**: `TemplateRenderingServiceImpl.render(...)` is called twice (once for subject, once for
     body) via `{{placeholder}}` substitution (see §9).
   - **Per-recipient loop**: for each entry in `event.recipients()`:
     - Skips null/blank recipients.
     - Trims whitespace.
     - If `channel == SMS` and the trimmed value doesn't start with `+`, prepends `+` (E.164-style
       normalization) — **this normalization is SMS-only**; it is not applied to EMAIL, WHATSAPP, or PUSH
       recipients.
     - Creates a `NotificationAudit` row with `status = PENDING`, `retryCount = 0`, and saves it
       (`auditRepository.save(audit)`) — this is the "PENDING audit" referenced by the module's design.
     - Looks up a sender: `channelSenders.stream().filter(s -> s.supports(event.channel())).findFirst()`.
       `channelSenders` is `List<NotificationChannelSender>`, auto-populated by Spring with every
       `@Service`-annotated implementation of the interface (`EmailChannelSender`, `SmsChannelSender`,
       `WhatsAppChannelSender` — 3 beans for 4 enum values; `PUSH` matches none of them).
       - If no sender supports the channel, the audit is immediately marked `FAILED` with
         `failureReason = "No sender found supporting channel: " + channel` and saved. This is the actual
         current behavior for `NotificationChannel.PUSH`.
       - Otherwise, `sender.send(audit)` is called inside a try/catch that only logs on exception (does not
         rethrow, does not touch the audit record itself — the sender's own `@Recover` method, where
         present, is responsible for finalizing the audit on terminal failure).

4. **Channel sender dispatch** — see §7 (retry) and §9 (external integrations) for exact per-channel
   behavior of `EmailChannelSender`, `SmsChannelSender`, and `WhatsAppChannelSender`.

### 3.1 `DemoBusinessService` (`notification/service/DemoBusinessService.java`)

Exists purely to exercise the pipeline end-to-end from the public demo controller:

- `registerUser(email, fullName, username, shouldRollback)` — `@Transactional`. Publishes one
  `GenericNotificationEvent("CUSTOMER_CREATED", [email], EMAIL, variables)` with hardcoded demo variables
  (`customerId=101L`, `mobileNo="+19999999999"`), then optionally throws `RuntimeException` to force a
  rollback and prove `AFTER_COMMIT` suppresses the notification.
- `createOrder(orderId, customerEmail, customerPhone, customerName, amount)` — `@Transactional`. Publishes
  up to two events using template code `"BOOKING_CREATED"`: one to `EMAIL` if `customerEmail` is non-blank,
  one to `SMS` if `customerPhone` is non-blank.

---

## 4. DTOs / Request-Response Models

The module has no dedicated request/response DTO classes under `notification/dto` (no such package exists).
Controllers use the JPA entities directly as request/response bodies, plus ad-hoc `Map<String,Object>` for
template variables:

| Type | Used by | Shape |
|---|---|---|
| `NotificationTemplate` (entity, §5) | `NotificationAdminController` POST/PUT `/templates` | Full entity round-tripped as request and response body |
| `NotificationAudit` (entity, §5) | `NotificationAdminController` GET `/audits`, `DemoNotificationController` GET `*-logs` | Full entity returned directly — no projection/DTO layer, no pagination |
| `Map<String,Object>` variables | `NotificationAdminController.sendTestNotification` request body; `GenericNotificationEvent.variables()` | Arbitrary key→value map consumed by `{{key}}` placeholder substitution |
| `GenericNotificationEvent` (record, §6) | Internal — constructed by every publisher | `(UUID eventId, String templateCode, List<String> recipients, NotificationChannel channel, Map<String,Object> variables, Instant timestamp)` |

There is no request wrapper validating `NotificationTemplate` input (no `@Valid`, no Bean Validation
annotations on the entity fields) — a POST/PUT with a blank `bodyTemplate` will hit the DB's `NOT NULL`
constraint and surface as a raw persistence exception rather than a clean 400.

---

## 5. Entities & Database Relationships

### 5.1 `NotificationTemplate` — table `notification_template`
(`notification/model/NotificationTemplate.java`, extends `AbstractModel`)

| Column | Java field | Type/Annotation | Notes |
|---|---|---|---|
| `id` | `id` | `Long`, `@Id @GeneratedValue(IDENTITY)` | |
| `template_code` | `templateCode` | `String`, `nullable=false` | e.g. `"CUSTOMER_CREATED"` |
| `channel` | `channel` | `NotificationChannel` enum, `STRING`, `nullable=false` | |
| `subject_template` | `subjectTemplate` | `String`, nullable | Only meaningful for EMAIL |
| `body_template` | `bodyTemplate` | `String`, `@Lob`, `LONGTEXT`, `nullable=false` | |
| `is_active` | `isActive` | `Boolean`, `nullable=false`, default `true` | |

Unique constraint: `@UniqueConstraint(columnNames = {"template_code", "channel"})` — one active/inactive
template per (code, channel) pair. No foreign keys; standalone table.

### 5.2 `NotificationAudit` — table `notification_audit`
(`notification/model/NotificationAudit.java`, extends `AbstractModel`; does **not** extend
`NotificationAuditBase`)

| Column | Java field | Type/Annotation | Notes |
|---|---|---|---|
| `id` | `id` | `Long`, `@Id @GeneratedValue(IDENTITY)` | |
| `event_id` | `eventId` | `UUID`, `nullable=false` | Correlates all audit rows spawned by one `GenericNotificationEvent` (one row per recipient) |
| `recipient` | `recipient` | `String`, `nullable=false` | Post-normalization value (trimmed, `+`-prefixed for SMS) |
| `channel` | `channel` | `NotificationChannel` enum, `STRING`, `nullable=false` | |
| `template_code` | `templateCode` | `String`, `nullable=false` | |
| `subject` | `subject` | `String`, nullable | Rendered subject (EMAIL only in practice) |
| `message_body` | `messageBody` | `String`, `@Lob`, `LONGTEXT`, `nullable=false` | Rendered body |
| `status` | `status` | `NotificationStatus` enum, `STRING`, `nullable=false` | `PENDING → PROCESSING/RETRYING → SENT` or `FAILED` |
| `retry_count` | `retryCount` | `Integer`, `nullable=false`, default `0` | Incremented by the sender itself, not the processing service |
| `failure_reason` | `failureReason` | `String`, `@Lob`, `LONGTEXT`, nullable | Set only by `@Recover` methods or the "no sender found" path |
| `sent_at` | `sentAt` | `LocalDateTime`, nullable | Set on success |
| `delivered_at` | `deliveredAt` | `LocalDateTime`, nullable | Set to the same instant as `sentAt` on success — there is no separate delivery-receipt/webhook flow updating this later |

No foreign keys to any other module's tables (e.g. no FK to `Customer`/`Staff`) — correlation to a business
record is only possible via the free-text `recipient` value and whatever context is embedded in
`message_body`.

### 5.3 `NotificationAuditBase` — `@MappedSuperclass`, not currently used by any entity
(`notification/model/NotificationAuditBase.java`, extends `AbstractModel`)

This is an abstract mapped-superclass with fields `id`, `eventCode`, `referenceId`, `status`, `sentAt`,
`deliveredAt`, `retryCount`, `failureReason` (`@Lob`), `providerResponse` (`@Lob`), `providerName`. **No
concrete entity in the codebase extends it** (verified: `NotificationAudit` extends `AbstractModel`
directly, not `NotificationAuditBase`). It appears to be a superseded/leftover design for a per-channel
audit split (`eventCode`/`referenceId`/`providerName`/`providerResponse` suggest a richer, provider-aware
audit model that was not carried through to `NotificationAudit`). Treat as dead code unless another module
is found extending it.

### 5.4 `AbstractModel` (shared base, `src/main/java/com/cargosetu/abstracts/AbstractModel.java`)

Both entities inherit: `isActive`, `isDeleted`, `createdBy`, `createdByName`, `createdAt` (auto-set via
`@PrePersist`), `updatedByName`, `updatedById`, `updatedAt` (declared but never set anywhere in
`AbstractModel` — no `@PreUpdate` hook exists, so `updated_at` stays `NULL` after edits unless a caller sets
it manually; `NotificationAdminController.updateTemplate` does not set it).

### 5.5 Supporting enums

- `NotificationChannel` (`notification/model/NotificationChannel.java`): `EMAIL`, `SMS`, `PUSH`, `WHATSAPP`.
- `NotificationStatus` (`notification/model/NotificationStatus.java`): `PENDING`, `PROCESSING`, `SENT`,
  `DELIVERED`, `FAILED`, `RETRYING`. Note `DELIVERED` is a defined enum value but is never assigned anywhere
  in the module's code — success always sets `SENT` (with `deliveredAt` timestamp populated at the same
  time), never the `DELIVERED` status itself.

---

## 6. Repositories

### `NotificationAuditRepository` (`notification/repository/NotificationAuditRepository.java`)
`extends JpaRepository<NotificationAudit, Long>`

| Method | Purpose |
|---|---|
| `findByEventId(UUID eventId)` → `List<NotificationAudit>` | All per-recipient audit rows for one published event |
| `findByEventIdAndRecipientAndChannel(UUID, String, NotificationChannel)` → `Optional<NotificationAudit>` | Look up a specific recipient's audit row for a specific event/channel — declared but not called anywhere else in the module's own code (verified via read of every file listed in the task); likely intended for future correlation/dedup logic |

### `NotificationTemplateRepository` (`notification/repository/NotificationTemplateRepository.java`)
`extends JpaRepository<NotificationTemplate, Long>`

| Method | Purpose |
|---|---|
| `findByTemplateCodeAndChannel(String, NotificationChannel)` → `Optional<NotificationTemplate>` | Used by `NotificationDatabaseInitializer` to avoid re-seeding duplicate templates on every boot |
| `findByTemplateCodeAndChannelAndIsActiveTrue(String, NotificationChannel)` → `Optional<NotificationTemplate>` | Used by `NotificationProcessingServiceImpl` at dispatch time — an inactive template (`is_active=false`) is treated identically to a missing template (silent no-op, no audit row) |

Neither repository defines pagination (`Pageable`) methods; both admin/demo "get all" endpoints load full
tables into memory (`findAll()`).

---

## 7. Exception Handling & Retry Flow

### 7.1 `EmailChannelSender` and `SmsChannelSender` — identical retry shape

Both (`notification/infrastructure/channel/EmailChannelSender.java`,
`notification/infrastructure/channel/SmsChannelSender.java`) annotate their `send(NotificationAudit audit)`
method identically:

```java
@Retryable(
    retryFor = { Exception.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
)
```

- **Attempts**: up to 3 total attempts (1 initial + 2 retries) — any `Exception` (not just
  `NotificationDeliveryException`) triggers a retry, since `retryFor = Exception.class`.
- **Backoff**: exponential, `delay=1000`ms initial, `multiplier=2.0` → attempt delays are ~1000ms, then
  ~2000ms, capped by `maxDelay=10000`ms (never actually reached at only 3 attempts).
- **Per-attempt bookkeeping**: at the top of every attempt (including retries — Spring Retry re-invokes the
  whole method body each time), the code does `audit.setRetryCount(audit.getRetryCount() + 1)`, sets status
  to `PROCESSING` on the first attempt or `RETRYING` on subsequent attempts, and saves the audit — so the
  DB row visibly transitions `PENDING → PROCESSING → RETRYING → RETRYING → ...` as retries happen, readable
  live via `GET /api/admin/notifications/audits` or the demo `-logs` endpoints.
  - Both senders also contain a **test-only failure-simulation hook**: if `recipient` contains the literal
    substring `"permanent-fail"`, they always throw `NotificationDeliveryException` (exhausting all 3
    attempts); if it contains `"transient-fail"` and `currentAttempt < 3`, they throw on attempts 1–2 and
    succeed on attempt 3. This is dead-simple manual chaos-testing baked into production code paths, keyed
    off the recipient string.
  - On success: `status = SENT`, `sentAt` and `deliveredAt` both set to `LocalDateTime.now()`, saved.
- **`@Recover` method** (`recover(Exception e, NotificationAudit audit)`), invoked automatically by Spring
  Retry once all 3 attempts are exhausted (the recover method's parameter list must match — `Exception`
  first, then the original argument, which Spring Retry satisfies here): sets
  `audit.setStatus(NotificationStatus.FAILED)`, `audit.setFailureReason(e.getMessage())`, and saves. This is
  the **only** place a `NotificationAudit` transitions to `FAILED` due to an actual send exception (as
  opposed to the "no sender found" path in `NotificationProcessingServiceImpl`).
- Back in `NotificationProcessingServiceImpl`, the call `sender.send(audit)` is wrapped in a try/catch that
  only logs — but by the time an exception could reach that catch block, `@Recover` has already run and the
  audit is already `FAILED` in the DB, so that outer catch is effectively unreachable for these two senders
  under normal Spring AOP proxying (it would only fire if `@Recover` itself threw).

### 7.2 `WhatsAppChannelSender` — no retry, no audit persistence at all

`notification/infrastructure/channel/WhatsAppChannelSender.java` (30 lines, full contents relevant):

```java
@Override
public void send(NotificationAudit audit) throws Exception {
    String recipient = audit.getRecipient();
    int currentAttempt = audit.getRetryCount();
    log.info("[WhatsApp Channel] Sending WhatsApp message to: {} (Attempt #{})", recipient, currentAttempt);
    // Mock WhatsApp sending
    log.info("[MOCK WHATSAPP] Dispatching WhatsApp to: {}. Message: '{}'", recipient, audit.getMessageBody());
    Thread.sleep(250); // Simulate network latency
    log.info("[WhatsApp Channel] WhatsApp message sent successfully to: {}", recipient);
}
```

Verified facts:
- **No `@Retryable`/`@Recover` annotations** — there is no retry behavior whatsoever for WhatsApp.
- **No Twilio SDK call** — it is a hardcoded mock (`Thread.sleep(250)` + log lines only), regardless of any
  `app.notification.*` mock flag. There is no `NotificationAuditRepository` field injected into this class
  at all, so:
  - It **never updates the audit's `status`** to `SENT` — a WhatsApp `NotificationAudit` row is created as
    `PENDING` by `NotificationProcessingServiceImpl` and is **never updated again** by this sender, even on
    the "success" path. It permanently reads `PENDING` in the DB and via any audit-listing endpoint.
  - It never sets `sentAt`/`deliveredAt`/`failureReason`.
  - Because `Thread.sleep` can throw `InterruptedException` (declared via `throws Exception`), a failure
    here would propagate to `NotificationProcessingServiceImpl`'s catch block (log-only) with the audit
    left at `PENDING` and `retryCount` unchanged (nothing increments it, since `currentAttempt` is only read,
    never written back).
- **No phone-number normalization logic of any kind** exists in this class — no `+` prefix handling, no
  `whatsapp:` URI-scheme prefixing, no digit-stripping. The only recipient normalization that runs before
  this sender ever sees the audit is the SMS-only `+`-prefix logic in
  `NotificationProcessingServiceImpl` (§3), which does not apply to `NotificationChannel.WHATSAPP`.

**This directly contradicts the module's own `README.md` architecture-overview claims and
`docs/services-and-known-issues.md`** (see §12, Discrepancies). Any other module planning to correlate an
inbound WhatsApp webhook by phone-number shape should **not** assume a `whatsapp:+<digits>` format is
produced anywhere in this codebase today — it isn't.

### 7.3 Listener-level exception isolation

`GenericNotificationEventListener.handleGenericNotificationEvent` wraps the entire
`processingService.processEvent(event)` call in try/catch that only logs
(`notification/listener/GenericNotificationEventListener.java`, lines 30–34). Combined with `@Async`, this
means **no exception from anywhere in the notification pipeline can ever propagate back to the publishing
business transaction** — publishing an event is fire-and-forget from the caller's perspective by
construction, independent of the `@Recover` behavior described above.

### 7.4 `NotificationDeliveryException`
(`src/main/java/com/cargosetu/exception/NotificationDeliveryException.java`) — a plain `RuntimeException`
subclass with `(String message)` and `(String message, Throwable cause)` constructors, used by
`EmailChannelSender` and `SmsChannelSender` to wrap SMTP/Twilio client errors before letting `@Retryable`
catch them (since `retryFor = Exception.class`, wrapping isn't strictly required for retry to trigger, but
it normalizes the failure-reason text stored on the audit).

---

## 8. Configuration

### 8.1 `app.notification.*` properties found in source

| Property | File(s) | Value found | Consumed by |
|---|---|---|---|
| `app.notification.thread-pool.core-pool-size` | `application.properties` | `10` | `NotificationProperties.ThreadPool.corePoolSize` (default `10` if absent) |
| `app.notification.thread-pool.max-pool-size` | `application.properties` | `20` | `NotificationProperties.ThreadPool.maxPoolSize` (default `20`) |
| `app.notification.thread-pool.queue-capacity` | `application.properties` | `2000` | `NotificationProperties.ThreadPool.queueCapacity` (default `2000`) |
| `app.notification.thread-pool.thread-name-prefix` | `application.properties` | `NotifWorker-` | `NotificationProperties.ThreadPool.threadNamePrefix` (default `"NotifWorker-"`) |
| `app.notification.email.from` | `application.properties` | set to a real-looking Gmail address | `EmailChannelSender.fromEmail` (`@Value`) |
| `app.notification.email.mock` | `application.properties` | `false` | `EmailChannelSender.mockEnabled` (`@Value`) — when `false`, `EmailChannelSender` sends real SMTP mail via `JavaMailSender` |
| `app.notification.email.support` | `application.properties` (one line commented out with a placeholder `support@cargosetu.com`, active line uses the same Gmail address as `email.from`) | active value = same Gmail address | Consumed outside this module by `InquiryService` (support-alert recipient for `INQUIRY_CREATED_SUPPORT`) |
| `app.notification.twilio.account-sid` | `application.properties` | a live-looking Twilio Account SID | `TwilioConfig.accountSid` (`@Value`), used in `Twilio.init(accountSid, authToken)` |
| `app.notification.twilio.auth-token` | `application.properties` | a live-looking Twilio Auth Token | `TwilioConfig.authToken` (`@Value`) |
| `app.notification.twilio.from-number` | `application.properties` | a `+1` US number | `TwilioConfig`/`SmsChannelSender.fromNumber` (`@Value`) — used as the `From` number for real Twilio SMS sends |
| `app.notification.twilio.mock` | `application.properties` | `true` | `TwilioConfig.mockEnabled` and `SmsChannelSender.mockEnabled` (`@Value`) — currently `true`, so `Twilio.init(...)` is skipped in `TwilioConfig.initTwilio()` and `SmsChannelSender` mock-logs instead of calling the Twilio SDK |

`application-local.properties` and `application-prod.properties` contain **no** `app.notification.*`
overrides — only datasource/JPA settings — so the single `application.properties` file's values above apply
in every Spring profile (`local` and `prod` alike), since `spring.profiles.active` only selects the
datasource-related overlay file.

**Security note (flagging, not quoting):** `src/main/resources/application.properties` currently commits, in
plaintext, what appear to be a real Gmail account + app password (under `spring.mail.username` /
`spring.mail.password`, consumed by the `JavaMailSender` this module depends on) and a real-looking Twilio
Account SID + Auth Token + From-number (under `app.notification.twilio.*`), alongside other unrelated
plaintext secrets in the same file (a production MySQL password and a Google OAuth client secret). None of
the actual secret values are reproduced in this document. Because `app.notification.twilio.mock=true` today,
the Twilio credentials are not currently exercised for real sends, but `app.notification.email.mock=false`
means the committed Gmail credentials **are** live and used for real outbound SMTP mail. This is a
plaintext-secret-in-VCS risk regardless of whether the credits are also expired/rotated; recommend rotating
and moving to environment variables / a secrets manager. (Same file also holds a plaintext production
datasource password and Google OAuth client secret, outside this module's direct scope but worth noting
since it's the same file this module reads from.)

### 8.2 `NotificationProperties` bean (`notification/config/NotificationProperties.java`)

`@ConfigurationProperties(prefix = "app.notification")`, `@Data`, holds one nested `ThreadPool` static class
with the four fields/defaults listed in §8.1. This is the only structured `@ConfigurationProperties` bean in
the module — `email.*` and `twilio.*` properties are consumed field-by-field via `@Value` in the individual
sender/config classes rather than being folded into `NotificationProperties`.

### 8.3 `AsyncThreadPoolConfig` (`notification/config/AsyncThreadPoolConfig.java`)

`@Configuration @EnableAsync @EnableRetry`, implements `AsyncConfigurer`:

- Defines the `notificationTaskExecutor` bean (`@Bean(name = "notificationTaskExecutor")`,
  `ThreadPoolTaskExecutor`) wired from `NotificationProperties.ThreadPool` — core/max/queue/prefix as above.
- `RejectedExecutionHandler = ThreadPoolExecutor.CallerRunsPolicy()` — when the queue (capacity 2000) is
  full and max pool size (20) is saturated, the **calling thread** (i.e., whatever thread invoked the
  `@Async` method — in practice, a Spring internal proxy thread, not the original HTTP thread since the
  transactional-event dispatch already happens post-commit) executes the task itself instead of dropping it
  or throwing. This is a deliberate backpressure choice, per the inline comment.
- `waitForTasksToCompleteOnShutdown(true)`, `awaitTerminationSeconds(30)` — graceful shutdown drains the
  queue for up to 30s.
- `getAsyncUncaughtExceptionHandler()` — logs any exception escaping a `void`-returning `@Async` method that
  itself doesn't catch it. In practice this is superseded by the try/catch inside
  `GenericNotificationEventListener` (§7.3), which already prevents exceptions from escaping — this handler
  is effectively a safety net for any *other* future `@Async` method in the app that doesn't self-protect,
  or for exceptions thrown by the async infrastructure itself before the method body runs.
- `@EnableRetry` here is what activates the `@Retryable`/`@Recover` annotations used across the whole
  `notification` package (Spring Retry AOP proxying is enabled application-wide from this one
  `@Configuration` class).

### 8.4 `TwilioConfig` (`notification/config/TwilioConfig.java`)

`@PostConstruct initTwilio()`: if `app.notification.twilio.mock=true`, logs and returns without touching the
Twilio SDK. Otherwise, if `account-sid` is blank/null or still equal to the literal placeholder string
`"your-twilio-account-sid"`, logs a warning (does not fail startup); otherwise calls `Twilio.init(accountSid,
authToken)` once at application startup. This global `Twilio.init` is a static SDK call — it is what makes
`SmsChannelSender`'s direct `Message.creator(...)` calls work without passing credentials explicitly.

### 8.5 `NotificationDatabaseInitializer` (`notification/config/NotificationDatabaseInitializer.java`)

`implements CommandLineRunner` — runs once on every application boot. Defines 14 hardcoded default
`NotificationTemplate` rows (`CUSTOMER_CREATED`×2 [EMAIL+SMS], `CUSTOMER_UPDATED`, `INQUIRY_CREATED`,
`INQUIRY_CREATED_SUPPORT`, `INQUIRY_REPLY`, `STAFF_CREATED`×2, `VEHICLE_ASSIGNED`, `BOOKING_CREATED`×2,
`INVOICE_GENERATED`, `PAYMENT_RECEIVED`, `OTP_SMS`) and upserts them idempotently: for each, checks
`findByTemplateCodeAndChannel(...)` and only inserts if absent — existing rows (including any manually
edited via `NotificationAdminController`) are never overwritten on restart.

---

## 9. External Integrations

### 9.1 Email — `JavaMailSender` / SMTP

`EmailChannelSender` depends on Spring Boot's auto-configured `JavaMailSender` bean, itself configured via
standard `spring.mail.*` properties (`host=smtp.gmail.com`, `port=587`, `username`, `password`,
`smtp.auth=true`, `smtp.starttls.enable=true` — Gmail SMTP with an app password). On a real (non-mock) send,
it builds a `SimpleMailMessage` (plain text only — no HTML/MIME/attachments support in this sender) with
`from`, `to`, `subject`, `text` and calls `mailSender.send(message)`.

### 9.2 SMS — Twilio Programmable SMS SDK

`SmsChannelSender` calls the Twilio Java SDK directly and statically:
```java
com.twilio.rest.api.v2010.account.Message.creator(
    new com.twilio.type.PhoneNumber(recipient), // To
    new com.twilio.type.PhoneNumber(fromNumber), // From
    audit.getMessageBody()
).create();
```
Relies on the process-wide `Twilio.init(...)` performed once by `TwilioConfig` at startup — no per-call
credential passing. The `recipient` here has already been through the E.164 `+`-prefix normalization done
in `NotificationProcessingServiceImpl` (§3) before this class ever sees it; `SmsChannelSender` itself does no
further phone formatting.

### 9.3 WhatsApp — no external integration currently wired

As detailed in §7.2, `WhatsAppChannelSender` does not call the Twilio SDK (or any other WhatsApp Business
API) at all — it is a pure log-and-sleep mock. There is a `com.twilio` dependency on the classpath (used by
`SmsChannelSender`/`TwilioConfig`) which does support WhatsApp messaging via the same
`Message.creator(...)` API with `whatsapp:`-prefixed `PhoneNumber`s, but that code path is not implemented
in this module as it stands.

### 9.4 Push — no external integration, no sender

No FCM/APNs/web-push client, config, or sender class exists anywhere under `notification/**` for the `PUSH`
channel.

---

## 10. Important Business Rules & Validations

- **Mock-mode toggles** — properties ending in `.mock` short-circuit real external calls:
  | Property | Default (in `application.properties`) | Effect when `true` |
  |---|---|---|
  | `app.notification.email.mock` | `false` | `EmailChannelSender` logs `[MOCK EMAIL]` and sleeps 300ms instead of calling `JavaMailSender.send(...)` |
  | `app.notification.twilio.mock` | `true` | `TwilioConfig.initTwilio()` skips `Twilio.init(...)`; `SmsChannelSender` logs `[MOCK SMS]` and sleeps 200ms instead of calling the Twilio SDK |
  | `app.auth.otp.mock` (outside this module, in `application.properties`, consumed by `OtpService`) | `true` | Returns the generated OTP code in the API response body (`response.put("mockOtp", otpCode)`) in addition to publishing the real `OTP_SMS` notification event — not a notification-dispatch mock, but directly affects what the `OTP_SMS` `GenericNotificationEvent` flow is used to verify in local/dev testing |
  - `WhatsAppChannelSender` has **no mock flag** because it has no non-mock code path — it is unconditionally
    mock behavior (§7.2).
- **Transaction-bound delivery**: notifications are only dispatched after successful commit
  (`AFTER_COMMIT`); a rolled-back business transaction produces zero notification audit rows (demonstrated
  by `DemoBusinessService.registerUser`'s `shouldRollback` parameter).
- **SMS-only E.164 normalization**: recipients for `NotificationChannel.SMS` are force-prefixed with `+` if
  missing; this is the only recipient-format validation/correction anywhere in the pipeline (no email format
  validation, no phone format validation for WHATSAPP or PUSH).
- **Template must be active**: `findByTemplateCodeAndChannelAndIsActiveTrue` means setting
  `NotificationTemplate.isActive = false` (via the admin `PUT /templates/{id}` endpoint) silently disables
  that template/channel combination for all future events — matching events are dropped with only a log
  line, no audit row, no error surfaced to the publisher (which never sees a return value anyway, since
  publishing is fire-and-forget).
- **One audit row per recipient per event**, sharing the same `eventId` — multi-recipient events (not
  currently constructed anywhere in this module's own call sites, which all use single-element
  `List.of(recipient)`, but supported by the `GenericNotificationEvent.recipients()` `List<String>` shape)
  would produce N independent audit rows and N independent send/retry lifecycles.

---

## 11. Dependencies Between Modules

This module is a **downstream, cross-cutting dependency** — other modules depend on it by publishing
`GenericNotificationEvent`s; it does not call back into them. Verified `publishEvent(new
GenericNotificationEvent(...))` call sites outside the `notification` package itself (via repo-wide grep for
`publishEvent`):

| Publisher | File | Template code(s) | Channel(s) |
|---|---|---|---|
| `CustomerService` (customer-shifting) | `src/main/java/com/cargosetu/customer/service/CustomerService.java`, lines 90 & 98 | `CUSTOMER_CREATED` | EMAIL, SMS (one publish each, guarded by non-blank email/mobile) |
| `CustomerService` (customer-shifting) | same file, line 167 | `CUSTOMER_UPDATED` | EMAIL only |
| `InquiryService` (inquiry-lead-capture) | `src/main/java/com/cargosetu/service/InquiryService.java`, lines 79 & 89 | `INQUIRY_CREATED` (customer ack), `INQUIRY_CREATED_SUPPORT` (support alert) | EMAIL (both) |
| `StaffService` (staff-rbac / auth-security) | `src/main/java/com/cargosetu/service/StaffService.java`, lines 94 & 102 | `STAFF_CREATED` | EMAIL, SMS (guarded by non-blank email/mobile) |
| `OtpService` (auth-security) | `src/main/java/com/cargosetu/service/OtpService.java`, line 121 | `OTP_SMS` | SMS only |
| `DemoBusinessService` (this module) | `notification/service/DemoBusinessService.java`, lines 38, 65, 68 | `CUSTOMER_CREATED`, `BOOKING_CREATED` | EMAIL/SMS |
| `NotificationAdminController` (this module) | `notification/controller/NotificationAdminController.java`, line 80 | Arbitrary, caller-supplied via `/send-test` | Arbitrary, caller-supplied |

Every one of these publishers only needs a Spring `ApplicationEventPublisher` and the
`GenericNotificationEvent`/`NotificationChannel` types from this module — there is no other compile-time
coupling (no shared DTOs, no direct method calls into `notification.service.*` from other packages). The
implicit contract each publisher relies on is: (a) a matching **active** `NotificationTemplate` row must
already exist for the `templateCode`/`channel` pair used (seeded by `NotificationDatabaseInitializer` for
all the codes above except free-form `/send-test` calls), and (b) the publish must happen inside a
`@Transactional` method (or accept `fallbackExecution` semantics) for the "only notify after commit"
guarantee to apply.

---

## 12. Discrepancies with existing docs (flagged per task instructions)

Both `README.md`'s "Decoupled Event-Driven Notification Engine" section and
`docs/services-and-known-issues.md` (§4, "Notification engine") describe `WhatsAppChannelSender` as sending
via the Twilio SDK (`Message.creator(...).create()`) with phone numbers normalized to a `whatsapp:+<digits>`
shape, gated by `app.notification.twilio.mock`, and covered by the same
`@Retryable(maxAttempts=3, backoff=...)` / `@Recover` pattern as Email/SMS.

**Direct reading of `notification/infrastructure/channel/WhatsAppChannelSender.java` (reproduced in full in
§7.2) shows none of that is true of the current source**: no Twilio import, no `@Retryable`/`@Recover`
annotation, no phone-number normalization of any kind, and no dependency on `app.notification.twilio.mock`
or any mock flag — it is an unconditional log+sleep stub, and it never writes back to the audit repository
at all. This document's §7.2/§9.3 describe the verified, actual behavior; treat the README and
services-and-known-issues.md descriptions of WhatsApp as **stale/aspirational** until the code is updated to
match (or vice versa).

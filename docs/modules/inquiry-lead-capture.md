# Public Inquiry Module (Lead Capture — Precursor)

## Overview

This module is the **only existing "lead capture" mechanism in the codebase today**: a single, unauthenticated
public endpoint that lets a website visitor submit a name/email/phone/subject/message contact form. The
submission is persisted as a `CustomerInquiry` row, and two email notifications are fired (a customer
acknowledgment and an internal support alert) through the shared notification-engine event bus.

It is deliberately simple and has **no relationship to any other business entity** in the system — no tenant
(`companyId`), no link to `Customer`, `Shifting`, or any authenticated staff/company context. It exists purely
to capture raw contact-form leads from the public marketing site (`code-setu-app-ui`'s `contect-us` *(sic)*
page, per `docs/services-and-known-issues.md` §3) before any qualification, ownership, or pipeline concept is
applied to them.

A forward-looking design doc, [`../WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md`](../WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md),
describes a **planned, separate `Lead` entity/pipeline** (WhatsApp-inbound, AI-extracted, tenant-scoped,
staff-owned) that this module is the direct historical precursor to but does **not** currently share any code,
table, or entity with. See the *Forward-Looking Note* at the end of this document for exactly how the two
differ.

Source files read in full for this document:
- `src/main/java/com/cargosetu/model/CustomerInquiry.java`
- `src/main/java/com/cargosetu/model/InquiryStatus.java`
- `src/main/java/com/cargosetu/controller/InquiryController.java`
- `src/main/java/com/cargosetu/service/InquiryService.java`
- `src/main/java/com/cargosetu/dto/InquiryRequest.java`
- `src/main/java/com/cargosetu/utils/BaseURL.java`
- `src/main/java/com/cargosetu/repo/InquiryRepository.java`
- `src/main/java/com/cargosetu/config/SecurityConfig.java`
- `src/main/java/com/cargosetu/config/JwtAuthenticationFilter.java`
- `src/main/java/com/cargosetu/abstracts/AbstractModel.java`
- `src/main/java/com/cargosetu/config/dto/ApiResponseDto.java`
- `src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`, `handler/CustomValidationException.java`
- `src/main/java/com/cargosetu/notification/event/GenericNotificationEvent.java`
- `src/main/java/com/cargosetu/notification/model/NotificationChannel.java`
- `src/main/java/com/cargosetu/notification/listener/GenericNotificationEventListener.java`
- `src/main/java/com/cargosetu/notification/service/NotificationProcessingServiceImpl.java`
- `src/main/java/com/cargosetu/notification/infrastructure/channel/EmailChannelSender.java`
- `src/main/java/com/cargosetu/notification/config/NotificationDatabaseInitializer.java`
- `src/main/resources/application.properties`
- `src/main/java/com/cargosetu/config/utils/UrlConstants.java`, `controller/PublicPlanControlor.java` (for
  contrast with the other `/public/**` controller)
- Cross-checked against `docs/entities.md`, `docs/controllers.md`, `docs/api-urls.md`,
  `docs/services-and-known-issues.md`, `docs/WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md`

> **Correction to a pre-existing doc claim**: `docs/entities.md` correctly lists `InquiryStatus` as `NEW,
> CONTACTED, RESOLVED, CLOSED` and marks it "verified in `InquiryStatus.java`" — this document independently
> re-verified the same four values by reading `InquiryStatus.java` directly (reproduced below). No discrepancy
> was found in this pass; flagging only because the task brief noted an earlier version of that doc had this
> wrong.

---

## API Controllers & Endpoints

### `InquiryController` (`src/main/java/com/cargosetu/controller/InquiryController.java`)

Class-level mapping: `@RequestMapping(BaseURL.PUBLIC_PLAN + BaseURL.INQUERY)`.

From `BaseURL.java`:

| Constant | Value |
|---|---|
| `PUBLIC_PLAN` | `"public"` (**no leading slash**) |
| `INQUERY` | `"/api/inquiries"` |

Raw Java-string concatenation therefore produces the literal pattern `"public/api/inquiries"` — no leading
`/`. This looks like a bug at first glance, but it isn't one in practice: Spring's `PathPatternParser`
(`org.springframework.web.util.pattern.PathPatternParser`, verified in the `spring-web-6.2.15-sources.jar` used
by this project's Gradle build) has a method `initFullPathPattern(String pattern)` whose documented purpose is
exactly this — *"By default, prepend a leading slash if needed for non-empty patterns"* — implemented as
`return (StringUtils.hasLength(pattern) && !pattern.startsWith("/") ? "/" + pattern : pattern);`. This
normalization runs when Spring registers the mapping, so the pattern actually matched against incoming
requests is **`/public/api/inquiries`**, not the raw unprefixed string. (An incoming HTTP request path always
starts with `/` per URI syntax, so this normalization is necessary for the mapping to ever match anything at
all — confirming it's relied-upon behavior here, not an accident that happens to work.)

| Method | Full Path (as normalized by Spring) | Handler | Auth | Request Body | Response |
|---|---|---|---|---|---|
| POST | `/public/api/inquiries` | `InquiryController.submit` | None (public) | JSON `InquiryRequest` | `ApiResponseDto` |

```java
@PostMapping
public ApiResponseDto submit(@RequestBody InquiryRequest request) {
    return inquiryService.createInquiry(request);
}
```

Notes:
- No `@Valid` on the `@RequestBody` parameter — see *Important Business Rules & Validations* below.
- Only one HTTP method/route exists in this controller — there is no GET/list, update, or admin-side endpoint
  for inquiries anywhere else in the codebase (confirmed by grep — `InquiryController` is the only class
  referencing `InquiryService`/`InquiryRepository`).
- Response is the shared `ApiResponseDto` envelope (see DTOs section), not the `GenericDataDTO` envelope used
  elsewhere (see `docs/services-and-known-issues.md` §2 on the two parallel envelopes).

---

## Business/Service Logic

### `InquiryService.createInquiry(InquiryRequest request)` (`src/main/java/com/cargosetu/service/InquiryService.java`)

Step by step, exactly as implemented:

1. Logs the incoming request's email + subject.
2. Generates an inquiry number: `"INC-" + yyyyMMdd + "-" + <first 8 chars of a random UUID, uppercased>`
   (`generateInquiryNumber()`), e.g. `INC-20260809-A1B2C3D4`. This is **not** guaranteed globally unique (no DB
   uniqueness constraint on `inquiryNumber`, no retry-on-collision logic) — collision probability is
   negligible in practice but not structurally prevented.
3. Builds a new `CustomerInquiry` and copies `name`, `email`, `phone`, `subject`, `message` straight from the
   request with **no transformation, trimming, or normalization**.
4. **Defaults applied by the service** (not client-controllable):
   - `status` → hardcoded to `InquiryStatus.NEW`
   - `source` → hardcoded to the literal string `"WEBSITE"`
   - `createdAt` → `LocalDateTime.now()` (set explicitly here, in addition to `AbstractModel`'s own
     `@PrePersist` that also sets `createdAt` — redundant but harmless since both run before insert with
     effectively the same timestamp)
5. Saves via `InquiryRepository.save(inquiry)`.
6. Builds a `Map<String,Object> variables` for template rendering: `inquiryNumber`, `name`, `email`, `phone`,
   `subject`, `message` — every request field is null-guarded with `... != null ? ... : ""` before going into
   `Map.of(...)` (`Map.of` throws `NullPointerException` on a null value, so this guard is load-bearing, not
   decorative).
7. **Publishes two `GenericNotificationEvent`s** via `ApplicationEventPublisher` (see *External Integrations*
   below for the full delivery path):
   - **Customer acknowledgment** — only if `request.getEmail()` is non-null/non-blank. Template code
     `"INQUIRY_CREATED"`, channel `EMAIL`, recipient list `[request.getEmail()]`.
   - **Support alert** — only if the injected `@Value("${app.notification.email.support}")` field
     (`supportEmail`) is non-null/non-blank. Template code `"INQUIRY_CREATED_SUPPORT"`, channel `EMAIL`,
     recipient list `[supportEmail]`.
   - If the request has no email, the customer acknowledgment is silently skipped (no error surfaced to the
     caller) — the inquiry is still saved.
8. Returns `ApiResponseDto{responseCode=200 (org.apache.http.HttpStatus.SC_OK), responseMessage="Inquiry
   submitted successfully.", responseBody=<inquiryNo>}`.

**Error handling inside the service** (both paths ultimately produce an HTTP 400 via `GlobalExceptionHandler` —
see that section):
- `catch (DataAccessException ex)` → logs, then `throw new RuntimeException("Unable to save inquiry. Please
  try again later.")`.
- `catch (Exception ex)` → logs, then `throw new RuntimeException("Something went wrong while processing your
  request.") { }` — note the trailing `{ }`, which defines an **anonymous subclass** of `RuntimeException`
  inline. This is functionally identical to a plain `RuntimeException` (no overridden behavior in the empty
  body) — an unusual but harmless code-style quirk, not a bug.
- Because the event-publishing block (step 7) sits **inside** the same `try` as the DB save, an exception
  thrown while publishing events (unlikely, since `ApplicationEventPublisher.publishEvent` for a
  non-transactional, non-blocking publish rarely throws) would also be caught and converted into the generic
  `RuntimeException` above, even though the DB row was already committed successfully at that point.

---

## DTOs / Request-Response Models

### `InquiryRequest` (`src/main/java/com/cargosetu/dto/InquiryRequest.java`)

```java
@Data
public class InquiryRequest {
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
}
```

| Field | Type | Bean Validation annotations | Notes |
|---|---|---|---|
| `name` | `String` | none | |
| `email` | `String` | none | No `@Email` — any string is accepted, including malformed or missing |
| `phone` | `String` | none | No format/length constraint |
| `subject` | `String` | none | |
| `message` | `String` | none | |

Plain Lombok `@Data` class — no `@NotNull`, `@NotBlank`, `@Email`, `@Size`, or any other `jakarta.validation`
annotation on any field, and the controller does not annotate the `@RequestBody` parameter with `@Valid`. See
*Important Business Rules & Validations* below for the practical effect.

### Response: `ApiResponseDto` (`src/main/java/com/cargosetu/config/dto/ApiResponseDto.java`)

Shared response envelope (also used by other controllers, not specific to this module):

| Field | Type | Used by this endpoint? |
|---|---|---|
| `responseCode` | `Integer` | Yes — `200` on success |
| `responseMessage` | `String` | Yes — e.g. `"Inquiry submitted successfully."` |
| `responseBody` | `Object` | Yes — the generated `inquiryNo` string |
| `dataList` | `List<?>` | No — left `null` |
| `timestamp` | `LocalDateTime` | No — left `null` (only set if the `@Builder`/all-args path is used with it explicitly, which `InquiryService` does not do) |

---

## Entities & Database Relationships

### `CustomerInquiry` (`src/main/java/com/cargosetu/model/CustomerInquiry.java`)

Table: **`customer_inquiry`** (`@Table(name = "customer_inquiry")`, explicit — not inferred).

| Column (implicit, no `@Column(name=...)` overrides except `message`) | Java field | Type | Constraints |
|---|---|---|---|
| `id` | `id` | `Long` | `@Id`, `@GeneratedValue(strategy = IDENTITY)` |
| `inquiry_number` | `inquiryNumber` | `String` | nullable (no `@Column(nullable=false)`), **no `UNIQUE` constraint** despite being called a "number" |
| `name` | `name` | `String` | nullable |
| `email` | `email` | `String` | nullable |
| `phone` | `phone` | `String` | nullable |
| `subject` | `subject` | `String` | nullable |
| `message` | `message` | `String` | `@Column(columnDefinition = "TEXT")`, nullable |
| `status` | `status` (`InquiryStatus`) | `@Enumerated(EnumType.STRING)` | nullable at the entity level (always set to `NEW` in practice by the only code path that creates rows) |
| `source` | `source` | `String` | nullable free-text (always set to the literal `"WEBSITE"` today — see Forward-Looking Note for how the planned `Lead.source` differs) |

Also extends `AbstractModel` (`abstracts/AbstractModel.java`, `@MappedSuperclass`), inheriting: `isActive`
(`is_active`, defaults `true`), `isDeleted` (`is_deleted`, defaults `false`), `createdBy` (`Long`),
`createdByName`, `createdAt` (`created_at`, `updatable = false`, auto-set via `@PrePersist`), `updatedByName`,
`updatedById`, `updatedAt` (`updated_at`, `updatable = false` — note this column is marked non-updatable at the
column level, so even if code tried to set it on an update it would not persist).

### `InquiryStatus` (`src/main/java/com/cargosetu/model/InquiryStatus.java`)

```java
public enum InquiryStatus {
    NEW,
    CONTACTED,
    RESOLVED,
    CLOSED
}
```

Verified by direct read — exactly these four values, stored as `STRING` (never ordinal) via
`@Enumerated(EnumType.STRING)` on `CustomerInquiry.status`.

### Deliberate gap — no tenant field, no relation to `Customer`/`Shifting`

**Flagging explicitly, not silently documenting as if it were normal:**

- `CustomerInquiry` has **no `companyId`/tenant field of any kind** — unlike `Customer`, `Shifting`, `Trip`,
  and `Staff` (which use `Integer companyId`) or `Department` (which uses `String companyUuid`). This is
  consistent with the endpoint being a pre-authentication, pre-tenant public contact form — there is no
  authenticated company context available at submission time to stamp onto the row. But it also means every
  inquiry from every company/website visitor lands in one single, tenant-less table with no way to filter
  "which company's contact form this was for" if the app is ever used to serve multiple company-branded
  public sites from the same backend.
- `CustomerInquiry` has **no `@ManyToOne`/`@JoinColumn` (or even a plain FK-by-ID column) to `Customer` or
  `Shifting`** — confirmed by reading the full entity source (reproduced above; every field is a primitive or
  `String`, no relation annotations). An inquiry is never linked to an existing `Customer` record even if the
  submitted email/phone matches one, and never converted into a `Shifting`/booking. There is no code anywhere
  in the repo that reads `CustomerInquiry` rows back out other than the insert path itself (`InquiryRepository`
  has no query methods — see Repositories below) — once saved, an inquiry is otherwise invisible to the rest
  of the application.
- This is a genuine functional gap (not a design choice with an offsetting mechanism elsewhere): today, a
  contact-form submission generates two emails and one DB row, and nothing else. There is no admin UI, no
  status-transition endpoint (despite `InquiryStatus` having four states, nothing in the codebase ever moves a
  row out of `NEW`), and no connection back into the CRM/ERP data model.

---

## Repositories

### `InquiryRepository` (`src/main/java/com/cargosetu/repo/InquiryRepository.java`)

```java
public interface InquiryRepository extends JpaRepository<CustomerInquiry, Long> {
}
```

**Verified: zero custom query methods.** The interface body is completely empty — only the inherited
`JpaRepository` CRUD methods (`save`, `findById`, `findAll`, `deleteById`, etc.) are available, and only
`save()` is ever actually called (from `InquiryService.createInquiry`). There is no `findByStatus`,
`findByEmail`, pagination method, or anything else — consistent with there being no read/list/admin endpoint
anywhere in the codebase for inquiries.

One incidental observation: the file has an unused import, `import com.cargosetu.dto.InquiryRequest;` — dead
import, not used anywhere in the interface body.

---

## Security & Authentication

This endpoint is made public through **two independent gates**, both of which must (and do) exempt it —
verified by direct read of both files:

1. **`SecurityConfig.filterChain`** (`src/main/java/com/cargosetu/config/SecurityConfig.java`): the
   `.authorizeHttpRequests(...)` block's `permitAll()` matcher list includes `"/public/**"` (among
   `"/auth/**"`, static assets, Swagger paths, `"/api/demo-notifications/**"`, `"/public/plan/public/**"`,
   `"/ws/**"`, `"/topic/**"`). Since the effective normalized mapping for this controller is
   `/public/api/inquiries` (see API section above), it falls under the broad `"/public/**"` Ant-style pattern
   and is therefore `permitAll()`'d — **not** the narrower, seemingly-unrelated `"/public/plan/public/**"`
   pattern (that one does not appear to correspond to any currently-mapped controller path at face value, and
   is out of scope for this module).
2. **`JwtAuthenticationFilter.shouldNotFilter(HttpServletRequest)`** (`src/main/java/com/cargosetu/config/JwtAuthenticationFilter.java`):

   ```java
   protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
       String path = request.getServletPath();
       return path.equals("/pages/login")
               || path.startsWith("/css")
               || path.startsWith("/js")
               || path.startsWith("/images") || path.startsWith("/oauth2/")
               || path.startsWith("/login/oauth2/")
               || path.startsWith("/auth/")
               || path.startsWith("/public/")
               || path.equals("/");
   }
   ```

   `request.getServletPath()` for this endpoint is `/public/api/inquiries`, which matches
   `path.startsWith("/public/")` → `shouldNotFilter` returns `true` → `JwtAuthenticationFilter.doFilterInternal`
   **never runs at all** for this request (Spring's `OncePerRequestFilter` skips `doFilterInternal` entirely
   when `shouldNotFilter` is true). This means even a request that *does* include an `Authorization: Bearer
   ...` header is never inspected — the filter doesn't just tolerate a missing token, it doesn't look at the
   header at all.

Together this is a genuine **double bypass**: the request both skips the JWT filter chain (so no
`SecurityContextHolder` authentication is ever attempted) *and* is explicitly `permitAll()`'d at the
`authorizeHttpRequests` layer, so even if the filter ran and left the context empty, the endpoint would still
be authorized. There is no CSRF exposure since CSRF is globally disabled (`csrf(csrf -> csrf.disable())`) and
the app is stateless (`SessionCreationPolicy.STATELESS`).

CORS: the shared `corsConfigurationSource()` bean applies here like every other endpoint — allowed origins are
a fixed list (`localhost:4200`, `code-setu-app.onrender.com`, `code-setu-app-ui.onrender.com`, a Cloudflare
Workers UI URL, `127.0.0.1:31201`), methods `GET/POST/PUT/DELETE`, headers `Authorization`/`Content-Type`,
credentials allowed. Since this endpoint needs no `Authorization` header, the CORS allow-list is really only
relevant here to restrict which browser origins can POST to it at all (a non-browser client, e.g. `curl` or a
server-to-server caller, is unaffected by CORS).

No rate limiting, CAPTCHA, or spam/abuse protection of any kind exists on this endpoint (verified — no such
filter, interceptor, or dependency appears anywhere in the security or web configuration). Being fully
public and unauthenticated, it is open to unlimited automated submission, which would each generate two real
outbound emails (see External Integrations — `app.notification.email.mock=false` in the checked-in
`application.properties`).

---

## Exception Handling

`GlobalExceptionHandler` (`src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`) is a
`@RestControllerAdvice` with exactly two handlers, both used generically across the whole app (nothing
inquiry-specific):

| Exception | HTTP status | Body |
|---|---|---|
| `CustomValidationException` | `400 Bad Request` (`ResponseEntity.badRequest()`) | `ApiResponseDto{responseCode = ex.getErrorCode(), responseMessage = ex.getMessage(), responseBody = null}` |
| Any other `Exception` | `400 Bad Request` | `ApiResponseDto{responseCode = CommonStatusCode.INTERNAL_SERVER_ERROR, responseMessage = ex.getMessage(), responseBody = null}` |

`InquiryController`/`InquiryService` never throw `CustomValidationException` (there is no validation to fail —
see below), so in practice the only exception path this module can hit is the generic `Exception` handler,
reached via the two `RuntimeException`s thrown from `InquiryService.createInquiry`'s `catch` blocks (see
Business/Service Logic above). Both result in an HTTP **400** response with `responseCode` claiming
`INTERNAL_SERVER_ERROR` in the JSON body — the same "wrong status line, correct-ish code buried in the body"
inconsistency already documented for the rest of the app in `docs/services-and-known-issues.md` §4/§7. A
caller (or a future automated lead-capture client) relying on HTTP status codes to distinguish "bad input" from
"server error" cannot do so for this endpoint.

---

## Configuration

No inquiry-specific configuration file or `@ConfigurationProperties` class exists. The only externalized value
this module reads is:

| Property | File | Value (checked in) | Used by |
|---|---|---|---|
| `app.notification.email.support` | `application.properties` (line 79) | `logiaca22@gmail.com` (a line 78 comment shows an intended `support@cargosetu.com` that is **not** the active value) | `InquiryService.supportEmail` (`@Value`) — the recipient of the `INQUIRY_CREATED_SUPPORT` event |

This property is only defined in the base `application.properties`, not overridden in
`application-local.properties`/`application-prod.properties` — so the same (real, personal-looking Gmail)
address receives support alerts in every environment/profile.

Indirectly, this module's outbound notifications also depend on the shared notification-engine configuration
(not specific to Inquiry, documented fully in `../modules/notification-engine.md` if/when that module doc
exists): `app.notification.email.mock=false`, `app.notification.email.from=logiaca22@gmail.com`,
`app.notification.thread-pool.*` (core size 10 / max 20 / queue 2000, prefix `NotifWorker-`) — all in
`application.properties`.

No `spring.jpa`/table-specific configuration exists for `customer_inquiry` beyond the app-wide
`spring.jpa.hibernate.ddl-auto=update` (per `docs/services-and-known-issues.md` §4) — the table is created and
evolved by Hibernate auto-DDL from the entity, not by a Liquibase changeset.

---

## External Integrations

`InquiryService.createInquiry` publishes exactly two `GenericNotificationEvent`s
(`notification/event/GenericNotificationEvent.java`, a `record`) via Spring's `ApplicationEventPublisher` —
this is the module's only external-facing integration (no third-party API is called directly by
`InquiryService` itself).

| # | Condition to fire | `templateCode` | Channel | Recipient(s) | Purpose |
|---|---|---|---|---|---|
| 1 | `request.getEmail()` non-null/non-blank | `"INQUIRY_CREATED"` | `EMAIL` | `[request.getEmail()]` | Customer acknowledgment |
| 2 | `supportEmail` (`app.notification.email.support`) non-null/non-blank | `"INQUIRY_CREATED_SUPPORT"` | `EMAIL` | `[supportEmail]` | Internal support-team alert |

Both events carry the same `variables` map: `inquiryNumber`, `name`, `email`, `phone`, `subject`, `message`
(each null-guarded to `""`).

**Downstream path** (shared infrastructure, not Inquiry-specific — see
`../modules/notification-engine.md` for the full module writeup):

1. `GenericNotificationEventListener.handleGenericNotificationEvent` (`@Async("notificationTaskExecutor")`,
   `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`) receives the event.
   `fallbackExecution = true` matters here specifically because `InquiryService.createInquiry` is **not**
   annotated `@Transactional` — with no active transaction to commit, `AFTER_COMMIT` semantics would normally
   never fire; `fallbackExecution` makes the listener run immediately (on the async executor thread) instead
   of being silently dropped.
2. `NotificationProcessingServiceImpl.processEvent` looks up an **active** `NotificationTemplate` matching
   `(templateCode, channel)` via `findByTemplateCodeAndChannelAndIsActiveTrue`. If none is found, it logs an
   error and returns — no exception, no audit row, nothing surfaces back to the original HTTP caller (who has
   already received a 200 response by this point regardless).
3. Both `INQUIRY_CREATED` and `INQUIRY_CREATED_SUPPORT` templates (channel `EMAIL`) are **pre-seeded on every
   application startup** by `NotificationDatabaseInitializer` (`notification/config/NotificationDatabaseInitializer.java`,
   a `CommandLineRunner`) if not already present in the DB — so this dependency is self-healing across
   environments rather than requiring manual template setup.
4. For each recipient, a `NotificationAudit` row is created with status `PENDING`, then dispatched via
   whichever `NotificationChannelSender` `.supports(NotificationChannel.EMAIL)` — that's
   `EmailChannelSender` (`notification/infrastructure/channel/EmailChannelSender.java`), which uses
   `JavaMailSender`. Because `app.notification.email.mock=false` in the checked-in `application.properties`,
   this is a **real SMTP send** (Gmail, `app.notification.email.from=logiaca22@gmail.com`), not a
   mock/no-op — a live test submission to this endpoint sends real email.
5. `EmailChannelSender.send` is `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier =
   2.0, maxDelay = 10000))`; on final failure, `@Recover` marks the audit `FAILED` with the failure reason.
   None of this retry/failure state is ever surfaced back to whoever submitted the inquiry form — it is purely
   internal bookkeeping in `notification_audit`.

No SMS, push, or WhatsApp channel is used by this module (both events are `NotificationChannel.EMAIL` only),
and no outbound webhook, external CRM, or third-party lead-capture API is called anywhere in this flow.

---

## Important Business Rules & Validations

1. **No Bean Validation of any kind on `InquiryRequest`.** No `@NotNull`, `@NotBlank`, `@Email`, `@Size`, or
   any other `jakarta.validation.constraints` annotation on any of the five fields, and the controller does
   not use `@Valid`/`@Validated` on the `@RequestBody` parameter (verified by reading both files directly). A
   request with an empty JSON body `{}` is accepted: all fields resolve to `null`, and the flow proceeds —
   `CustomerInquiry` is saved with `name`/`email`/`phone`/`subject`/`message` all `null` (none of those
   columns are `NOT NULL` at the entity level either), and only the customer-acknowledgment event (guarded on
   `email` being non-blank) is skipped; the support-alert event still fires (since it only depends on the
   server-side `supportEmail` config, not on request content) with empty-string placeholders in its rendered
   template.
2. **No email-format validation.** Any string can be submitted as `email`, including non-email text — it is
   used as-is as the `To` address for the acknowledgment email; the actual send would fail at the SMTP layer
   (caught by `EmailChannelSender`'s try/catch → `FAILED` audit, not surfaced to the caller) rather than being
   rejected up front.
3. **`status` and `source` are not client-settable** — `InquiryRequest` has no fields for either, and
   `InquiryService` hardcodes `NEW`/`"WEBSITE"` unconditionally. This is effectively a business rule enforced
   by omission (the DTO shape), not by explicit validation logic.
4. **`inquiryNumber` uniqueness is not enforced** — no `@Column(unique = true)`, no DB index, no pre-save
   existence check. Collision is astronomically unlikely (8 hex-ish chars of UUID plus date) but is not
   structurally guaranteed unique the way an inquiry *number* implies it should be.
5. **No duplicate-submission / spam protection** — the same email/phone/message can be submitted any number of
   times with no deduplication, rate limit, or CAPTCHA (see Security section).
6. **Status is never transitioned by any code path** — although `InquiryStatus` defines `CONTACTED`,
   `RESOLVED`, `CLOSED` in addition to `NEW`, there is no service method, controller endpoint, or scheduled
   job anywhere in the repository that moves a `CustomerInquiry` row out of `NEW` (confirmed by the
   `InquiryRepository` having no read methods and no other class referencing `CustomerInquiry`/
   `InquiryStatus` for a write). These states appear to be planned-but-unimplemented — an admin workflow
   likely intended but not yet built.

---

## Dependencies Between Modules

- **Notification engine** (`com.cargosetu.notification.*`): the only real cross-module dependency —
  `InquiryService` depends on `ApplicationEventPublisher` + the `GenericNotificationEvent` contract, and
  transitively on `NotificationTemplate`/`NotificationAudit` persistence and `EmailChannelSender`/SMTP config.
  This is the same integration pattern `CustomerService` uses for customer-creation notifications
  (`docs/services-and-known-issues.md` §1) — worth reusing this exact pattern (not reinventing it) for any new
  lead-creation notification.
- **Security/Auth module** (`SecurityConfig`, `JwtAuthenticationFilter`): this module's only interaction with
  security is being explicitly carved out of it (the double-bypass documented above) — it has no dependency on
  `Staff`, `LoggedInUser`, roles, or permissions at all, since the caller is never authenticated.
- **No dependency on `Customer`, `Shifting`, `Trip`, or `Company`/tenant modules** — this is the point flagged
  in *Entities & Database Relationships* above: architecturally, `CustomerInquiry` is an island table with
  exactly one inbound write path (`InquiryController` → `InquiryService` → `InquiryRepository`) and zero
  outbound reads from anywhere else in the codebase.
- **Frontend dependency**: per `docs/services-and-known-issues.md` §3, the Angular UI's public `contect-us`
  *(sic)* page is presumably the caller of this endpoint (not independently verified from the backend repo,
  since the frontend source lives in the separate `code-setu-app-ui` project — flagged as **inferred**, not
  verified against frontend source in this pass).

---

## Forward-Looking Note: `CustomerInquiry` vs. the planned `Lead` entity

Per [`../WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md`](../WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md) (`com.cargosetu.lead`
package, not yet implemented), the planned `Lead` entity is a **superset and a different tier of the same
concept**, not a drop-in replacement built on the same table. Concretely: `Lead` will carry `companyId`
(tenant-scoped, matching `Customer`/`Shifting`/`Trip`'s numeric convention) and a plain-column FK to
`customerId`, neither of which `CustomerInquiry` has at all; its `source` will be a real enum (`WHATSAPP`,
`WEBSITE`, `PHONE`, `REFERRAL`, `WALK_IN`) replacing `CustomerInquiry.source`'s free-text string (which today
only ever holds the literal `"WEBSITE"`); and its `status` will use an eight-value `LeadStatus` (`NEW,
CONTACTED, QUALIFICATION, QUOTATION_PENDING, QUOTATION_SENT, NEGOTIATION, WON, LOST`) modeling an actual sales
pipeline, versus `InquiryStatus`'s four values (`NEW, CONTACTED, RESOLVED, CLOSED`) which model a simple
support-ticket lifecycle that, as noted above, nothing in the code currently drives forward past `NEW` anyway.
The one thing the plan explicitly keeps rather than replaces is the *notification-event pattern* itself —
`Lead` creation is expected to publish `GenericNotificationEvent`s the same way `InquiryService` and
`CustomerService` already do, and the plan's WhatsApp webhook controller is designed to be routed under
`/public/webhooks/whatsapp/**` specifically so it clears the same two security gates
(`SecurityConfig`'s `/public/**` and `JwtAuthenticationFilter.shouldNotFilter()`'s `/public/` prefix check)
that this module's `public/api/inquiries` mapping already relies on. In short: the public, no-DB-relations,
no-tenant contact-form flow documented in this file is expected to keep existing largely as-is (or be
deprecated quietly) once the WhatsApp/Lead pipeline ships — it is not something that migration would delete or
rewrite, since it serves a different (simpler, unauthenticated marketing-site) purpose than the tenant-scoped,
staff-owned `Lead` pipeline the plan describes.

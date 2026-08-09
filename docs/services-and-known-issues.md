# Service Layer, DTOs, Frontend & Known Issues

> Companion to [`controllers.md`](./controllers.md), [`entities.md`](./entities.md) and
> [`api-urls.md`](./api-urls.md). Those three cover *what endpoints and tables exist*. This doc covers
> *the business-logic layer that sits between them, the DTOs that cross the wire, the Angular frontend,
> and the gaps/risks a new feature (or a new engineer) needs to know about before building on top of this
> codebase.*

---

## 1. Service / business-logic layer

Pattern used almost everywhere: an interface (`XService`) + one implementation (`XServiceImpl` or `XImpl`,
naming is inconsistent) in `service/impl/`. A few features (`CustomerService`, `ShiftingService`,
`AuthService`, `OtpService`, `InquiryService`, `TemplateService`, notification services) skip the
interface and are a single concrete `@Service` class instead. There is **no shared base service class**.

| Service | File | Responsibility | Tenant (`companyId`) scoping enforced? |
|---|---|---|---|
| `AuthService` | `service/AuthService.java` | Username/password login, issues JWT via `JwtUtils`; `refreshToken()` re-derives a new JWT from the claims of the *old* JWT — does **not** check the DB-backed `RefreshToken`/revocation | N/A |
| `OtpService` | `service/OtpService.java` | OTP send/verify for staff *and* customers. Contains the codebase's only real phone-number-matching logic: strips non-digits, tries raw / with-without-`+` / last-10-digits fallback against both `CustomerRepository` and `StaffRepository`. **Reuse this for WhatsApp inbound phone matching instead of reimplementing.** | N/A (login flow) |
| `SeedSuperAdmin` | `service/SeedSuperAdmin.java` | `ApplicationRunner` — seeds one hardcoded SUPERADMIN staff + fixed permission set on every startup (bcrypt hash literal in source) | N/A |
| `UserDetailsServiceImpl` | `service/UserDetailsServiceImpl.java` | Spring Security `UserDetailsService` bridge for `Staff` | — |
| `StaffService` | `service/StaffService.java` | Staff CRUD, paginated list | **Not verified scoped** — needs check before reuse |
| `CompanyService` / `CompanyServImpl` | `service/CompanyService.java`, `service/impl/CompanyServImpl.java` | Company CRUD, asset upload | Company *is* the tenant root, so "scoping" here means access control (who can see which company), not row filtering — not enforced beyond JWT authentication |
| `DepartmentService` / `DepartmentImpl` | `service/DepartmentService.java`, `service/impl/DepartmentImpl.java` | Department CRUD | **No — confirmed gap.** `findAll()`/`findById()` with zero company filter; any authenticated user can read/edit/delete any company's department by ID |
| `RolesService` / `RolesImpl`, `PermissionService` / `PermissionImpl` | `service/{Roles,Permission}Service.java` + impls | Role/Permission CRUD | Not scoped — same shape as Department |
| `BranchService` | `service/BranchService.java` | Branch CRUD | Not verified |
| `CustomerService` | `customer/service/CustomerService.java` | Customer CRUD, nested `Shifting` creation. **This is the one place tenant scoping is done correctly**: `getAllCustomer()` branches on `user.getStaffId() == 1` (bootstrap superadmin sees everything) vs. `customerRepository.findByCompanyIdAndIsDeleteOrderByCustomerIdDesc(user.getCompanyId(), ...)` for everyone else. Publishes `GenericNotificationEvent` on customer creation (same event pattern to reuse for lead-created events) | **Yes** — use as the reference pattern |
| `ShiftingService` | `customer/service/ShiftingService.java` | Shifting (job) CRUD, nested `Charges`/`ExtraCharges` | Not verified independently of Customer |
| `ShiftInventoryService` | `shifting/service/ShiftInventoryService.java` | Per-item packed/loaded/unloaded status on a shift | Not verified |
| `DriverService`, `VehicleService`, `VehicleLocationServ`, `DriverVehicleAssignmentService` + impls | `service/{Driver,Vehicle,VehicleLocation,DriverVehicleAssignment}*.java` | Driver/vehicle CRUD, GPS location updates, assignment | Not scoped — plain `JpaRepository` CRUD, no companyId filter seen |
| `TripService`, `TripDocumentService`, `TripExpenseService`, `TripStatusHistoryService` | `service/Trip*.java` | Trip CRUD, document/expense/status-history sub-records | Not scoped |
| `InquiryService` | `service/InquiryService.java` | Creates `CustomerInquiry` from the public contact form, fires two `GenericNotificationEvent`s (customer ack + support alert) | N/A — no tenant concept on `CustomerInquiry` at all (see gap below) |
| `TemplateService` | `service/TemplateService.java` | Loads a `Shifting` + `Customer` + `Charges`, computes `subTotal`/`grandTotal` into a transient `QuotationDTO`, renders a Thymeleaf `Template` (`type="QUOTATION"`), converts to PDF via `PdfGenerator` (`utils/utillity/PdfGenerator.java`) — **correction: this actually uses
Microsoft Playwright (headless Chromium), not openhtmltopdf/flying-saucer as originally stated here; those
libraries are on the classpath but their call sites in `TemplateService` are dead code, see
`modules/template-quotation.md`**. **No persisted `Quotation` entity — this is PDF rendering only, not a stateful quotation workflow.** | Via the parent `Shifting`/`Customer` |
| `DocumentMasterService`, `DocumentUploadService` | `document/service/*.java` | Document-type definitions + uploaded-file records | Not verified |
| `ClientServices` / `ClientServiceImpl` | `service/ClientServices.java` + impl | Generic key/value system-config entries | Not scoped |
| `PlanService` / `PlanServiceImpl` | `service/PlanService.java` + impl | Subscription plan CRUD, has a `/public` plan-listing endpoint | N/A (platform-level, not tenant data) |
| **Notification engine** (`notification/service/*`) | `NotificationProcessingService(Impl)`, `TemplateRenderingService(Impl)`, `DemoBusinessService` | Loads `NotificationTemplate` → renders → creates `NotificationAudit` (PENDING) → dispatches via channel-sender strategy (`EmailChannelSender`/`SmsChannelSender`/`WhatsAppChannelSender`, matched by `.supports(channel)`) | N/A — cross-tenant infrastructure |

**Takeaway for new work**: `CustomerService` and `OtpService` are the two pieces of prior art worth reusing
directly (tenant-scoped query pattern, and phone-normalization/matching logic respectively). Everything
else that does CRUD without scoping should be treated as **not a safe pattern to copy** for any new
multi-tenant-sensitive feature (leads, conversations, quotations, payments).

---

## 2. DTO catalog

MapStruct (`@Mapper(config = GlobalMapperConfig.class)`, `componentModel="spring"`,
`NullValuePropertyMappingStrategy.IGNORE`) pairs most entities with a DTO via
`BaseUpdateMapper<D,E>` (`toEntity`/`toDto`/`updateFromDto`). DTOs live in `com.cargosetu.dto` (top-level),
or a feature-local `dtos`/`dto` package (`customer.dtos`, `shifting.dtos`, `document.model`-adjacent).

| DTO | Package | Pairs with entity | Notes |
|---|---|---|---|
| `StaffDTO` | `dto` | `Staff` | Used by both registration (`AuthController`) and staff CRUD (`StaffController`) |
| `CompanyDTO` | `dto` | `Company` | Bound via `@ModelAttribute` (multipart) on create |
| `DepartmentDTO` | `dto` | `Department` | Minimal — field names match entity 1:1 |
| `RolesDTO` | `dto` | `Role` | — |
| `PermissionDTO` | `dto` | `Permission` | — |
| `PlanDTO` | `dto` | `Plans` | — |
| `ClientServiceDTO` | `dto` | `Clientservice` | — |
| `DriverDto`, `VehicleDto` | `dto` | `Driver`, `Vehicle` | — |
| `DriverVehicleAssignmentDto`, `AssignedDriverToVehicleDto` | `dto` | `DriverVehicleAssignment` | Two DTOs for the same entity — request vs. response shape |
| `TripDTO`, `TripDocumentDTO`, `TripExpenseDTO`, `TripStatusHistoryDTO` | `dto` | `Trip` + sub-entities | — |
| `CompanyAssetDTO` | `dto` | `CompanyAsset` | — |
| `DocumentMasterDTO`, `DocumentUploadDTO`, `BaseDocDTO` | `dto` | `DocumentMaster`, `DocumentUpload` | `BaseDocDTO` looks like a shared parent for upload metadata |
| `InquiryRequest` | `dto` | `CustomerInquiry` | Public-form request shape: `name, email, phone, subject, message` — no tenant field |
| `QuotationDTO` | `dto` | *(none — transient)* | `{quoteDate, subTotal, grandTotal, tokenMoney}`, computed on the fly by `TemplateService`, never persisted — **this is the gap a real `Quotation` entity needs to fill** |
| `ValidationResponse` | `dto` | — | Generic validation-error shape |
| `CustomerDTO`, `CustomerUpdateDto` | `customer.dtos` | `Customer` | Separate create vs. update shapes |
| `ChargesDTO`, `ExtraChargesDTO` | `customer.dtos` | `Charges`, `ExtraCharges` | — |
| `ShiftingDTO` | `customer.dtos` | `Shifting` | — |
| `PackagingMaterialDTO`, `ShiftInventoryItemDTO`, `ShiftInventoryPackagingDTO`, `ShiftInventoryPhotoDTO` | `shifting.dtos` | corresponding `shifting.entity.*` | — |
| `TokenResponse` | `config.dto` | — | `{accessToken, tokenType="Bearer ", expiresIn}` — returned by login/OTP-verify/refresh |
| `LoginRequest` | `config.dto` | — | `{username, password}` |
| `ApiResponseDto` | `config.dto` | — | Shared success/error envelope: `{responseCode, responseMessage, responseBody, dataList, timestamp}` |
| `LoggedInUser` | `config.dto` | — | The `@AuthenticationPrincipal` shape injected into controllers (carries `staffId`, `companyId`, roles/permissions) |
| `GenericDataDTO` | `utils` | — | The other common response envelope (note: its message field is spelled `responseMessege`, not a typo to "fix" without checking every caller) |

There are **two parallel response envelopes** in active use (`ApiResponseDto` and `GenericDataDTO`) with
no clear rule for which controller uses which — new controllers should pick one and it's worth asking
which the team now considers canonical rather than introducing a third.

---

## 3. Frontend (`code-setu-app-ui`) summary

Angular **14.2** (CLI-generated), Angular Material **13** (a major version behind Angular itself — unusual
but not necessarily broken), plus `@stomp/stompjs`/`sockjs-client` (matches the backend's `/ws/**`
WebSocket config), `jwt-decode`, `ngx-image-cropper`, `html2pdf.js`, `@angular/google-maps`.

This is a working internal ERP admin UI, not just a marketing shell:

- **Public/marketing pages**: `home`, `about`, `services`, `contect-us` *(sic)*, `featurescreen`,
  `downloadscreen`, `planscreen`.
- **Auth**: `login`, `signup`, OTP flow.
- **Admin/ops modules** (all behind `AuthGuard`): `dashboard`, `company`, `staffuser`/`staff-details`,
  `department`, `role`/`updaterole`/`roles-details`, `permission`, `customer`, `createplan`,
  `vehicle-management` (+ `vehicles`/`drivers`/`assignments` sub-routes), `system-configuration`,
  `quotaion` *(sic — quotation list, front-end name for the PDF-generation flow above)*, `trip`, and an
  elaborate `shifting-workspace` module (overview/details/inventory/inventory-drawer/packing/loading/
  documents/charges/timeline/trip sub-components) — the operational "job workspace" for a shifting order.

**Known-broken auth flow** (`src/app/services/login.service.ts`, `core/interceptors/auth.interceptor.ts`,
`services/guard/auth.guard.ts`):
- JWT stored in `localStorage` under `token`, plus separate `user_session`/`staffInfo`/`user` blobs.
- `loginUser()` calls `this.refreshToken()` and treats the returned `Observable` as if it were already
  resolved — `updateTokens(newTokens)` / `this.loginAndFetchUser` reference looks like a dead/no-op typo.
- `refreshToken()` POSTs to a **relative** `/api/auth/refresh`, which matches nothing on the backend (the
  real endpoint is `GET /api/v2/cargosetu/refreshtoken` with a Bearer header — see `BaseApiController` in
  `controllers.md`). Token refresh is effectively non-functional today.
- `auth.guard.ts` only checks `localStorage.getItem('token')` presence — no expiry check, no
  role/permission gating at the route level.
- There's a stray `services/guard/auth.interceptor.spec.ts` with no matching `.ts` file — leftover/dead
  test scaffold.

**Implication for new work**: any new frontend screens for leads/WhatsApp inbox/quotations should not
copy the current refresh-token wiring as-is — it needs fixing first or the new screens will silently log
users out whenever a token expires mid-session.

---

## 4. Cross-cutting systems (verified against source)

**Security** (`config/SecurityConfig.java`, `config/JwtAuthenticationFilter.java`,
`config/MethodSecurityConfig.java`):
- Stateless JWT, CORS allow-list includes `localhost:4200`, `code-setu-app.onrender.com`,
  `code-setu-app-ui.onrender.com`, a Cloudflare Workers URL — confirms Render.com + Cloudflare Workers as
  the deployment targets.
- `permitAll()` matchers: `/auth/**`, `/login.html`, `/css/**`, `/js/**`, `/images/**`, `/swagger-ui/**`,
  `/v3/api-docs/**`, `/api/demo-notifications/**`, `/public/plan/public/**`, `/public/**`, `/ws/**`,
  `/topic/**`. **`/public/**` is the pattern any new public webhook should mount under** (it's also in
  `JwtAuthenticationFilter.shouldNotFilter()`'s path list, so it skips the JWT filter entirely — a true
  double-bypass, exactly like `InquiryController`'s `public/api/inquiries`).
- **`@EnableMethodSecurity(prePostEnabled = false)`** in `MethodSecurityConfig.java` — confirmed by
  direct read. Every `@PreAuthorize("hasAuthority(...)")` annotation across every controller (Company,
  Customer, Shifting, Branch, Template, etc.) is **silently inert**. Any authenticated user can call any
  endpoint regardless of role/permission today.
- **`GoogleRoleMapper.mapAuthorities()`** — confirmed by direct read: unconditionally adds
  `new SimpleGrantedAuthority("ROLE_SUPERADMIN")` for any successful Google OAuth login. If Google login
  is ever wired into a real sign-in button, this is a full-privilege-escalation backdoor as written.

**Notification engine** (`notification/**`): `ApplicationEventPublisher.publishEvent(new
GenericNotificationEvent(...))` → `GenericNotificationEventListener` (`@Async("notificationTaskExecutor")`,
`@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`) → `NotificationProcessingServiceImpl`
loads template, creates a `PENDING` `NotificationAudit`, dispatches via whichever `NotificationChannelSender`
`.supports(channel)` (strategy pattern: `EmailChannelSender`/`SmsChannelSender`/`WhatsAppChannelSender`).
Retry via `spring-retry`: `@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2.0,
maxDelay=10000))` with a `@Recover` that marks the audit `FAILED`. **Correction (found by a deeper module-level audit, see `modules/notification-engine.md`): despite what
this section previously claimed and what the backend's own top-level `README.md` describes,
`WhatsAppChannelSender` is currently a pure `log + Thread.sleep(250)` stub** — it has no Twilio SDK call,
no `@Retryable`/`@Recover`, no phone-number normalization, and never updates the `NotificationAudit` row
past `PENDING`. `EmailChannelSender`/`SmsChannelSender` do have the real
`@Retryable(maxAttempts=3, backoff=@Backoff(delay=1000, multiplier=2.0, maxDelay=10000))` behavior described
above — WhatsApp does not, today. Any new inbound-webhook work should not assume an existing outbound
Twilio WhatsApp integration to mirror; there isn't one live yet, only the `EMAIL`/`SMS` channel senders
are real. See `modules/notification-engine.md` §12 ("Discrepancies with existing docs") for the full
detail.

**Exception handling** (`handler/GlobalExceptionHandler.java`): `@RestControllerAdvice` with exactly two
handlers — `CustomValidationException` and generic `Exception` — **both return `ResponseEntity.badRequest()`
(HTTP 400) unconditionally**, even the generic handler that constructs a `CommonStatusCode.INTERNAL_SERVER_ERROR`
body. The "real" error code only shows up inside the JSON body's `responseCode` field, never in the HTTP
status line. A webhook consumer (Meta/Twilio) that inspects HTTP status for retry logic will see 400 for
everything — worth deciding explicitly rather than reusing this handler unmodified for webhook errors.

**Liquibase**: `Changelog/main.xml` only includes `changelog-master.xml` (legacy `tblmcompany` table +
seed data) and `changelog-templete.xml` (notification template seed rows). **Almost all tables are
actually created by Hibernate** (`spring.jpa.hibernate.ddl-auto=update` in both `application-local.properties`
and `application-prod.properties`) — there is no per-feature migration file convention. New entities
follow this same auto-DDL convention unless a deliberate decision is made to start hand-writing
changesets (worth raising explicitly, since production data + `ddl-auto=update` is itself a risk).

---

## 5. Known issues / gaps (consolidated)

These are pre-existing and **not** specific to any one new feature — flagged here once so future feature
docs (e.g. the WhatsApp/Lead/AI plan) can reference this list instead of re-describing it:

1. **Hardcoded secrets committed in plaintext**: `application.properties`/`application-prod.properties`
   contain a real Twilio Account SID + Auth Token, a Gmail SMTP app password, a Google OAuth2 client
   secret, and a production MySQL password (Railway-hosted). `JwtUtils.java` hardcodes the JWT HMAC
   signing key directly in source. **These should be rotated and moved to environment variables/secrets
   manager independent of any new feature work.**
2. **`@PreAuthorize` is inert** (`MethodSecurityConfig.prePostEnabled = false`) — no method-level
   authorization is actually enforced anywhere, despite extensive `@PreAuthorize` annotations existing.
3. **No systematic tenant isolation** — `companyId` exists as a plain column on several entities but its
   *type* is inconsistent (`Integer` on `Customer`/`Shifting`/`Trip`/`Staff` vs. `String companyUuid` on
   `Department`), and enforcement is inconsistent (`CustomerService` does it correctly; `DepartmentImpl`,
   `RolesImpl`, `PermissionImpl`, and most `Driver`/`Vehicle`/`Trip` services do not filter by tenant at
   all). `CustomerInquiry` has no tenant field whatsoever.
4. **`GoogleRoleMapper` superadmin backdoor** — see Security section above.
5. **Broken refresh-token flow, both ends**: backend `AuthService.refreshToken()` doesn't check DB
   revocation state; frontend `login.service.ts` posts to a URL that doesn't exist on the backend.
6. **No real automated test coverage**: `CargoSetuAppApplicationTests.contextLoads()` is the only test in
   the entire backend; the frontend has unmodified Angular CLI `.spec.ts` scaffolds (including at least
   one, `auth.interceptor.spec.ts`, with no matching source file). CI (`Jenkinsfile`) runs
   `./gradlew clean build -x test`, explicitly skipping tests.
7. **`GlobalExceptionHandler` always returns HTTP 400** regardless of the logical error — see Exception
   handling section above.
8. **Two parallel API response envelopes** (`ApiResponseDto` vs. `GenericDataDTO`) with no documented
   rule for which to use where.

None of these are addressed by the WhatsApp/Lead/AI plan in
[`WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md`](./WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md) — they're called
out here so they aren't silently assumed fixed, and so a future security-hardening pass has a single
starting checklist instead of re-discovering all of this from scratch.

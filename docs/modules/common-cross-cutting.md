# Common / Cross-Cutting Infrastructure Module

> Verified against source on 2026-08-09. Every class/field/behavior below was read directly from the
> files cited; anything not directly confirmed is explicitly marked "inferred" or "unverified". This
> file does not duplicate `docs/modules/auth-security.md` — JWT issuance/parsing, `SecurityConfig`
> request-matcher rules, and `LoggedInUser`'s authentication role are covered there; this file only
> touches security-adjacent code (`ApplicationUtils`, CORS) to the extent every other module depends on
> it directly.

## 1. Overview

This module is not a feature module — it is the shared substrate that every controller, service,
mapper, and entity in the app is built on top of. It has no single package; its pieces live scattered
across `abstracts/`, `handler/`, `config/`, `config/dto/`, `config/utils/`, `utils/`, and
`mapper/base/`. Concretely it covers:

- The two competing JSON response envelopes returned by controllers (`ApiResponseDto`, `GenericDataDTO`).
- The one `@MappedSuperclass` (`AbstractModel`) that about a third of the JPA entities extend for audit
  columns.
- Global exception handling (`GlobalExceptionHandler`, `CustomValidationException`).
- Cross-cutting configuration: OpenAPI/Swagger, WebSocket/STOMP, CORS (`SwaggerConfig`,
  `WebSocketConfig`, `SecurityConfig.corsConfigurationSource()`).
- The shared MapStruct config every generated mapper opts into (`GlobalMapperConfig`,
  `BaseUpdateMapper`).
- Centralized URL-prefix constants (`BaseURL`, `UrlConstants`).
- Small stateless utility classes used from many services (`ApplicationUtils`, `UUIDGenerator`).

Because it has no independent behavior of its own — it only exists to be consumed — its "known issues"
are really inconsistencies in *how* other modules consume it (e.g. two incompatible response DTOs, an
entity base class only ~30% of entities use). Those are called out inline below rather than filed as a
separate section.

## 2. DTOs / Request-Response Models

The app has **two independent, structurally different response envelopes** that controllers return
directly (not wrapped in `ResponseEntity` in most cases). There is no shared interface or base class
between them — they are entirely separate classes that happen to serve the same purpose.

### `ApiResponseDto` — `src/main/java/com/cargosetu/config/dto/ApiResponseDto.java`

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApiResponseDto {
    private Integer responseCode;
    private String responseMessage;
    private Object responseBody;
    private List<?> dataList;
    private LocalDateTime timestamp;

    public ApiResponseDto(Integer responseCode, String responseMessage, Object responseBody) {
        this.responseBody = responseBody;
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }
}
```

| Field | Type | Notes |
|---|---|---|
| `responseCode` | `Integer` | App-defined status code (see `CommonStatusCode`, e.g. `200`/`400`/`500`), independent of the actual HTTP status returned. |
| `responseMessage` | `String` | Spelled correctly (contrast with `GenericDataDTO` below). |
| `responseBody` | `Object` | Single-object payload. |
| `dataList` | `List<?>` | Present on the class but **not set** by the 3-arg constructor — every call site using that constructor leaves it `null`. |
| `timestamp` | `LocalDateTime` | Also not set by the 3-arg constructor; only populated if a caller uses the Lombok `@Builder` directly, which no call site observed does. |

The class has both a Lombok-generated all-args constructor (5 params, via `@AllArgsConstructor`) and a
hand-written 3-arg convenience constructor. All call sites found (`GlobalExceptionHandler`,
`AuthController.generateResponse`, `PlanService`/`PlanServiceImpl`, `InquiryService`) use the 3-arg one,
so `dataList` and `timestamp` are `null` in every observed real response.

### `GenericDataDTO` — `src/main/java/com/cargosetu/utils/GenericDataDTO.java`

```java
@Data @NoArgsConstructor @AllArgsConstructor
public class GenericDataDTO {
    private String responseMessege;   // sic — see note
    private Integer responseCode;
    private List dataList;            // raw type, unchecked
    private Object data;
    private Object[] errors;
    private long totalRecords = 0;
    private long pageRecords = 0;
    private long currentPageNumber;
    private long totalPages;

    public GenericDataDTO(Integer responseCode, String responseMessege, Object data, Object[] errors) {
        this.responseCode = responseCode;
        this.responseMessege = responseMessege;
        this.data = data;
        this.errors = errors;
    }
}
```

| Field | Type | Notes |
|---|---|---|
| `responseMessege` | `String` | **Misspelled** ("Messege" not "Message") in the field name and therefore in the Lombok-generated `getResponseMessege()`/`setResponseMessege()` accessors and in the raw JSON key clients receive. This is load-bearing across every consumer of this DTO (controllers, services, and — per the task's own caution — any frontend/API-contract code that reads this JSON key). **Do not silently rename this field** without grepping every caller; it is used from at least the 30+ files listed below plus whatever frontend expects the `responseMessege` JSON key. |
| `responseCode` | `Integer` | Same convention as `ApiResponseDto.responseCode` (`CommonStatusCode` constants). |
| `dataList` | `List` (raw) | Unlike `ApiResponseDto.dataList` (`List<?>`), this is a raw, unparameterized `List` — a compiler-warning-level looseness, not just a naming difference. |
| `data` | `Object` | Single-object payload, same role as `ApiResponseDto.responseBody`. |
| `errors` | `Object[]` | Not present at all on `ApiResponseDto`. |
| `totalRecords` / `pageRecords` / `currentPageNumber` / `totalPages` | `long` | Pagination metadata `ApiResponseDto` has no equivalent for. `totalRecords`/`pageRecords` default to `0` via field initializers; `currentPageNumber`/`totalPages` have no initializer (default `0` from Java's `long` default). The 4-arg constructor does not set any of these four, so any caller using it (most call sites) leaves them at `0`. |

`GenericDataDTO` is materially richer than `ApiResponseDto` (adds `errors[]` and full pagination
metadata) — it is the newer/preferred shape for list-returning, paginated endpoints, while
`ApiResponseDto` is the leaner shape used by a small set of controllers.

### Which controllers use which

This is **not** a clean architectural split by concern — it looks like two overlapping code-generations
that were never reconciled (inferred; no comment in source explains the choice). Spot-checked directly:

| Controller | DTO(s) returned | Evidence |
|---|---|---|
| `PlanController`, `PublicPlanControlor` | `ApiResponseDto` only | Every handler method's return type is `ApiResponseDto` (`src/main/java/com/cargosetu/controller/PlanController.java`, `.../PublicPlanControlor.java`). |
| `InquiryController` | `ApiResponseDto` only | `submit()` returns `ApiResponseDto` (`src/main/java/com/cargosetu/controller/InquiryController.java`). |
| `AuthController` | **Both** | `login()` builds an `ApiResponseDto` only on the `EXPECTATION_FAILED` error branch via a local `generateResponse(...)` helper that wraps `new ApiResponseDto(...)`; `registerUser()` returns `GenericDataDTO` directly (`src/main/java/com/cargosetu/controller/AuthController.java`). |
| `StaffController`, `CompanyController`, `DepartmentController`, `RoleController`, `PermissionController`, `BranchController`, `TripController`, `TripDocumentController`, `TripExpenseController`, `TripStatusHistoryController`, `TemplateGenerationController`, `ClientServiceController`, `customer/controller/CustomerController`, `customer/controller/ShiftingController`, `document/controller/DocumentController`, `shifting/controller/ShiftInventoryController` | `GenericDataDTO` only | Confirmed by `grep -rl GenericDataDTO` across `src/main/java/com/cargosetu/**/controller`; e.g. `StaffController.getStaff()` builds `new GenericDataDTO()` and calls `dto.setResponseMessege(...)` (`src/main/java/com/cargosetu/controller/StaffController.java`). |

Net effect: a frontend integrating against this API cannot assume one response envelope — it must know,
per-endpoint, whether to read `responseMessage`/`responseBody` (`ApiResponseDto`) or
`responseMessege`/`data`/`dataList`/pagination fields (`GenericDataDTO`). This mirrors what
`docs/services-and-known-issues.md` already flags about inconsistent response shapes, confirmed here at
the DTO-definition level.

## 3. Entities & Database Relationships

### `AbstractModel` — `src/main/java/com/cargosetu/abstracts/AbstractModel.java`

```java
@Data
@MappedSuperclass
public abstract class AbstractModel {
    @Column(name = "is_active")        private Boolean isActive = true;
    @Column(name = "is_deleted")       private Boolean isDeleted = false;
    @Column(name = "created_by")       private Long createdBy;
    @Column(name = "created_by_name")  private String createdByName;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_by_name")  private String updatedByName;
    @Column(name = "updated_by_id")    private Long updatedById;
    @PrePersist
    private void setCreateDate() { createdAt = LocalDateTime.now(); }
    @Column(name = "updated_at", updatable = false) private LocalDateTime updatedAt;
}
```

It is a plain JPA `@MappedSuperclass` (not `@Entity` — contributes columns to whatever entity extends
it, no table of its own), annotated with Lombok `@Data` for getters/setters/equals/hashCode/toString.

Fields it contributes, exactly as declared:

| Field | Column | Default | Notes |
|---|---|---|---|
| `isActive` | `is_active` | `true` | |
| `isDeleted` | `is_deleted` | `false` | Soft-delete flag. |
| `createdBy` | `created_by` | — | `Long`, presumably a staff ID (not FK-annotated). |
| `createdByName` | `created_by_name` | — | Denormalized display name. |
| `createdAt` | `created_at`, `updatable = false` | — | Set automatically via the `@PrePersist` hook `setCreateDate()` on insert only. |
| `updatedByName` | `updated_by_name` | — | |
| `updatedById` | `updated_by_id` | — | |
| `updatedAt` | `updated_at`, `updatable = false` | — | **No `@PreUpdate` hook exists anywhere in this class.** Combined with `updatable = false`, this column can never be set through JPA's dirty-checking/auto-update mechanism at all — only an explicit `setUpdatedAt(...)` call before the *initial* insert would ever populate it, and even that would be immediately followed by `updatable=false` preventing any later change. In practice (confirmed at `service/StaffService.java:180`, `ApplicationUtils.getCurrentDateTime()` is called to set `updatedAt` manually in application code), callers work around this by setting the field directly in service code, but the `updatable = false` annotation means Hibernate will silently drop that value from any `UPDATE` statement on an already-persisted row. This looks like a latent bug (inferred) rather than intentional design — flagged here since it affects every entity extending `AbstractModel`.

### Entities extending `AbstractModel`

Confirmed via `grep -rn "extends AbstractModel"` across `src/main/java`:

| Entity | File |
|---|---|
| `Clientservice` | `src/main/java/com/cargosetu/model/Clientservice.java` |
| `Company` | `src/main/java/com/cargosetu/model/Company.java` |
| `CustomerInquiry` | `src/main/java/com/cargosetu/model/CustomerInquiry.java` |
| `Department` | `src/main/java/com/cargosetu/model/Department.java` |
| `Permission` | `src/main/java/com/cargosetu/model/Permission.java` |
| `Plans` | `src/main/java/com/cargosetu/model/Plans.java` |
| `Role` | `src/main/java/com/cargosetu/model/Role.java` |
| `Staff` | `src/main/java/com/cargosetu/model/Staff.java` |
| `DocumentMaster` | `src/main/java/com/cargosetu/document/model/DocumentMaster.java` |
| `NotificationAudit` | `src/main/java/com/cargosetu/notification/model/NotificationAudit.java` |
| `NotificationTemplate` | `src/main/java/com/cargosetu/notification/model/NotificationTemplate.java` |

That is **11 of 38** `@Entity`-annotated classes found in the codebase (grep for `@Entity` across
`src/main/java`). Note also `notification/model/NotificationAuditBase.java`: it is itself an abstract
`@MappedSuperclass` that *extends* `AbstractModel`, apparently intended as an intermediate base for
notification audit entities — but `grep -rl "extends NotificationAuditBase"` returns no results, so it
currently has zero users; `NotificationAudit` extends `AbstractModel` directly instead. Flagging as
dead/unused code (inferred).

### Entities that do NOT extend `AbstractModel`

The remaining 27 `@Entity` classes manage their own `createdAt`/`updatedAt`/soft-delete columns (or omit
them) independently, entity by entity — confirmed by their absence from the `extends AbstractModel`
grep:

| Package | Entities |
|---|---|
| `customer/entity` | `Charges`, `Customer`, `ExtraCharges`, `PackagingMaterial`, `ShiftItemDamage`, `ShiftPackage`, `ShiftRoom`, `Shifting` |
| `document/model` | `DocumentUpload` |
| `model` | `Branch`, `CompanyAsset`, `Driver`, `DriverAdvance`, `DriverVehicleAssignment`, `OtpVerification`, `RefreshToken`, `Template`, `Trip`, `TripDocument`, `TripExpense`, `TripStatusHistory`, `TripTracking`, `Vehicle`, `VehicleLocation` |
| `shifting/entity` | `ShiftInventoryItem`, `ShiftInventoryPackaging`, `ShiftInventoryPhoto` |

So the audit-column base class covers less than a third of entities; most of the app's tables (customer,
shifting, trip, vehicle/driver, document-upload domains) have no shared audit-column contract at all.
This is consistent with `docs/entities.md`'s per-entity field listings — this file adds the
extends/doesn't-extend split which that doc does not call out explicitly.

## 4. Exception Handling

This module *is* the app's exception-handling layer — there is exactly one `@RestControllerAdvice`.

### `CustomValidationException` — `src/main/java/com/cargosetu/handler/CustomValidationException.java`

```java
@Getter
public class CustomValidationException extends RuntimeException {
    private final Integer errorCode;

    public CustomValidationException(Integer errorCode, String errorMessage) { super(errorMessage); this.errorCode = errorCode; }
    public CustomValidationException(Integer errorCode, String errorMessage, Throwable cause) { super(errorMessage, cause); this.errorCode = errorCode; }
}
```

An unchecked `RuntimeException` carrying an app-defined `errorCode` (typically a `CommonStatusCode`
constant, e.g. `400`/`401`/`422`) alongside the message. It is thrown throughout the service layer for
business-rule violations (e.g. `AuthService`, `OtpService` — see `auth-security.md` for those call
sites).

### `GlobalExceptionHandler` — `src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomValidationException.class)
    public ResponseEntity<ApiResponseDto> handleCustomValidation(CustomValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ApiResponseDto responseDto = new ApiResponseDto(ex.getErrorCode(), ex.getMessage(), null);
        return ResponseEntity.badRequest().body(responseDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto> handleExceptionValidation(Exception ex) {
        log.warn("Exception occurred, error: {}", ex.getMessage());
        ApiResponseDto responseDto = new ApiResponseDto(CommonStatusCode.INTERNAL_SERVER_ERROR, ex.getMessage(), null);
        return ResponseEntity.badRequest().body(responseDto);
    }
}
```

There are exactly two `@ExceptionHandler` methods, and the prior claim in
`docs/services-and-known-issues.md` (lines 156–160) is **confirmed accurate by direct reading**:

| Handler | Catches | HTTP status returned | Body `responseCode` | Body `responseMessage` |
|---|---|---|---|---|
| `handleCustomValidation` | `CustomValidationException` | `ResponseEntity.badRequest()` → **HTTP 400**, unconditionally | `ex.getErrorCode()` — whatever the throw site passed (could be `401`, `422`, etc., but the *HTTP* status is always 400 regardless) | `ex.getMessage()` |
| `handleExceptionValidation` | `Exception` (catch-all, so anything not already handled) | `ResponseEntity.badRequest()` → **HTTP 400**, unconditionally | `CommonStatusCode.INTERNAL_SERVER_ERROR` = `500` (`src/main/java/com/cargosetu/utils/CommonStatusCode.java`) | `ex.getMessage()` (raw exception message, e.g. a `NullPointerException`'s message, leaked to the client) |

Both handlers call `ResponseEntity.badRequest()` — there is no branching on `ex.getErrorCode()` or any
other condition to pick a different `ResponseEntity` status. So: a `CustomValidationException(401, ...)`
thrown deep in a service produces an HTTP response whose **transport-level status is 400** while the
**JSON body says `responseCode: 401`**, and a genuinely unexpected server error (NPE, DB failure, etc.)
also produces **HTTP 400** with `responseCode: 500` in the body. Any HTTP-status-aware client/proxy/retry
logic (webhook consumers, API gateways, monitoring) that trusts the transport status rather than parsing
the JSON body will misclassify every error as a 400 Bad Request, including true 401/403/500-class
failures. This matches and is fully verified against `docs/services-and-known-issues.md`'s claim.

Both response bodies use `ApiResponseDto`, not `GenericDataDTO` — even for exceptions thrown out of
`GenericDataDTO`-based controllers (e.g. `StaffController`). So a client of a `GenericDataDTO` endpoint
that hits an exception path gets back an `ApiResponseDto`-shaped body instead, a further envelope
inconsistency worth noting alongside §2.

## 5. Configuration

### OpenAPI / Swagger — `src/main/java/com/cargosetu/utils/SwaggerConfig.java`

```java
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cargo-Setu API")
                        .version("1.0")
                        .description("API documentation for the Cargo-Setu "));
    }
}
```

Minimal `springdoc-openapi`-style config: sets only title/version/description metadata. No
`SecurityScheme` (e.g. bearer-JWT scheme) is registered, so — inferred — the generated Swagger UI has no
built-in "Authorize" button wired to send a `Bearer` token; testing authenticated endpoints via
`/swagger-ui/**` would require manually adding the header some other way. `SecurityConfig` permits
`/swagger-ui/**` and `/v3/api-docs/**` unauthenticated (see `auth-security.md`/`SecurityConfig`), so the
docs UI itself is reachable without a token even though most documented endpoints require one.

### WebSocket / STOMP — `src/main/java/com/cargosetu/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:4200").withSockJS();
    }
}
```

- Uses Spring's simple in-memory STOMP broker (not an external broker like RabbitMQ) at destination
  prefix `/topic` for server→client broadcast, and `/app` as the prefix for client→server
  `@MessageMapping` destinations.
- The single STOMP endpoint is `/ws`, with SockJS fallback enabled.
- `setAllowedOriginPatterns("http://localhost:4200")` — **only `http://localhost:4200` is allowed** to
  open a STOMP/SockJS handshake. This is a single hardcoded origin, not a list, and does **not** include
  any of the production origins that `SecurityConfig.corsConfigurationSource()` allows for regular REST
  calls (see below) — e.g. `https://code-setu-app-ui.onrender.com` is not in this list. Inferred
  consequence: a deployed frontend on those production origins would have its WebSocket handshake
  rejected by this allowed-origin-patterns check even though its plain HTTP/REST calls pass CORS. This
  has not been confirmed against a running deployment; flagged as an inferred inconsistency between the
  two configs.
- `SecurityConfig` separately `permitAll()`s `/ws/**` and `/topic/**` at the HTTP security-filter level
  (see `auth-security.md`), which is necessary but not sufficient — this `WebSocketConfig` origin
  restriction is a second, independent gate.

### CORS — `SecurityConfig.corsConfigurationSource()` (`src/main/java/com/cargosetu/config/SecurityConfig.java`)

Quoted exactly from source:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(
        "http://127.0.0.1:31201",
        "http://localhost:4200",
        "https://code-setu-app.onrender.com",
        "https://code-setu-app-ui.umeshsharma-code.workers.dev",
        "https://code-setu-app-ui.onrender.com"
    ));
    configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE"));
    configuration.setAllowedHeaders(List.of("Authorization","Content-Type"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

| Setting | Value |
|---|---|
| Allowed origins | `http://127.0.0.1:31201`, `http://localhost:4200`, `https://code-setu-app.onrender.com`, `https://code-setu-app-ui.umeshsharma-code.workers.dev`, `https://code-setu-app-ui.onrender.com` (exact list, `setAllowedOrigins` — not origin *patterns*, so no wildcards are honored here) |
| Allowed methods | `GET`, `POST`, `PUT`, `DELETE` — **note `PATCH` and `OPTIONS` are not explicitly listed**; `OPTIONS` preflight is typically handled by Spring's CORS filter regardless, but any endpoint using `PATCH` (none observed in this codebase) would be blocked. |
| Allowed headers | `Authorization`, `Content-Type` only — any other custom request header a client sends would be rejected by CORS preflight. |
| Credentials | `setAllowCredentials(true)` — cookies/auth headers are allowed cross-origin for the listed origins. |
| Path | Registered for `/**` (every path). |

This CORS config applies globally via `http.cors(Customizer.withDefaults())` in the same
`SecurityFilterChain` bean, wired to this `corsConfigurationSource()` bean by Spring Security's default
CORS integration. Confirms and reproduces exactly what `docs/services-and-known-issues.md` line 129
already summarized ("CORS allow-list includes `localhost:4200`, `code-setu-app.onrender.com`, ...").

## 6. Mapper Infrastructure

### `GlobalMapperConfig` — `src/main/java/com/cargosetu/config/GlobalMapperConfig.java`

```java
@MapperConfig(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface GlobalMapperConfig {
}
```

A MapStruct `@MapperConfig` marker interface (no methods) that centralizes two settings for every mapper
that references it:
- `componentModel = "spring"` — generated mapper implementations are Spring beans (`@Component`),
  injectable via constructor/field injection rather than accessed through a static `INSTANCE`.
- `nullValuePropertyMappingStrategy = IGNORE` — for `updateFromDto(...)`-style partial-update methods, a
  `null` field on the source DTO is **not** written over the target entity's existing value (i.e. `null`
  means "leave unchanged", not "clear the field"). This is the behavior every partial-update endpoint in
  the app relies on implicitly.

Confirmed used by 19 mapper interfaces (`grep -rl GlobalMapperConfig`), all via
`@Mapper(config = GlobalMapperConfig.class, ...)`, e.g. `StaffMapper`:

```java
@Mapper(config = GlobalMapperConfig.class, uses = {RoleMapper.class, DepartmentMapper.class})
public interface StaffMapper extends BaseUpdateMapper<StaffDTO, Staff> {
    @Mapping(source = "password", target = "password", ignore = true)
    StaffDTO toDto(Staff entity);
}
```
(`src/main/java/com/cargosetu/mapper/StaffMapper.java`)

Other mappers using it: `ChargesMapper`, `ClientServiceMapper`, `CustomerMapper`, `DepartmentMapper`,
`DocumentMasterMapper`, `DocumentUploadMapper`, `ExtraChargesMapper`, `PermissionMapper`, `RoleMapper`,
`ShiftingMapper`, `TripDocumentMapper`, `TripExpenseMapper`, `TripMapper`, `TripStatusHistoryMapper`
(all under `src/main/java/com/cargosetu/mapper/`), plus `PackagingMaterialMapper`,
`ShiftInventoryItemMapper`, `ShiftInventoryPackagingMapper`, `ShiftInventoryPhotoMapper` (under
`src/main/java/com/cargosetu/shifting/mapper/`).

### `BaseUpdateMapper<D, E>` — `src/main/java/com/cargosetu/mapper/base/BaseUpdateMapper.java`

```java
public interface BaseUpdateMapper<D, E> {
    E toEntity(D dto);                             // CREATE
    D toDto(E entity);                             // READ
    void updateFromDto(D dto, @MappingTarget E entity);  // UPDATE (in-place, null-safe per GlobalMapperConfig)
}
```

A generic contract (`D` = DTO type, `E` = entity type) that standardizes the three operations every
feature mapper needs: create-from-DTO, read-to-DTO, and in-place partial update. Every mapper listed
above extends `BaseUpdateMapper<SomeDTO, SomeEntity>` and lets MapStruct generate the implementation at
compile time, only overriding individual field mappings (like `StaffMapper`'s `password` exclusion)
where the default 1:1-by-name mapping isn't correct.

## 7. URL Constants Reference

### `BaseURL` — `src/main/java/com/cargosetu/utils/BaseURL.java`

A plain interface (all members implicitly `public static final`), providing the global API prefix plus
per-resource path segments:

| Constant | Value |
|---|---|
| `BASE_URL` | `/api/v2/cargosetu` |
| `ROLES` | `/role` |
| `PERMISSION` | `/permission` |
| `DEPARTMENT` | `/department` |
| `COMPANY` | `/company` |
| `DOCUMENT` | `/document` |
| `SYSTEM_CONFIG` | `/system/configuration` |
| `DOCS_UPLOADS` | `/docs/uploads` |
| `VEHICLE` | `/vehicle` |
| `LOCATION` | `/location` |
| `DRIVER` | `/drivers` |
| `ASSIGNMENTS` | `/assignments` |
| `TEMPLATES` | `/templates` |
| `BRANCHES` | `/branches` |
| `PUBLIC_PLAN` | `public` (no leading slash — differs from every other constant here) |
| `INQUERY` | `/api/inquiries` (sic — misspelled "INQUERY"; also notable that it hardcodes its own `/api` prefix rather than composing with `BASE_URL`, so `InquiryController`'s effective path is `BaseURL.PUBLIC_PLAN + BaseURL.INQUERY` = `"public" + "/api/inquiries"` = `public/api/inquiries`, not nested under `/api/v2/cargosetu` at all — confirmed from `InquiryController`'s `@RequestMapping(BaseURL.PUBLIC_PLAN + BaseURL.INQUERY)`). |

### `UrlConstants` — `src/main/java/com/cargosetu/config/utils/UrlConstants.java`

```java
public class UrlConstants {
    public static final String LOGIN_URL = "/login";
    public static final String OTP = "/otp";
    public static final String REGISTER_USER = "/user/register";
    public static final String LOGOUT_URL = "/logout";
    public static final String AUTH = "/auth";
    public static final String pages = "/pages";
    public static final String DOCUMENT_MASTER = "/document-master";
    public static final String UPLOAD_DOCUMENT = "/doc-upload";
    public static final String PLAN = "/plan";
    public static final String PUBLIC = "/public";
    public static final String SERVER_ALIVE = "/isAlive";

    public interface StaffUrlConstants { String STAFF = "/staff"; }
    public interface CustomerUrlConstants { String CUSTOMER = "/customer"; String SHIFTS = "/shifts"; }
    public interface ShiftingUrlConstants { String SHIFT_INVENTORY = "/shift-inventory"; }
    public interface TripUrlConstants {
        String TRIP = "/trip";
        String TRIP_STATUS_HISTORY = "/trip-status-history";
        String TRIP_EXPENSE = "/trip-expense";
        String TRIP_DOCUMENT = "/trip-document";
    }
}
```

| Constant | Value | Notes |
|---|---|---|
| `LOGIN_URL` | `/login` | |
| `OTP` | `/otp` | Declared but not confirmed as the literal segment used by `OtpAuthController` — that controller hardcodes `/auth/otp` directly per `auth-security.md` rather than composing `AUTH + OTP`; flagged there already. |
| `REGISTER_USER` | `/user/register` | |
| `LOGOUT_URL` | `/logout` | |
| `AUTH` | `/auth` | |
| `pages` | `/pages` | Only constant with a lowercase name (inconsistent naming convention vs. every other `UPPER_SNAKE_CASE` constant in this file). |
| `DOCUMENT_MASTER` | `/document-master` | |
| `UPLOAD_DOCUMENT` | `/doc-upload` | |
| `PLAN` | `/plan` | |
| `PUBLIC` | `/public` | |
| `SERVER_ALIVE` | `/isAlive` | |
| `StaffUrlConstants.STAFF` | `/staff` | |
| `CustomerUrlConstants.CUSTOMER` | `/customer` | |
| `CustomerUrlConstants.SHIFTS` | `/shifts` | |
| `ShiftingUrlConstants.SHIFT_INVENTORY` | `/shift-inventory` | |
| `TripUrlConstants.TRIP` | `/trip` | |
| `TripUrlConstants.TRIP_STATUS_HISTORY` | `/trip-status-history` | |
| `TripUrlConstants.TRIP_EXPENSE` | `/trip-expense` | |
| `TripUrlConstants.TRIP_DOCUMENT` | `/trip-document` | |

This reproduces the full constant set from `docs/api-urls.md`'s base-path summary but sourced directly
from the two constant files rather than inferred from controller annotations — cross-check against
`docs/api-urls.md` (base path table, line 4 onward) shows no discrepancies for the resources it lists.

## 8. Utilities

### `ApplicationUtils` — `src/main/java/com/cargosetu/utils/ApplicationUtils.java`

```java
public class ApplicationUtils {
    public static LoggedInUser getCurrentUser() { ... }        // static
    public static LocalDateTime getCurrentDateTime() { ... }   // static
    public Long getLoggedInStaffId() { ... }                   // instance method
    public LoggedInUser getLoggedInUser() { ... }               // instance method
}
```

Not a Spring bean — no `@Component`/`@Service` annotation, no constructor, so `getLoggedInStaffId()` and
`getLoggedInUser()` (the two *instance* methods) can only be called by first `new ApplicationUtils()`-ing
it or, if it were ever injected, would require a no-arg-constructible bean registration that doesn't
exist here (unverified whether any call site actually instantiates it via `new` — none was found in the
grep below, meaning the two instance methods may currently be dead code).

| Method | Static? | Behavior |
|---|---|---|
| `getCurrentUser()` | Yes | Reads `SecurityContextHolder.getContext().getAuthentication()`; if non-null, authenticated, and its principal is a `LoggedInUser` (`config/dto/LoggedInUser.java`, implements Spring Security's `UserDetails` — see `auth-security.md`), returns it cast; otherwise returns `null`. |
| `getCurrentDateTime()` | Yes | Returns `LocalDateTime.now()`. Pure convenience wrapper. |
| `getLoggedInStaffId()` | No (instance) | Same `SecurityContextHolder` read, but defaults to `1L` if authentication is null or any exception occurs (broad `catch (Exception e)`, swallowed), rather than returning `null`. **No call sites found** for this method specifically. |
| `getLoggedInUser()` | No (instance) | Functionally identical logic to the static `getCurrentUser()` (duplicated, not delegated) — same null-safety checks, same cast. **No call sites found** for this method specifically. |

Confirmed usage sites (`grep -rn "ApplicationUtils\."`), all using the two **static** methods:

| Call site | Method used | Purpose |
|---|---|---|
| `demo/DemoController.java:14` | `getCurrentUser()` | Reads `.getUsername()` for a demo/greeting endpoint. |
| `service/impl/ClientServiceImpl.java:82` | `getCurrentUser()` | `.getStaffId()` used to populate `createdBy` on a new `Clientservice` entity. |
| `service/impl/DepartmentImpl.java:184` | `getCurrentUser()` | Same pattern, populating `Department.createdBy`. |
| `service/impl/PermissionImpl.java:180` | `getCurrentUser()` | Same pattern, populating `Permission.createdBy`. |
| `service/StaffService.java:180-182` | `getCurrentDateTime()` + `getCurrentUser()` | Sets `Staff.updatedAt`, `updatedByName` (concatenated first+last name, with `"-"` fallback), and `updatedById` — this is the manual work-around for `AbstractModel.updatedAt` having no `@PreUpdate` hook, noted in §3. |

So in practice only the two static methods (`getCurrentUser`, `getCurrentDateTime`) are actually used
across the codebase to get "the current logged-in user," always via `ApplicationUtils.getCurrentUser()`
directly (static call), never through injection.

### `UUIDGenerator` — `src/main/java/com/cargosetu/utils/UUIDGenerator.java`

```java
public class UUIDGenerator {
    private static final String PREFIX = "SCSETU";
    private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int RANDOM_LENGTH = 9;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate() { ... }                       // PREFIX + 9 random chars
    public static String generate(String prefix, String suffix) { ... } // prefix (or PREFIX if blank) + 9 random chars + suffix
}
```

- Default format: `SCSETU` followed by 9 characters drawn (with replacement, via `SecureRandom`) from
  `A-Z0-9` — e.g. `SCSETU7K2QM9XZP` (illustrative, not an actual generated value). Total length 15
  characters (`"SCSETU".length()` = 6 + 9 = 15).
- Overload lets a caller substitute a different prefix and/or append a fixed suffix; if the given prefix
  is null/blank it falls back to `SCSETU`.
- Not a cryptographic/collision-checked UUID (no DB uniqueness re-check visible in this class itself —
  any dedup enforcement would have to be a DB unique-constraint or caller-side check, not verified here).

Confirmed call sites (`grep -rn "UUIDGenerator.generate"`):

| Call site | Usage |
|---|---|
| `customer/service/CustomerService.java:70` | `UUIDGenerator.generate()` — default form, likely for `Customer`'s UUID-like business identifier. |
| `service/impl/CompanyServImpl.java:67` | `UUIDGenerator.generate(prefix, suffix)` — custom prefix/suffix form for `Company.companyUUID`. |
| `service/impl/DepartmentImpl.java:172` | `UUIDGenerator.generate()` — default form. |
| `service/StaffService.java:64` | `UUIDGenerator.generate()` — default form, for a `Staff` business identifier. |
| `utils/utillity/FileUtility.java:267` | `UUID.randomUUID() + UUIDGenerator.generate() + "_" + originalName` — combined with Java's standard `UUID.randomUUID()` to build a unique uploaded-file name; note this is the one call site that also uses `java.util.UUID`, not just this class. |

So `SCSETU`-prefixed identifiers are the app's convention for human-facing business IDs (company UUID,
staff/department/customer identifiers), distinct from the database's numeric primary keys and distinct
from standard RFC-4122 UUIDs (only used incidentally in `FileUtility` for filename uniqueness, not as a
business identifier).

## 9. Dependencies Between Modules

This module has no dependency *on* any feature module — it is a leaf/foundation layer. Every other
module in this codebase depends on it, directly and pervasively:

- **Every controller** returns `ApiResponseDto` or `GenericDataDTO` (§2) and is registered under a path
  built from `BaseURL`/`UrlConstants` (§7).
- **Every exception thrown from a service** (`CustomValidationException` or any uncaught `Exception`) is
  caught by `GlobalExceptionHandler` (§4) — there is no per-module exception handling anywhere else in
  the app.
- **~29% of entities** (11 of 38, §3) extend `AbstractModel` for audit columns; the rest manage their own.
- **19 of the app's MapStruct mappers** (§6) share `GlobalMapperConfig`'s Spring-bean/null-ignore
  settings via `BaseUpdateMapper`.
- **Service-layer code that needs "who is the current user"** (`ClientServiceImpl`, `DepartmentImpl`,
  `PermissionImpl`, `StaffService`) goes through `ApplicationUtils.getCurrentUser()` (§8), which in turn
  reads the `LoggedInUser` principal that the auth/security module's `JwtAuthenticationFilter` places on
  the `SecurityContext` — for how that principal gets there in the first place (JWT parsing, claims,
  `SecurityConfig`'s filter chain and matcher rules), see **`../modules/auth-security.md`**, not this
  file.
- **CORS and the WebSocket/STOMP endpoint** (§5) gate which origins any frontend (web or otherwise) can
  call the REST API or open a `/ws` STOMP connection from — relevant to every module that exposes an
  endpoint, i.e. all of them.
- **Business-identifier generation** (`UUIDGenerator`, §8) is used by `Customer`, `Company`,
  `Department`, and `Staff` creation flows specifically (per the confirmed call sites above) — not a
  blanket convention applied to every entity.

In short: there is no module in this codebase that could function with this one removed. Changes here
(especially to `ApiResponseDto`/`GenericDataDTO` field shapes, `GlobalExceptionHandler`'s status-code
logic, or `AbstractModel`'s columns) are the highest-blast-radius changes possible in this app and should
be cross-checked against every consumer listed in the tables above before modification.

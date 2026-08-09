# System Configuration (Clientservice) & Subscription Plans

> Standalone module doc covering two small, unrelated-but-adjacent admin modules that happen to live at a
> similar "platform admin" altitude: generic key/value **system configuration** (`Clientservice`) and
> **subscription Plans**. They share nothing at the data or business-logic level — they are documented
> together purely for organizational convenience. Cross-reference: [`../controllers.md`](../controllers.md),
> [`../entities.md`](../entities.md), [`../api-urls.md`](../api-urls.md),
> [`../services-and-known-issues.md`](../services-and-known-issues.md). Every claim below was checked
> against the source files listed in each section; anything not directly verifiable is explicitly marked
> **[inferred]** or **[unverified]**.

---

# Part A: System Configuration (`Clientservice`)

## A.1 Overview

`Clientservice` is a generic key/value store table (`tblmclientservice`) used to hold miscellaneous
system-configuration entries — a `name` (lookup key), a `value`, a `module` tag, and a `status`. It is
exposed via a plain CRUD REST controller (`ClientServiceController`) with no pagination, no search, and
(as shown in Part A.6) no method-level permission checks. Several unrelated feature services look up rows
in this table by `name` to determine a **file-upload directory name** — see Part A.9 for the verified (and
notably broken) way that value is actually consumed.

Files read in full for this section:
- `src/main/java/com/cargosetu/model/Clientservice.java`
- `src/main/java/com/cargosetu/controller/ClientServiceController.java`
- `src/main/java/com/cargosetu/service/ClientServices.java`
- `src/main/java/com/cargosetu/service/impl/ClientServiceImpl.java`
- `src/main/java/com/cargosetu/dto/ClientServiceDTO.java`
- `src/main/java/com/cargosetu/repo/ClientServiceRepo.java`
- `src/main/java/com/cargosetu/mapper/ClientServiceMapper.java`

## A.2 API Controller & Endpoints

**Controller:** `ClientServiceController` (`src/main/java/com/cargosetu/controller/ClientServiceController.java`)
**Base path:** `BaseURL.BASE_URL + BaseURL.SYSTEM_CONFIG` = `/api/v2/cargosetu/system/configuration`
(`src/main/java/com/cargosetu/utils/BaseURL.java` lines 4, 10)

| Method | Path | Handler | Request Body | Response | `@PreAuthorize` |
|---|---|---|---|---|---|
| GET | `/api/v2/cargosetu/system/configuration` | `getClientService()` | — | `GenericDataDTO` (list in `dataList`) | **None present** |
| POST | `/api/v2/cargosetu/system/configuration` | `createClientService(ClientServiceDTO)` | `ClientServiceDTO` | `GenericDataDTO` | **None present** |
| GET | `/api/v2/cargosetu/system/configuration/{id}` | `getClientServiceById(Integer id)` | — | `GenericDataDTO` | **None present** |
| PUT | `/api/v2/cargosetu/system/configuration/{id}` | `updateClientService(Integer id, ClientServiceDTO)` | `ClientServiceDTO` | `GenericDataDTO` | **None present** |

There is **no DELETE endpoint** for `Clientservice` — confirmed absent from the controller (only GET/GET-by-id/POST/PUT exist).

No `@PreAuthorize` annotation appears anywhere in `ClientServiceController.java` — verified by reading the
full file (no `org.springframework.security.access.prepost.PreAuthorize` import, no annotation on any
method). See Part A.6 for what actually gates these endpoints.

## A.3 Business / Service Logic

**Interface:** `ClientServices` (`src/main/java/com/cargosetu/service/ClientServices.java`) — oddly
annotated `@Service` even though it's an interface (has no functional effect; Spring only registers the
`@Service` on the concrete `ClientServiceImpl`).

**Implementation:** `ClientServiceImpl` (`src/main/java/com/cargosetu/service/impl/ClientServiceImpl.java`)

| Method | Logic |
|---|---|
| `getClientServiceList()` | `clientServiceRepo.findAll()` → wraps whole list into `GenericDataDTO.dataList`, code 200 ("Success"). On exception, sets a failure message but **does not set a response code** (leaves default `null`) — verified at lines 134–146. |
| `getClientServiceById(Integer id)` | `findById(id)`; 200 + row in `data` if found, 404 + message if not, 500 on exception. |
| `createClientService(ClientServiceDTO pojo)` | First checks `clientServiceRepo.existsByNameAndValue(name, value)` — if a row with the same `(name, value)` pair already exists, returns **409 Conflict** ("...cannot create duplicate!") without inserting. Otherwise builds a new `Clientservice`, sets `staffId` from the current authenticated principal (see below), `status` from `pojo.getIsActive()`, `createdByName`/`createdBy` from the security context, and saves. Returns 201 on success, 500 on exception. |
| `updateClientService(Integer id, ClientServiceDTO pojo)` | Null-body guard → 400. Otherwise loads the row by id, applies `clientServiceMapper.updateFromDto(pojo, data)` (MapStruct partial update, null-safe — see A.4), stamps `updatedAt = now()`, saves. 200 on success, 404 if id not found, 500 on exception. |
| `getStaffIdFromCurrentStaff()` / `getLoggedInUSer()` (sic) | Both pull `SecurityContextHolder.getContext().getAuthentication()` and cast `getPrincipal()` to `LoggedInUser`; swallow all exceptions via `e.printStackTrace()` (not the injected `log`) and return `null` on failure. |

**Validation note:** `ClientServiceDTO` has **no Bean Validation annotations** (`@NotNull`/`@NotBlank`
etc. — verified absent from the DTO source) and the controller does not mark `@Valid` on the request body,
so `createClientService`/`updateClientService` accept any field combination including nulls; the only
server-side guard against garbage data is the DB `NOT NULL` constraints on `name`/`value`/`module`/`status`/
`staffId` (see A.5), which would surface as an unhandled `DataIntegrityViolationException` → caught by the
generic `try/catch(Exception e)` in the service methods → 500 response.

## A.4 DTOs / Request-Response Models

**`ClientServiceDTO`** (`src/main/java/com/cargosetu/dto/ClientServiceDTO.java`):

| Field | Type | Notes |
|---|---|---|
| `clientId` | `Integer` | Not mapped to the entity's `id` field by name (MapStruct would need explicit mapping; not present — see mismatch note below) |
| `value` | `String` | |
| `module` | `String` | |
| `name` | `String` | |
| `staffId` | `Integer` | Present on the DTO, but **never read** by `ClientServiceImpl.createClientService` — the service sets `staffId` itself from the security context, not from the incoming DTO |
| `isActive` | `String` | Maps to entity's `status` field on create (`clientservice.setStatus(pojo.getIsActive())`) — **not** to `AbstractModel.isActive` (`Boolean`); field-name collision with the inherited boolean flag is a likely source of confusion |
| `isDeleted` | `Boolean` | Present on DTO; not explicitly set by the service (falls back to `AbstractModel.isDeleted` default `false` via MapStruct's `updateFromDto`, or is left null on create since the service builds the entity manually field-by-field) |
| `createdByName` | `String` | |
| `createdBy` | `Long` | |
| `createdAt` | `LocalDateTime` | |
| `updatedAt` | `LocalDateTime` | |

**`ClientServiceMapper`** (`src/main/java/com/cargosetu/mapper/ClientServiceMapper.java`) — MapStruct
interface, `@Mapper(config = GlobalMapperConfig.class)`, extends
`BaseUpdateMapper<ClientServiceDTO, Clientservice>` (`toEntity`/`toDto`/`updateFromDto`).
`GlobalMapperConfig` (`src/main/java/com/cargosetu/config/GlobalMapperConfig.java`) sets
`componentModel="spring"` and `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE`
— so `updateFromDto` (used by `updateClientService`) only overwrites fields that are non-null in the
incoming DTO, leaving the rest of the persisted row untouched. Only `updateFromDto` is actually called
anywhere in the codebase for this entity; `toEntity`/`toDto` are unused by `ClientServiceImpl` (it builds/
reads the entity by hand in create/list/getById).

**Response envelope:** `GenericDataDTO` (`src/main/java/com/cargosetu/utils/GenericDataDTO.java`) —
`{responseMessege (sic), responseCode, dataList, data, errors, totalRecords, pageRecords,
currentPageNumber, totalPages}`. The pagination fields are declared but never populated by this module
(no pagination logic in `ClientServiceImpl`).

## A.5 Entity & Database

**Table:** `tblmclientservice`
**File:** `src/main/java/com/cargosetu/model/Clientservice.java`
**Extends:** `AbstractModel` (`@MappedSuperclass`, `src/main/java/com/cargosetu/abstracts/AbstractModel.java`)

| Column | Java field | Type | Constraints |
|---|---|---|---|
| `serviceid` | `id` | `Integer` | `@Id`, `IDENTITY`, `NOT NULL`, length 40 |
| `name` | `name` | `String` | `NOT NULL`, length 40 |
| `value` | `value` | `String` | `NOT NULL`, length 40 |
| `STAFFID` | `staffId` | `Integer` | `NOT NULL`, length 40, **`updatable = false`** (JPA will silently skip this column on UPDATE statements after initial insert) |
| `module` | `module` | `String` | `NOT NULL`, length 40 |
| `status` | `status` | `String` | `NOT NULL`, length 40 |

Inherited from `AbstractModel` (all plain columns, no special constraints beyond nullability defaults):
`is_active` (`Boolean`, default `true`), `is_deleted` (`Boolean`, default `false`), `created_by` (`Long`),
`created_by_name` (`String`), `created_at` (`LocalDateTime`, `updatable=false`, auto-set via `@PrePersist`),
`updated_by_name` (`String`), `updated_by_id` (`Long`), `updated_at` (`LocalDateTime`, — note:
`@Column(updatable = false)` is set on `updated_at` in `AbstractModel`, which is almost certainly a
copy-paste mistake since `ClientServiceImpl.updateClientService` explicitly calls
`data.setUpdatedAt(LocalDateTime.now())` before save, but Hibernate will not emit that column in the
UPDATE SQL because of the `updatable=false` flag — **[inferred]**: the timestamp set in Java is likely
discarded silently at the SQL level; not runtime-verified against an actual DB).

No `companyId` / tenant column exists on `Clientservice` or `AbstractModel` — this table is **platform-wide**, not tenant-scoped (consistent with `ClientServiceRepo.findAll()` returning every row to every caller regardless of company).

## A.6 Repository

**`ClientServiceRepo`** (`src/main/java/com/cargosetu/repo/ClientServiceRepo.java`):
```java
public interface ClientServiceRepo extends JpaRepository<Clientservice, Integer> {
    boolean existsByNameAndValue(String name, String value);
    Optional<Clientservice> findByName(String name);
}
```
Plain Spring Data JPA, no custom `@Query`. `findByName` is the method every downstream consumer relies on
(Part A.9) — note it returns the **first/only** match; if two rows ever share the same `name` (nothing in
the schema or service layer prevents this — the uniqueness check in `createClientService` is on
`(name, value)` together, not `name` alone), `findByName` behavior for duplicate names is whatever
Spring Data JPA does for a non-`List`/`Optional`-of-single-result query when multiple rows match
(`IncorrectResultSizeDataAccessException` at runtime) — **[inferred]**, not exercised in tests.

## A.7 Security & Authentication

Verified directly from `src/main/java/com/cargosetu/config/SecurityConfig.java` and
`src/main/java/com/cargosetu/config/MethodSecurityConfig.java`:

- `ClientServiceController` has **zero `@PreAuthorize` annotations** on any endpoint (confirmed by reading
  the full controller file).
- The global filter chain (`SecurityConfig.filterChain`) does **not** list `/api/v2/cargosetu/system/**` or
  `/system/configuration/**` in its `permitAll()` matchers, and falls through to
  `.anyRequest().authenticated()`. So every `Clientservice` endpoint requires a **valid authenticated JWT**
  (any logged-in staff user, of any role/permission set), but there is **no permission-code gate at all** —
  any authenticated user, regardless of role, can list/create/read/update system-config rows.
- Even if a `@PreAuthorize` were added to this controller, it would currently be **inert**:
  `MethodSecurityConfig` sets `@EnableMethodSecurity(prePostEnabled = false)`
  (`src/main/java/com/cargosetu/config/MethodSecurityConfig.java` line 7), which globally disables
  `@PreAuthorize`/`@PostAuthorize` processing for the whole application (this is the same finding already
  flagged in [`../services-and-known-issues.md`](../services-and-known-issues.md) §4 for other modules —
  confirmed again here by direct read of the same file).

**Net effect:** any authenticated staff member, in any company, can read and modify every system-config
row in the system.

## A.8 Exception Handling

No feature-specific exceptions are thrown in this module. All service methods wrap their body in a plain
`try/catch (Exception e)` and translate failures into a `GenericDataDTO` with a `responseCode`/message
(500 on unexpected exceptions, 404/409/400 for the explicit business branches described in A.3) — this
module **does not** go through `GlobalExceptionHandler`
(`src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`) in the normal case, because it never
lets an exception propagate out of the controller method (everything is caught inside the service layer
and turned into a 200-status HTTP response carrying a non-200 `responseCode` in the JSON body — the HTTP
status line itself is always 200 OK for this controller, since no method returns `ResponseEntity` with an
explicit status). This is a distinct pattern from the Plans module (Part B.8), which does propagate
exceptions to `GlobalExceptionHandler`.

## A.9 Configuration — is `Clientservice` actually consumed as runtime config?

**Yes, partially, and in a way that looks broken.** Four other places in the codebase look up a
`Clientservice` row by a hardcoded `name` key to obtain what is intended to be an **upload-directory path**
for `FileUtility.saveMultipartFile`/`saveMultipleFiles`. Verified by direct read of each call site:

| Consumer | File | Lookup key (`findByName(...)`) | What it does with the result |
|---|---|---|---|
| `TemplateService.createTemplate` | `src/main/java/com/cargosetu/service/TemplateService.java` (line 51) | `"Template"` | `FileUtility.saveMultipartFile(files, clientServiceRepo.findByName("Template").orElseThrow().getName())` |
| `CompanyServImpl` (company asset upload) | `src/main/java/com/cargosetu/service/impl/CompanyServImpl.java` (lines 308–310) | `"COMPANY_ASSET"` | `Clientservice clientservice = ...findByName("COMPANY_ASSET").orElse(null);` then `FileUtility.saveMultipartFile(file, clientservice.getName())` |
| `TripDocumentService.create` | `src/main/java/com/cargosetu/service/TripDocumentService.java` (line 56) | `"TRIP_DOCS"` | `FileUtility.saveMultipartFile(files, clientServiceRepo.findByName("TRIP_DOCS").orElseThrow().getName())` |
| `FileController.uploadDocuments` | `src/main/java/com/cargosetu/controller/FileController.java` (lines 59–63) | `"Staff Document"` | `Clientservice clientService = ...findByName("Staff Document").orElse(null);` then `String fullPath = clientService.getName();` passed to `fileUtility.saveMultipleFiles(files, fullPath)` |

**Verified bug/dead-config finding:** in every one of these four call sites, the code calls
**`.getName()`** on the resolved `Clientservice` row — not **`.getValue()`**. Since `getName()` simply
returns the same literal string that was passed into `findByName(...)` in the first place (e.g.
`"Template"`, `"COMPANY_ASSET"`, `"TRIP_DOCS"`, `"Staff Document"`), the row's `value` column — which is
the field an admin would actually edit via `PUT /api/v2/cargosetu/system/configuration/{id}` to configure
a real upload path — **is fetched from the database and then completely discarded**. The effective upload
base path passed to `FileUtility.saveMultipartFile`
(`src/main/java/com/cargosetu/utils/utillity/FileUtility.java` line 242, which does
`Paths.get(basePath).toAbsolutePath().normalize()`) is always just the literal lookup-key string resolved
relative to the application's working directory (e.g. `./Template/`, `./TRIP_DOCS/`), regardless of what
value the admin has configured through the `Clientservice` CRUD API. **This means the entire
system-configuration module, for its one demonstrated real use case, has no observable effect on
application behavior** — editing or deleting the `Clientservice` row for a given `name` only changes
whether `findByName(...).orElseThrow()`/`.orElse(null)` succeeds (i.e., whether the row *exists* at all,
which some call sites treat as a hard failure via `orElseThrow()` and others tolerate via `orElse(null)`
followed by an unguarded `.getName()` call that would NPE if the row is missing) — the row's actual content
(`value`) plays no role.

This confirms and sharpens the note already in [`../entities.md`](../entities.md) §13 ("Stores system
configuration entries used by internal services (e.g., document upload path config)") — the consumption is
real, but the specific field consumed appears to be the wrong one.

## A.10 External Integrations

None. `Clientservice`/`ClientServices` has no outbound HTTP calls, message queues, or third-party SDK
usage — it is a pure local CRUD-on-MySQL feature.

## A.11 Important Business Rules & Validations

- **Uniqueness:** enforced only in application code, only on create, only on the combined pair
  `(name, value)` — `ClientServiceRepo.existsByNameAndValue(...)` (`ClientServiceImpl.createClientService`,
  line 65). There is **no unique DB constraint** visible on the entity (`@Column` declarations show no
  `unique = true` — verified absent from `Clientservice.java`), and no uniqueness check at all on `name`
  alone, so two rows with the same `name` but different `value` can coexist (which, per A.6, makes
  `findByName` behavior for that `name` ambiguous downstream).
- **Required fields (DB-level only):** `name`, `value`, `staffId`, `module`, `status` are all
  `nullable = false` at the column level; there is no `@Valid`/bean-validation layer catching bad input
  before it reaches the DB (see A.3).
- **`staffId` immutability:** `@Column(updatable = false)` — once a row is created, its `staffId` can never
  be changed via `updateClientService` (Hibernate omits it from UPDATE statements).

## A.12 Dependencies Between Modules

`Clientservice` is consumed (in the broken fashion described in A.9) by:
- `TemplateService` (quotation/template PDF generation — `service/TemplateService.java`)
- `CompanyServImpl` (company asset upload — `service/impl/CompanyServImpl.java`)
- `TripDocumentService` (trip document upload — `service/TripDocumentService.java`)
- `FileController` (staff document upload — `controller/FileController.java`)

All four are one-directional dependencies (they read `ClientServiceRepo`; nothing in `ClientServices`/
`ClientServiceImpl` depends on them). No other module writes to or reads `Clientservice` besides the CRUD
controller itself.

---

# Part B: Subscription Plans

## B.1 Overview

`Plans` is a small, flat, **platform-level** (not tenant-scoped — see B.5) subscription-plan catalog:
name, validity date, price, category. Full CRUD is exposed through `PlanController`, each endpoint gated
by one of three `PLAN_*` permission constants via `@PreAuthorize` — though, as detailed in B.6, those
checks are currently inert application-wide. `PlanController` also exposes a same-named `/plan/public`
endpoint intended (per naming and per the existing `api-urls.md` doc) to be an unauthenticated public
listing for a marketing/pricing page — which, verified against `SecurityConfig`, is **not actually
unauthenticated**. There is, however, a genuinely separate, genuinely unauthenticated controller,
**`PublicPlanControlor`** (sic — typo in the class name), mounted at a completely different top-level path
(`/public/plan`, not under `/api/v2/cargosetu`) that also returns the full plan list with zero auth checks
of any kind — this is almost certainly the controller the front-end's `planscreen` marketing page actually
calls. See B.2 for the full comparison.

Files read in full for this section:
- `src/main/java/com/cargosetu/model/Plans.java`
- `src/main/java/com/cargosetu/controller/PlanController.java`
- `src/main/java/com/cargosetu/controller/PublicPlanControlor.java`
- `src/main/java/com/cargosetu/service/PlanService.java`
- `src/main/java/com/cargosetu/service/impl/PlanServiceImpl.java`
- `src/main/java/com/cargosetu/dto/PlanDTO.java`
- `src/main/java/com/cargosetu/repo/PlanRepository.java`
- `src/main/java/com/cargosetu/config/SecurityConfig.java`, `src/main/java/com/cargosetu/config/JwtAuthenticationFilter.java` (to verify the security claims below)

## B.2 API Controller & Endpoints

**Controller:** `PlanController` (`src/main/java/com/cargosetu/controller/PlanController.java`)
**Base path:** `BaseURL.BASE_URL + UrlConstants.PLAN` = `/api/v2/cargosetu/plan`
(`src/main/java/com/cargosetu/utils/BaseURL.java` line 4, `src/main/java/com/cargosetu/config/utils/UrlConstants.java` line 13)

| Method | Path | Handler | Request Body | Response | `@PreAuthorize` (as written) |
|---|---|---|---|---|---|
| POST | `/api/v2/cargosetu/plan` | `create(PlanDTO)` | `PlanDTO` (`@Valid`) | `ApiResponseDto` | `PLAN_CREATE` |
| PUT | `/api/v2/cargosetu/plan/{id}` | `update(Integer id, PlanDTO)` | `PlanDTO` (`@Valid`) | `ApiResponseDto` | `PLAN_UPDATE_DELETE` |
| GET | `/api/v2/cargosetu/plan/{id}` | `getById(Integer id)` | — | `ApiResponseDto` | `PLAN_READ` |
| GET | `/api/v2/cargosetu/plan` | `getAll()` | — | `ApiResponseDto` (list) | `PLAN_READ` |
| DELETE | `/api/v2/cargosetu/plan/{id}` | `delete(Integer id)` | — | `ResponseEntity<String>` (plain text) | `PLAN_UPDATE_DELETE` |
| GET | `/api/v2/cargosetu/plan/public` | `getPublicPlan()` | — | `ApiResponseDto` (list) | **`PLAN_READ`** (identical to `getAll()`) |

**Second controller — the actual public endpoint:** `PublicPlanControlor`
(`src/main/java/com/cargosetu/controller/PublicPlanControlor.java`, class name has a typo — "Controlor").
`@RequestMapping(UrlConstants.PUBLIC)` = `/public` (`UrlConstants.PUBLIC = "/public"`,
`src/main/java/com/cargosetu/config/utils/UrlConstants.java` line 14); note this is a **standalone
top-level mapping, not appended to `BaseURL.BASE_URL`** — it does not live under `/api/v2/cargosetu`.

| Method | Path | Handler | `@PreAuthorize` |
|---|---|---|---|
| GET | `/public/plan` | `getPublicPlan()` → `planService.getAllPlans()` | **none** (the `PreAuthorize` import in the file is unused — verified, no annotation on either method) |
| GET | `/public/isAlive` | `isServerLive()` → `ResponseEntity.ok("SUCCESS")` | **none** |

**Comparing the two `/public`-named endpoints (verified, corrects a claim in `../api-urls.md`):**

- `getPublicPlan()` (`PlanController.java` lines 58–63) is annotated
  `@PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).PLAN_READ)")` — the **same**
  permission required by the regular authenticated `GET /plan` listing — and its body just calls
  `planService.getAllPlans()`, i.e. it is not even a functionally distinct "public" projection; it returns
  the exact same data as the authenticated list endpoint.
- The path itself, `/api/v2/cargosetu/plan/public`, does **not** match any `permitAll()` matcher in
  `SecurityConfig.filterChain()` (`src/main/java/com/cargosetu/config/SecurityConfig.java` lines 68–81).
  The two plan-adjacent permitAll patterns present there are `"/public/plan/public/**"` and `"/public/**"`
  — both require the path to **start with** `/public/`, which `/api/v2/cargosetu/plan/public` does not
  (it starts with `/api/v2/cargosetu/plan/`). It therefore falls through to `.anyRequest().authenticated()`
  and **requires a valid JWT** like every other endpoint in the app.
- Separately, `MethodSecurityConfig` (`src/main/java/com/cargosetu/config/MethodSecurityConfig.java`)
  sets `@EnableMethodSecurity(prePostEnabled = false)`, which makes **every** `@PreAuthorize` in the
  application inert, including all six annotations on this controller.
- **Net, verified conclusion for `PlanController.getPublicPlan()` (`/api/v2/cargosetu/plan/public`):**
  it is presently indistinguishable in effect from `GET /plan` — it requires a valid authenticated session
  (via the filter chain) but, due to the globally disabled method security, does **not** actually enforce
  the `PLAN_READ` authority, exactly like the other five endpoints on this controller. It is **not** an
  unauthenticated/public endpoint today despite its name and its own annotation choice. The `/public` path
  segment on `PlanController.java` line 59 is a hardcoded string literal — it does not reference either of
  the two constants that look like they were meant for this (`BaseURL.PUBLIC_PLAN = "public"`, unused by
  any controller — verified via grep; `UrlConstants.PUBLIC = "/public"`, which **is** used, but by the
  *other* controller below).
- **`PublicPlanControlor.getPublicPlan()` (`/public/plan`) is the genuinely unauthenticated one.** Three
  independent, verified layers all agree here: (1) neither method in the class carries a `@PreAuthorize`
  annotation, so even with method security enabled there would be no permission gate; (2) its path
  `/public/plan` matches the `permitAll()` matcher `"/public/**"` in
  `SecurityConfig.filterChain()` (`SecurityConfig.java` line 80), so Spring Security lets the request
  through without requiring any authentication at all; (3) `JwtAuthenticationFilter.shouldNotFilter()`
  (`JwtAuthenticationFilter.java` line 43) also skips any path starting with `"/public/"`, so the JWT
  filter doesn't even attempt to populate a `SecurityContext` for this request — a genuine double-bypass,
  the same pattern already noted for `InquiryController`'s `public/api/inquiries` in
  `../services-and-known-issues.md` §4. This confirms the `permitAll()` comment/pattern
  `"/public/plan/public/**"` in `SecurityConfig` (line 79) is itself stale/dead — no controller is actually
  mapped at that exact path; the pattern that matters for Plans is the broader `"/public/**"` on the next
  line, which is what actually shields `PublicPlanControlor`.
- **Practical implication:** the front-end's public marketing/pricing page (`planscreen`, per
  `../services-and-known-issues.md` §3) should be calling `GET /public/plan`
  (`PublicPlanControlor`) — the endpoint on `PlanController` named `/public` is a same-purpose duplicate
  that, despite its name, is not actually reachable without a valid JWT. This existing-doc claim in
  `../api-urls.md` — `GET /api/v2/cargosetu/plan/public` "(no auth)" — is **incorrect for that specific
  endpoint**; the "(no auth)" behavior does exist in this codebase, just on a different path
  (`/public/plan`) served by a different, undocumented controller (`PublicPlanControlor`, absent from
  `../controllers.md`/`../api-urls.md` entirely as of this writing).
- Both public-listing variants return the identical unfiltered result set: `planService.getAllPlans()`
  (B.3) applies no `isActive`/`isDeleted` filtering, so soft-deleted plans (B.11) are visible even on the
  genuinely public, unauthenticated `/public/plan` endpoint.

## B.3 Business / Service Logic

**Interface:** `PlanService` (`src/main/java/com/cargosetu/service/PlanService.java`)
**Implementation:** `PlanServiceImpl` (`src/main/java/com/cargosetu/service/impl/PlanServiceImpl.java`)

| Method | Logic |
|---|---|
| `createPlan(PlanDTO dto)` | Maps DTO → new `Plans` entity (`mapToEntity`), stamps `createdBy`/`createdByName` from `ApplicationUtils.getLoggedInStaffId()`/`getLoggedInUser()`, `createdAt = now()`, saves, returns 200 + created plan as DTO. **No duplicate-name check** — unlike `Clientservice`'s create path, nothing here calls an `existsBy...` guard before insert (verified absent from the method body). |
| `updatePlan(Integer planId, PlanDTO dto)` | `planRepository.findById(planId).orElseThrow(() -> new ResourceNotFoundException("Plan","planId", new Exception()))` — note this is `org.apache.kafka.common.errors.ResourceNotFoundException` (a Kafka-client exception class repurposed here for an unrelated domain — verified via the import on line 13 of `PlanServiceImpl.java`), not a custom or Spring-native not-found exception. Overwrites all four business fields plus audit fields, saves, returns 200. |
| `getPlanById(Integer planId)` | Same `findById(...).orElseThrow(ResourceNotFoundException)` pattern; returns 200 + DTO. |
| `getAllPlans()` | `planRepository.findAll()` → maps every row to `PlanDTO` via `.stream().map(this::mapToDTO).toList()`; **no filtering, no pagination** — every plan in the table is returned to every caller. |
| `deletePlan(Integer planId)` | **Soft delete**: `findById(...).orElseThrow(...)`, then `plan.setIsDeleted(true)` and `plan.setIsActive(false)` (both inherited from `AbstractModel`), stamps `updatedById`/`updatedByName`/`updatedAt`, saves. The row is **never actually removed from the table** — despite the controller's DELETE handler returning the literal string `"Plan deleted successfully"`. **Verified caveat:** `getAllPlans()`/`getPlanById()` do **not** filter on `isDeleted`/`isActive` at all (no `WHERE` clause, no repository method, no in-memory filter in `PlanServiceImpl`) — a "deleted" plan continues to appear in `GET /plan`, `GET /plan/{id}`, and `GET /plan/public` exactly as before. This is a verified functional gap, not an inference. |

`ApplicationUtils` is injected via field `@Autowired` (`@Autowired private ApplicationUtils applicationUtils;`) alongside the constructor-injected `planRepository` (`@RequiredArgsConstructor` + `final`) — an inconsistent DI style within the same class (verified at lines 28–31).

## B.4 DTOs / Request-Response Models

**`PlanDTO`** (`src/main/java/com/cargosetu/dto/PlanDTO.java`):

| Field | Type | Validation |
|---|---|---|
| `planId` | `Integer` | none (not required on create; ignored by `mapToEntity`, only used to identify the row on `getById`) |
| `name` | `String` | `@NotBlank`, `@Size(min = 3, max = 50)` |
| `planValidity` | `LocalDate` | `@NotNull`; `@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd")` — comment in source notes it was changed from `LocalDateTime` to `LocalDate` |
| `price` | `Double` | `@NotNull`, `@Positive` (must be `> 0`) |
| `plannCategory` | `String` | `@NotBlank` — **note the field name has a typo (double "n": `plannCategory`)**, consistently used across `PlanDTO`, `PlanServiceImpl.mapToEntity`/`mapToDTO`; this is the actual wire field name JSON clients must send (`"plannCategory"`, not `"planCategory"`) |

Validated via `@Valid` on the controller's `create`/`update` request bodies (`PlanController.java` lines 27, 35) — a `CustomValidationException`/`MethodArgumentNotValidException` on failure is handled by `GlobalExceptionHandler` (see B.8). Mapping between `Plans` and `PlanDTO` is **hand-written** inline in `PlanServiceImpl` (`mapToEntity`/`mapToDTO` private methods, lines 170–187) — there is **no MapStruct mapper** for Plans (unlike `Clientservice`), so `updateFromDto`-style partial updates are not available; `updatePlan` always overwrites all four business fields unconditionally.

**Response envelope:** `ApiResponseDto` (`src/main/java/com/cargosetu/config/dto/ApiResponseDto.java`) — `{responseCode, responseMessage, responseBody, dataList, timestamp}`, built via Lombok `@Builder`. Message strings come from `CommonConstants.PlanConstant` (`src/main/java/com/cargosetu/utils/CommonConstants.java` lines 15–30): `PLAN_CREATED`, `PLAN_UPDATED`, `PLAN_FETCHED`, `PLAN_LIST_FETCHED`, `PLAN_DELETED` (this last constant exists but is **not actually used** by `deletePlan()`, which doesn't build an `ApiResponseDto` at all — the controller's `delete()` returns a raw `ResponseEntity.ok("Plan deleted successfully")` string instead, verified at `PlanController.java` line 56).

## B.5 Entity & Database

**Table:** `tblmplans`
**File:** `src/main/java/com/cargosetu/model/Plans.java`
**Extends:** `AbstractModel` (same mapped-superclass as `Clientservice` — see A.5 for its columns)

| Column | Java field | Type |
|---|---|---|
| `plan_id` | `planId` | `Integer`, `@Id`, `IDENTITY` |
| `plan_name` | `planName` | `String` |
| `plan_validity` | `planValidity` | `LocalDate` |
| `price` | `price` | `Double` |
| `plan_category` | `planCategory` | `String` |

**No `companyId` (or any tenant-identifying column) exists on `Plans`** — confirmed by reading the full
entity source (only the five columns above plus the `AbstractModel` audit/soft-delete columns). This
confirms `Plans` is a **platform-level catalog**, not per-tenant data: there is one shared set of plans
visible to (and, per B.3, editable by) any authenticated caller regardless of company, with no row-level
scoping mechanism at all (unlike, e.g., `Customer`, which does filter by `companyId` — see
`../services-and-known-issues.md` §1).

No `unique` constraint on `plan_name` at the column level (`@Column(name = "plan_name")` has no
`unique = true`) and, per B.3, no application-level uniqueness check either — duplicate plan names are
possible.

## B.6 Repository

**`PlanRepository`** (`src/main/java/com/cargosetu/repo/PlanRepository.java`):
```java
public interface PlanRepository extends JpaRepository<Plans, Integer> {
    @Query(value = "SELECT * FROM tblmplans WHERE plan_id = :planId", nativeQuery = true)
    Optional<Plans> findByPlanId(@Param("planId") Integer planId);
}
```
Notably, `findByPlanId` is a **hand-written native SQL query duplicating what `JpaRepository.findById`
already provides for free** — and it is not actually called anywhere in `PlanServiceImpl` (verified: every
lookup in the service uses the inherited `findById(planId)`, not `findByPlanId`). `findByPlanId` appears to
be dead code.

## B.7 Security & Authentication

Permission codes (`src/main/java/com/cargosetu/utils/PermissionConstants.java` lines 41–44):

| Constant | String value | Used on |
|---|---|---|
| `PLAN_READ` | `PERM_ + PLAN_READ` | `GET /plan/{id}`, `GET /plan`, `GET /plan/public` |
| `PLAN_CREATE` | `PERM_ + PLAN_CREATE` | `POST /plan` |
| `PLAN_UPDATE_DELETE` | `PERM_ + PLAN_UPDATE_DELETE` | `PUT /plan/{id}`, `DELETE /plan/{id}` |

These three permission codes are seeded for the bootstrap superadmin in `SeedSuperAdmin.java`
(`src/main/java/com/cargosetu/service/SeedSuperAdmin.java` lines 72–74: `"PLAN_UPDATE_DELETE"`,
`"PLAN_CREATE"`, `"PLAN_READ"` — plain codes without the `PERM_` prefix, matching the DB-storage
convention documented in the `PermissionConstants` class Javadoc).

**As established in B.2, these checks are currently non-functional**: `MethodSecurityConfig`
(`src/main/java/com/cargosetu/config/MethodSecurityConfig.java`) sets
`@EnableMethodSecurity(prePostEnabled = false)`, which disables Spring Security's `@PreAuthorize`
processing globally. This was independently confirmed here by direct inspection of that file (not merely
carried over from `../services-and-known-issues.md`). **Practical effect:** any authenticated user, holding
any permission or none at all, can call every `Plan` endpoint on `PlanController` including
create/update/delete — the only real gate is `SecurityConfig`'s `anyRequest().authenticated()`, i.e. "must
be logged in," full stop.

**`PublicPlanControlor` (`/public/plan`, `/public/isAlive`) sits entirely outside this permission model** —
it was never gated by `PLAN_READ` in the first place (no `@PreAuthorize` on either method), and, per B.2,
its path is both `permitAll()`-listed in `SecurityConfig` and skipped outright by
`JwtAuthenticationFilter.shouldNotFilter()`. So while the *intent* of a permission system exists for Plans
(three well-named `PLAN_*` codes, seeded on startup), in the current build there are, in effect, two
independent failure modes stacked on top of each other for this module: `PlanController`'s checks are
written correctly but never evaluated (global method-security switch), and `PublicPlanControlor` was never
given any checks to begin with.

## B.8 Exception Handling

Unlike `Clientservice`, `PlanServiceImpl` does **not** catch exceptions locally — `findById(...).orElseThrow(...)` throws `org.apache.kafka.common.errors.ResourceNotFoundException` (a `RuntimeException` subclass) directly out of the service method, up through the controller, to `GlobalExceptionHandler`
(`src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`). Since `ResourceNotFoundException` is not `CustomValidationException`, it is caught by the generic `@ExceptionHandler(Exception.class)` handler, which **unconditionally returns HTTP 400** with body `{responseCode: 500 (CommonStatusCode.INTERNAL_SERVER_ERROR), responseMessage: <exception message>, responseBody: null}` — so a not-found Plan lookup surfaces to API clients as **HTTP 400** carrying a **500** in the JSON body, never HTTP 404. This is the same systemic issue already flagged in `../services-and-known-issues.md` §4/§5, reconfirmed here specifically for the Plans module. `@Valid` failures on `PlanDTO` (e.g. missing `name`, non-positive `price`) are handled by Spring's standard `MethodArgumentNotValidException` path, which is **not** one of the two handlers registered in `GlobalExceptionHandler` — **[unverified]**: whichever Spring Boot default error-handling kicks in (typically HTTP 400 with a `ProblemDetail`/default error body) applies instead, since no bespoke handler intercepts it.

## B.9 Configuration

No externalized/runtime configuration is associated with the Plans module — plan data is 100% row data in `tblmplans`, created/edited only through the CRUD endpoints. Unlike `Clientservice`, there is no evidence anywhere in the codebase of a `Plans` row being read by any business-logic service (verified: `grep` for `Plans`/`PlanRepository`/`PlanService` usage across the codebase returns only `PlanController.java`, `PublicPlanControlor.java`, `PlanService.java`, `PlanServiceImpl.java`, and `PlanRepository.java` — no other module joins against or reads plan data, e.g. there is no visible link from a `Company`/tenant record to a selected `Plans` row).

## B.10 External Integrations

None. No payment gateway, billing provider, or external subscription-management system is wired to this module — `Plans` is a static catalog table with no integration hooks (no Stripe/Razorpay/etc. references found anywhere near this code).

## B.11 Important Business Rules & Validations

- **Pricing:** `price` must be `> 0` (`@Positive` on `PlanDTO.price`) and non-null; enforced only at the DTO/`@Valid` layer, not at the DB column level (`price` column has no `CHECK` constraint — plain `Double`).
- **Validity:** `planValidity` is a single `LocalDate` (not a start/end range) — modeled as one date, `@NotNull`, formatted `yyyy-MM-dd` over JSON. **[inferred]**: the field name and single-date shape suggest this represents an expiry/valid-until date rather than a duration, but nothing in the codebase (comments, usage elsewhere) confirms how it's interpreted at enforcement time — no code was found that checks a plan's validity date against "now" to gate access to anything.
- **Uniqueness:** none enforced — neither `name` nor any other field has a uniqueness constraint at the DB or service layer (contrast with `Clientservice`, which at least checks `(name, value)` pairs on create).
- **Soft delete only:** `deletePlan` never removes rows (B.3); combined with B.3's finding that list/get-by-id never filter on `isDeleted`, this means **soft-deleted plans remain fully visible and retrievable** through every read endpoint today — a verified functional bug, not a design choice documented anywhere.
- **Category field typo:** the wire/DTO field is `plannCategory` (double "n"), not `planCategory` — API consumers must match this exact spelling.

## B.12 Dependencies Between Modules

- **Plans is platform-level, not tenant-scoped** — confirmed in B.5: no `companyId` field exists on `Plans.java`, and no query anywhere filters plans by company.
- **Two controllers depend on `PlanService`**: `PlanController` (full CRUD, `/api/v2/cargosetu/plan/**`)
  and `PublicPlanControlor` (`/public/plan`, read-only, unauthenticated) — both call
  `planService.getAllPlans()` for their respective listing endpoints; there is exactly one service/
  repository implementation behind both. No other module depends on `Plans`/`PlanService`/
  `PlanRepository`** — confirmed by source search; nothing joins `Company`, `Staff`, or any tenant entity
  to a specific plan. The module currently exists as an **isolated CRUD catalog** with no wiring into
  billing, company onboarding, or feature-gating logic anywhere else in the codebase (**[inferred]**
  absence — based on an exhaustive grep for `Plans`/`PlanRepository`/`PlanService` across
  `src/main/java`, which surfaced only the five files listed in "Files read in full" above for Part B).

---

## Summary of the most consequential verified findings

1. **`PlanController`'s `/plan/public` is not actually public — but a second, undocumented controller,
   `PublicPlanControlor`, exposes a genuinely unauthenticated `GET /public/plan` that does the same thing.**
   `PlanController.getPublicPlan()` carries the same `PLAN_READ` `@PreAuthorize` as the regular listing
   endpoint, its path doesn't match any `permitAll()` rule in `SecurityConfig`, and method security is
   globally disabled anyway — so in practice it behaves exactly like `GET /plan`: requires a valid JWT,
   enforces no permission. Meanwhile `PublicPlanControlor` (`controller/PublicPlanControlor.java`, not
   mentioned in `../controllers.md`/`../api-urls.md`) has no `@PreAuthorize` at all, matches
   `SecurityConfig`'s `"/public/**"` permitAll rule, and is skipped entirely by
   `JwtAuthenticationFilter.shouldNotFilter()` — this is the endpoint that is actually reachable without
   authentication. The "(no auth)" note in `../api-urls.md` is correct in spirit but attached to the wrong
   path/controller.
2. **`Clientservice`'s configured `value` is never consumed.** All four real-world consumers
   (`TemplateService`, `CompanyServImpl`, `TripDocumentService`, `FileController`) call `.getName()` instead
   of `.getValue()` on the resolved row, so the admin-editable configuration value has no effect on the
   actual upload path used.
3. **Plan soft-delete doesn't hide anything.** `getAllPlans()`/`getPlanById()` apply no `isDeleted`/
   `isActive` filter, so a "deleted" plan stays fully visible via every read endpoint.
4. **All `@PreAuthorize` checks across both modules are inert** (`MethodSecurityConfig.prePostEnabled =
   false`), reconfirming the app-wide finding in `../services-and-known-issues.md` specifically for the
   `PLAN_*` permission codes and for the (absent, in `Clientservice`'s case) permission gating.
5. **Neither module is tenant-scoped.** `Clientservice` and `Plans` are both platform-wide tables with no
   `companyId`, consistent with their role as shared/admin-level configuration rather than tenant data.

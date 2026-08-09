# Company / Tenant Module

> Standalone module reference for the **Company** entity — the multi-tenant root of the application —
> plus its two dependent aggregates, **Branch** and **CompanyAsset**. Cross-references
> [`../controllers.md`](controllers.md), [`../entities.md`](entities.md), [`../api-urls.md`](api-urls.md)
> and [`../services-and-known-issues.md`](services-and-known-issues.md); every claim below was checked
> directly against source in `src/main/java/com/cargosetu/` rather than copied from those docs. For
> security wiring details, [`../modules/auth-security.md`](auth-security.md) is the source of truth (see its
> §7, "`MethodSecurityConfig`") — the security facts in §7 below are a summary of that file's findings,
> re-confirmed here directly against `config/MethodSecurityConfig.java`, not an independent re-derivation.

---

## 1. Overview

`Company` (table `tblmcompany`) is the tenant root of this ERP. Every other business entity that needs
tenant scoping (staff, customers, shiftings, trips, departments, drivers/vehicles, etc.) carries some form
of a `companyId`/`companyUuid` pointer back to this table. The module also owns two directly dependent
tables:

- **Branch** (`branch`) — a company's physical branch/office locations.
- **CompanyAsset** (`tbl_company_assets`) — uploaded brand assets (logo, seal, signature, stamp) used
  when rendering documents/quotations for a company.

Source files covered:

| Concern | File |
|---|---|
| Entity | `src/main/java/com/cargosetu/model/Company.java` |
| Entity | `src/main/java/com/cargosetu/model/Branch.java` |
| Entity | `src/main/java/com/cargosetu/model/CompanyAsset.java` |
| Controller | `src/main/java/com/cargosetu/controller/CompanyController.java` |
| Controller | `src/main/java/com/cargosetu/controller/BranchController.java` |
| Service (interface) | `src/main/java/com/cargosetu/service/CompanyService.java` |
| Service (impl) | `src/main/java/com/cargosetu/service/impl/CompanyServImpl.java` |
| Service (concrete, no interface) | `src/main/java/com/cargosetu/service/BranchService.java` |
| DTO | `src/main/java/com/cargosetu/dto/CompanyDTO.java` |
| DTO | `src/main/java/com/cargosetu/dto/CompanyAssetDTO.java` |
| DTO | `src/main/java/com/cargosetu/dto/ValidationResponse.java` |
| Repository | `src/main/java/com/cargosetu/repo/CompanyRepository.java` |
| Repository | `src/main/java/com/cargosetu/repo/BranchRepository.java` |
| Repository | `src/main/java/com/cargosetu/repo/CompanyAssetRepository.java` |
| Base class | `src/main/java/com/cargosetu/abstracts/AbstractModel.java` |
| File storage helper | `src/main/java/com/cargosetu/utils/utillity/FileUtility.java` |
| Permission source of truth | `src/main/java/com/cargosetu/utils/PermissionConstants.java` |
| URL prefixes | `src/main/java/com/cargosetu/utils/BaseURL.java` |

**No MapStruct mapper exists for `Company`.** A repo-wide `grep` for `CompanyMapper` returns nothing.
Unlike most other entities in this codebase (which go through a `BaseUpdateMapper<D,E>` MapStruct pair per
`docs/services-and-known-issues.md` §2), `Company` conversion is done by hand: a `Company(CompanyDTO)`
constructor on the entity itself (`model/Company.java:87-106`) and a static `CompanyDTO.from(Company, ...)`
factory on the DTO (`dto/CompanyDTO.java:89-114`).

---

## 2. API Controllers & Endpoints

### `CompanyController` (`controller/CompanyController.java`, base path `/api/v2/cargosetu/company`)

| Method | Path | `@PreAuthorize` permission | Body shape | Notes |
|---|---|---|---|---|
| POST | `/api/v2/cargosetu/company` | `COMPANY_CREATE` | **multipart/form-data**, `@ModelAttribute CompanyDTO` | Creates a company; also injects `@AuthenticationPrincipal LoggedInUser` to link the creating staff |
| PUT | `/api/v2/cargosetu/company` | `COMPANY_UPDATE_DELETE` | JSON, `@Valid @RequestBody CompanyDTO` | Company ID comes from the body (`company.getCompanyId()`), not the path |
| GET | `/api/v2/cargosetu/company/{uuid}` | `COMPANY_READ` | — (path variable `uuid`, a `String`) | Looks up by `companyUUID`, not the numeric PK |
| GET | `/api/v2/cargosetu/company` | `COMPANY_READ` | — | "Get all" — see §3 for the actual scoping logic |
| DELETE | `/api/v2/cargosetu/company/{id}` | `COMPANY_UPDATE_DELETE` | — (path variable `id`, `Integer`) | Soft delete (see §3) |
| GET | `/api/v2/cargosetu/company/currentCompany` | `COMPANY_READ` | — | Resolves the caller's own company via JWT principal |
| POST | `/api/v2/cargosetu/company/assets` | `COMPANY_CREATE` | **multipart/form-data**, `MultipartFile[] file` + JSON string part `data` | **Dead endpoint — see below** |

Dead-endpoint finding (verified by reading `CompanyController.createCompanyAssets`,
`controller/CompanyController.java:93-116`): the handler parses the `data` part into
`List<CompanyAssetDTO>` and validates it's non-empty, but the actual call to
`companyService.createCompanyAssets(companyAssets, files)` is commented out
(`controller/CompanyController.java:107`). The endpoint always returns a fabricated
`"Company assets created successfully"` response **without persisting anything or saving any file** — a
caller of this endpoint gets a false-positive success. The real asset-upload path is invoked internally
during company *creation* (see §3), not through this endpoint.

`ObjectMapper` and `DocumentUploadDTO` are `@Autowired`/imported at the top of the controller;
`DocumentUploadDTO` is imported but never referenced in the class body — a leftover import.

### `BranchController` (`controller/BranchController.java`, base path `/api/v2/cargosetu/branches`)

| Method | Path | `@PreAuthorize` permission | Body shape | Notes |
|---|---|---|---|---|
| POST | `/api/v2/cargosetu/branches` | `COMPANY_CREATE` | JSON, `@RequestBody Branch` (raw entity, no DTO) | Validated via `BranchService.validateRequest` before save |
| PUT | `/api/v2/cargosetu/branches/{branchId}` | `COMPANY_CREATE` | JSON, `@RequestBody Branch` | Path `branchId` overwrites `branch.branchId` before validation (`controller/BranchController.java:53`) |
| GET | `/api/v2/cargosetu/branches/{branchId}` | `COMPANY_CREATE` | — | — |
| DELETE | `/api/v2/cargosetu/branches/{branchId}` | `COMPANY_CREATE` | — | Soft delete — see §3 |

Notable: **every** `BranchController` endpoint — including read, update and delete — is gated by
`COMPANY_CREATE`, never `COMPANY_READ`/`COMPANY_UPDATE_DELETE`. This is verified directly from the four
`@PreAuthorize` annotations in `controller/BranchController.java` (lines 24, 46, 70, 86) — it is not a
transcription error in this doc. Whether intentional or a copy-paste artifact is not determinable from
source alone.

Also note `BranchController` takes the raw `Branch` **entity** as the request body (no `BranchDTO` exists
in the codebase) — validation constraints live only in `BranchService.validateRequest`/
`validateUpdateRequest` (`branchName` and `mobile` required), not as Bean Validation annotations on the
entity itself.

---

## 3. Business/Service Logic

### `CompanyServImpl` (`service/impl/CompanyServImpl.java`)

**`createCompany(CompanyDTO, Long staffId)`** (lines 46-86):
1. Rejects if `companyRepository.existsByCompanyEmail(...)` is true → `409` "Company email already exists".
2. Derives a UUID prefix/suffix for `UUIDGenerator.generate(prefix, suffix)`:
   - `prefix` = first 4 chars of `companyName` (uppercased) if `companyName.length() >= 5`, else the
     literal fallback `"COPTIC"`.
   - `suffix` = first 4 characters of `contactNo1` **reversed** (i.e., last 4 digits of the phone number,
     reversed-then-substringed), else fallback `"6293"`.
   - Result stored on `Company.companyUUID` (column `company_uuid`), e.g. `"<PREFIX><9 random A-Z0-9><suffix>"`
     per `UUIDGenerator.generate(String,String)` (`utils/UUIDGenerator.java:20-27`).
3. Builds a `Company` via the `Company(CompanyDTO)` constructor and saves it.
4. If a `Staff` exists for `staffId`, sets that staff's `companyId` to the newly created company's ID and
   saves the staff — i.e., **company creation immediately binds the creating staff member to the new
   tenant.**
5. Calls `createCompanyAssets(companyDto.getAssets(), saved.getCompanyId())` — so **asset upload actually
   happens as a side effect of company creation**, driven by the `assets` field nested inside the
   multipart `CompanyDTO`, not by the separate `/company/assets` endpoint (which is dead — see §2).
6. The whole method is wrapped in a single `try/catch(Exception)` that on failure returns a generic `500`
   — no rollback annotation (`@Transactional`) on `createCompany` itself, so if `createCompanyAssets`
   throws after the company row and staff update already committed (autocommit / no encompassing
   transaction here), those writes are **not** rolled back even though the response reports failure.

**`updateCompany(Integer id, CompanyDTO)`** (lines 88-154): partial update — each field is only applied
`if (company.getXxx() != null)`. Fields actually wired for update: `companyName`, `companyEmail`,
`whatsappNumber`, `contactNo1`, `contactNo2`, `address`, `city`, `state`, `country`, `website`. Notably
**not** updatable through this method even if present in the request: `panNumber`, `gstNumber`,
`landlineNo`, `tollFreeNumber`, `branch`, `mainBranch`. The method also calls
`existing.setUpdatedAt(LocalDateTime.now())` before saving — see §5 for why this call is likely a no-op at
the database level.

**`getCompanyById(String uuid)`**: looks up via `companyRepository.findByCompanyUUIDAndIsDeletedAndIsDeleted(uuid, false)`
(actual method name `findByCompanyUUIDAndIsDeleted`) — i.e. soft-deleted companies are excluded.

**`getAllCompanies(LoggedInUser user)`** (lines 182-219) — the only row-level tenant-scoping logic in this
module:
- If `user.getStaffId() == 1` → returns **every** company (`companyRepository.findAll()`). Staff ID `1` is
  the hardcoded bootstrap superadmin seeded by `SeedSuperAdmin` per `docs/services-and-known-issues.md`
  §1.
- Otherwise, if `user.getCompanyId() != null` → returns a one-element list containing only that user's own
  company.
- Otherwise → `404` "Company not found".

**`deleteCompany(Integer id)`**: soft delete only — sets `isActive = false` and `isDeleted = true` on the
`AbstractModel`-inherited fields and saves; the row is never physically removed.

**`getCompany(Integer companyId)`** and **`getCompanyByStaffId(Long staffId)`**: straight lookups backing
the `/currentCompany` endpoint (see below).

**`currentCompany` resolution** (`CompanyController.getCurrentCompany`, lines 76-90, delegating into the
two service methods above): given the JWT-derived `LoggedInUser`,
1. If `user.getCompanyId()` is non-null → `companyService.getCompany(user.getCompanyId())` (direct PK
   lookup, **no** `isDeleted` filter, unlike `getCompanyById`).
2. Else if `user.getStaffId()` is non-null → `companyService.getCompanyByStaffId(user.getStaffId())`, which
   runs the native query `findCompanyByStaffId` (see §6) — this is the fallback path for a staff member
   whose JWT claims don't carry a `companyId` yet.
3. Else → `404` "Company not found".

**Company asset upload flow** — `createCompanyAssets(List<CompanyAssetDTO>, Integer companyId)`
(lines 289-325), `@Transactional`:
1. No-ops if the asset list is null/empty.
2. For each `CompanyAssetDTO`, skips entries whose `file` array is null/empty (an asset entry can carry
   just metadata with no file, and is silently dropped).
3. `validateCompanyAssetFiles(file)` (lines 251-287) enforces, **per file**: non-null/non-empty, `≤ 1 MB`
   (`1024 * 1024` bytes literal), extension in `{pdf, jpg, jpeg, png, svg}` (case-insensitive, derived from
   the substring after the last `.` in the original filename). Throws unchecked `RuntimeException` on any
   violation — caught by the outer `createCompany` try/catch (turns into a generic `500`), or propagates
   uncaught if `createCompanyAssets` is ever called from elsewhere.
4. Looks up `clientServiceRepo.findByName("COMPANY_ASSET")` to get a `Clientservice` config row, then calls
   `FileUtility.saveMultipartFile(file, clientservice.getName())` — see §9/§10, this is very likely a bug:
   it passes the **name** field (`"COMPANY_ASSET"`, a relative directory literal) as the storage base path
   instead of the row's **`value`** field, which is seeded as the absolute path
   `D:\cargo project\document` (see §9).
5. `FileUtility.saveMultipartFile` returns `List<String>`; the code calls `.toString()` on that `List`
   directly (`CompanyServImpl.java:310`) and stores the result as `CompanyAsset.filePath`. For any asset
   with more than zero files this produces a Java `List.toString()` rendering like
   `[D:\...\uuid_name.png]` — **including the square brackets** — not a bare filesystem path. This value is
   later fed straight into `Files.readAllBytes(Path.of(filePath))` in `getAssetAsBase64` (line 348), which
   will throw `InvalidPathException`/`NoSuchFileException` for any bracket-containing path on most
   filesystems. This is a likely-broken round-trip, not confirmed fixed anywhere else in the class.
6. Builds `CompanyAsset` entities (`assetType`, `filePath`, `companyId`) and `saveAll`s them.

**`getAssetAsBase64(Integer companyId, String assetType)`** (lines 332-357): looks up one
`CompanyAsset` by `(companyId, assetType)`, reads the file bytes off disk at the stored `filePath`, base64
encodes them, and returns a `data:<mime>;base64,<...>` URI (mime sniffed from file extension in
`detectMimeType`, defaulting to `image/jpeg`). Returns `null` on any failure — callers (e.g. template
rendering) are expected to fall back to a placeholder. Given the bracket-path bug above, this is likely to
fail for freshly uploaded assets.

### `BranchService` (`service/BranchService.java`) — concrete `@Service`, no interface, no `impl` package

- `validateRequest(Branch)` / `validateUpdateRequest(Branch)`: hand-rolled validation returning a
  `ValidationResponse{isValid, message}`. Required fields: `branchName`, `mobile` (both via
  `StringUtils.hasText`). `validateUpdateRequest` additionally requires `branchId` to be present and to
  resolve via `branchRepository.findById`.
- `createBranch` / `updateBranch`: plain CRUD, no tenant-scoping check on `companyId` at all (the caller
  can set any `companyId` on the `Branch` body).
- `getBranch(Long branchId)`: plain `findById`, no company-ownership check either — **any authenticated
  user with `COMPANY_CREATE` can read any branch of any company by ID**, since there is no
  `companyId == callerCompanyId` filter anywhere in this class.
- `deleteBranch(Long branchId)`: soft delete via the entity's own `active` flag (`Branch.active`, default
  `true`) — note this is **not** the `AbstractModel.isDeleted` pattern, because `Branch` does not extend
  `AbstractModel` at all (see §5).

---

## 4. DTOs / Request-Response Models

### `CompanyDTO` (`dto/CompanyDTO.java`)

Lombok `@Data @Builder`. Used as the multipart-bound request shape on create (`@ModelAttribute`) and the
JSON request/response shape on update.

| Field | Type | Validation |
|---|---|---|
| `companyId` | `Integer` | — |
| `companyName` | `String` | `@NotBlank`, `@Size(max=150)` |
| `companyEmail` | `String` | `@NotBlank`, `@Email`, `@Size(max=150)` |
| `whatsappNumber` | `Long` | `@NotNull`, `@Digits(integer=15, fraction=0)` |
| `contactNo1` | `Long` | `@NotNull`, `@Digits(integer=15, fraction=0)` |
| `contactNo2` | `Long` | `@Digits(integer=15, fraction=0)` |
| `landlineNo` | `String` | `@Size(max=20)` |
| `tollFreeNumber` | `String` | `@Size(max=20)` |
| `panNumber` | `String` | `@NotBlank`, `@Pattern("[A-Z]{5}[0-9]{4}[A-Z]{1}")` |
| `gstNumber` | `String` | `@NotBlank`, `@Pattern("[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}")` |
| `address` | `String` | `@NotBlank`, `@Size(max=300)` |
| `country` | `String` | `@NotBlank`, `@Size(max=100)` |
| `state` | `String` | `@NotBlank`, `@Size(max=100)` |
| `city` | `String` | `@NotBlank`, `@Size(max=100)` |
| `branch` | `String` | `@Size(max=100)` |
| `mainBranch` | `String` | `@Size(max=100)` |
| `companyUUID`, `companyNameFirst`, `companyNameRest`, `logoBase64`, `sealBase64`, `website` | `String` | — |
| `assets` | `List<CompanyAssetDTO>` | — (drives the create-time asset upload, §3) |
| `bankName`, `accountHolder`, `accountNo`, `ifscCode` | `String`, default `""` | present on the DTO but **not** persisted anywhere — `Company` entity has no matching columns; these fields are write-only dead weight on this DTO today |

Note on validation enforcement: these `@NotBlank`/`@Pattern`/etc. annotations only take effect where the
controller method is also annotated `@Valid` — `createCompany` (`POST /company`) is **not** `@Valid`
(`@ModelAttribute CompanyDTO company` with no `@Valid`), so PAN/GST format and required-field validation
is **not actually enforced on create**, only on `updateCompany` (`@Valid @RequestBody CompanyDTO`,
`controller/CompanyController.java:47`) — and there only for whichever fields the client happens to send,
since the same DTO is used for a request that in practice performs a partial update.

`CompanyDTO.from(Company c, String logoBase64, String sealBase64)` (lines 89-114): a hand-written
projection used to build the "print-ready" DTO (e.g. for document/quotation templates) — splits
`companyName` into `companyNameFirst`/`companyNameRest` on the first space, and folds in
pre-fetched base64 asset strings.

### `CompanyAssetDTO` (`dto/CompanyAssetDTO.java`)

| Field | Type |
|---|---|
| `id` | `Long` |
| `companyId` | `Integer` |
| `assetType` | `String` (free-form — comment lists `LOGO`, `SEAL`, `SIGNATURE`, `STAMP` as examples, not an enum) |
| `filePath` | `String` |
| `file` | `MultipartFile[]` |

### `ValidationResponse` (`dto/ValidationResponse.java`)

Generic `{isValid: boolean (default false), message: String (default "FAIL")}` — used only by
`BranchService`'s hand-rolled validation, not Bean Validation.

---

## 5. Entities & Database Relationships

### `Company` (`model/Company.java`, table `tblmcompany`)

Extends `AbstractModel` (see below for inherited columns).

| Column | Java type | Constraints | Notes |
|---|---|---|---|
| `company_id` | `Integer` (PK, `IDENTITY`) | — | |
| `company_name` | `String`, length 150 | `NOT NULL` (`@NotBlank`) | |
| `company_email` | `String`, length 150 | `NOT NULL`, `UNIQUE` (`@NotBlank @Email`) | Only true uniqueness constraint on this table |
| `whatsapp_number` | **`Long`** | `NOT NULL` (`@NotNull`) | See implications below |
| `contact_no_1` | `Long` | `NOT NULL` | |
| `contact_no_2` | `Long` | nullable | |
| `landline_no` | `String` | nullable | |
| `toll_free_number` | `String` | nullable | |
| `pan_number` | `String`, length 10 | `NOT NULL` (`@Size(10,10)`) | Entity-level length check only; format (`[A-Z]{5}[0-9]{4}[A-Z]`) is validated only in `CompanyDTO`, not on the entity itself |
| `gst_number` | `String`, length 15 | `NOT NULL` (`@Size(15,15)`) | Same — format regex lives only on the DTO |
| `address` | `String`, length 300 | `NOT NULL` | |
| `country` | `String`, length 100 | `NOT NULL` | |
| `state` | `String`, length 100 | `NOT NULL` | |
| `city` | `String`, length 100 | `NOT NULL` | |
| `branch` | `String`, length 100 | nullable | Free-text field, unrelated to the `Branch` entity/table below — same name, different concept |
| `main_branch` | `String`, length 100 | nullable | |
| `company_uuid` | `String` | nullable at the JPA level (no `@Column(nullable=false)`) | The public-facing tenant identifier used in `GET /company/{uuid}`; generated server-side on create (§3), never validated as required by Bean Validation |
| `created_by_id` | `Long` | nullable | Declared directly on `Company`, **separate from** the inherited `createdBy`/`created_by` column from `AbstractModel` — two differently-named "who created this" columns coexist on the same table |
| `website` | `String` | nullable, no explicit `@Column` (column name defaults to `website`) | |

`whatsapp_number: Long` implications: a `Long` can hold values with leading zeros stripped and no `+`
prefix or separators — there is no dedicated phone-formatting/country-code column, so international
numbers, leading `0`s, or `+` prefixes cannot be represented as entered; the field also has no validation
tying its digit count to a real phone number length beyond the DTO's generic `@Digits(integer=15,
fraction=0)`. The same shape/limitation applies to `contact_no_1`/`contact_no_2`.

**Inherited from `AbstractModel`** (`abstracts/AbstractModel.java`, `@MappedSuperclass`, columns folded
into `tblmcompany` directly — no separate table):

| Column | Java type | Notes |
|---|---|---|
| `is_active` | `Boolean`, default `true` | |
| `is_deleted` | `Boolean`, default `false` | Soft-delete flag used by `deleteCompany`/`getCompanyById` |
| `created_by` | `Long` | Set nowhere in `CompanyServImpl` for `Company` — always `null` on companies created through this service |
| `created_by_name` | `String` | Same — never set |
| `created_at` | `LocalDateTime` | Auto-populated via `@PrePersist setCreateDate()`; `@Column(updatable = false)` |
| `updated_by_name` | `String` | Never set |
| `updated_by_id` | `Long` | Never set |
| `updated_at` | `LocalDateTime` | **Also declared `@Column(updatable = false)`** (`abstracts/AbstractModel.java:38`) |

**Verified quirk**: `updated_at` is mapped `updatable = false`, which tells Hibernate to *never include
this column in an `UPDATE` statement*, regardless of runtime value. `CompanyServImpl.updateCompany`
explicitly calls `existing.setUpdatedAt(LocalDateTime.now())` before saving
(`service/impl/CompanyServImpl.java:140`) — because of the mapping, this call has no effect on the
persisted row; `updated_at` stays frozen at its `@PrePersist`-set creation-time value forever. This affects
every entity that extends `AbstractModel`, not just `Company`.

### `Branch` (`model/Branch.java`, table `branch`)

Does **not** extend `AbstractModel` — no `isActive`/`isDeleted`/audit-by columns inherited; it defines its
own `active: Boolean` (default `true`) flag instead, used as its soft-delete marker.

| Column | Java type | Constraints |
|---|---|---|
| `branchId` | `Long` (PK, `GenerationType.AUTO`) | |
| `branchName` | `String` | required only via `BranchService` hand validation |
| `branchCode` | `String` | — |
| `contactPerson` | `String` | — |
| `mobile` | `String` | required only via `BranchService` hand validation |
| `email` | `String` | — |
| `gstNumber` | `String` | — |
| `addressLine1` / `addressLine2` | `String` | — |
| `city` / `state` / `country` / `pincode` | `String` | — |
| `headOffice` | `Boolean`, default `false` | — |
| `active` | `Boolean`, default `true` | Soft-delete flag |
| `companyId` | **`Long`** | **No `@ManyToOne`/`@JoinColumn` — plain scalar FK-by-convention, not JPA-enforced**, and its type (`Long`) does not match `Company.companyId` (`Integer`) — see §12 |

No unique constraint on `(companyId, branchCode)` or similar — nothing stops two branches under the same
company sharing a `branchCode`.

### `CompanyAsset` (`model/CompanyAsset.java`, table `tbl_company_assets`)

Does **not** extend `AbstractModel` either.

| Column | Java type | Constraints |
|---|---|---|
| `id` | `Long` (PK, `IDENTITY`) | |
| `company_id` | `Integer` | `NOT NULL` — matches `Company.companyId`'s type (unlike `Branch`) |
| `asset_type` | `String`, length 20 | `NOT NULL` — free text, not an enum (`LOGO`/`SEAL`/`SIGNATURE`/`STAMP` per the code comment only) |
| `file_path` | `String` | nullable |

A commented-out `base64Data` `@Lob` column (`model/CompanyAsset.java:23-25`) indicates an abandoned
alternate design (storing the file inline as base64 in a `LONGTEXT` column) in favor of the current
disk-path approach. No unique constraint on `(company_id, asset_type)` — nothing in the schema stops
duplicate `LOGO` rows for the same company; `CompanyAssetRepository.findByCompanyIdAndAssetType` would
non-deterministically return one row if duplicates existed (its return type is a bare `CompanyAsset`, not
`List<CompanyAsset>` or `Optional`).

Neither `Branch` nor `CompanyAsset` declares a JPA relationship annotation (`@ManyToOne`) back to
`Company` — both are "FK by convention" (a plain scalar column that happens to store a `Company` PK
value), not enforced at the ORM or (per §9) database level either, since schema is Hibernate
`ddl-auto=update`-generated rather than hand-written with explicit foreign-key constraints.

---

## 6. Repositories

### `CompanyRepository extends JpaRepository<Company, Integer>` (`repo/CompanyRepository.java`)

| Method | Kind | Notes |
|---|---|---|
| `boolean existsByCompanyEmail(String companyEmail)` | derived query | Used for the create-time duplicate-email guard |
| `Optional<Company> findByCompanyUUIDAndIsDeleted(String companyUUID, Boolean isDeleted)` | derived query | Backs `GET /company/{uuid}` |
| `Optional<Company> findCompanyByStaffId(@Param("staffId") Long staffId)` | **native query** | `SELECT * FROM cargosetuapp.tblmcompany WHERE created_by = :staffId` (`repo/CompanyRepository.java:16-20`) — hardcodes the schema name `cargosetuapp` (fragile if the schema is ever named differently per environment) and filters on `created_by` (the inherited `AbstractModel` column), which — per §5 — `CompanyServImpl.createCompany` never sets. In practice this query will return empty for essentially every company created through the current `createCompany` flow. |

### `BranchRepository extends JpaRepository<Branch, Long>` (`repo/BranchRepository.java`)

No custom query methods at all — plain CRUD only. Nothing that filters by `companyId`; every lookup in
`BranchService` is a bare `findById`.

### `CompanyAssetRepository extends JpaRepository<CompanyAsset, Long>` (`repo/CompanyAssetRepository.java`)

| Method | Kind |
|---|---|
| `CompanyAsset findByCompanyIdAndAssetType(Integer companyId, String assetType)` | derived query, returns a single (possibly `null`) entity |

---

## 7. Security & Authentication

Every endpoint in both controllers carries a `@PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).XXX)")`
annotation. The permission constants actually referenced (`utils/PermissionConstants.java:35-39`):

| Constant | Value | Used on |
|---|---|---|
| `COMPANY_READ` | `PERM_COMPANY_READ` | `CompanyController`: get-by-uuid, get-all, currentCompany |
| `COMPANY_CREATE` | `PERM_COMPANY_CREATE` | `CompanyController`: create, create-assets; **all four** `BranchController` endpoints |
| `COMPANY_UPDATE_DELETE` | `PERM_COMPANY_UPDATE_DELETE` | `CompanyController`: update, delete |

Two permission constants are declared but **never referenced by any controller in this module**:
`COMPANY_DELETE` and `COMPANY_UPDATE` (`utils/PermissionConstants.java:37-38`) — the code uses the
combined `COMPANY_UPDATE_DELETE` instead, leaving the two split constants as dead code (at least for this
module).

**These authorization checks do not currently take effect.** Confirmed by direct read of
`config/MethodSecurityConfig.java`:

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = false)
public class MethodSecurityConfig {
}
```

`prePostEnabled = false` globally disables Spring Security's `@PreAuthorize`/`@PostAuthorize` processing.
Every `@PreAuthorize` annotation shown in the tables above — across both `CompanyController` and
`BranchController` — is inert. Any authenticated (JWT-valid) user can call every endpoint in this module
today regardless of role/permission. This matches the finding already recorded in
`docs/services-and-known-issues.md` §4/§5.2 and, in more detail, in
[`../modules/auth-security.md`](auth-security.md) §7 ("`MethodSecurityConfig`"), which lists `CompanyController`
and `BranchController` among the 14 controller files whose `@PreAuthorize` annotations are silently inert
for this reason. It is called out again here because it is the single most important fact for anyone
building new authorization-sensitive behavior on top of this module — see `auth-security.md` for the JWT
issuance/validation mechanics (`JwtUtils`, `JwtAuthenticationFilter`) that remain the *only* thing actually
gating access to these endpoints today.

Beyond method security, request-level auth is still enforced by the stateless JWT filter chain
(`config/SecurityConfig.java`/`config/JwtAuthenticationFilter.java`, not re-derived here) — so endpoints in
this module still require a valid bearer token; what's missing is the *fine-grained* permission check on
top of "is authenticated."

---

## 8. Exception Handling

No module-specific exception types or handlers. Both controllers mostly use ad hoc `try/catch(Exception)`
blocks that build a `GenericDataDTO`/`ResponseEntity` by hand (`CompanyController.createCompanyAssets`,
every `BranchController` method) rather than letting exceptions propagate to
`handler/GlobalExceptionHandler.java`. `CompanyServImpl`'s public methods likewise swallow exceptions
internally and return a `GenericDataDTO` with `responseCode = 500`, so `GlobalExceptionHandler` rarely gets
invoked for this module's normal-flow errors — it would only fire for exceptions thrown *outside* those
try/catch blocks (e.g., a validation failure inside `@Valid @RequestBody` on `updateCompany`, which Spring
raises as `MethodArgumentNotValidException` before the controller method body runs).

Per `docs/services-and-known-issues.md` §4: `GlobalExceptionHandler` has exactly two handlers
(`CustomValidationException`, generic `Exception`), and **both unconditionally return HTTP 400**
regardless of the semantic error — so a validation failure on `PUT /company` surfaces as HTTP 400 with an
`ApiResponseDto` body, while the module's own hand-built error responses (e.g., "Company not found") come
back as HTTP 200 with a `404`/`500` value inside the JSON `responseCode` field. Callers must inspect the
JSON body, not the HTTP status line, to get the real result in most cases here — the two error-reporting
conventions coexist within this single module (`createCompanyAssets` in particular does return a real
`ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)`/`.badRequest()`, unlike the rest of the module).

---

## 9. Configuration

No dedicated `@ConfigurationProperties` class or `application*.properties` entries specific to Company
were found (checked `application.properties`, `application-local.properties`, `application-prod.properties`
for `company`/`upload`/`storage`/`file` keys — no matches). The one piece of "configuration" that exists
for this module is **data-driven**, not a properties file:

- `tblmclientservice` row seeded by Liquibase changeset `SKP0005`
  (`src/main/resources/Changelog/changelog-master.xml:249-278`):
  `name="COMPANY_ASSET"`, `value="D:\cargo project\document"`, `module="COMPANY_ASSET"`, `status="ACTIVE"`.
  This row is clearly meant to hold the configured base upload directory for company assets — but, per
  §3/§10, `CompanyServImpl.createCompanyAssets` reads `clientservice.getName()` (`"COMPANY_ASSET"`, a
  relative path literal) rather than `clientservice.getValue()` (the actual absolute path). The seeded
  `value` is also a developer-machine-specific Windows path (`D:\cargo project\document`), which would not
  exist on any deployment target even if the code were fixed to read it — this looks like local-dev
  scaffolding accidentally checked into the shared Liquibase changelog.

---

## 10. External Integrations

**Asset storage is local disk, not S3 or any cloud object store.** Verified by reading
`CompanyController.createCompanyAssets` → `CompanyServImpl.createCompanyAssets` →
`FileUtility.saveMultipartFile` (`utils/utillity/FileUtility.java:242-286`):

- `saveMultipartFile(MultipartFile[] files, String basePath)` resolves `basePath` to an absolute path via
  `Paths.get(basePath).toAbsolutePath().normalize()`, creates the directory if missing
  (`Files.createDirectories`), and for each file generates a name
  `UUID.randomUUID() + UUIDGenerator.generate() + "_" + originalName`, then copies the multipart file's
  bytes to that location with `Files.copy(..., StandardCopyOption.REPLACE_EXISTING)`.
- Includes a path-traversal guard: rejects original filenames containing `".."`, and double-checks the
  resolved `targetLocation.startsWith(uploadPath)` before writing.
- As noted in §3/§9, the `basePath` actually passed in for company assets is the literal string
  `"COMPANY_ASSET"` (from `Clientservice.name`), which resolves to a directory named `COMPANY_ASSET`
  **relative to the JVM's current working directory** at runtime — not the seeded absolute
  `D:\cargo project\document` path, and not any deployment-appropriate location (e.g. under `/data` on
  Render.com, where `docs/services-and-known-issues.md` records this app is deployed).
- No cloud SDK (S3, GCS, Azure Blob) import appears anywhere in `FileUtility.java` or `CompanyServImpl.java`.

No other external integrations (payment, SMS, email, third-party APIs) are wired into the Company/Branch
module itself — those live in the notification engine (`notification/**`), which this module does not call
directly.

---

## 11. Important Business Rules & Validations

1. **Company email must be unique** — enforced both at the DB level (`@Column(unique = true)` on
   `company_email`) and pre-emptively in `createCompany` (`existsByCompanyEmail` check → `409`).
2. **PAN/GST format validation only applies on the DTO, and only when `@Valid` is honored.** `panNumber`
   pattern `[A-Z]{5}[0-9]{4}[A-Z]{1}` and `gstNumber` pattern
   `[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}` are declared on `CompanyDTO`
   (`dto/CompanyDTO.java:44,48-51`) but **`createCompany` does not use `@Valid`**
   (`controller/CompanyController.java:40`), so these formats are not actually enforced when a company is
   created — only `@Size` length constraints on the `Company` entity itself (10/15 chars respectively)
   provide any backstop, and those don't check character shape at all.
3. **Required fields for company creation** (entity-level `@NotBlank`/`@NotNull`, `model/Company.java`):
   `companyName`, `companyEmail`, `whatsappNumber`, `contactNo1`, `panNumber`, `gstNumber`, `address`,
   `country`, `state`, `city`. Not required at the entity level: `contactNo2`, `landlineNo`,
   `tollFreeNumber`, `branch`, `mainBranch`, `website`, `companyUUID` (though `companyUUID` is always
   populated server-side in practice).
4. **Company asset files**: max 1&nbsp;MB per file, extensions limited to `pdf, jpg, jpeg, png, svg`
   (case-insensitive), enforced in `CompanyServImpl.validateCompanyAssetFiles` — but only reachable via the
   create-company flow, since the dedicated `/company/assets` endpoint is dead (§2).
5. **Branch creation/update requires `branchName` and `mobile`** — the only validation on `Branch`, and
   it's hand-rolled in `BranchService`, not Bean Validation (the `Branch` entity has no `@NotBlank`/etc.
   annotations at all).
6. **Soft delete, not hard delete**, for both `Company` (`isActive=false, isDeleted=true` via
   `AbstractModel`) and `Branch` (`active=false` via its own field) — rows are retained in the database
   indefinitely.
7. **Tenant visibility rule for "get all companies"**: staff ID `1` (the seeded bootstrap superadmin) sees
   every company; every other authenticated staff member sees at most their own single company (§3). This
   is enforced in application code only, not by any row-level security feature of MySQL.
8. **No uniqueness constraint on `panNumber`, `gstNumber`, or `companyUUID`** at the entity/column level —
   only `companyEmail` is declared unique. Duplicate PAN/GST across two `Company` rows is possible today.

---

## 12. Dependencies Between Modules

`Company` is the tenant root; the following modules/entities carry a pointer back to it (confirmed by
`grep`-ing for `companyId`/`companyUuid` field declarations across `src/main/java/com/cargosetu/`):

| Module / entity | Field | Type | Matches `Company.companyId` (`Integer`)? |
|---|---|---|---|
| `Staff` (`model/Staff.java`) | `companyId` | `Integer` | Yes |
| `Trip` (`model/Trip.java`) | `companyId` | `Integer` | Yes |
| `Customer` (`customer/entity/Customer.java`) | `companyId` | `Integer` | Yes |
| `Shifting` (`customer/entity/Shifting.java`) | `companyId` | `Integer` | Yes |
| `CompanyAsset` (`model/CompanyAsset.java`, this module) | `companyId` | `Integer` | Yes |
| `LoggedInUser` (`config/dto/LoggedInUser.java`, the JWT principal shape) | `companyId` | `Integer` | Yes |
| `Branch` (`model/Branch.java`, this module) | `companyId` | **`Long`** | **No** |
| `Template` (`model/Template.java`) | `companyId` | **`Long`** | **No** |
| `Department` (`model/Department.java`) | `companyUuid` | **`String`** | **No — different key entirely** (points at `Company.companyUUID`, not the numeric PK) |

This confirms and extends the type-inconsistency already flagged in
`docs/services-and-known-issues.md` §5.3 ("`companyId` exists as a plain column on several entities but
its type is inconsistent... `Integer` ... vs `String companyUuid` on `Department`"): within this module's
own direct dependents, `Branch.companyId` and `Template.companyId` are typed `Long` rather than `Integer`,
a **third** distinct representation of the same logical tenant-FK relationship, on top of the
`Integer`/`String companyUuid` split already documented. None of `Branch`, `Template`, `Customer`,
`Shifting`, `Trip`, or `Staff` declares an actual `@ManyToOne`/`@JoinColumn` relationship to `Company` —
every one of these is a scalar convention-based FK, not a JPA-managed or database-enforced one (no
`FOREIGN KEY` constraint is defined; per `docs/services-and-known-issues.md` §4 the schema is Hibernate
`ddl-auto=update`-generated).

Practical implications for anyone extending this module:
- Any new entity that needs tenant scoping should use `Integer companyId` to match `Company.companyId`
  and `Staff`/`Trip`/`Customer`/`Shifting`/`LoggedInUser` — not `Long` (`Branch`/`Template`'s pattern) and
  not a `String` UUID pointer (`Department`'s pattern) — to avoid adding a fourth representation.
  `CustomerService`'s scoping pattern (`docs/services-and-known-issues.md` §1) is the one worth copying for
  query-level enforcement; `CompanyServImpl.getAllCompanies`'s staff-ID-1-bootstraps-superadmin pattern is
  the one worth copying for "does this caller see everything" checks.
- `Staff.companyId` is set as a side effect of `CompanyServImpl.createCompany` (§3) — any staff-management
  feature that reassigns a staff member to a different company must go through (or replicate) that same
  write, since nothing else in the codebase keeps it in sync.
- The `/company/currentCompany` endpoint is the de facto "who is my tenant" lookup used by
  `LoggedInUser.companyId` — any new controller needing "the caller's company" should reuse
  `CompanyService.getCompany`/`getCompanyByStaffId` rather than re-querying `CompanyRepository` directly.
- Because `@PreAuthorize` is globally inert (§7), any *new* endpoint added to this module gets no real
  protection from copying the existing `@PreAuthorize` annotations alone — until
  `MethodSecurityConfig.prePostEnabled` is flipped to `true` (and the fallout from that repo-wide change is
  assessed), authorization for sensitive new Company/Branch operations would need an explicit in-method
  check against `LoggedInUser.getAuthorities()`/`permissions`.

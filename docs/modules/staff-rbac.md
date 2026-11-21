# Staff / Role / Permission / Department Module (RBAC + Org Structure)

This document covers the module that models **who the internal users of the system are**
(`Staff`), **what they're allowed to do** (`Role` → `Permission`), and **how they're grouped
inside a company** (`Department`). This module *is* the permission model that the rest of the
application enforces against via `@PreAuthorize` + `PermissionConstants` — it does not itself
depend on any other business module.

All file paths below are relative to `D:\cargo project\code-setu-app\src\main\java\com\cargosetu\`
unless stated otherwise.

---

## 1. Overview

| Concern | Entity | Table |
|---|---|---|
| Internal user account | `Staff` | `tblm_staff` |
| Named bundle of permissions, assignable to staff | `Role` | `tblm_roles` |
| Atomic, checkable capability | `Permission` | `tblm_permissions` |
| Org-structure grouping of staff within a company | `Department` | `tblm_department` |

Shape: **`Staff` → (many-to-many) → `Role` → (many-to-many) → `Permission`**, plus **`Staff` →
(many-to-one) → `Department`** and a plain `companyId` column on `Staff` tying it to a tenant
(`Company`, documented in the tenant-company module).

This module owns CRUD for all four entities. It does **not** perform authentication itself
(login, JWT issuance, OTP) — that lives in `AuthService` / `OtpService` / the JWT filter chain,
covered in `../modules/auth-security.md` (see [Security & Authentication](#7-security--authentication-rbac-shape)
for exactly where the two modules meet).

---

## 2. API Controllers & Endpoints

Base URL: `/api/v2/cargosetu` (`utils/BaseURL.java`, `config/utils/UrlConstants.java`).
Permission checks below are transcribed directly from each controller's `@PreAuthorize`
annotation — **endpoints with no `@PreAuthorize` shown are reachable by any authenticated
request that passes the JWT filter, with no permission or role check at the controller layer.**

### 2.1 `StaffController` (`controller/StaffController.java`) — base `/api/v2/cargosetu/staff`

| Method | Path | Permission check (`@PreAuthorize`) | Notes |
|---|---|---|---|
| GET | `/staff/{id}` | `hasAuthority(PermissionConstants.STAFF_READ)` **or** `hasRole(CommonConstants.RolesConstants.COMPANY_ADMIN)` | `hasRole` auto-prepends `ROLE_`, so this matches authority `ROLE_COMPANY_ADMIN` |
| POST | `/staff` | `hasAuthority(PermissionConstants.STAFF_CREATE)` | Validates request, checks duplicate email/username, hashes password, saves |
| DELETE | `/staff/{staffId}` | **none** | No `@PreAuthorize` at all — any authenticated caller can soft-delete any staff record by ID |
| GET | `/staff` | `hasAuthority(PermissionConstants.STAFF_READ)` **or** `hasRole(...COMPANY_ADMIN)` | Paginated (`page`, `size`, default `0`/`5`); `@AuthenticationPrincipal LoggedInUser` used for company scoping (see §3.1) |

### 2.2 `RoleController` (`controller/RoleController.java`) — base `/api/v2/cargosetu/role`

| Method | Path | Permission check | Notes |
|---|---|---|---|
| GET | `/role` | **none** | Any authenticated user can list all non-deleted roles |
| GET | `/role/{id}` | **none** | Any authenticated user can fetch any role by ID |
| POST | `/role` | `hasAuthority(PermissionConstants.STAFF_CREATE)` | **Uses the `STAFF_CREATE` permission, not `ROLE_CREATE`**, even though `PermissionConstants.ROLE_CREATE` exists and is seeded (`SeedSuperAdmin`) — likely a copy-paste bug carried over from `StaffController` |
| PUT | `/role` (body carries `roleId`) | **none** | No permission check on role update |
| DELETE | `/role/{id}` | **none** | No permission check on role delete (soft delete) |

### 2.3 `PermissionController` (`controller/PermissionController.java`) — base `/api/v2/cargosetu/permission`

| Method | Path | Permission check | Notes |
|---|---|---|---|
| GET | `/permission` | **none** | |
| GET | `/permission/{id}` | **none** | |
| POST | `/permission` | **none** | Creating new permission *definitions* has no permission gate at all |
| PUT | `/permission/{id}` | **none** | |
| DELETE | `/permission/{id}` | **none** | |

None of `PermissionConstants`' role/permission-scoped constants (`ROLE_CREATE`, `ROLE_READ`,
`ROLE_UPDATE_DELETE`) are referenced anywhere in `RoleController` or `PermissionController` —
grep confirms they are defined in `utils/PermissionConstants.java` and seeded by
`SeedSuperAdmin`, but never consumed by a `@PreAuthorize` expression anywhere in the codebase.

### 2.4 `DepartmentController` (`controller/DepartmentController.java`) — base `/api/v2/cargosetu/department`

| Method | Path | Permission check | Notes |
|---|---|---|---|
| GET | `/department` | **none** | |
| GET | `/department/{id}` | **none** | |
| POST | `/department` | **none** | |
| PUT | `/department/{id}` | **none** | |
| DELETE | `/department/{id}` | **none** | |

No `PermissionConstants` entries exist for Department at all (there is no `DEPARTMENT_READ` /
`DEPARTMENT_CREATE` constant in `utils/PermissionConstants.java`), even though `SeedSuperAdmin`
seeds DB-level permission rows named `DEPARTMENT_CREATE`, `DEPARTMENT_READ`,
`DEPARTMENT_UPDATE_DELETE` for the SUPERADMIN role (`service/SeedSuperAdmin.java` lines 69–71).
Those seeded permissions are consequently unreachable dead data — nothing in
`DepartmentController` checks for them.

**Summary of the permission-enforcement gap:** of the 19 endpoints across all four controllers,
only 3 carry a `@PreAuthorize` check (`StaffController` GET-by-id, GET-all, POST). Every Role,
Permission, and Department endpoint — including destructive operations (role delete, permission
delete, department delete) and privilege-escalating ones (permission create, which is
immediately auto-bound to the `SUPERADMIN` role, see §3.3) — is open to any authenticated
caller regardless of role or permission.

---

## 3. Business / Service Logic

### 3.1 Staff (`service/StaffService.java` — a concrete `@Service`, no separate interface)

- **`saveStaff(StaffDTO, boolean isAdmin)`** (`@Transactional`):
  - Generates a UUID (`UUIDGenerator.generate()`) and derives `userName` from the email local-part
    plus the last part of the UUID: `email.substring(0, indexOf('@')) + uuid.substring(4)` — the
    caller-supplied `userName` on `StaffDTO` is effectively ignored.
  - Role assignment branches on `isAdmin`:
    - `true` → looks up the `COMPANY_ADMIN` role by code; if missing, calls
      `RolesImpl.createCompanyAdminDefaultRole()` to create it on the fly.
    - `false` → loads roles by the IDs in `staffDTO.getRoleIds()` via `rolesRepo.findAllById(...)`.
  - **Password hashing**: `new BCryptPasswordEncoder().encode(staff.getPassword())` — a new
    `BCryptPasswordEncoder` instance is constructed inline here rather than an injected shared
    bean (there is a `PasswordEncoder` bean elsewhere per `auth-security.md`; worth noting as an
    inconsistency rather than a bug, since `BCryptPasswordEncoder()` with no args uses default
    strength 10 either way).
  - Saves the staff, then publishes **two** `GenericNotificationEvent`s via
    `ApplicationEventPublisher` (`STAFF_CREATED` over EMAIL if an email is present, `STAFF_CREATED`
    over SMS if a mobile number is present) — fire-and-forget, consumed by the notification
    engine (out of scope for this module).
  - Returns the saved entity mapped back to `StaffDTO` via `StaffMapper`.
- **`getStaffById(Long)`**: plain `findById`, no company filter — same "any authenticated user
  can read any staff record by ID (if they clear the `STAFF_READ`/`COMPANY_ADMIN` gate)" pattern,
  though at least this one *is* gated at the controller.
- **`validateCreateStaffRequest(StaffDTO)`**: throws `CustomValidationException` (HTTP 417,
  `EXPECTATION_FAILED`, but see §8 — the exception handler always returns HTTP 400 on the wire
  regardless) if `firstName`, `lastName`, `mobileCode`, `email`, or `mobileNumber` is null/blank.
  `lastName`'s blank-check reuses the message `"First Name cannot be blank"` (copy-paste bug).
  The `userName` check is present in source but commented out.
- **`isDuplicateUser(StaffDTO)`**: `staffRepository.findByEmailOrUserName(email, userName)` —
  used by the controller to reject duplicate signups before calling `saveStaff`.
- **`deleteStaff(Long)`** (`@Transactional`): **soft delete only** — sets `isDeleted = true`,
  stamps `updatedAt`/`updatedByName`/`updatedById` from `ApplicationUtils.getCurrentUser()`, saves.
  Throws `CustomValidationException` (code from `CommonStatusCode.VALIDATION_ERROR`, 422) if
  `staffId` is null or `<= 0`. Note `getAllStaff`/`getStaffById` do **not** filter out
  soft-deleted staff (`findById`/`findAll` on a JPA repo returns rows regardless of `isDeleted`).
- **`getAllStaff(LoggedInUser, Pageable)`** (`@Transactional`, declared `throws Exception`):
  - **This is the one place in the module that does tenant scoping correctly** — mirrors the
    pattern `services-and-known-issues.md` calls out for `CustomerService`:
    - If `user.getStaffId() == 1` (bootstrap superadmin, hardcoded ID) → `staffRepository.findAll(pageable)`, i.e. sees all companies' staff.
    - Else if `user.getCompanyId()` is null → throws a plain `Exception("Company not found,create Company first!")` (not a `CustomValidationException`, so it's caught by the generic `Exception` handler in `GlobalExceptionHandler`, returning HTTP 400 with `CommonStatusCode.INTERNAL_SERVER_ERROR` (500) as the body's embedded error code — a mismatch between the transport HTTP status and the embedded code).
    - Else → `staffRepository.findAllByCompanyIdOrderByStaffIdDesc(user.getCompanyId(), pageable)`.

### 3.2 Roles (`service/RolesService.java` interface + `service/impl/RolesImpl.java`)

`RolesService` interface only formally declares `getRolseList()` [sic]; `RolesImpl` (the class
actually wired via `@Autowired RolesImpl` in `RoleController`, not the interface type) implements
several more methods that aren't on the interface (`getRoleById`, `updateRole`, `deleteRole`,
`createRole`, `createCompanyAdminDefaultRole`) — the interface is effectively decorative here since
the controller depends on the impl class directly.

- **`getRolseList()`**: `rolesRepo.findAllByIsDeletedIsFalseOrderByRolesIdDesc()` → wrapped in
  `GenericDataDTO.dataList`. On exception, message is set to "Failed to fetch roles" but
  `responseCode` is left `null` (not explicitly set in the catch block) and HTTP status is
  still 200 (no exception propagates to `GlobalExceptionHandler`).
- **`getRoleById(Long)`**: `findById`, treated as not-found (404 in the DTO body, HTTP 200
  transport status since nothing throws) if absent OR if `isDeleted == true`.
- **`updateRole(RolesDTO)`**: looks up by `pojo.getRoleId()`; if the `code` is being changed,
  checks `findByCodeAndIsDeletedIsFalse(newCode)` and rejects (409/`CONFLICT`) unless the
  matched row is the same role being updated. Copies `roleName`, `description`→`discription`,
  `code`, `scope` onto the entity **manually via setters** (not through `RoleMapper`) and saves.
  **Does not touch permissions** — there is no endpoint/method to change a role's permission set
  after creation other than recreating it.
- **`deleteRole(Long)`**: soft delete (`isDeleted=true`, `isActive=false`).
- **`createRole(RolesDTO)`**: rejects duplicate `code` (409). Builds a new `Role` **manually via
  setters** (not through `RoleMapper`), sets `isActive=true`/`isDeleted=false`, resolves
  `pojo.getPermissionIds()` via `permissionRepo.findAllById(...)` into `role.setPermissions(...)`,
  saves.
- **`createCompanyAdminDefaultRole()`**: called from `StaffService.saveStaff` when creating an
  admin staff and no `COMPANY_ADMIN` role exists yet. Builds the role with `scope="COMPANY"` and
  — per the source comment "for temporary purpose only" — grants it
  `permissionRepo.findAllByCreatedByName(CommonConstants.SYSTEM_DEFAULT)`, i.e. **every
  permission whose `createdByName` happens to equal `"SYSTEM-DEFAULT"`**, not a curated
  company-admin permission set. This is explicitly flagged in-source as a stopgap.

### 3.3 Permissions (`service/PermissionService.java` interface + `service/impl/PermissionImpl.java`)

- **`getPermissionList()`**: `findAllByIsDeletedIsFalseOrderByIdDesc()`.
- **`getPermissionById(Long)`**: `findById`, 404 if absent or soft-deleted.
- **`updatePermission(Long, PermissionDTO)`**: same duplicate-code-guard pattern as roles (409 on
  code collision with a different row), delegates field copy to `PermissionMapper.updateFromDto`
  (this one *does* go through the MapStruct mapper, unlike Role's update).
- **`deletePermission(Long)`**: soft delete.
- **`createPermission(PermissionDTO)`**: rejects duplicate code (409); sets
  `createdBy = ApplicationUtils.getCurrentUser().getStaffId()` (note: sets `createdBy`, not
  `createdByName` — so this permission will **not** be picked up by
  `createCompanyAdminDefaultRole()`'s `findAllByCreatedByName(SYSTEM_DEFAULT)` query above).
  **Then unconditionally looks up the role with code `SUPERADMIN` and adds the new permission to
  its permission set**, saving the role. This means every newly created permission is
  automatically granted to the SUPERADMIN role, with no restriction on who can call this
  (unauthenticated-permission-wise; see §2.3 — the endpoint has no `@PreAuthorize` at all).

### 3.4 Department (`service/DepartmentService.java` interface + `service/impl/DepartmentImpl.java`)

- **`getDepartmentsList()`**: `departmentRepo.findAll()` — **no `isDeleted` filter and no
  company filter** (contrast with Role/Permission's list methods, which at least filter
  `isDeleted`).
- **`getDepartmentById(Integer)`**: `findById`, checked against `isDeleted` in the service (not
  the query) — no company filter.
- **`updateDepartment(Integer, DepartmentDTO)`**: `findById` (no company filter); duplicate-code
  guard via `existsByCode`/`findByCodeAndIsDeletedIsFalse`, **which is also global, not
  company-scoped** — so a department code is unique across the entire table, not per company,
  despite the entity declaring a composite unique constraint on `(company_uuid, dept_code)`
  (see §5 and §11). Field copy goes through `DepartmentMapper.updateFromDto`.
- **`deleteDepartment(Integer)`**: soft delete, `findById` with no company filter.
- **`createDepartment(DepartmentDTO)`**: rejects duplicate code via `existsByCode(pojo.getCode())`
  — again global, ignoring `company_uuid`. Sets `createdBy` from the current user. **Sets
  `departmentUuid` and `companyUuid` to `pojo.getName() + generatedUUID`** — i.e. `companyUuid`
  is **never actually derived from the authenticated user's real company**; it's a throwaway
  string built from the department's own name plus a random UUID. This is a second, independent
  bug on top of the missing tenant-scoping filters: even if the read/update/delete paths were
  fixed to filter by `companyUuid`, the column itself is not currently populated with any
  meaningful tenant identifier.

---

## 4. DTOs / Request-Response Models

| DTO | File | Notable fields / mapping quirks |
|---|---|---|
| `StaffDTO` | `dto/StaffDTO.java` | `roles: Set<RolesDTO>` (response shape) vs `roleIds: Set<Long>` (request-only, used to resolve roles on create); `password` is present but `StaffMapper.toDto` explicitly ignores mapping it back out (`@Mapping(source="password", target="password", ignore=true)`, verified in the MapStruct-generated `StaffMapperImpl.toDto`, which has no `setPassword` call) so it never appears in a GET/create response body. `StaffMapper.toEntity`, by contrast, *does* map `password` straight through (needed so `StaffService.saveStaff` can BCrypt-encode the raw value from the request). `department: DepartmentDTO`, `companyId: Integer`. |
| `RolesDTO` | `dto/RolesDTO.java` | `roleId` (entity field is `rolesId` — mapped explicitly via `@Mapping` in `RoleMapper`), `permissions: Set<PermissionDTO>` (response) vs `permissionIds: Set<Long>` (request-only, read directly by `RolesImpl`, not by the mapper). **Confirmed bug**: `RolesDTO.description` never reaches/leaves `Role.discription` through `RoleMapper`. The entity field is misspelled `discription` while the DTO field is spelled `description`; MapStruct matches by exact property name, so the generated `RoleMapperImpl` (`build/generated/sources/annotationProcessor/java/main/com/cargosetu/mapper/RoleMapperImpl.java`) has **no `setDiscription`/`getDiscription` call anywhere** in `toEntity`, `toDto`, or `updateFromDto` — the field is silently dropped whenever `RoleMapper` is used. In practice this mainly affects the nested `Staff.roles → StaffDTO.roles` conversion done by `StaffMapper` (so a created/fetched `StaffDTO`'s embedded roles never carry a description) — `RolesImpl.createRole`/`updateRole` sidestep this by setting `role.setDiscription(pojo.getDescription())` by hand rather than using `RoleMapper`, so the standalone Role CRUD endpoints are unaffected by this particular mapper bug. |
| `PermissionDTO` | `dto/PermissionDTO.java` | `permissionId` maps to entity's `id` (explicit `@Mapping` in `PermissionMapper`, confirmed 1:1 field-for-field in generated `PermissionMapperImpl`). `module` is free text, not an enum — matches `Permission.module`. |
| `DepartmentDTO` | `dto/DepartmentDTO.java` | Mirrors entity 1:1 including audit fields (`isActive`, `isDeleted`, `createdByName`, `createdBy`, `createdAt`, `updatedAt`) — unusual in that most other DTOs in this module don't expose audit columns directly (confirmed field-for-field in generated `DepartmentMapperImpl`). `companyUuid` is present on the DTO but, per §3.4, is not populated from the real tenant on create. |

All four DTOs use `@Data` (Lombok) plus MapStruct mappers extending a shared
`mapper/base/BaseUpdateMapper<DTO, Entity>` (declares `toEntity`, `toDto`, `updateFromDto`, the
last used by the `update*` service methods that opt into it) configured via
`config/GlobalMapperConfig` (`@MapperConfig(componentModel = "spring",
nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` — this is why
`updateFromDto` implementations null-guard every field before copying it, leaving existing entity
values untouched when the incoming DTO field is `null`). `StaffDTO`/`RolesDTO` are also annotated
`@JsonIgnoreProperties(ignoreUnknown = true)`; `PermissionDTO`/`DepartmentDTO` are not.

---

## 5. Entities & Database Relationships

### `Staff` (`model/Staff.java`) → table `tblm_staff`

| Column | Field | Notes |
|---|---|---|
| `staff_id` | `staffId` (Long, PK, IDENTITY) | |
| `staff_uuid` | `uuid` | `NOT NULL`, `UNIQUE` |
| `username` | `userName` | `NOT NULL` |
| — | `firstName`, `lastName`, `mobileNumber`, `mobileCode`, `email`, `password` | plain columns, default naming |
| — (join) | `roles: Set<Role>` | `@ManyToMany`, `FetchType.LAZY`, join table **`tblm_user_roles`** (`staff_id` → `role_id`) |
| `staff_type` | `staffType` (VARCHAR 100) | free-text: `SUPERADMIN, COMPANY_ADMIN, MANAGER, DRIVER, ACCOUNTANT` per comment — not an enum, not validated |
| `profile_image_url`, `alternate_mobile`, `address_line`, `city`, `state`, `pincode`, `license_number`, `license_expiry_date` | — | HR/profile fields |
| `last_login_at`, `failed_login_attempts` (default 0), `password_changed_at`, `previous_password` | — | present on the entity but **not populated anywhere in this module** (no code in `StaffService`/`AuthService`/`OtpService` was found updating `lastLoginAt`, `failedLoginAttempts`, or `previousPassword` from a grep of this module — likely intended for future lockout/password-history features; unverified against `auth-security.md`'s login path in full) |
| `department_id` | `department: Department` | `@ManyToOne`, `FetchType.LAZY`, FK to `tblm_department.department_id` |
| `company_id` | `companyId` (Integer) | plain FK-by-convention to the `Company` entity/table (tenant-company module) — **not** a JPA `@ManyToOne`, just an int column |

Extends `abstracts/AbstractModel` (see §5's `AbstractModel` entry below) for audit columns.

### `Role` (`model/Role.java`) → table `tblm_roles`

| Column | Field | Notes |
|---|---|---|
| `role_id` | `rolesId` (Long, PK, IDENTITY) | |
| `code` | `code` (VARCHAR 50) | `NOT NULL`, `UNIQUE` — global uniqueness, not per-company (there is no company concept on `Role` at all) |
| `role_name` | `roleName` (VARCHAR 100) | `NOT NULL` |
| `description` | `discription` (TEXT) | column name is correctly spelled `description`; the **Java field is misspelled `discription`**, which is what breaks `RoleMapper`'s auto-generated mapping — see §4 |
| `scope` | `scope` (VARCHAR 50) | `NOT NULL` — free-text, not an enum. Observed values in code: `"PLATFORM"` (seeded `SUPERADMIN` role, `SeedSuperAdmin.seedDefaultRole()`) and `"COMPANY"` (`createCompanyAdminDefaultRole()`). No other module reads or branches on `scope`'s value — it is stored but, as far as this codebase's current logic is concerned, **not enforced anywhere** (e.g. nothing stops a `"COMPANY"`-scoped role from being assigned platform-wide, and nothing filters role listing by scope). Treat it as documentation-only metadata unless/until enforcement code is added. |
| — (join) | `permissions: Set<Permission>` | `@ManyToMany`, `FetchType.LAZY`, join table **`tblm_role_permissions`** (`role_id` → `permission_id`) |

`@EqualsAndHashCode(onlyExplicitlyIncluded = true)` with no field marked `@EqualsAndHashCode.Include`
means `Role.equals`/`hashCode` fall back to identity-based defaults from that Lombok mode
(effectively reference equality) — worth knowing if `Role` instances are ever put in a `Set` and
compared before being persisted (unverified whether this causes any observable bug; noted as a
code-smell).

### `Permission` (`model/Permission.java`) → table `tblm_permissions`

| Column | Field | Notes |
|---|---|---|
| `permission_id` | `id` (Long, PK, IDENTITY) | |
| `code` | `code` (VARCHAR 100) | `NOT NULL`, `UNIQUE` |
| `name` | `name` (VARCHAR 150) | `NOT NULL` |
| `description` | `description` (VARCHAR 500) | |
| `module` | `module` (VARCHAR 50) | `NOT NULL` — free-text grouping label (e.g. `"Role Module"`, `"Staff Module"` per `SeedSuperAdmin`), not a foreign key to anything |

No entity-level relationship back to `Role` (owned entirely from the `Role` side via
`tblm_role_permissions`).

### `Department` (`model/Department.java`) → table `tblm_department`

| Column | Field | Notes |
|---|---|---|
| — | `departmentId` (Integer, PK, IDENTITY) | note: **`Integer`**, not `Long` like the other three PKs |
| `department_uuid` | `departmentUuid` | `NOT NULL`, `updatable=false` |
| `company_uuid` | `companyUuid` | `NOT NULL` — intended tenant key, but see §3.4/§11 for the population bug |
| `dept_code` | `code` (VARCHAR 50) | comment: "ex: MARKETING_EXECUTIVE, PACKAGING, ONSITE" |
| `name` | `name` (VARCHAR 100) | `NOT NULL` |
| `description` | `description` | |
| — | table-level `@UniqueConstraint(columnNames = {"company_uuid", "dept_code"})` | **Declared** as a composite unique constraint (dept code unique per company), but the service layer's duplicate check (`DepartmentRepo.existsByCode(code)`) only checks `code` globally, not `(companyUuid, code)` — so the DB constraint and the application-level duplicate check are inconsistent; the DB constraint would in fact reject legitimate same-code departments in different companies once `companyUuid` is populated correctly, while today (with `companyUuid` always unique garbage per §3.4) the DB constraint is effectively never triggered since every row's `companyUuid` is unique. |

Overrides `equals`/`hashCode` explicitly based on `departmentId` only (standard JPA entity
equality pattern, unlike `Role`'s incomplete Lombok setup above).

### `AbstractModel` (`abstracts/AbstractModel.java`) — `@MappedSuperclass`

Shared audit columns inherited by `Staff`, `Role`, `Permission`, `Department` (and other
entities across the app): `is_active` (default `true`), `is_deleted` (default `false`),
`created_by`, `created_by_name`, `created_at` (`updatable=false`, stamped via `@PrePersist`),
`updated_by_name`, `updated_by_id`, `updated_at` (also marked `updatable=false`, which looks like
a bug — an `updatedAt` column that JPA will never actually update via `@Column(updatable=false)`
even though service code repeatedly calls `entity.setUpdatedAt(LocalDateTime.now())` before
`save()`; those explicit-setter values are silently dropped by Hibernate on `UPDATE` statements
because of the `updatable=false` annotation — **flagged as inferred, not runtime-verified**).

### Relationship summary

```
Staff *---* Role         (tblm_user_roles: staff_id, role_id)
Role  *---* Permission   (tblm_role_permissions: role_id, permission_id)
Staff  ---> Department   (tblm_staff.department_id -> tblm_department.department_id, ManyToOne)
Staff  ---> Company      (tblm_staff.company_id, plain int column, no JPA relationship)
Department ---> Company  (tblm_department.company_uuid, plain string column, no JPA relationship)
```

---

## 6. Repositories

| Repository | File | Query methods |
|---|---|---|
| `StaffRepository` | `repo/StaffRepository.java` | `findByUserName(String)`; `findByEmail(String)`; `findByEmailOrUserName(String, String)`; `findByMobileNumberAndIsDeleted(String, boolean)`; `findAllByCompanyIdOrderByStaffIdDesc(Integer, Pageable)` |
| `RolesRepo` | `repo/RolesRepo.java` | `findByCode(String)`; `findByCodeAndIsDeletedIsFalse(String)`; `existsByCode(String)`; `findAllByIsDeletedIsFalse()`; `findAllByIsDeletedIsFalseOrderByRolesIdDesc()` |
| `PermissionRepo` | `repo/PermissionRepo.java` | `existsByCode(String)`; `findByCodeAndIsDeletedIsFalse(String)`; `findAllByIsDeletedIsFalse()`; `findAllByIsDeletedIsFalseOrderByIdDesc()`; `findAllByCreatedByName(String)` |
| `DepartmentRepo` | `repo/DepartmentRepo.java` | `existsByCode(String)`; `findByCodeAndIsDeletedIsFalse(String)` — **no `findAllBy...` or company-scoped query method exists on this repository at all**, consistent with §3.4/§11's finding that the service never filters by company |

**Cross-check against `OtpService` (per `docs/services-and-known-issues.md` line ~21):**
`StaffRepository.findByMobileNumberAndIsDeleted(String mobileNumber, boolean isDeleted)` **is**
the phone-lookup method this module exposes for staff OTP login, and `OtpService` (`service/OtpService.java`)
does call it directly as an exact-match query — not a fuzzy search — but it calls it **up to three
times per lookup**, against three differently-normalized forms of the caller-supplied number, in
both places `OtpService` needs to resolve a `Staff` by phone:
- `sendOtp(String mobileNumber)` (~lines 60–80): strips everything but digits/`+`
  (`cleanNumber`), drops a leading `+` for a `searchNumber` first attempt
  (`staffRepository.findByMobileNumberAndIsDeleted(searchNumber, false)`), retries with the
  `+`-prefixed `cleanNumber` if empty, then retries again with just the last 10 digits of
  `searchNumber` if still empty.
- `verifyOtp(...)` (~lines 186–193): repeats the exact same three-attempt normalization/lookup
  sequence against `StaffRepository`, after OTP-code validation succeeds, in order to build the
  `LoggedInUser`/JWT claims for the authenticated staff member — this is a second, independent
  place in `auth-security.md`'s territory that reads `Staff.roles`/`Role.permissions` the same way
  `AuthService.loginUser` does (see §7).

So `findByMobileNumberAndIsDeleted` is the module's one exact-match phone primitive, and
`OtpService` layers its own digit-normalization/fallback logic on top of repeated calls to it
rather than doing a single lookup — treat this repository method as the supporting primitive, and
`OtpService`'s normalization loop (documented in more detail in `services-and-known-issues.md`) as
the actual matching behavior exposed to OTP login.

None of the four repositories apply an `isDeleted = false` filter automatically — every
`findById`/`findAll` from `JpaRepository` returns soft-deleted rows too; each service filters
`isDeleted` manually in code (Department's `getDepartmentsList()` doesn't even do that, see §3.4).

---

## 7. Security & Authentication (RBAC Shape)

This module **is** the data model that Spring Security's `@PreAuthorize` checks are evaluated
against; it does not itself run the JWT filter or login flow (that's `auth-security.md`'s
domain). The shape, traced end-to-end from this module's entities through to enforcement:

1. **`Staff.roles`** (`Set<Role>`, via `tblm_user_roles`) — a staff member has zero or more roles.
2. **`Role.permissions`** (`Set<Permission>`, via `tblm_role_permissions`) — a role has zero or
   more permissions. Permission codes stored in the DB are **plain, unprefixed** strings (e.g.
   `STAFF_READ`, `ROLE_CREATE` — see `SeedSuperAdmin.seedDefaultRole()` and
   `utils/PermissionConstants.java`'s own doc-comment, which explicitly instructs "Database
   permission codes must be plain… Always build permission constants using `PERM_PREFIX` +
   `<DB_PERMISSION_CODE>`").
3. **`Role.scope`** — free-text (`"PLATFORM"`, `"COMPANY"` observed), stored but **not read or
   branched on anywhere** in the codebase found by this review. It documents intent (platform-wide
   vs. company-scoped role) but there is no enforcement code gating role assignment or visibility
   by scope value — treat it as metadata only, not a security boundary, unless later code adds
   enforcement.
4. **At login**, both `AuthService.loginUser` (password login, outside this module) and
   `OtpService.verifyOtp` (OTP login, see §6) independently traverse this module's data the same
   way: role codes via `staff.getRoles().stream().map(role -> role.getCode())`, and permission
   codes via `staff.getRoles().stream().flatMap(r -> r.getPermissions().stream()).map(Permission::getCode)`
   — both **still plain, unprefixed** at this point — and placed on `LoggedInUser`/the JWT claims.
5. **At request time**, `config/JwtAuthenticationFilter.java` reads the `permissions` claim from
   the validated JWT and builds `SimpleGrantedAuthority("PERM_" + p)` for each — **this is where
   the `PERM_` prefix from `PermissionConstants` actually gets attached**, and role claims become
   `SimpleGrantedAuthority("ROLE_" + r)`. `@PreAuthorize("hasAuthority(...STAFF_READ)")` then
   matches because `PermissionConstants.STAFF_READ` itself already equals `"PERM_STAFF_READ"`.
   (Verified directly in `config/JwtAuthenticationFilter.java`: the active code path builds
   `new SimpleGrantedAuthority("PERM_"+p)` for permissions and `new SimpleGrantedAuthority("ROLE_" + r)`
   for roles; an earlier, unprefixed version of the same logic is present but commented out in the
   same file.)
6. **Inconsistency worth flagging** (found while reading this module's own
   `LoggedInUser`/`UserDetailsServiceImpl` dependency for context, both technically part of
   `auth-security.md`'s territory but directly affecting how this module's `Role`/`Permission`
   data is consumed): `config/dto/LoggedInUser.getAuthorities()` — the `UserDetails` method
   invoked when Spring's `DaoAuthenticationProvider` authenticates via
   `UserDetailsServiceImpl.loadUserByUsername` — adds permission authorities **without** the
   `PERM_` prefix (verified in `config/dto/LoggedInUser.java`:
   `permissions.forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)))`), and
   `UserDetailsServiceImpl.convertStaffToLoggedInUser` was only confirmed (by grep) to set `roles`
   on the `LoggedInUser` it builds — whether it also populates `permissions` on that same object
   was not fully traced line-by-line here. So if that authentication path's `getAuthorities()` is
   ever consulted directly for authorization (rather than only as a password-check step before JWT
   issuance), permission-based `@PreAuthorize` checks would not match a plain, unprefixed
   authority. **Unverified**: whether `UserDetailsServiceImpl`'s `LoggedInUser` is used only
   transiently during login (most likely, since both `AuthService.loginUser` and
   `OtpService.verifyOtp` separately construct their own fully-populated `LoggedInUser` for JWT
   issuance, per §7.4) or also wired into a request-time filter — confirm in `auth-security.md`
   before relying on this observation for a fix.

**Bootstrap**: `service/SeedSuperAdmin.java` (an `ApplicationRunner`, runs once at startup) seeds
a `SUPERADMIN` staff (username `SUPERADMIN`, hardcoded bcrypt hash in source) and a `SUPERADMIN`
role (`scope="PLATFORM"`) with a hardcoded list of permission codes covering Role, Staff,
Department, Plan, Company, and Customer modules — this is the origin point for the entire
permission set until an admin creates more via `PermissionController`. Detailed bootstrap
mechanics (idempotency check via `findByUserName`, hash provenance) belong to
`../modules/auth-security.md`; this doc notes the dependency rather than re-deriving it.

---

## 8. Exception Handling

- `handler/GlobalExceptionHandler.java` (`@RestControllerAdvice`) handles two cases app-wide,
  including for this module:
  - `CustomValidationException` → HTTP 400 (`ResponseEntity.badRequest()`), body =
    `ApiResponseDto(ex.getErrorCode(), ex.getMessage(), null)` — note the **transport status is
    always 400** regardless of the `errorCode` embedded in the exception (e.g.
    `StaffService.validateCreateStaffRequest` throws with `HttpStatus.EXPECTATION_FAILED.value()`
    (417) as the embedded code, but the actual HTTP response is still 400).
  - Any other `Exception` → also mapped to `ResponseEntity.badRequest()` (HTTP 400) with
    `CommonStatusCode.INTERNAL_SERVER_ERROR` (500) as the embedded code — so a genuine server error
    (e.g. `StaffService.getAllStaff`'s `throw new Exception("Company not found...")`) surfaces to
    the client as HTTP 400, not 500.
- **Most of this module's own service methods (`RolesImpl`, `PermissionImpl`, `DepartmentImpl`)
  do not let exceptions propagate to `GlobalExceptionHandler` at all** — every CRUD method wraps
  its body in a local `try/catch (Exception e)` and returns a `GenericDataDTO` with a failure
  message and (usually) a `responseCode` of 500, but the **controller still returns HTTP 200**
  for these, since the controllers simply return the `GenericDataDTO` object directly (no
  `ResponseEntity`, no status mapping) — the numeric `responseCode` field inside the JSON body is
  the only signal of failure, not the actual HTTP status.
- `handler/CustomValidationException.java`: simple `RuntimeException` subtype carrying an
  `Integer errorCode` alongside the message; thrown from `StaffService` validation and the
  duplicate-staff / staffId-validation checks in `deleteStaff`.

---

## 9. Configuration

- Base path: `BASE_URL = "/api/v2/cargosetu"` (`utils/BaseURL.java`); segment constants
  `ROLES = "/role"`, `PERMISSION = "/permission"`, `DEPARTMENT = "/department"` (also in
  `BaseURL.java`); `StaffUrlConstants.STAFF = "/staff"` (`config/utils/UrlConstants.java`).
- `utils/PermissionConstants.java` is the single source of truth for `PERM_`-prefixed authority
  strings used in `@PreAuthorize`; see §7 for exactly how DB-stored plain codes become these
  prefixed authorities at request time. Full permission list defined there (verified by reading
  the file end to end):
  `ROLE_CREATE`, `ROLE_READ`, `ROLE_UPDATE_DELETE`, `STAFF_CREATE`, `STAFF_READ`,
  `STAFF_UPDATE_DELETE`, `COMPANY_READ`, `COMPANY_CREATE`, `COMPANY_DELETE`, `COMPANY_UPDATE`,
  `COMPANY_UPDATE_DELETE`, `PLAN_READ`, `PLAN_CREATE`, `PLAN_UPDATE_DELETE`, `CUSTOMER_CREATE`,
  `CUSTOMER_DELETE`, `CUSTOMER_UPDATE`, `CUSTOMER_VIEW`, `SHIFT_CREATE`, `SHIFT_READ`,
  `SHIFT_UPDATE`, `SHIFT_INVENTORY_CREATE`, `SHIFT_INVENTORY_READ`, `SHIFT_INVENTORY_UPDATE`,
  `SHIFT_INVENTORY_STATUS_UPDATE`, `TRIP_CREATE`, `TRIP_READ`, `TRIP_UPDATE`, `TRIP_DELETE`,
  `TRIP_STATUS_HISTORY_CREATE`, `TRIP_STATUS_HISTORY_READ`, `TRIP_STATUS_HISTORY_UPDATE`,
  `TRIP_STATUS_HISTORY_DELETE`, `TRIP_EXPENSE_CREATE`, `TRIP_EXPENSE_READ`,
  `TRIP_EXPENSE_UPDATE`, `TRIP_EXPENSE_DELETE`, `TRIP_DOCUMENT_CREATE`, `TRIP_DOCUMENT_READ`,
  `TRIP_DOCUMENT_UPDATE`, `TRIP_DOCUMENT_DELETE`. Note there is **no `DEPARTMENT_*` constant**
  in this file (see §2.4) even though `DEPARTMENT_*` permission rows are seeded in the DB.
- No module-specific `application.yml`/`.properties` keys, feature flags, or `@ConfigurationProperties`
  classes were found for Staff/Role/Permission/Department — CRUD is unconditional, not
  feature-gated.
- `config/GlobalMapperConfig` (MapStruct, `@MapperConfig(componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`) is shared config
  for all four mappers in this module — every generated `updateFromDto` null-guards each field
  before copying (verified in all four generated `*MapperImpl` classes under
  `build/generated/sources/annotationProcessor/java/main/com/cargosetu/mapper/`).

---

## 10. External Integrations

**None.** This module has no outbound calls to third-party services, payment gateways, SMS/email
providers, or cloud storage. The only cross-cutting side effect is publishing
`GenericNotificationEvent`s from `StaffService.saveStaff` (EMAIL/SMS channel), which are consumed
by the in-app notification engine (`notification/service/*`, documented elsewhere) — that
consumer, not this module, is what talks to any external SMS/email provider.

---

## 11. Important Business Rules & Validations

- **Role code uniqueness**: enforced at both DB level (`tblm_roles.code` `UNIQUE`) and
  application level (`RolesImpl.createRole`/`updateRole` check `existsByCode`/
  `findByCodeAndIsDeletedIsFalse` before saving, returning HTTP-body 409 on collision).
- **Permission code uniqueness**: same pattern, DB (`tblm_permissions.code` `UNIQUE`) +
  application-level check in `PermissionImpl`.
- **Department code uniqueness**: DB constraint is **composite** (`company_uuid, dept_code`
  unique together — i.e., intended to allow the same code across different companies), but the
  application-level check (`DepartmentRepo.existsByCode`) checks `code` **alone**, globally
  across all companies. See the confirmed tenant-scoping gap below for why this matters in
  practice.
- **Staff duplicate check**: `email` OR `userName` collision blocks creation
  (`StaffService.isDuplicateUser`), checked by the controller *before* calling `saveStaff`.
- **Staff required fields on create**: `firstName`, `lastName`, `mobileCode`, `email`,
  `mobileNumber` (see §3.1's `validateCreateStaffRequest`); `userName` validation is present in
  source but commented out, consistent with `userName` being server-generated rather than
  client-supplied (§3.1).
- **Password hashing**: BCrypt (`BCryptPasswordEncoder`, default strength), applied once at
  staff creation in `StaffService.saveStaff`. No module code in this review path re-hashes or
  rotates passwords on update — `previousPassword`/`passwordChangedAt` columns exist on `Staff`
  but were not found to be written anywhere in this module (see §5's `Staff` table notes).
- **Soft delete everywhere**: `Staff`, `Role`, `Permission`, `Department` all use `isDeleted`
  (from `AbstractModel`) rather than hard `DELETE` — but as noted in §6, JPA's default
  `findById`/`findAll` do not exclude soft-deleted rows, so each service must (and mostly does,
  except `DepartmentImpl.getDepartmentsList`) filter manually.
- **`Role.discription`/`RolesDTO.description` mismatch**: confirmed dead field mapping through
  `RoleMapper` for the nested `Staff.roles → StaffDTO.roles` path; not a data-loss bug for direct
  Role CRUD since `RolesImpl` sets the entity field by hand. See §4 for the full explanation and
  §5 for the entity-level root cause (Java field misspelled `discription`, DB column correctly
  named `description`).
- **Bootstrap dependency**: the very first `SUPERADMIN` staff/role/permission set is created by
  `SeedSuperAdmin` (`service/SeedSuperAdmin.java`) on application startup, not through this
  module's own controllers. Its idempotency and credential provenance are documented in
  `../modules/auth-security.md` — **not re-derived in depth here**; this module only supplies the
  `Staff`/`Role`/`Permission` entities and repositories that `SeedSuperAdmin` writes through.

### Confirmed tenant-scoping gap (Department)

Verified directly against `service/impl/DepartmentImpl.java` (not just repeated from
`docs/services-and-known-issues.md` — independently re-read line-by-line for this doc):

| Method | Repository call | Behavior |
|---|---|---|
| `getDepartmentsList()` | `departmentRepo.findAll()` | Returns **every department for every company** in the database — no `WHERE company_uuid = ?` anywhere, no `isDeleted` filter either. |
| `getDepartmentById(Integer id)` | `departmentRepo.findById(id)` | Looks up by primary key only — any authenticated caller who can guess/enumerate a `departmentId` can read another company's department, since there is no ownership check against the caller's `companyId`/`companyUuid`. |
| `updateDepartment(Integer id, DepartmentDTO pojo)` | `departmentRepo.findById(id)` | Same as above — no verification that the department being updated belongs to the caller's company before applying `departmentMapper.updateFromDto(pojo, department)` and saving. |
| `deleteDepartment(Integer id)` | `departmentRepo.findById(id)` | Same as above — a caller from Company A can soft-delete Company B's department by ID. |
| `createDepartment(DepartmentDTO pojo)` | `departmentRepo.existsByCode(pojo.getCode())` | Duplicate check is global by `code` alone, not scoped to a company (see the uniqueness note above). Separately, and independently of the missing filter methods, this method also **never reads the caller's real company** — it manufactures `companyUuid` as `pojo.getName() + generatedUUID` (a fresh, effectively-random string per department), so even a hypothetical future company-scoping fix on the read/update/delete side would have no correct `companyUuid` value to filter against until this write path is also fixed. |

`DepartmentRepo` (`repo/DepartmentRepo.java`) itself only exposes `existsByCode` and
`findByCodeAndIsDeletedIsFalse` beyond the inherited `JpaRepository<Department, Integer>`
methods — there is no `findAllByCompanyUuid(...)`/`findByIdAndCompanyUuid(...)` method defined at
all, confirming the gap exists at the repository-contract level, not just because the service
forgot to call an existing scoped method.

Net effect: **Department is currently a global, cross-tenant-readable/writable resource in
practice**, despite the DB schema's composite unique constraint implying company-scoped
uniqueness was the design intent. Role and Permission share the same "no company filter" shape
architecturally, but that is arguably by design for them (roles/permissions in this codebase
appear to be intended as platform-wide/shared definitions — there is no `companyId`/`companyUuid`
column on `Role` or `Permission` at all, unlike `Department`, which does have `company_uuid` and
a matching unique constraint that implies scoping was intended but not implemented).

---

## 12. Dependencies Between Modules

- **auth-security** (`../modules/auth-security.md`) **depends on this module** for:
  - `Staff` lookup and password/OTP verification (`AuthService.loginUser`,
    `OtpService.sendOtp`/`verifyOtp`, `UserDetailsServiceImpl.loadUserByUsername`, all querying
    `StaffRepository`).
  - Role/permission data to populate JWT claims and `LoggedInUser` (`Staff.roles` →
    `Role.permissions` traversal, see §7).
  - `PermissionConstants` for every `@PreAuthorize` expression across the whole app, not just
    this module's own controllers.
  - The `SeedSuperAdmin` bootstrap process writes directly into this module's `Staff`, `Role`,
    and `Permission` tables.
- **tenant-company module** (Company entity/service — see `../modules/tenant-company.md`) — this
  module depends on it one-directionally:
  - `Staff.companyId` (plain `Integer` column, `model/Staff.java`) is a soft FK to the `Company`
    entity/table — there is no JPA `@ManyToOne` relationship object, so referential integrity is
    not enforced by JPA/Hibernate for this link.
  - `Department.companyUuid` is intended as the equivalent soft FK for departments, though as
    documented in §11 it is not currently populated with a real company identifier.
  - `LoggedInUser.companyId` (used throughout `StaffService.getAllStaff` for scoping, see §3.1)
    ultimately originates from `Staff.companyId` at login time.
- **Notification engine** (`notification/*`) — this module is an event *producer* only
  (`StaffService.saveStaff` publishes `GenericNotificationEvent` on staff creation); no
  synchronous dependency in either direction.
- **No dependency found** from this module onto Trip/Customer/Vehicle/Driver/Plan modules —
  `Staff` is referenced *from* many of those modules (e.g. `createdById`/`staffId` FK-by-convention
  columns noted in `docs/entities.md`), but this module's own code does not import or call into
  any of them.

---

## Appendix: Files read for this document

- `model/Staff.java`, `model/Role.java`, `model/Permission.java`, `model/Department.java`
- `controller/StaffController.java`, `controller/RoleController.java`,
  `controller/PermissionController.java`, `controller/DepartmentController.java`
- `service/StaffService.java`, `service/RolesService.java`, `service/impl/RolesImpl.java`,
  `service/PermissionService.java`, `service/impl/PermissionImpl.java`,
  `service/DepartmentService.java`, `service/impl/DepartmentImpl.java`
- `dto/StaffDTO.java`, `dto/RolesDTO.java`, `dto/PermissionDTO.java`, `dto/DepartmentDTO.java`
- `mapper/StaffMapper.java`, `mapper/RoleMapper.java`, `mapper/PermissionMapper.java`,
  `mapper/DepartmentMapper.java`, `mapper/base/BaseUpdateMapper.java`, `config/GlobalMapperConfig.java`
- Generated MapStruct implementations (read to verify actual field-mapping behavior, not just the
  `@Mapper` interface declarations): `build/generated/sources/annotationProcessor/java/main/com/cargosetu/mapper/StaffMapperImpl.java`,
  `RoleMapperImpl.java`, `PermissionMapperImpl.java`, `DepartmentMapperImpl.java`
- `repo/StaffRepository.java`, `repo/RolesRepo.java`, `repo/PermissionRepo.java`,
  `repo/DepartmentRepo.java`
- `utils/PermissionConstants.java`, `utils/BaseURL.java`, `utils/CommonConstants.java`,
  `utils/GenericDataDTO.java`, `utils/CommonStatusCode.java`
- `config/utils/UrlConstants.java`, `config/dto/LoggedInUser.java`, `config/dto/ApiResponseDto.java`,
  `config/JwtAuthenticationFilter.java` (grepped for role/permission authority construction —
  filter-chain internals otherwise belong to `auth-security.md`), `service/AuthService.java`
  (grepped for the `Staff.roles`/`Role.permissions` traversal — full login flow belongs to
  `auth-security.md`), `service/UserDetailsServiceImpl.java` (grepped, not read in full),
  `service/SeedSuperAdmin.java`, `service/OtpService.java` (read in full for the phone-lookup
  cross-check in §6)
- `abstracts/AbstractModel.java`
- `handler/GlobalExceptionHandler.java`, `handler/CustomValidationException.java`
- Cross-referenced: `docs/entities.md`, `docs/controllers.md`, `docs/api-urls.md`,
  `docs/services-and-known-issues.md`, `docs/modules/auth-security.md`, `docs/modules/tenant-company.md`

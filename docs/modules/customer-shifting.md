# Customer + Shifting Module

> Source root: `src/main/java/com/cargosetu/customer/` (entities, controllers, services, repository, DTOs) plus
> `com.cargosetu.repo.ShiftingRepository` and `com.cargosetu.mapper.*` (MapStruct mappers live in a shared top-level
> package, not under `customer`).

This is the core commercial domain of the ERP: **`Customer`** is the client record, and **`Shifting`** is the
"shifting order" — a single moving job/quote — that carries the route, dates, status and (via `Charges`) the pricing
breakdown that everything downstream (trips, inventory, quotation PDFs) is built on.

All claims below were verified by reading the source files directly on 2026-08-09; anything not directly observed in
code is explicitly flagged as **Unverified/Inferred**.

---

## 1. Overview

- **`Customer`** (`tblm_customers`) — a client of the moving company. Holds contact info, is scoped to a company via
  `companyId`, is soft-deleted via `isDelete`, and owns a list of `Shifting` orders.
- **`Shifting`** (`tblm_shifting`) — one moving job for a customer: from/to city and address, shifting type (a free
  string, e.g. "House Shifting"), scheduled/actual dates, completion flag, a free-text `shiftStatus`, and a 1:1
  `Charges` breakdown.
- **`Charges`** (`tblm_charges`) — the pricing breakdown for one `Shifting` (transport, packing, loading, GST, etc.)
  plus a list of ad-hoc `ExtraCharges` line items.
- Four more entities live in the same `customer.entity` package but are **not wired into `Customer` or `Shifting`
  as bidirectional relations** in this codebase today: `ShiftRoom`, `ShiftPackage`, `PackagingMaterial`,
  `ShiftItemDamage` — see §5 for the precise (surprising) wiring of each.

This module is intentionally the reference example of *correct* company-scoped (multi-tenant) querying in the
codebase — see §7.

---

## 2. API Controllers & Endpoints

Base URL: `BaseURL.BASE_URL` = `/api/v2/cargosetu` (`src/main/java/com/cargosetu/utils/BaseURL.java:4`).

### 2.1 `CustomerController` (`customer/controller/CustomerController.java`)

Mapped at `/api/v2/cargosetu/customer` (`UrlConstants.CustomerUrlConstants.CUSTOMER = "/customer"`).

| Method | Path | Handler | `@PreAuthorize` | Request | Response |
|---|---|---|---|---|---|
| POST | `/customer` | `createCustomer` | `hasAuthority(PermissionConstants.CUSTOMER_CREATE)` | `CustomerDTO` body | `GenericDataDTO` — validates via `validateCustomerCreateRequest` first; on failure returns `VALIDATION_ERROR` (422) with the validator's message |
| PUT | `/customer` | `updateCustomer` | `hasAuthority(PermissionConstants.CUSTOMER_UPDATE)` | `CustomerDTO` body | `GenericDataDTO` — validated via `validateCustomerUpdateRequest` first |
| DELETE | `/customer` | `deleteCustomer` | `hasAuthority(PermissionConstants.CUSTOMER_DELETE)` | `CustomerUpdateDto` body (uses `customerId`/`uuid`) | `GenericDataDTO` — soft delete |
| GET | `/customer/{uuid}` | `getCustomerData(uuid)` | `hasAuthority(PermissionConstants.CUSTOMER_VIEW)` | path var `uuid` | `GenericDataDTO` with a `CustomerDTO` in `data` |
| GET | `/customer` | `getCustomerData(user, pageNumber, pageSize)` | `hasAuthority(PermissionConstants.CUSTOMER_VIEW)` | query params `pageNumber`, `pageSize` (default 0/5 if null), `@AuthenticationPrincipal LoggedInUser` | `GenericDataDTO` with a `Page<CustomerDTO>` in `data` |

Note: there is no `SHIFT_DELETE`/`CUSTOMER`-style single-entity delete-by-id — delete goes through a request body
DTO (`CustomerUpdateDto`), not a path variable.

### 2.2 `ShiftingController` (`customer/controller/ShiftingController.java`)

Mapped at `/api/v2/cargosetu/shifts` (`UrlConstants.CustomerUrlConstants.SHIFTS = "/shifts"`).

| Method | Path | Handler | `@PreAuthorize` | Request | Response |
|---|---|---|---|---|---|
| POST | `/shifts` | `createShifting` | `hasAuthority(PermissionConstants.SHIFT_CREATE)` | `ShiftingDTO` body | `ResponseEntity<GenericDataDTO>` (200, or 500 on exception) |
| GET | `/shifts/{shiftId}` | `getShifting` | `hasAuthority(PermissionConstants.SHIFT_READ)` | path var `shiftId` | `ResponseEntity<GenericDataDTO>` |
| PUT | `/shifts/{shiftId}` | `updateShifting` | `hasAuthority(PermissionConstants.SHIFT_UPDATE)` | path var `shiftId` + `ShiftingDTO` body (`shiftId` overwrites `shiftingDTO.shiftingId`) | `ResponseEntity<GenericDataDTO>` |
| GET | `/shifts` | `getAllShiftings` | `hasAuthority(PermissionConstants.SHIFT_READ)` | query params `pageNumber` (default 0), `pageSize` (default 10), `@AuthenticationPrincipal LoggedInUser` | `ResponseEntity<GenericDataDTO>` with `Page<ShiftingDTO>` |
| GET | `/shifts/shifting-types` | `getShiftingTypes` | `hasAuthority(PermissionConstants.CUSTOMER_VIEW)` **(not `SHIFT_READ`)** | none | `GenericDataDTO` with a hardcoded `List<String>` of 13 shifting-type labels |

**Observations verified against `PermissionConstants.java` (`utils/PermissionConstants.java:46-55`):**
- `CUSTOMER_CREATE`, `CUSTOMER_DELETE`, `CUSTOMER_UPDATE`, `CUSTOMER_VIEW` exist for customers.
- Only `SHIFT_CREATE`, `SHIFT_READ`, `SHIFT_UPDATE` exist for shiftings — **there is no `SHIFT_DELETE` permission constant**, and correspondingly `ShiftingController` has no delete endpoint at all.
- `/shifts/shifting-types` is gated on `CUSTOMER_VIEW` rather than a shift permission — likely intentional (it's a static reference list used while building a customer/shifting form) but flagged here as an inconsistency worth confirming with the team.
- The controller's exception handling is inconsistent between the two controllers: `CustomerController` has no try/catch (relies on a global exception handler, per `docs/services-and-known-issues.md` conventions — not independently verified in this pass), while `ShiftingController` wraps every handler body in try/catch and returns `ResponseEntity.internalServerError().body(response)` on failure with `response.setErrors(new Object[]{e.getMessage()})` — i.e. it leaks the raw exception message to the client.

---

## 3. Business / Service Logic

### 3.1 `CustomerService` (`customer/service/CustomerService.java`)

**`validateCustomerCreateRequest` / `validateCustomerUpdateRequest`**: minimal — null-check the DTO and require
`firstName`. Update validation additionally checks `customerRepository.countCustomersByCustomerId(customerId) > 0`
when an id is supplied. There is **no duplicate-customer check** (e.g. by mobile number) anywhere in create/update
validation — creating two customers with the same `mobileNo` is not prevented at the service layer.

**`createCustomer(CustomerDTO)`** (`@Transactional`), the flow, exactly as written (lines 66-110):
1. Maps `CustomerDTO` → `Customer` via `customerMapper.toEntity(customerDTO)` (MapStruct, cascades into nested
   `Shifting`/`Charges`/`ExtraCharges` DTOs because `CustomerMapper` `uses = ShiftingMapper.class`, which in turn
   `uses = {ChargesMapper.class, CustomerMapper.class}`).
2. Generates a UUID via `UUIDGenerator.generate()` (format: `"SCSETU"` + 9 random chars from
   `A-Z0-9`, `utils/UUIDGenerator.java`) and sets it on the customer.
3. Forces `customer.setIsDelete(false)`.
4. Calls the private helper `linkCustomerToShifting(customer.getShiftings(), customer)`, which, for every nested
   `Shifting` produced by the mapper, sets `shifting.setCompanyId(customer.getCompanyId())` and
   `shifting.setCustomer(customer)` — this is how a customer created with an embedded `shiftings` array gets its
   child shiftings persisted in the same call (cascade `ALL` from `Customer.shiftings`, see §5).
5. Saves via `customerRepository.save(customer)`.
6. If `customer.getCustomerId()` is non-null after save, builds a `Map<String,Object> variables` containing
   `customerId`, `email` (or `""`), `name` (`firstName + " " + lastName`, with lastName defaulted to `""`), and
   `mobileNo` (or `""`).
7. **Event publication** (via the injected `ApplicationEventPublisher eventPublisher`):
   - If `customer.getEmail()` is non-blank: publishes
     `new GenericNotificationEvent("CUSTOMER_CREATED", List.of(customer.getEmail()), NotificationChannel.EMAIL, variables)`.
   - If `customer.getMobileNo()` is non-blank: publishes a **second**, independent
     `GenericNotificationEvent("CUSTOMER_CREATED", List.of(customer.getMobileNo()), NotificationChannel.SMS, variables)`.
   - Both are separate events (not one event with two channels) — each carries a single-element `recipients` list.
   - If neither email nor mobile is present, **no event is published** and no error is raised.
8. On success, returns `GenericDataDTO` with `responseCode = CommonStatusCode.CREATED` (201) and `data = customer`
   (the raw JPA entity, not a DTO — see §4 note). On failure (`customerId` still null after save), returns `FAIL`
   (400) with message `"customer not created!"`.

**`GenericNotificationEvent`** (`notification/event/GenericNotificationEvent.java`) is a `record` implementing
`NotificationEvent`, auto-stamped with a random `UUID eventId` and `Instant.now()` timestamp via its 4-arg
convenience constructor. It is consumed by `GenericNotificationEventListener`
(`notification/listener/GenericNotificationEventListener.java`), which is annotated
`@Async("notificationTaskExecutor")` and `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT,
fallbackExecution = true)` — meaning the listener runs **after the customer-creation transaction commits**, on a
separate thread pool (`notificationTaskExecutor`), and `fallbackExecution = true` means it still fires even if there
is no active transaction. The listener delegates to `NotificationProcessingService.processEvent(event)`. **Cross-
reference: `../modules/notification-engine.md`** for how `templateCode` ("CUSTOMER_CREATED"), `NotificationChannel`,
and `variables` are resolved into an actual outbound email/SMS.

**`updateCustomer(CustomerDTO)`** (not `@Transactional` — no annotation on this method, unlike `createCustomer`):
1. Looks up the existing customer via `customerRepository.findByCustomerIdAndIsDelete(customerId, false)` (i.e. a
   soft-deleted customer cannot be "found" for update).
2. Field-by-field null-safe copy for `firstName`, `lastName`, `email`, `mobileNo`, `alternateMobileNo`, `workEmail`
   (only overwrites if the incoming DTO field is non-null — partial update semantics).
3. If `customerDTO.getShiftings()` is non-empty, **maps every DTO in the list to a new `Shifting` entity and calls
   `shiftingRepository.saveAll(list)` directly**, then `customer.setShiftings(list)`. This bypasses
   `linkCustomerToShifting` — the mapped `Shifting` entities get their `customer`/`companyId` from
   `ShiftingMapper`'s `@Mapping(target = "customer.customerId", source = "customerId")` (i.e. the DTO must carry a
   `customerId`), not from the `Customer` object being updated. **This is a plausible correctness gap**: if a
   `ShiftingDTO` in the update payload omits `customerId`, the saved `Shifting.customer` will be null/unset before
   `customer.setShiftings(list)` re-links it in memory only (the `saveAll` already ran).
4. Saves the customer, maps to `CustomerDTO` for the response.
5. Publishes **one** `GenericNotificationEvent("CUSTOMER_UPDATED", …, NotificationChannel.EMAIL, variables)` — only
   if email is present. **Unlike create, there is no SMS event on update.**
6. Wrapped in try/catch; any exception returns `INTERNAL_SERVER_ERROR` (500) with the exception message in `errors`.

**`deleteCustomer(CustomerUpdateDto)`**: looks up by `findByUuidAndIsDelete(uuid, false)`; if found, sets
`isDelete = true` and saves (soft delete only — no cascade soft-delete of child `Shifting`/`Charges` rows, which
remain `isDelete = false`-equivalent, i.e. shiftings of a deleted customer are still fully live rows with an
unreachable-via-API parent).

**`getCustomer(uuid)`**: `findByUuidAndIsDelete(uuid, false)` → maps to `CustomerDTO`. If not found, still returns
`responseCode = SUCCESS` (200) with message `"Customer not found!"` — **a 200 response body signaling "not found"
instead of 404**, which callers must check by message string, not status code.

**`getAllCustomer(LoggedInUser, pageNumber, pageSize)`**: see §7 (Security) for the full branching logic — this is
the method the task calls out as the tenant-scoping reference implementation.

### 3.2 `ShiftingService` (`customer/service/ShiftingService.java`)

- **`createShifting`** (`@Transactional`): requires `shiftingDTO.getCustomerId()`; 422 if missing. Looks up the
  `Customer` by id (`customerRepository.findById`, **not** filtered by `isDelete` — a shifting can be created
  against a soft-deleted customer). Maps DTO → entity, explicitly re-sets `shifting.setCustomer(...)` (redundant
  with the mapper's `@Mapping(target = "customer.customerId", ...)`, but harmless since it uses the fully-loaded
  entity instead of a stub with only an id). Saves via `shiftingRepository.save`.
- **`getShifting(shiftId)`**: plain `shiftingRepository.findById`.
- **`updateShifting`**: requires `shiftingId`; if `customerId` changed, re-resolves and re-links the `Customer`.
  Otherwise manually copies every scalar field (`isShiftCompleted`, cities, addresses, type, dates, `shiftCompletedBy`
  /`ById`) plus `charge` from a freshly-mapped `Shifting` onto the persisted one, then saves. **`shiftStatus` and
  `shiftActualReachedDate` are declared on the entity but are conspicuously absent from this manual copy list** —
  they can never be changed through this update path.
- **`getAllShiftings(user, pageNumber, pageSize)`**: branches on `Objects.equals(user.getStaffId(), 1)` — see §7,
  this is the **buggy counterpart** to `CustomerService.getAllCustomer`'s correct branch.

---

## 4. DTOs / Request-Response Models

| DTO | File | Backing Entity | Notes |
|---|---|---|---|
| `CustomerDTO` | `customer/dtos/CustomerDTO.java` | `Customer` | `@JsonIgnoreProperties(ignoreUnknown = true)`. **Oddity, confirmed by reading the file**: alongside `customerId, firstName, lastName, mobileNo, alternateMobileNo, email, workEmail, companyId, uuid, isDelete` and a nested `List<ShiftingDTO> shiftings`, it *also* independently declares flattened shift-shaped fields — `isShiftCompleted, shiftFromCity, shiftToCity, toAddress, fromAddress, shiftingType, shiftingStartDate, shiftReachedDate` — that have **no corresponding mapping to any `Customer` entity field** (MapStruct's `NullValuePropertyMappingStrategy.IGNORE` config plus lack of a matching entity property means these are effectively dead/unused inputs unless a caller relies on them, which no service code observed does). Treat these as legacy/unused unless proven otherwise. |
| `CustomerUpdateDto` | `customer/dtos/CustomerUpdateDto.java` | `Customer` (partial) | Used only for `DELETE /customer` in this codebase; despite the name, it carries `customerId`, `uuid`, and full contact fields but the delete flow only reads `customerId` (for a null-check) and `uuid` (for the actual lookup). |
| `ChargesDTO` | `customer/dtos/ChargesDTO.java` | `Charges` | Mirrors all charge fields 1:1; `extraChargesList` maps to `Charges.extraCharges` via `@Mapping` in `ChargesMapper`. |
| `ExtraChargesDTO` | `customer/dtos/ExtraChargesDTO.java` | `ExtraCharges` | 1:1 field mirror (`extraChargesId, chargeName, chargeValue, chargeRate`). |
| `ShiftingDTO` | `customer/dtos/ShiftingDTO.java` | `Shifting` | `customerId` (Long) and `companyId` (Integer) are flattened FKs; nested pricing is under `@JsonProperty("charges") ChargesDTO chargesDTO`, mapped to `Shifting.charge` via `ShiftingMapper`'s explicit `@Mapping(target = "charge", source = "chargesDTO")`. |

**Mapper wiring** (all under `com.cargosetu.mapper`, MapStruct, `@Mapper(config = GlobalMapperConfig.class, ...)`
with `GlobalMapperConfig` = `@MapperConfig(componentModel = "spring", nullValuePropertyMappingStrategy =
NullValuePropertyMappingStrategy.IGNORE)`, `config/GlobalMapperConfig.java:6`):

- `CustomerMapper` (`mapper/CustomerMapper.java`) — `uses = ShiftingMapper.class`.
- `ShiftingMapper` (`mapper/ShiftingMapper.java`) — `uses = {ChargesMapper.class, CustomerMapper.class}` (mutual
  `uses` between `CustomerMapper` and `ShiftingMapper`; MapStruct resolves this at generation time without
  infinite recursion because it generates separate mapping methods per direction/type). Explicit overrides:
  `toEntity`: `@Mapping(target = "customer.customerId", source = "customerId")`, `@Mapping(target = "charge", source = "chargesDTO")`.
  `toDto`: the inverse mappings.
- `ChargesMapper` (`mapper/ChargesMapper.java`) — `uses = {ExtraChargesMapper.class}`; explicit `extraCharges` ↔
  `extraChargesList` mapping.
- `ExtraChargesMapper` (`mapper/ExtraChargesMapper.java`) — plain 1:1, no custom mappings.
- All four extend `BaseUpdateMapper<D,E>` (`mapper/base/BaseUpdateMapper.java`), which declares `toEntity`, `toDto`,
  and `void updateFromDto(D dto, @MappingTarget E entity)` — **note `updateFromDto` is declared but not called
  anywhere in `CustomerService`/`ShiftingService`**; both services do manual field-by-field copies instead (see §3),
  so this in-place-update mapper method appears to be unused/dead in the current update flows.

`GenericDataDTO` (`utils/GenericDataDTO.java`) is the shared response envelope for both controllers:
`responseMessege` (sic — this exact spelling is used throughout), `responseCode` (`Integer`, from
`CommonStatusCode`), `dataList`, `data` (`Object` — untyped, holds either a raw entity, a DTO, or a `Page<DTO>`
depending on the call site), `errors` (`Object[]`), plus pagination counters (`totalRecords`, `pageRecords`,
`currentPageNumber`, `totalPages`) that are **declared but never populated** by any code path in `CustomerService`/
`ShiftingService` observed — callers must derive paging info from the `Page` object nested inside `data` instead.

---

## 5. Entities & Database Relationships

| Entity | Table | Notes |
|---|---|---|
| `Customer` | `tblm_customers` | PK `customer_id` (IDENTITY). |
| `Shifting` | `tblm_shifting` | PK `shifting_id` (IDENTITY). |
| `Charges` | `tblm_charges` | PK `charge_id` (IDENTITY, `Integer`). |
| `ExtraCharges` | `tblm_extra_charges` | PK `extra_charges_id` (IDENTITY, `Integer`). |
| `ShiftRoom` | `tblt_shift_room` | PK `roomId` (IDENTITY, `Long`, unmapped `@Column` name). |
| `ShiftPackage` | `tblt_shift_package` | PK `packageId` (IDENTITY, `Long`). |
| `PackagingMaterial` | `tblm_packaging_material` | PK `materialId` (IDENTITY, `Long`). |
| `ShiftItemDamage` | `tblt_shift_item_damage` | PK `damageId` (IDENTITY, `Long`). |

Relationship graph, verified field-by-field:

- **`Customer` 1:N `Shifting`** — `Customer.shiftings` is `@OneToMany(mappedBy = "customer", cascade =
  CascadeType.ALL, fetch = FetchType.LAZY)` (`customer/entity/Customer.java:56`); the owning side is
  `Shifting.customer`, `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id")`
  (`customer/entity/Shifting.java:53-55`). Cascade `ALL` from `Customer` means saving/deleting a `Customer` cascades
  to its `Shifting`s at the JPA level — but note `deleteCustomer` never actually calls `repository.delete()` (it's a
  soft delete via a field flag), so this cascade is effectively only exercised on the create path (§3.1 step 4/5).
- **`Shifting` 1:1 `Charges`**, cascade ALL, owned by `Shifting` — `Shifting.charge` is
  `@OneToOne(cascade = CascadeType.ALL) @JoinColumn(name = "charge_id", referencedColumnName = "charge_id")`
  (`customer/entity/Shifting.java:57-59`); the inverse, non-owning side is `Charges.shifting`,
  `@OneToOne(mappedBy = "charge")` (`customer/entity/Charges.java:64-65`). Saving a `Shifting` cascades to persist/
  update its `Charges` in the same transaction.
- **`Charges` 1:N `ExtraCharges`**, cascade ALL + `orphanRemoval = true`, unidirectional — `Charges.extraCharges` is
  `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)`
  (`customer/entity/Charges.java:61-62`) with **no `mappedBy`/`@JoinColumn` specified**, meaning Hibernate manages
  this via an implicit join table (default naming, e.g. `charges_extra_charges`) rather than a FK column on
  `tblm_extra_charges` itself — `ExtraCharges` has no back-reference to `Charges` at all. `orphanRemoval = true`
  means removing an `ExtraCharges` item from the in-memory list and saving the parent `Charges` deletes that row.
  **Unverified**: the exact join-table name/columns were not confirmed against a schema/migration file in this pass.
- **`ShiftRoom` → `Shifting`**: unidirectional `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name =
  "shifting_id")` (`customer/entity/ShiftRoom.java:15-17`). `Shifting` has **no** `@OneToMany` back-reference to
  `ShiftRoom` — you can only navigate `ShiftRoom → Shifting`, not the reverse, in JPA object terms (a repository
  query would be needed to go the other way). Also has no `@Table`-column mapping on `roomId`/`roomName` (no
  explicit `@Column`, relies on default snake_case-from-camelCase naming strategy — **inferred**, not confirmed
  against a naming-strategy config file).
- **`ShiftPackage` → `Shifting`**: same shape, unidirectional `@ManyToOne @JoinColumn(name = "shifting_id")`
  (`customer/entity/ShiftPackage.java:25-27`), no back-reference from `Shifting`.
- **`PackagingMaterial`**: **no relationship to `Shifting` or `Customer` at all** — it is a standalone master/lookup
  table (`materialName`, `unit`, `active` flag) with no FK to anything in this module. **Confirmed by reading the
  full file** (`customer/entity/PackagingMaterial.java`) — any linkage to a shifting job (e.g. "packaging materials
  used on this job") must happen through a different entity not covered here (candidate:
  `shifting.entity.ShiftInventoryPackaging`, in the sibling `shifting-inventory` module — **not verified in this
  pass**, flagging as a cross-module lead only).
- **`ShiftItemDamage` → `ShiftInventoryItem`, not → `Shifting` directly**: `@ManyToOne @JoinColumn(name =
  "item_id")` points at `com.cargosetu.shifting.entity.ShiftInventoryItem` (`customer/entity/ShiftItemDamage.java:19-21`),
  which itself has a `@ManyToOne @JoinColumn(name = "shift_id", nullable = false)` to `Shifting`
  (`shifting/entity/ShiftInventoryItem.java:19-21`). So `Shifting` is reachable from `ShiftItemDamage` only
  transitively, through `ShiftInventoryItem` — this entity is effectively part of the `shifting-inventory` module's
  domain even though the `.java` file physically sits in `customer.entity`.

Summary chain: `Customer` (1) → (N) `Shifting` (1) → (1) `Charges` (1) → (N) `ExtraCharges`; separately,
`ShiftRoom`/`ShiftPackage` each point *up* at a `Shifting` unidirectionally; `PackagingMaterial` is disconnected;
`ShiftItemDamage` connects only via the external `ShiftInventoryItem` entity.

---

## 6. Repositories

### `CustomerRepository` (`customer/repository/CustomerRepository.java`, extends `JpaRepository<Customer, Long>`)

| Method | Signature | Used by |
|---|---|---|
| `countCustomersByCustomerId` | `int countCustomersByCustomerId(Long customerId)` | `CustomerService.validateCustomerUpdateRequest` — existence check |
| `findByUuidAndIsDelete` | `Optional<Customer> findByUuidAndIsDelete(String uuid, boolean isDelete)` | `getCustomer`, `deleteCustomer` |
| `findByCustomerIdAndIsDelete` | `Optional<Customer> findByCustomerIdAndIsDelete(Long customerId, boolean isDelete)` | `updateCustomer` |
| `findByIsDelete` | `Page<Customer> findByIsDelete(boolean isDelete, Pageable pageable)` | **declared but not called** from either service observed — appears unused/superseded by the `OrderByCustomerIdDesc` variant below |
| `findByIsDeleteOrderByCustomerIdDesc` | `Page<Customer> findByIsDeleteOrderByCustomerIdDesc(Boolean isDelete, Pageable pageable)` | `getAllCustomer`, super-admin branch (`staffId == 1`) |
| `findByCompanyIdAndIsDeleteOrderByCustomerIdDesc` | `Page<Customer> findByCompanyIdAndIsDeleteOrderByCustomerIdDesc(Integer customerId, Boolean isDelete, Pageable pageable)` | `getAllCustomer`, normal-tenant branch — **this is the pattern flagged elsewhere in the repo's own docs (`docs/services-and-known-issues.md`) as the one correct example of company-scoped querying** |
| `findByMobileNoAndIsDelete` | `Optional<Customer> findByMobileNoAndIsDelete(String mobileNo, boolean isDelete)` | **Not called from `CustomerService`/`ShiftingService`.** Used by `service/OtpService.java` (lines 82, 218) for phone-number-based customer lookup during OTP login/verification — i.e. this repository method is shared cross-module with the auth/OTP flow, not just this module's own controllers. |

### `ShiftingRepository` (`com.cargosetu.repo.ShiftingRepository`, extends `JpaRepository<Shifting, Long>` — note the package is `com.cargosetu.repo`, **not** `com.cargosetu.customer.repository`)

| Method | Signature | Used by |
|---|---|---|
| `findAllByOrderByShiftingIdDesc` | `Page<Shifting> findAllByOrderByShiftingIdDesc(Pageable pageable)` | `getAllShiftings`, intended super-admin branch |
| `findAllByCompanyIdOrderByShiftingIdDesc` | `Page<Shifting> findAllByCompanyIdOrderByShiftingIdDesc(int company, Pageable pageable)` | `getAllShiftings`, normal-tenant branch |
| `findShifting` | native `@Query("SELECT ts.* FROM tblm_shifting ts WHERE ts.shifting_id = :shiftingId")` → `Optional<Shifting>` | Not called from `ShiftingService`/`CustomerService` observed — likely used elsewhere (e.g. `shifting-inventory` module) or dead code; **not independently verified**. |
| `findShiftingWithCustomerAndCharges` | JPQL `SELECT s FROM Shifting s JOIN FETCH s.customer JOIN FETCH s.charge WHERE s.shiftingId = :shiftingId` | `service/TemplateService.java` (lines 61, 246) — the quotation-PDF rendering path, eager-fetches customer + charges in one query to avoid N+1/lazy-init issues when building the PDF. |

---

## 7. Security & Authentication — the tenant-scoping reference pattern

`@PreAuthorize` gates every endpoint (see §2 tables) using `PermissionConstants` string constants resolved via
Spring EL's `T(...)` static-field syntax, e.g. `hasAuthority(T(com.cargosetu.utils.PermissionConstants).CUSTOMER_VIEW)`.
Permission values are prefixed `PERM_` (`PermissionConstants.PERM_PREFIX`) and granted to a `LoggedInUser`
(`config/dto/LoggedInUser.java`, implements Spring Security `UserDetails`) via its `permissions: Set<String>` field,
converted to `SimpleGrantedAuthority` in `getAuthorities()`.

**`CustomerService.getAllCustomer` — the correct pattern** (`customer/service/CustomerService.java:222-240`), quoted
verbatim:

```java
if(user.getStaffId() == 1){
     customers = customerRepository.findByIsDeleteOrderByCustomerIdDesc
            (false, pageable);
}else{
    if(user.getCompanyId() == null){
        response.setResponseCode(CommonStatusCode.NOT_FOUND);
        response.setResponseMessege("Please Create your Company");
        return response;
    }
    customers = customerRepository.findByCompanyIdAndIsDeleteOrderByCustomerIdDesc
            (user.getCompanyId(),false, pageable);
}
```

Reading this precisely: `user.getStaffId()` returns a `Long`; `== 1` against the `int` literal `1` triggers
auto-unboxing of the `Long` to a primitive `long`, which is then widened-compared to `1` — this works correctly for
identifying "staff id 1" as a platform super-admin/bypass account. When true, the query fetches **all** non-deleted
customers with no company filter at all — i.e. staff id 1 is a hardcoded global-access account, not something
configured through `roles`/`permissions`. For every other user, the code **requires** `user.getCompanyId()` to be
non-null (returning a `NOT_FOUND` "Please Create your Company" response otherwise) and then scopes the query with
`findByCompanyIdAndIsDeleteOrderByCustomerIdDesc(user.getCompanyId(), false, pageable)` — every customer row
returned is guaranteed to belong to the caller's own company.

**`ShiftingService.getAllShiftings` — the same pattern, but broken** (`customer/service/ShiftingService.java:156-165`):

```java
if(Objects.equals(user.getStaffId(),1)){
    shiftings = shiftingRepository.findAllByOrderByShiftingIdDesc(pageable);
}else {
    shiftings = shiftingRepository.findAllByCompanyIdOrderByShiftingIdDesc(user.getCompanyId(),pageable);
}
```

This looks like the same super-admin bypass, but `user.getStaffId()` is a **`Long`** while the literal `1` is
autoboxed to **`Integer`**. `Objects.equals(Long, Integer)` calls `Long.equals(Object)`, which returns `false`
whenever the argument is not itself a `Long` — **`Objects.equals(user.getStaffId(), 1)` is always `false`,
regardless of the actual staff id value.** The practical effect: the intended super-admin branch in
`ShiftingService.getAllShiftings` is dead code; every caller, including staff id 1, always falls into the
company-scoped `else` branch. This is functionally "safer" (nobody accidentally gets unscoped access) but it means
the super-admin "view all shiftings" feature described by the code's own structure does not work, and — separately —
this branch performs **no null-check on `user.getCompanyId()`** before calling
`findAllByCompanyIdOrderByShiftingIdDesc(user.getCompanyId(), ...)`, which unboxes a `null` `Integer` to `int` for
the repository method's `int company` parameter and will throw an `NPE` for any user without a company, unlike
`CustomerService`'s explicit guard. **This is flagged here as a functional bug worth a ticket**, contrasted directly
against the correct `CustomerService` implementation it was evidently modeled on.

No method-level or row-level ownership check beyond company scoping was found (e.g. no check that a `Shifting`
being fetched by id in `getShifting`/`updateShifting` belongs to the caller's company — both accept any `shiftId`
without a company filter, relying solely on the coarse `@PreAuthorize` permission check).

---

## 8. Exception Handling

- `CustomerController`: no controller-level try/catch. `createCustomer`/`updateCustomer` pre-validate and return a
  `VALIDATION_ERROR` (422) `GenericDataDTO` on invalid input; `getCustomerData` methods pattern-match on
  `responseCode == null` to backfill a `FAIL`/`400` default. Any unhandled exception inside `CustomerService`
  (outside the explicit try/catch in `updateCustomer` and `getAllCustomer`) would propagate to whatever global
  exception handling exists app-wide — **not verified in this pass** (see `docs/services-and-known-issues.md` for
  any documented global handler).
- `CustomerService.updateCustomer` and `getAllCustomer` each wrap their bodies in try/catch, converting
  `DataAccessException` → `FAIL` (400, `getAllCustomer` only) and any other `Exception` → `INTERNAL_SERVER_ERROR`
  (500), logging via `log.error`.
- `ShiftingController`: every handler wraps its service call in try/catch and, on exception, returns
  `ResponseEntity.internalServerError().body(response)` with `response.setErrors(new Object[]{e.getMessage()})` —
  i.e. the controller itself (not just the service) does redundant exception handling on top of
  `ShiftingService`'s own internal try/catch (all four `ShiftingService` methods already catch `Exception` and
  return an `INTERNAL_SERVER_ERROR` `GenericDataDTO` instead of throwing), so the controller's catch blocks are
  effectively unreachable dead code for exceptions thrown inside the service layer — they'd only fire for
  exceptions thrown before/after the service call (e.g. `shiftingDTO.setShiftingId(shiftId)` on a null DTO).

---

## 9. Configuration

- No module-specific `@ConfigurationProperties`/`application.yml` block was found for `customer`/`shifting`.
- MapStruct code generation is governed globally by `GlobalMapperConfig`
  (`config/GlobalMapperConfig.java:6`): `componentModel = "spring"` (mappers are Spring beans, hence `@Autowired`-
  injectable into the services) and `nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE`
  (a null field on the source DTO does not overwrite a target field during mapping — relevant to why
  `CustomerDTO`'s unused flattened shift-fields, see §4, don't clobber anything even if MapStruct were to attempt a
  mapping for them).
- Async notification dispatch depends on a `notificationTaskExecutor` bean (referenced by
  `GenericNotificationEventListener`'s `@Async("notificationTaskExecutor")`) — its thread-pool sizing/config was
  **not located/verified in this pass**; see `../modules/notification-engine.md` for that module's own config
  documentation.

---

## 10. External Integrations

None directly. The one integration point is internal: `CustomerService.createCustomer`/`updateCustomer` publish
`GenericNotificationEvent`s that are picked up asynchronously, after commit, by the notification-engine module's
`GenericNotificationEventListener` → `NotificationProcessingService`, which is responsible for actually resolving a
template by `templateCode` ("CUSTOMER_CREATED"/"CUSTOMER_UPDATED") and channel (`EMAIL`/`SMS`) and dispatching via
whatever provider that module wires up (e.g. SMTP/SMS gateway — **see `../modules/notification-engine.md`, not
independently re-verified here**). This module has no outbound HTTP clients, payment gateways, or third-party SDKs
of its own.

---

## 11. Important Business Rules & Validations

- **Soft delete via `isDelete`** (`Customer.isDelete`, default `false`): enforced consistently — every customer
  lookup used by the controllers (`findByUuidAndIsDelete`, `findByCustomerIdAndIsDelete`, the two paginated "all
  customers" queries) filters `isDelete = false`. `deleteCustomer` only ever sets the flag; there is no hard-delete
  path and no cascading soft-delete to child `Shifting`/`Charges` rows (confirmed — `deleteCustomer` never touches
  `customer.getShiftings()`).
- **`Shifting` has no `isDelete`/soft-delete field at all** (confirmed by reading the full entity) — shiftings are
  either hard-deleted (never observed in this module's service code) or effectively permanent once created.
- **No duplicate-customer prevention**: neither `validateCustomerCreateRequest` nor `createCustomer` checks
  `mobileNo`/`email` uniqueness before insert; `findByMobileNoAndIsDelete` exists on the repository but is not
  invoked from this module's own create flow (only from `OtpService` for login lookup).
- **`shiftingType` is a free-form `String`, but a fixed reference list of 13 values is exposed for UI dropdowns**
  via `GET /shifts/shifting-types` (hardcoded in the controller, not persisted anywhere): "House Shifting", "Office
  Shifting", "Factory Shifting", "Mall Shifting", "Shop Shifting", "Warehouse Shifting", "Goods Shifting", "Govt.
  Related Shifting", "Medical Item Shifting", "Electronic Item Shift", "Liquid Items Shift", "Industrial Items
  Shift", "Others". Nothing in the entity or service layer validates that a submitted `shiftingType` is one of
  these — it's advisory/UI-only.
- **`shiftStatus` is confirmed to be a plain, unconstrained `String` column** (`Shifting.shiftStatus`,
  `customer/entity/Shifting.java:61-62`) — there is **no enum, no constants class, and no code anywhere in
  `src/main/java` that reads or writes `shiftStatus`/`getShiftStatus`/`setShiftStatus` outside of its own field
  declaration** (verified via a repo-wide grep). It is not touched by `ShiftingService.createShifting`, is not in
  the manual field-copy list in `updateShifting`, and is not referenced by `TemplateService`'s quotation rendering.
  **In the current codebase this field is effectively dead/unused** — any "shift status" values a caller sets via
  a raw entity or a future feature would need to be defined and enforced from scratch; treat any assumed status
  vocabulary (e.g. "PENDING"/"CONFIRMED"/"IN_TRANSIT"/"COMPLETED") as **unverified speculation**, not fact.
- **`isShiftCompleted` (boolean, not `Boolean`) is the only status-like field actually driven by application
  fields on `Shifting`** — `shiftCompletedBy`/`shiftCompletedById`/`shiftReachedDate`/`shiftActualReachedDate`
  exist alongside it but, per §3.2, `shiftActualReachedDate` is excluded from `updateShifting`'s manual copy, so in
  practice only `shiftReachedDate` (mapped as `shiftReachedDate` DTO field → `shiftReachedDate` entity field) is
  updatable through the API today.
- **Company scoping (`companyId`) is `Integer` on both `Customer` and `Shifting`**, consistent with the wider
  codebase's `Integer companyId` convention (contrast with `Department`'s `String companyUuid`, per
  `docs/services-and-known-issues.md`) — but is enforced correctly only in `CustomerService.getAllCustomer` (§7);
  `ShiftingService.getAllShiftings` has the `Objects.equals` bug described above, and neither `getShifting` nor
  `updateShifting` company-scope their `findById` lookups at all.

---

## 12. Dependencies Between Modules

**Depends on:**
- **tenant/company module** — for `companyId` values stamped onto `Customer`/`Shifting` and used for scoping
  queries (`user.getCompanyId()` sourced from `LoggedInUser`, which is populated at login by the auth/security
  layer — the tenant-company module itself was not read in this pass).
- **auth-security module** — `@AuthenticationPrincipal LoggedInUser` (`config/dto/LoggedInUser.java`) supplies
  `staffId`, `companyId`, and the `permissions` set that drives every `@PreAuthorize` check in this module's two
  controllers.
- **notification-engine module** — consumes the `GenericNotificationEvent`s published from `CustomerService`
  (see §3.1, §10). Cross-reference: `../modules/notification-engine.md`.

**Depended on by:**
- **fleet-trip module** — `Trip` (`model/Trip.java:20-21`) holds `@ManyToOne private Shifting shifting;`, i.e. a
  trip is always for one shifting order; trip planning/dispatch reads route/customer context transitively through
  this FK.
- **template-quotation module** — `TemplateService` (`service/TemplateService.java`) calls
  `shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)` (lines 61, 246) to eager-load `Shifting` +
  `Customer` + `Charges` in one query, then builds a `QuotationDTO`/renders a Thymeleaf template and a PDF from
  those three objects (`renderHtml(..., Shifting shifting, Customer customer, Charges charges, ...)`, lines 103/124,
  and `getShiftingMap(Shifting shifting)`, line 167). This is the module whose pricing data (`Charges`/
  `ExtraCharges`) ultimately appears on customer-facing quotation PDFs.
- **shifting-inventory module** — `ShiftInventoryItem` (`shifting/entity/ShiftInventoryItem.java:19-21`) holds
  `@ManyToOne @JoinColumn(name = "shift_id", nullable = false) private Shifting shifting;` (a **mandatory** FK,
  unlike `Trip`'s), and `ShiftInventoryService` (`shifting/service/ShiftInventoryService.java`) imports `Shifting`
  directly — per-item packed/loaded/unloaded inventory tracking is anchored to a `Shifting` row.
- **document-upload module** — `DocumentUpload` (`document/model/DocumentUpload.java:53-55`) holds an optional
  (`nullable = true`) `@ManyToOne @JoinColumn(name = "customer_id") private Customer customer;`, and `Customer`
  itself declares the inverse `@OneToMany(mappedBy = "customer") private List<DocumentUpload> documentUploads`
  (`customer/entity/Customer.java:60-61`, **no cascade specified** — deleting a `Customer` at the JPA level would
  not cascade to its documents). This is a genuine bidirectional relationship, distinct from the `Shifting`-side
  dependents above.
- **OTP/auth login flow** — `OtpService` (`service/OtpService.java`) calls
  `customerRepository.findByMobileNoAndIsDelete(searchNumber, false)` (lines 82, 218) to resolve a customer by
  phone for OTP-based login, and on success does `loggedInUser.setStaffId(customer.getCustomerId())` (line 230) —
  i.e. **a customer's login session reuses the `staffId` field to carry the customer's own id**, an overload worth
  knowing about since it means `staffId == 1` (the super-admin bypass in §7) would collide with "customer id 1"
  if a customer-login `LoggedInUser` were ever passed into `CustomerService.getAllCustomer`/
  `ShiftingService.getAllShiftings` (**not observed to actually happen** in the controllers reviewed, both of which
  are staff-facing endpoints gated by staff-style permissions — flagged as a latent risk, not a confirmed bug).

---

## Appendix: File Index

| File | Role |
|---|---|
| `src/main/java/com/cargosetu/customer/entity/Customer.java` | Customer entity |
| `src/main/java/com/cargosetu/customer/entity/Shifting.java` | Shifting order entity |
| `src/main/java/com/cargosetu/customer/entity/Charges.java` | Pricing breakdown entity |
| `src/main/java/com/cargosetu/customer/entity/ExtraCharges.java` | Ad-hoc extra charge line item |
| `src/main/java/com/cargosetu/customer/entity/ShiftRoom.java` | Per-room breakdown, unidirectional → `Shifting` |
| `src/main/java/com/cargosetu/customer/entity/ShiftPackage.java` | Per-package tracking, unidirectional → `Shifting` |
| `src/main/java/com/cargosetu/customer/entity/PackagingMaterial.java` | Standalone materials catalog, no FK |
| `src/main/java/com/cargosetu/customer/entity/ShiftItemDamage.java` | Damage record, → `ShiftInventoryItem` (external module) |
| `src/main/java/com/cargosetu/customer/controller/CustomerController.java` | Customer REST endpoints |
| `src/main/java/com/cargosetu/customer/controller/ShiftingController.java` | Shifting REST endpoints |
| `src/main/java/com/cargosetu/customer/service/CustomerService.java` | Customer business logic + events |
| `src/main/java/com/cargosetu/customer/service/ShiftingService.java` | Shifting business logic |
| `src/main/java/com/cargosetu/customer/repository/CustomerRepository.java` | Customer JPA repository |
| `src/main/java/com/cargosetu/repo/ShiftingRepository.java` | Shifting JPA repository (note: different package) |
| `src/main/java/com/cargosetu/customer/dtos/CustomerDTO.java` | Customer request/response DTO |
| `src/main/java/com/cargosetu/customer/dtos/CustomerUpdateDto.java` | Customer delete DTO |
| `src/main/java/com/cargosetu/customer/dtos/ChargesDTO.java` | Charges DTO |
| `src/main/java/com/cargosetu/customer/dtos/ExtraChargesDTO.java` | Extra charges DTO |
| `src/main/java/com/cargosetu/customer/dtos/ShiftingDTO.java` | Shifting DTO |
| `src/main/java/com/cargosetu/mapper/CustomerMapper.java` | MapStruct: `CustomerDTO` ↔ `Customer` |
| `src/main/java/com/cargosetu/mapper/ShiftingMapper.java` | MapStruct: `ShiftingDTO` ↔ `Shifting` |
| `src/main/java/com/cargosetu/mapper/ChargesMapper.java` | MapStruct: `ChargesDTO` ↔ `Charges` |
| `src/main/java/com/cargosetu/mapper/ExtraChargesMapper.java` | MapStruct: `ExtraChargesDTO` ↔ `ExtraCharges` |
| `src/main/java/com/cargosetu/utils/PermissionConstants.java` | `CUSTOMER_*`/`SHIFT_*` authority constants |

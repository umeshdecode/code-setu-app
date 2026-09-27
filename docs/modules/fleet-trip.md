# Fleet & Trip Module

> Covers Trip execution and its sub-records (documents, expenses, status history), Driver, Vehicle, GPS
> location tracking, and Driver↔Vehicle assignment. This is the most feature-complete module in the
> codebase (real MapStruct DTOs, a consistent response envelope, `@PreAuthorize` annotations on the Trip
> sub-controllers) but it also has the sharpest edges — see the **Known Bugs & Quirks** callouts throughout.
>
> Every claim below was verified by reading the referenced source file directly. Anything not directly
> observed is marked **[inferred]** or **[unverified]**.

---

## 1. Overview

The Fleet & Trip module models a single physical run of a vehicle (a "Trip") that fulfills a
`Shifting` (a customer's shifting/moving job, defined in the customer module), plus the resources that
make a trip happen:

- **Trip** (`tbl_trip`) — the execution record: links a `Shifting` to a `Driver` and a `Vehicle`, tracks
  planned vs. actual timing/distance, and carries a `TripStatus`.
- **TripDocument** (`trip_document`) — uploaded files (e.g. LR copy, POD) attached to a trip.
- **TripExpense** (`tbl_trip_expense`) — trip-related costs (fuel, tolls, driver allowance, etc.) with an
  approval flag.
- **TripStatusHistory** (`trip_status_history`) — an audit trail of trip status transitions.
- **TripTracking** — a GPS breadcrumb entity tied to `Trip`, but **it has no repository, service, or
  controller anywhere in the codebase** — it is dead/unwired code (confirmed via
  `Grep` — the only hit for `TripTracking` across `src/main/java` is the entity file itself).
- **Driver** — driver master data (license, contact, status) with real Bean Validation constraints.
- **Vehicle** — vehicle master data (registration, insurance/pollution expiry, capacity) with real Bean
  Validation constraints.
- **VehicleLocation** (`tblmvahicletrack`) — an append-only GPS ping history for a vehicle, broadcast live
  over WebSocket/STOMP.
- **DriverVehicleAssignment** (`driver_vehicle_assignments`) — the current/historical pairing of a driver
  to a vehicle, with soft "unassign" (active flag + `unassignedAt`).
- **DriverAdvance** — a `Trip`+`Driver` cash-advance entity, defined but, like `TripTracking`, **has no
  repository, service, or controller** (confirmed via `Grep` — only the entity file itself references
  the class). Effectively a stub/future-feature model.

This module is one of the few with genuine field-level Bean Validation on its JPA entities
(`Driver`, `Vehicle`, `DriverVehicleAssignment`), unlike most of the rest of the codebase. However, as
detailed in §7 and §11, that validation is not wired up the way it looks (DTOs carry no annotations, and
the Driver/Vehicle/Assignment/Location controllers are wide open with no `@PreAuthorize` at all — and even
where `@PreAuthorize` exists on Trip sub-controllers, it is repo-wide **inert**, see §7).

---

## 2. API Controllers & Endpoints

**Global base path:** `BaseURL.BASE_URL = "/api/v2/cargosetu"` (`src/main/java/com/cargosetu/utils/BaseURL.java`).
Server port: `31201` (`application.properties`).

> **Known bug — inconsistent base path:** `VehicleController` is mapped with
> `@RequestMapping(BaseURL.VEHICLE)` only (`"/vehicle"`) — it **omits `BaseURL.BASE_URL`**, unlike every
> other controller in this module (and the rest of the app). Vehicle CRUD therefore actually lives at
> `/vehicle/**`, not `/api/v2/cargosetu/vehicle/**`. Verified by direct read of
> `src/main/java/com/cargosetu/controller/VehicleController.java` line 14.

### 2.1 TripController
**File:** `src/main/java/com/cargosetu/controller/TripController.java`
**Base mapping:** `/api/v2/cargosetu/trip`

| Method | Endpoint | Permission (`@PreAuthorize`) | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/trip` | `TRIP_CREATE` | Create a trip from `TripDTO`; resolves `shiftingId`/`driverId`/`vehicleId` to entities |
| GET | `/api/v2/cargosetu/trip/{tripId}` | `TRIP_READ` | Get one trip by ID |
| GET | `/api/v2/cargosetu/trip` | `TRIP_READ` | Get all trips (no pagination, no filtering) |
| PUT | `/api/v2/cargosetu/trip/{tripId}` | `TRIP_UPDATE` | Partial update (only non-null DTO fields applied); status transitions to `STARTED`/`COMPLETED` auto-stamp `actualStartTime`/`actualEndTime` |
| DELETE | `/api/v2/cargosetu/trip/{tripId}` | `TRIP_DELETE` | **Soft delete** — sets `active=false`, does not remove the row |

### 2.2 TripDocumentController
**File:** `src/main/java/com/cargosetu/controller/TripDocumentController.java`
**Base mapping:** `/api/v2/cargosetu/trip-document`

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/trip-document` (multipart: `file`, `tripId`, `documentType`) | `TRIP_DOCUMENT_CREATE` | Upload a document for a trip |
| GET | `/api/v2/cargosetu/trip-document/{id}` | `TRIP_DOCUMENT_READ` | Get one document |
| GET | `/api/v2/cargosetu/trip-document` | `TRIP_DOCUMENT_READ` | Get all documents (all trips, unfiltered) |
| GET | `/api/v2/cargosetu/trip-document/by-trip/{tripId}` | `TRIP_DOCUMENT_READ` | Get documents for one trip (via `findByTripTripId`) |
| PUT | `/api/v2/cargosetu/trip-document/{id}` (multipart, all params optional) | `TRIP_DOCUMENT_UPDATE` | Replace file/type/trip link; deletes the old file from disk if a new one is uploaded |
| DELETE | `/api/v2/cargosetu/trip-document/{id}` | `TRIP_DOCUMENT_DELETE` | **Hard delete** — removes DB row and disk file |

### 2.3 TripExpenseController
**File:** `src/main/java/com/cargosetu/controller/TripExpenseController.java`
**Base mapping:** `/api/v2/cargosetu/trip-expense`

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/trip-expense` | `TRIP_EXPENSE_CREATE` | Create an expense line for a trip |
| GET | `/api/v2/cargosetu/trip-expense/{id}` | `TRIP_EXPENSE_READ` | Get one expense |
| GET | `/api/v2/cargosetu/trip-expense` | `TRIP_EXPENSE_READ` | Get all expenses (unfiltered) |
| GET | `/api/v2/cargosetu/trip-expense/by-trip/{tripId}` | `TRIP_EXPENSE_READ` | Get expenses for one trip (via `findByTripTripId`) |
| PUT | `/api/v2/cargosetu/trip-expense/{id}` | `TRIP_EXPENSE_UPDATE` | Partial update; `approvedBy`/`approvedAt` are **never set anywhere in the service** despite existing on the entity/DTO — see §11 |
| DELETE | `/api/v2/cargosetu/trip-expense/{id}` | `TRIP_EXPENSE_DELETE` | **Hard delete** |

### 2.4 TripStatusHistoryController
**File:** `src/main/java/com/cargosetu/controller/TripStatusHistoryController.java`
**Base mapping:** `/api/v2/cargosetu/trip-status-history`

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/trip-status-history` | `TRIP_STATUS_HISTORY_CREATE` | Manually create a history row (bypasses `TripService`'s own auto-logging) |
| GET | `/api/v2/cargosetu/trip-status-history/{id}` | `TRIP_STATUS_HISTORY_READ` | Get one row |
| GET | `/api/v2/cargosetu/trip-status-history` | `TRIP_STATUS_HISTORY_READ` | Get all rows (unfiltered) |
| GET | `/api/v2/cargosetu/trip-status-history/by-trip/{tripId}` | `TRIP_STATUS_HISTORY_READ` | Get a trip's history, newest first (`findByTripTripIdOrderByChangedAtDesc`) |
| PUT | `/api/v2/cargosetu/trip-status-history/{id}` | `TRIP_STATUS_HISTORY_UPDATE` | Edit an existing history row (mutable audit trail — see §11) |
| DELETE | `/api/v2/cargosetu/trip-status-history/{id}` | `TRIP_STATUS_HISTORY_DELETE` | **Hard delete** of an audit row |

### 2.5 DriverController — **no `@PreAuthorize` at all**
**File:** `src/main/java/com/cargosetu/controller/DriverController.java`
**Base mapping:** `/api/v2/cargosetu/drivers`

Verified by direct read: the file imports no `@PreAuthorize`, no `PermissionConstants`, and has zero
security annotations on the class or any method.

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/drivers` | **none** | Create driver from `DriverDto` |
| GET | `/api/v2/cargosetu/drivers/{id}` | **none** | Get one driver |
| GET | `/api/v2/cargosetu/drivers` | **none** | Get all drivers |
| PUT | `/api/v2/cargosetu/drivers/{id}` | **none** | Update driver (only `name`, `phoneNumber`, `email`, `status`, `licenseExpiryDate` are actually applied — see §3.5) |
| DELETE | `/api/v2/cargosetu/drivers/{id}` | **none** | **Hard delete** |

Also confirmed: `PermissionConstants.java` (`src/main/java/com/cargosetu/utils/PermissionConstants.java`)
contains **no `DRIVER_*` constants whatsoever** — there is no permission string to even authorize against.

### 2.6 VehicleController — **no `@PreAuthorize` at all**
**File:** `src/main/java/com/cargosetu/controller/VehicleController.java`
**Base mapping:** `/vehicle` (note: not under `/api/v2/cargosetu` — see the bug callout above)

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/vehicle` | **none** | Create vehicle from `VehicleDto` |
| GET | `/vehicle/{id}` | **none** | Get one vehicle |
| GET | `/vehicle` | **none** | Get all vehicles |
| PUT | `/vehicle/{id}` | **none** | Update (only `vehicleType`, `model`, `manufacturer`, `capacityKg`, `status` applied — see §3.6) |
| DELETE | `/vehicle/{id}` | **none** | **Hard delete** |

`PermissionConstants.java` also has **no `VEHICLE_*` constants**.

### 2.7 VehicleLocationController — **no `@PreAuthorize` at all**
**File:** `src/main/java/com/cargosetu/controller/VehicleLocationController.java`
**Base mapping:** `BaseURL.BASE_URL + BaseURL.VEHICLE + BaseURL.LOCATION` = `/api/v2/cargosetu/vehicle/location`

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/vehicle/location` | **none** | Body: `{"vehicleId": ..., "lat": ..., "lng": ...}` (untyped `Map<String,Object>`, manually parsed with `Long.valueOf`/`Double.parseDouble` — malformed input throws an unhandled `NumberFormatException`) |
| GET | `/api/v2/cargosetu/vehicle/location/{vehicleId}/location` | **none** | **Known quirk:** the `@GetMapping` value is `"/{vehicleId}" + BaseURL.LOCATION`, which appends a *second* `/location` segment onto the class-level mapping (already ending in `/location`), producing this doubled path. Verified by direct read of line 32. |

### 2.8 DriverVehicleAssignmentController — **no `@PreAuthorize` at all**
**File:** `src/main/java/com/cargosetu/controller/DriverVehicleAssignmentController.java`
**Base mapping:** `BaseURL.BASE_URL + BaseURL.ASSIGNMENTS` = `/api/v2/cargosetu/assignments`

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/v2/cargosetu/assignments?driverId=X&vehicleId=Y` | **none** | Assign a driver to a vehicle (query params, not a body) |
| GET | `/api/v2/cargosetu/assignments` | **none** | List all assignments as `AssignedDriverToVehicleDto`, newest-first |
| PUT | `/api/v2/cargosetu/assignments/unassign/{id}` | **none** | Deactivate an assignment (`id` is the **assignment ID**, not driver/vehicle ID); request body/param is ignored — the controller hardcodes the reason string `"discreption"` (sic) regardless of caller input |

Also has a dead import: `import java.awt.print.Pageable;` (AWT's printing `Pageable`, not Spring Data's) —
unused, but a sign this controller was scaffolded carelessly; no pagination is actually implemented.

---

## 3. Business / Service Logic

### 3.1 Trip creation (`TripService.createTrip`, `@Transactional`)
File: `src/main/java/com/cargosetu/service/TripService.java`

1. `TripMapper.toEntity(tripDTO)` (MapStruct) builds a `Trip` and stub-sets `shifting.shiftingId` /
   `driver.id` / `vehicle.id` from the DTO's FK id fields.
2. The service then **re-resolves** each FK by an explicit `findById(...).ifPresent(trip::set...)` call —
   fetching the fully-hydrated `Shifting`/`Driver`/`Vehicle` rather than relying on the mapper's proxy
   stubs. If an id doesn't resolve, the field is silently left as whatever the mapper produced (a
   detached stub with only the id set) rather than rejected.
3. `createdAt`/`updatedAt` stamped manually to `now()`; `active` defaults to `true` if the DTO didn't
   supply it.
4. `tripRepository.save(trip)`.
5. **Immediately** creates a `TripStatusHistory` row with **`oldStatus` == `newStatus`** (both set to
   `savedTrip.getStatus().toString()`) — i.e. on creation, "history" is really just recording the initial
   status, not a transition.
6. Returns the saved trip via `TripMapper.toDto`.

> **Known bug — NPE + silent partial commit on missing status.** `TripDTO.status` has no `@NotNull`/
> validation, and `Trip.status` has no default. If a caller POSTs a trip without a `status`, step 5 calls
> `savedTrip.getStatus().toString()` on a `null` field → `NullPointerException`. This is caught by the
> method's own `try/catch (Exception e)` and turned into an `INTERNAL_SERVER_ERROR` `GenericDataDTO`
> response — **but** the exception never propagates out of the `@Transactional` method, so Spring's
> transaction manager sees a normal return and **commits the transaction anyway**. Net effect: the client
> sees a 500-style error body, but the `Trip` row (from step 4) is actually persisted in the DB with no
> corresponding `TripStatusHistory` row. Verified by reading lines 44–87 end-to-end; this pattern (catch
> inside a `@Transactional` method → no rollback) also appears in `updateTrip`.

### 3.2 Trip update (`TripService.updateTrip`, `@Transactional`)
Partial-update semantics: each field is only overwritten if the corresponding `TripDTO` field is
non-null. Special cases:
- `status == STARTED` → also stamps `actualStartTime = now()`.
- `status == COMPLETED` → also stamps `actualEndTime = now()`.
- FKs (`shiftingId`/`driverId`/`vehicleId`) re-resolved the same way as in create.

> **Known bug — status-history logging on update is dead code.** The method does:
> ```java
> if (tripDTO.getStatus() != null) {
>     existingTrip.setStatus(tripDTO.getStatus());   // (A) mutates existingTrip FIRST
> }
> ...
> if (!Objects.equals(existingTrip.getStatus(), tripDTO.getStatus())) {   // (B) compares AFTER (A) ran
>     TripStatusHistory tripStatusHistory = new TripStatusHistory();
>     tripStatusHistory.setOldStatus(existingTrip.getStatus().toString());
>     tripStatusHistory.setNewStatus(tripDTO.getStatus().toString());
>     ...
> }
> ```
> Because `existingTrip`'s status is overwritten with `tripDTO`'s status **before** the equality check,
> whenever a caller actually supplies a new status the two sides of the comparison are already identical
> — so `!Objects.equals(...)` is `false` and the history block **never runs**. The only way the block
> *can* run is when `tripDTO.getStatus()` is `null` (so step (A) is skipped) while `existingTrip`'s
> pre-existing status is non-null — but then `tripStatusHistory.setNewStatus(tripDTO.getStatus().toString())`
> throws an NPE on the null DTO status, hitting the same silent-swallow-then-commit pattern as §3.1.
> **Net result: `TripStatusHistory` is effectively never populated by normal trip status updates through
> this endpoint** — despite the code visibly trying to. (The `TripStatusHistoryController`'s own CRUD
> endpoints are the only reliable way to write history rows, and they require the caller to do it
> manually.) Verified by reading lines 128–211.

### 3.3 Trip delete
Soft delete only: sets `active=false`, `updatedAt=now()`, saves. The row (and all its
`TripDocument`/`TripExpense`/`TripStatusHistory` children, which have no cascade or orphan-removal
configured — see §5) remains in the DB and remains reachable via `getTrip`/`getAllTrips` (neither method
filters on `active`).

### 3.4 GPS location update flow (`VehicleLocationImpl`) — **append-only history, not upsert**
File: `src/main/java/com/cargosetu/service/impl/VehicleLocationImpl.java`

`saveLocation(vehicleId, lat, lng)`:
```java
VehicleLocation loc = new VehicleLocation();
loc.setVehicleId(vehicleId);
loc.setLatitude(lat);
loc.setLongitude(lng);
loc.setTimestamp(LocalDateTime.now());
VehicleLocation saved = repo.save(loc);
messagingTemplate.convertAndSend("/topic/vehicle/" + vehicleId, saved);
return saved;
```
Every call constructs a **new** `VehicleLocation` entity (no lookup/reuse of an existing row for the
vehicle) and inserts it — this is **append-only history**, i.e. every GPS ping creates a brand-new
`tblmvahicletrack` row keyed by its own auto-increment `id`. There is no upsert/latest-row-per-vehicle
table design; historical pings accumulate indefinitely (no cleanup/retention logic found).

Each save is also broadcast live to any WebSocket/STOMP subscribers of `/topic/vehicle/{vehicleId}` (see
§10).

`getLatestLocation(vehicleId)` delegates to `VehicleLocationRepo.findLatestLocationToday`:
```java
@Query(value = "SELECT * FROM tblmvahicletrack t " +
        "WHERE t.id = :vehicleId " +
        "AND t.timestamp > CURRENT_DATE() " +
        "ORDER BY t.timestamp DESC LIMIT 1", nativeQuery = true)
Optional<VehicleLocation> findLatestLocationToday(@Param("vehicleId") Long vehicleId);
```
> **Known bug — wrong column filtered.** The `WHERE` clause filters on `t.id` (the location row's own
> primary key), **not** `t.vehicle_id`. Since `id` is an auto-increment PK, this query will only ever
> "coincidentally" match when a row's primary key happens to equal the requested `vehicleId` — for any
> real fleet with more than one GPS ping ever recorded, this returns the wrong vehicle's location or
> nothing at all. Verified by direct read of `VehicleLocationRepo.java` lines 14–19. The controller also
> exposes a second, unfiltered-by-date finder, `findTopByVehicleIdOrderByTimestampDesc`, which is
> correctly written (filters on `vehicle_id`) but is **never called** by any service — the buggy
> `findLatestLocationToday` is what's actually wired to the `GET .../location` endpoint.

### 3.5 Driver CRUD (`DriverServiceImpl`)
- `createDriver`: `new Driver(driverDto)` (entity's DTO-constructor, copies all fields), stamps
  `createdAt`/`updatedAt`, saves. Bean Validation on the `Driver` entity (see §11) fires here at
  `save()` time via JPA's default Bean-Validation-on-persist integration — **not** at the controller/DTO
  layer, since `DriverDto` carries no validation annotations and the controller doesn't use `@Valid`.
- `updateDriver`: fetches by id, then **only copies** `name`, `phoneNumber`, `email`, `status`,
  `licenseExpiryDate` from the incoming DTO — `licenseNumber`, `experienceYears`, `address`,
  `joiningDate` are silently ignored even if present in the request body. Verified lines 61–80.
- `deleteDriver`: hard delete (`driverRepository.delete(driver)`) — no check for active trips or
  active `DriverVehicleAssignment` rows referencing this driver; deleting an assigned driver leaves the
  `DriverVehicleAssignment.driver_id` FK pointing at a gone row (see §5 — no cascade/restrict declared,
  Hibernate `ddl-auto=update` means the DB-level FK constraint behavior is whatever Hibernate happened to
  generate, not an explicit `ON DELETE` policy in code).

### 3.6 Vehicle CRUD (`VehicleServiceImpl`)
Same shape as Driver: `updateVehicle` only copies `vehicleType`, `model`, `manufacturer`, `capacityKg`,
`status` (lines 63–83) — `chassisNumber`, `engineNumber`, `mileage`, `registrationNumber`,
`insuranceExpiryDate`, `pollutionExpiryDate`, `fuelType`, `registrationDate`, `currentLocation` are all
silently ignored on update even though they're all present on `VehicleDto`. `deleteVehicle` is a hard
delete with no active-trip/active-assignment check.

### 3.7 Driver↔Vehicle assignment (`DriverVehicleAssignmentServiceImpl`)
`assignDriver(driverId, vehicleId)`:
1. Fetch `Driver` and `Vehicle` by id (404-style `RuntimeException` if either missing).
2. Fetch all of that **driver's** currently-active assignments
   (`findByDriverIdAndActiveTrue`, a native query) and deactivate them
   (`active=false`, `unassignedAt=now()`), saved via `saveAll`.
3. Create a new `DriverVehicleAssignment` (`assignedAt=now()`, `active=true`).
4. Set `driver.status = "Assigned"` and `vehicle.status = "Assigned"` (free-text strings, **not** the
   `Driver.status` regex-validated enum values `ACTIVE|INACTIVE|SUSPENDED`, nor the `Vehicle.status`
   regex-validated values `AVAILABLE|ON_TRIP|MAINTENANCE|OUT_OF_SERVICE` — see §11. Writing `"Assigned"`
   here **violates the entity's own `@Pattern` constraint** and would fail Bean Validation on the
   subsequent `driverRepository.save(driver)` / `vehicleRepository.save(vehicle)` calls **unless**
   Bean-Validation-on-persist isn't actually being triggered for an update to an already-managed entity in
   this code path — this needs runtime verification; flagged here as a **likely bug** based on static
   reading of both files together).
5. Persists the new assignment.

> **Known bug — only the driver side is cleared, not the vehicle side.** Step 2 only deactivates the
> *driver's* other active assignments. If `vehicleId` already has a different active assignment (to a
> different driver), that old row is left `active=true` — so the same vehicle can end up with **two
> simultaneously-active assignment rows** to two different drivers. `findByVehicleIdAndActiveTrue` exists
> on the repository (see §6) but is **never called** anywhere in the assignment service.

`unassign(assignmentId, description)`: fetches the assignment, sets `active=false` +
`unassignedAt=now()`, then unconditionally resets both `driver.status` and `vehicle.status` to
`"Available"` (again, a free-text string not matching either entity's `@Pattern`). Does **not** check
whether the assignment was even still active before flipping statuses, and — because it always resets
*both* driver and vehicle status to `"Available"` — unassigning a stale/already-inactive assignment could
incorrectly mark a driver/vehicle "Available" even if they're actively tied to a different, newer
assignment.

The `description` parameter passed in from `DriverVehicleAssignmentController.unassign()` is **never
used** — the method signature has a `discreption` parameter (`unassign(Long assignmentId, String
discreption)`) but the implementation body never reads it, and no `remarks`/reason field on the
`DriverVehicleAssignment` is ever populated by this flow despite the entity having a `remarks` column.

None of `DriverServiceImpl`, `VehicleServiceImpl`, `VehicleLocationImpl`, or
`DriverVehicleAssignmentServiceImpl` are annotated `@Transactional` (only `TripService`,
`TripDocumentService`, `TripExpenseService`, and `TripStatusHistoryService` use it, and only on their
write methods). No `spring.jpa.open-in-view` override was found in any `application*.properties`, so
Spring Boot's default (`true`, Open Session In View) is in effect — this is why lazy `driver`/`vehicle`
associations can still be read in `getAllAssignments()` outside an explicit transaction without a
`LazyInitializationException`, but it's an implicit dependency on a Spring Boot default rather than an
explicit design choice.

---

## 4. DTOs / Request-Response Models

| DTO | File | Fields | Notes |
|---|---|---|---|
| `TripDTO` | `dto/TripDTO.java` | `tripId, tripNumber, shiftingId, driverId, vehicleId, plannedStartTime, actualStartTime, actualEndTime, plannedDistanceKm, actualDistanceKm, status (TripStatus), remarks, active` | No validation annotations. Note: entity has both `plannedEenTime` (sic — typo for `plannedEndTime`, see §5) and no `plannedEndTime`; the DTO has neither planned-end field at all, so the entity's `plannedEenTime` is **unreachable from the API** (never mapped, never settable/gettable via `TripDTO`/`TripMapper`). |
| `TripDocumentDTO` | `dto/TripDocumentDTO.java` | `id, tripId, documentType, documentUrl, fileName` | 1:1 with entity. |
| `TripExpenseDTO` | `dto/TripExpenseDTO.java` | `expenseId, tripId, expenseType (ExpenseType), amount, description, billNumber, receiptUrl, expenseDate, approved, approvedBy` | Entity also has `approvedAt` (`LocalDate`) — **missing from the DTO**, so it can never be set/read through the API even though nothing else sets it either (see §11). |
| `TripStatusHistoryDTO` | `dto/TripStatusHistoryDTO.java` | `id, tripId, oldStatus, newStatus, changedBy, changedAt` | `oldStatus`/`newStatus` are plain `String`, not `TripStatus` — no enum validation at this layer. |
| `DriverDto` | `dto/DriverDto.java` | `id, name, phoneNumber, email, licenseNumber, licenseExpiryDate, experienceYears, address, status, joiningDate, createdAt, updatedAt` | **No validation annotations** — all constraint enforcement happens only if/when the mapped `Driver` entity is persisted (see §3.5, §11). |
| `VehicleDto` | `dto/VehicleDto.java` | `id, chassisNumber, engineNumber, mileage, registrationNumber, vehicleType, model, manufacturer, capacityKg, status, insuranceExpiryDate, createdAt, updatedAt, fuelType, registrationDate, pollutionExpiryDate, currentLocation` | Same — no validation annotations on the DTO itself. |
| `DriverVehicleAssignmentDto` | `dto/DriverVehicleAssignmentDto.java` | `id, driverId, vehicleId, assignedAt, unassignedAt, active, remarks` | **Defined but never used** by any controller or service in this module — `DriverVehicleAssignmentController.assign()` takes `driverId`/`vehicleId` as `@RequestParam`s, not this DTO. Appears to be a dead/unused "request" shape. |
| `AssignedDriverToVehicleDto` | `dto/AssignedDriverToVehicleDto.java` | `id, driverId, driverName, vehicleId, registrationNumber, assignedAt, unassignedAt, active, remarks, createdAt, updatedAt` | The actual **response** shape used by `GET /assignments` (flattened driver name + vehicle registration number for UI display). `createdAt`/`updatedAt` fields exist on the DTO but are **never populated** by `DriverVehicleAssignmentServiceImpl.getAllAssignments()` (it only sets the fields listed in §3.7's mapping loop) — they'll always serialize as `null`. |

Two DTOs exist for the same `DriverVehicleAssignment` entity (request-shaped `DriverVehicleAssignmentDto`,
unused; response-shaped `AssignedDriverToVehicleDto`, used) — consistent with the "two DTOs, one for each
direction" pattern noted in `docs/services-and-known-issues.md`.

Response envelope: Trip/TripDocument/TripExpense/TripStatusHistory controllers all wrap responses in
`GenericDataDTO` (`utils/GenericDataDTO.java`: `responseMessege` (sic), `responseCode`, `data`,
`dataList`, `errors[]`, plus unused pagination fields `totalRecords`/`pageRecords`/`currentPageNumber`/
`totalPages` that are never set by any method in this module). Driver/Vehicle/VehicleLocation/
DriverVehicleAssignment controllers **do not** use `GenericDataDTO` at all — they return the raw entity
(`Driver`, `Vehicle`, `VehicleLocation`) or DTO list directly inside a plain `ResponseEntity<>`, which is
itself an inconsistency with the rest of the module (Trip-family endpoints are consistent; the
Driver/Vehicle/Assignment/Location endpoints are a completely different, older-looking response style).

---

## 5. Entities & Database Relationships

| Entity | Table (from `@Table`, or inferred default if absent) | File |
|---|---|---|
| `Trip` | `tbl_trip` | `model/Trip.java` |
| `TripDocument` | `trip_document` | `model/TripDocument.java` |
| `TripExpense` | `tbl_trip_expense` | `model/TripExpense.java` |
| `TripStatusHistory` | `trip_status_history` | `model/TripStatusHistory.java` |
| `TripTracking` | *(no `@Table`; Hibernate default → `trip_tracking`)* **[inferred]** | `model/TripTracking.java` |
| `Driver` | *(no `@Table`; Hibernate default → `driver`)* **[inferred]** | `model/Driver.java` |
| `Vehicle` | `vehicles` | `model/Vehicle.java` |
| `VehicleLocation` | `tblmvahicletrack` | `model/VehicleLocation.java` |
| `DriverVehicleAssignment` | `driver_vehicle_assignments` | `model/DriverVehicleAssignment.java` |
| `DriverAdvance` | *(no `@Table`; Hibernate default → `driver_advance`)* **[inferred]**; unwired (no repo/service/controller) | `model/DriverAdvance.java` |

### Relationships (confirmed by direct read of each entity file)

- **`Trip.shifting`** → `@ManyToOne` to `com.cargosetu.customer.entity.Shifting` (no `@JoinColumn`, so
  Hibernate defaults the FK column name; no `fetch` specified → default `EAGER` for `@ManyToOne`). Many
  trips can reference one shifting job (a shift could in principle have multiple trip legs, though nothing
  in the code enforces a 1:1 or cardinality limit).
- **`Trip.driver`** → `@ManyToOne` to `Driver`, default-named FK, default `EAGER` fetch.
- **`Trip.vehicle`** → `@ManyToOne` to `Vehicle`, default-named FK, default `EAGER` fetch.
- **`TripDocument.trip`** → `@ManyToOne` to `Trip`. Many documents per trip.
- **`TripExpense.trip`** → `@ManyToOne` to `Trip`. Many expenses per trip.
- **`TripStatusHistory.trip`** → `@ManyToOne` to `Trip`. Many history rows per trip.
- **`TripTracking.trip`** → `@ManyToOne` to `Trip` (defined but unreachable — see §1).
- **`DriverAdvance.trip`** → `@ManyToOne` to `Trip`; **`DriverAdvance.driver`** → `@ManyToOne` to `Driver`
  (defined but unreachable — see §1).
- **`DriverVehicleAssignment.driver`** → `@ManyToOne(fetch = LAZY)` to `Driver`, explicit
  `@JoinColumn(name="driver_id", nullable=false)`.
- **`DriverVehicleAssignment.vehicle`** → `@ManyToOne(fetch = LAZY)` to `Vehicle`, explicit
  `@JoinColumn(name="vehicle_id", nullable=false)`.
- **`VehicleLocation.vehicleId`** is a **plain `Long` column, not a JPA relationship** — there is no
  `@ManyToOne` to `Vehicle` and no FK constraint enforced by JPA; it's an unvalidated foreign key by
  convention only (any `Long` value, including one for a vehicle that doesn't exist, can be inserted).

**None of `Trip`, `TripDocument`, `TripExpense`, `TripStatusHistory`, or `TripTracking` declare any
`cascade`, `orphanRemoval`, `@OneToMany` back-reference, or fetch-join** — every relationship in this
module is a unidirectional `@ManyToOne` from the "child" side; `Trip` itself has no `@OneToMany` collection
of its documents/expenses/history. This means: deleting/soft-deleting a `Trip` never cascades to its
children, and there is no way to fetch a trip "with all its documents" in a single JPA query — every
sub-resource controller fetches its own list independently via `by-trip/{tripId}`.

`Driver` and `Vehicle` have no direct relationship to each other — they're only ever linked indirectly via
`Trip` (both FK'd from the same trip row) or via `DriverVehicleAssignment` (the explicit pairing entity).

---

## 6. Repositories

| Repository | File | Extends | Custom query methods |
|---|---|---|---|
| `TripRepository` | `repo/TripRepository.java` | `JpaRepository<Trip, Long>` | None — pure CRUD, no custom finders at all (no filter by `companyId`, `custId`, `active`, or `status`). |
| `TripDocumentRepository` | `repo/TripDocumentRepository.java` | `JpaRepository<TripDocument, Long>` | `findByTripTripId(Long tripId)` — used by `TripDocumentService.getByTripId` and the controller's `by-trip/{tripId}` route. |
| `TripExpenseRepository` | `repo/TripExpenseRepository.java` | `JpaRepository<TripExpense, Long>` | `findByTripTripId(Long tripId)` — same pattern, used by `TripExpenseService.getByTripId`. |
| `TripStatusHistoryRepository` | `repo/TripStatusHistoryRepository.java` | `JpaRepository<TripStatusHistory, Long>` | `findByTripTripIdOrderByChangedAtDesc(Long tripId)` — used by `TripStatusHistoryService.getByTripId`; the only "by-trip" finder that also orders results. |
| `DriverRepository` | `repo/DriverRepository.java` | `JpaRepository<Driver, Long>` | `findByLicenseNumber(String)` — defined but **never called** by any service in this module (license-number uniqueness is only enforced at the DB level via `@Column(unique=true)` on `Driver.licenseNumber`, not proactively checked in `DriverServiceImpl`). |
| `VehicleRepository` | `repo/VehicleRepository.java` | `JpaRepository<Vehicle, Long>` | `findByRegistrationNumber(String)` — same story: defined, never called; uniqueness relies solely on the DB constraint from `@Column(unique=true)`. |
| `VehicleLocationRepo` | `repo/VehicleLocationRepo.java` | `JpaRepository<VehicleLocation, Long>` | `findTopByVehicleIdOrderByTimestampDesc(Long)` (correct, but unused — see §3.4); `findLatestLocationToday(Long)` (native SQL, buggy `WHERE t.id = :vehicleId` — see §3.4, this is the one actually wired to the service). |
| `DriverVehicleAssignmentRepository` | `repo/DriverVehicleAssignmentRepository.java` | `JpaRepository<DriverVehicleAssignment, Long>` | `findByDriverIdAndActiveTrue(Long)` (native SQL) — used by `assignDriver`. `findByVehicleIdAndActiveTrue(Long)` (native SQL) — defined but **never called** (see the "vehicle side never cleared" bug in §3.7). `findAllByOrderByIdDesc()` — used by `getAllAssignments`. Also has two unused/incorrect imports: `org.hibernate.query.Page` and `java.awt.print.Pageable` (AWT printing API, not Spring's) — neither is used anywhere in the interface; no pagination is implemented despite the imports suggesting an intent to add it. |

No repository in this module has any `companyId`/tenant-scoping query method — consistent with
`docs/services-and-known-issues.md §"Not scoped"` findings for Driver/Vehicle/Trip services.

---

## 7. Security & Authentication

**Permission constants** (`utils/PermissionConstants.java`, `PERM_PREFIX = "PERM_"`):

| Constant | Value |
|---|---|
| `TRIP_CREATE` / `TRIP_READ` / `TRIP_UPDATE` / `TRIP_DELETE` | `PERM_TRIP_CREATE` / `PERM_TRIP_READ` / `PERM_TRIP_UPDATE` / `PERM_TRIP_DELETE` |
| `TRIP_STATUS_HISTORY_CREATE` / `_READ` / `_UPDATE` / `_DELETE` | `PERM_TRIP_STATUS_HISTORY_*` |
| `TRIP_EXPENSE_CREATE` / `_READ` / `_UPDATE` / `_DELETE` | `PERM_TRIP_EXPENSE_*` |
| `TRIP_DOCUMENT_CREATE` / `_READ` / `_UPDATE` / `_DELETE` | `PERM_TRIP_DOCUMENT_*` |
| `DRIVER_*`, `VEHICLE_*`, `ASSIGNMENT_*`/`DRIVER_VEHICLE_ASSIGNMENT_*`, `VEHICLE_LOCATION_*` | **Do not exist.** Confirmed by `Grep` across the entire `PermissionConstants.java` file — there is no permission constant for Driver, Vehicle, VehicleLocation, or DriverVehicleAssignment resources at all. |

**Enforcement reality, confirmed against `docs/services-and-known-issues.md` and cross-checked here:**
`config/MethodSecurityConfig.java` sets `@EnableMethodSecurity(prePostEnabled = false)` — this makes
**every** `@PreAuthorize` annotation in the entire application (including all the `TRIP_*` ones listed
above, on `TripController`/`TripDocumentController`/`TripExpenseController`/`TripStatusHistoryController`)
**silently inert**. So even though the four Trip-family controllers *look* permission-gated with specific
`TRIP_*` codes, and Driver/Vehicle/VehicleLocation/DriverVehicleAssignment controllers have **no
annotations to even be inert** — in practice, **all eight controllers in this module are equally
unprotected at the method level today**: any authenticated (JWT-bearing) user can call any endpoint in
this module regardless of role or permission.

The only access control that does apply is upstream, at the HTTP-filter level
(`config/SecurityConfig.java`/`config/JwtAuthenticationFilter.java`): a request must carry a valid JWT
(none of this module's paths appear in the `permitAll()`/`shouldNotFilter()` allow-lists), so the module
is not literally public/anonymous — it just has no per-role/per-permission distinction once authenticated.

**Practical implication for this module specifically:** Driver, Vehicle, VehicleLocation, and
DriverVehicleAssignment endpoints have *no* `@PreAuthorize` annotations to begin with (so there's nothing
even nominally documenting an intended permission model for them), while the four Trip-family controllers
at least document an *intended* permission model via their `TRIP_*` annotations — even though neither
group is actually enforced differently today.

---

## 8. Exception Handling

`handler/GlobalExceptionHandler.java` (`@RestControllerAdvice`) has exactly two handlers:

```java
@ExceptionHandler(CustomValidationException.class)  // -> ResponseEntity.badRequest() (400)
@ExceptionHandler(Exception.class)                  // -> ResponseEntity.badRequest() (400), even though
                                                     //    the body's responseCode is set to 500
```

Both **always return HTTP 400** regardless of the actual error — the "real" status only appears inside the
JSON body's `responseCode` field (via `ApiResponseDto`), never in the HTTP status line. This applies to
this module the same as everywhere else: e.g. a `Driver`/`Vehicle` Bean Validation failure surfaced as a
raw `ConstraintViolationException` (see §11) from `DriverServiceImpl`/`VehicleServiceImpl` — which have no
local try/catch translating it into a friendlier shape — would propagate up to this global handler and
come back as a bare 400 with the raw exception message, no field-level error breakdown.

Within the module's own service layer (`TripService`, `TripDocumentService`, `TripExpenseService`,
`TripStatusHistoryService`), every public method wraps its body in its own local `try/catch (Exception e)`
and returns a `GenericDataDTO` with `responseCode = CommonStatusCode.INTERNAL_SERVER_ERROR` (500) **inside
a `200 OK` HTTP response** (the controller layer does `return ResponseEntity.ok(response)` unconditionally
in the success path, and only falls through to `ResponseEntity.internalServerError()` if the *controller's
own* try/catch — wrapping the service call — throws, which it normally won't since the service already
caught everything). So in practice, TripController-family failures usually come back as **HTTP 200** with
an embedded `responseCode: 500` in the body, while Driver/Vehicle/Assignment/Location-family failures
(which have no such internal try/catch, letting exceptions bubble to `GlobalExceptionHandler`) come back as
**HTTP 400**. Two different failure-reporting shapes exist side-by-side within the same module depending
on which sub-controller you call — worth being explicit about since a frontend/integration cannot rely on
HTTP status codes consistently across this module's own endpoints.

As noted in §3.1/§3.2, some of those internally-caught exceptions occur **after** a partial write already
happened inside a `@Transactional` method — since the exception never escapes the method, the transaction
still commits, so an "error" response can coexist with a successful (partial) database write.

---

## 9. Configuration

- **Base path / port**: `/api/v2/cargosetu` on port `31201` (`application.properties`) — except
  `VehicleController`, which is not under the base path (see §2 bug callout).
- **Database**: MySQL, `spring.jpa.hibernate.ddl-auto=update` in both `application-local.properties` and
  `application-prod.properties` — none of this module's tables (`tbl_trip`, `trip_document`,
  `tbl_trip_expense`, `trip_status_history`, `vehicles`, `tblmvahicletrack`,
  `driver_vehicle_assignments`, and the default-named `driver`/`trip_tracking`/`driver_advance`) have a
  Liquibase changeset — they are all auto-created/altered by Hibernate at startup. No explicit
  `spring.jpa.open-in-view` override was found (default `true` applies — see §3.7).
- **File storage for `TripDocument` uploads**: `TripDocumentService` looks up a `Clientservice`
  config row by `name = "TRIP_DOCS"` (seeded via Liquibase changeset `SKP00010` in
  `resources/Changelog/changelog-master.xml`, which sets `value = "D:\\cargo project\\document"` as the
  intended upload directory) — **but** `TripDocumentService.create()` calls
  `clientServiceRepo.findByName("TRIP_DOCS").orElseThrow().getName()`, i.e. it re-reads the row's `name`
  field (which is always the literal string `"TRIP_DOCS"`) instead of its `value` field (the actual
  configured path). **This is a bug**: the base path handed to `FileUtility.saveMultipartFile(files,
  basePath)` is the literal string `"TRIP_DOCS"` (a relative directory created wherever the JVM's working
  directory is), not the intended `D:\cargo project\document`. Verified by reading
  `TripDocumentService.java` line 56 against `Clientservice.java`'s field names and the Liquibase seed
  data. The `update()`/`delete()` paths, by contrast, use a hardcoded local constant
  `UPLOAD_DIR = "./uploads/trip-documents/"` — a **third**, different path from either of the above — so a
  document's file can be created under `./TRIP_DOCS/...` but later "deleted"/looked-up under
  `./uploads/trip-documents/...`, which will not find the file.
- **WebSocket** (`config/WebSocketConfig.java`): `@EnableWebSocketMessageBroker`, simple broker on
  `/topic`, application prefix `/app`, STOMP endpoint `/ws` with SockJS fallback, CORS restricted to
  `http://localhost:4200` only (no production frontend origin allowed — see §10).

---

## 10. External Integrations

No third-party GPS/telematics provider integration was found anywhere in this module — `Grep` across
`VehicleLocationController`/`VehicleLocationImpl`/`VehicleLocationRepo` shows no references to any external
tracking SDK, webhook receiver, or provider name. GPS coordinates are pushed **into** the system by
whatever external device/mobile app calls `POST /api/v2/cargosetu/vehicle/location` — the API is a receiver
only, not a client of any GPS vendor **[inferred from absence]**.

The one real "integration" is internal: **WebSocket/STOMP push** via `SimpMessagingTemplate` — every saved
`VehicleLocation` is broadcast to `/topic/vehicle/{vehicleId}` (§3.4), which the frontend consumes via
`@stomp/stompjs`/`sockjs-client` (per `docs/services-and-known-issues.md`'s frontend summary). This
broker's allowed origin is hardcoded to `http://localhost:4200` in `WebSocketConfig.java` — no production
domain is whitelisted, meaning the live-tracking broadcast **[likely does not work against
`code-setu-app-ui.onrender.com` or any non-localhost frontend]** — flagged as **[unverified — would need
the actual deployed frontend's origin confirmed against this allow-list]**.

No `GenericNotificationEvent` publication was found anywhere in this module. `Grep` for
`GenericNotificationEvent|NotificationEvent|publishEvent|ApplicationEventPublisher` across
`src/main/java/com/cargosetu` returns hits only in `notification/**`, `customer/service/CustomerService.java`,
`service/OtpService.java`, `service/InquiryService.java`, and `service/StaffService.java` — **none** in
`TripService`, `TripDocumentService`, `TripExpenseService`, `TripStatusHistoryService`,
`DriverServiceImpl`, `VehicleServiceImpl`, `VehicleLocationImpl`, or
`DriverVehicleAssignmentServiceImpl`. So despite the notification engine existing and being wired up
elsewhere (customer creation, inquiries, OTP), **no trip lifecycle event (creation, status change,
assignment, GPS update) triggers any notification today** — this would be new integration work, not an
existing wire-up to document.

---

## 11. Important Business Rules & Validations

### Driver (`model/Driver.java`)

| Field | Constraint | Message |
|---|---|---|
| `name` | `@NotBlank`, `@Size(min=2, max=100)` | "Name must be between 2 and 100 characters" |
| `phoneNumber` | `@NotBlank`, `@Pattern(regexp = "^\\+?[0-9]{10,15}$")` | Optional leading `+`, then **10–15 digits** |
| `email` | `@NotBlank`, `@Email` | Standard email format |
| `licenseNumber` | `@NotBlank`, `@Column(unique = true)` | DB-level uniqueness only — no pre-check in service (see §6) |
| `licenseExpiryDate` | `@NotNull`, `@Future` | **Must be a future date** — a driver record cannot be created/persisted with an already-expired or today-dated license |
| `experienceYears` | `@Min(0)`, `@Max(60)` | Non-negative, capped at 60 |
| `address` | `@Size(max = 255)` | — |
| `status` | `@Pattern(regexp = "ACTIVE\|INACTIVE\|SUSPENDED")` | Free text otherwise rejected — but see §3.7, the assignment flow writes `"Assigned"`/`"Available"`, neither of which matches this pattern |
| `joiningDate` | `@PastOrPresent` | Cannot be a future date |
| `createdAt` / `updatedAt` | `@CreationTimestamp` / `@UpdateTimestamp`, `createdAt` non-updatable | Hibernate-managed |

**Important:** these constraints live on the **entity**, not the DTO, and the controller does not use
`@Valid` — so they are only enforced when Hibernate's Bean-Validation-on-persist integration runs at
`repository.save()` time (JPA's default `javax.persistence.validation.mode=AUTO` with Hibernate Validator
on the classpath), throwing a `jakarta.validation.ConstraintViolationException` that is *not* the same as
`CustomValidationException` handled specially by `GlobalExceptionHandler` — it falls through to the
generic `Exception` handler (§8), so validation failures come back as a bare, unstructured 400.

### Vehicle (`model/Vehicle.java`)

| Field | Constraint | Message |
|---|---|---|
| `chassisNumber` | `@NotBlank`, `@Column(unique=true, nullable=false)` | |
| `engineNumber` | `@NotBlank`, `@Column(unique=true, nullable=false)` | |
| `mileage` | `@PositiveOrZero` | |
| `registrationNumber` | `@NotBlank`, `@Size(min=5, max=20)`, `@Column(unique=true, nullable=false)` | |
| `vehicleType` | `@NotBlank` | Free text (comment suggests `TRUCK`/`VAN`/`LCV` but no enum/pattern enforced) |
| `model` / `manufacturer` | `@NotBlank` each | |
| `capacityKg` | `@NotNull`, `@Min(1)` | Must be at least 1 kg |
| `status` | `@Pattern(regexp = "AVAILABLE\|ON_TRIP\|MAINTENANCE\|OUT_OF_SERVICE")` | Same conflict with the assignment flow's `"Assigned"`/`"Available"` writes as Driver, above |
| `insuranceExpiryDate` | `@NotNull`, `@Future` | Must be future-dated — **cannot register a vehicle with expired/today's-date insurance** |
| `pollutionExpiryDate` | `@NotNull`, `@Future` | Same rule for pollution (PUC) certificate |
| `fuelType` | `@Pattern(regexp = "DIESEL\|PETROL\|ELECTRIC\|CNG")` | |
| `registrationDate` | `@PastOrPresent` | |
| `createdAt` / `updatedAt` | `@CreationTimestamp` / `@UpdateTimestamp` | |

Same enforcement caveat as Driver: entity-level only, triggered at `save()`, not at the DTO/controller
boundary (`VehicleDto` has no annotations, controller has no `@Valid`).

### DriverVehicleAssignment (`model/DriverVehicleAssignment.java`)

| Field | Constraint |
|---|---|
| `driver` | `@NotNull`, `@JoinColumn(nullable=false)` |
| `vehicle` | `@NotNull`, `@JoinColumn(nullable=false)` |
| `assignedAt` | `@NotNull`, `@PastOrPresent`, `@CreationTimestamp` (Hibernate sets it — the `@PastOrPresent` check is effectively unreachable since the value is always "now" at insert time) |
| `active` | `@NotNull`, defaults to `true` in the field initializer |
| `remarks` | `@Size(max=500)` |

### Other business rules confirmed in code

- **Trip status is a large, flat enum** (`TripStatus`: `CREATED, ASSIGNED, STARTED, IN_PROGRESS,
  COMPLETED, CANCELLED, DRIVER_ASSIGNED, VEHICLE_ASSIGNED, READY_FOR_DISPATCH, REACHED_PICKUP, LOADING,
  IN_TRANSIT, REACHED_DESTINATION, UNLOADING, CLOSED, ON_HOLD, BREAKDOWN, DELAYED`) — there is **no state
  machine / transition-validity check anywhere in `TripService`**; any status can be set from any other
  status via `updateTrip`, in any order, with only `STARTED`/`COMPLETED` triggering the timestamp
  side-effects described in §3.2.
- **`ExpenseType`** enum: `FUEL, TOLL, DRIVER_ALLOWANCE, DRIVER_SALARY, FOOD, HOTEL, REPAIR, MAINTENANCE,
  PARKING, PERMIT, CHALLAN, OTHER` — no per-type validation (e.g. no rule requiring a `receiptUrl`/
  `billNumber` for certain types).
- **`TripExpense.approved`/`approvedBy`/`approvedAt`** exist on the entity to model an approval workflow,
  but: (a) `TripExpenseDTO` omits `approvedAt` entirely (§4), and (b) no code anywhere sets `approvedBy` or
  `approvedAt` — `TripExpenseService.update()` only ever copies `dto.getApproved()` (a plain boolean) if
  present; there is no endpoint or logic that stamps who approved an expense or when. The approval
  workflow is effectively half-implemented (a boolean flag only, not a full audit).
- **`Trip.plannedEenTime`** (`model/Trip.java` line 30) is a typo for `plannedEndTime` — confirmed by
  direct read — and, as noted in §4, has no DTO/mapper path to ever be set from the API, making the field
  permanently `null` in practice.

---

## 12. Dependencies Between Modules

- **Customer/Shifting module** (`com.cargosetu.customer.entity.Shifting`, `customer/service/*`): `Trip`
  has a hard compile-time dependency via `@ManyToOne private Shifting shifting;` (`model/Trip.java` line
  3, 21) and `TripService` depends on `ShiftingRepository` to resolve `TripDTO.shiftingId` on
  create/update. This is a **one-way dependency** — nothing in the customer/shifting module references
  `Trip`, `Driver`, or `Vehicle` back (confirmed: `Shifting.java` and `CustomerService`/`ShiftingService`
  were not found to import anything from this module in the greps performed for §10). The frontend does
  join them conceptually (per `docs/services-and-known-issues.md`, the Angular `shifting-workspace` module
  has a `trip` sub-component), but that's a UI-level composition, not a backend one.
- **Notification engine** (`notification/**`): **no dependency exists today** — confirmed by direct grep,
  no `GenericNotificationEvent` is published anywhere in this module (§10). Any "trip created" / "driver
  assigned" / "document expiring soon" notification would be new work, not a reuse of an existing hook.
- **Document/File infrastructure** (`utils/utillity/FileUtility.java`, `service/ClientServices` via
  `repo/ClientServiceRepo`): `TripDocumentService` depends on both — `FileUtility` for
  disk read/write, and `ClientServiceRepo` for the (buggy, see §9) upload-path lookup. This is the same
  generic file-utility class used elsewhere in the app (e.g. company asset uploads), not something
  fleet/trip-specific.
- **Security/Auth** (`config.dto.LoggedInUser`): `TripController`/`TripService` depend on
  `@AuthenticationPrincipal LoggedInUser` to stamp `TripStatusHistory.changedBy` with the current
  username (`user.getUsername()`) on create/update. Driver/Vehicle/VehicleLocation/
  DriverVehicleAssignment flows **do not** take a `LoggedInUser` anywhere — none of their audit-ish fields
  (e.g. `DriverVehicleAssignment` has no `changedBy`-equivalent) capture *who* performed the action.
- **Nothing in this module depends on the Document module** (`com.cargosetu.document.*` —
  `DocumentMaster`/`DocumentUpload`) despite `TripDocument` looking conceptually similar; `TripDocument` is
  a fully separate, parallel implementation with its own upload logic rather than reusing
  `DocumentUploadService`.

---

## Summary of Verified Bugs / Quirks (quick reference)

1. `VehicleController` base path omits `BaseURL.BASE_URL` → lives at `/vehicle`, not
   `/api/v2/cargosetu/vehicle` (§2).
2. `VehicleLocationController`'s GET route doubles the `/location` segment (§2.7).
3. Driver, Vehicle, VehicleLocation, and DriverVehicleAssignment controllers have **zero**
   `@PreAuthorize`/permission checks, and no `DRIVER_*`/`VEHICLE_*` permission constants exist at all (§7).
4. `@EnableMethodSecurity(prePostEnabled=false)` makes the Trip-family `@PreAuthorize` annotations inert
   anyway, so in practice all 8 controllers are equally unprotected (§7).
5. `TripService.createTrip` can NPE on a null `status`, and — because the exception is swallowed inside a
   `@Transactional` method — the `Trip` row still commits while the API reports a 500 (§3.1).
6. `TripService.updateTrip`'s status-history logging never actually fires on a real status change (dead
   comparison logic), and can NPE (with the same silent-partial-commit issue) in an edge case (§3.2).
7. ~~`VehicleLocationRepo.findLatestLocationToday` filters on `t.id` instead of `t.vehicle_id` — effectively
   broken for any fleet with more than one location row (§3.4).~~ **Fixed 2026-09-27** — now filters on
   `t.vehicle_id`. (The frontend `VehicleLocationService` also called the wrong GET path; fixed to
   `/vehicle/location/{id}/location`.)
8. `DriverVehicleAssignmentServiceImpl.assignDriver` only deactivates the driver's prior assignments, not
   the vehicle's — a vehicle can end up double-assigned (§3.7).
9. `assignDriver`/`unassign` write free-text `"Assigned"`/`"Available"` into `Driver.status`/
   `Vehicle.status`, which don't match either entity's own `@Pattern` constraint (§3.7, §11).
10. `TripDocumentService.create()` reads the wrong field (`getName()` instead of `getValue()`) from the
    `Clientservice` config row, so uploads don't go to the intended configured directory; `update()`/
    `delete()` use yet a third, different hardcoded path (§9).
11. ~~`Trip.plannedEenTime` (typo for `plannedEndTime`) has no DTO/mapper path — permanently unreachable
    (§4, §11).~~ **Fixed 2026-09-27** — exposed as `TripDTO.plannedEndTime` (mapped both ways, applied by
    `updateTrip`). The entity/column name is unchanged.
12. `TripExpense` approval fields (`approvedBy`, `approvedAt`) are never populated by any code path; the
    DTO doesn't even include `approvedAt` (§11).
13. `TripTracking` and `DriverAdvance` entities exist with no repository/service/controller anywhere —
    dead/stub code (§1, §5).
14. `DriverVehicleAssignmentController` hardcodes the unassign reason to the literal string
    `"discreption"` regardless of caller input, and the corresponding service parameter is never read
    (§2.8, §3.7).

---

## 13. Planned Route (Google Maps) — added 2026-09-27

Implements `CargoSetu_Google_Maps_Trip_Route_Implementation_Plan.docx` (MVP + stops). A **planned general
driving route**. It is not live tracking and not truck-compliant routing.

### Where the data lives
- Trip origin/destination come from the linked **Shifting** (`shiftFromCity`/`fromAddress`,
  `shiftToCity`/`toAddress`). Trip itself has no address fields.
- Confirmed coordinates are stored on `tblm_shifting` (`from_latitude`, `from_longitude`,
  `from_location_source`, `from_location_confirmed_at`, `from_location_confirmed_by`, and the same `to_*`
  columns) and on `tblm_trip_stop` (`latitude`, `longitude`, `location_source`). Liquibase:
  `Changelog/trip-route.changelog.xml`. All columns are nullable and there is **no backfill**.
- `ShiftingService.updateShifting` clears a confirmed coordinate when its address or city changes, unless
  new coordinates are sent in the same update.
- Nothing returned by Google is persisted. `RouteCache` reuses an identical coordinate sequence in memory
  (`app.maps.route-cache-ttl-seconds`, default 900; set it to 0 to disable). Changing any point changes
  the cache key.

### Endpoints (`maps/controller/TripRouteController`, base `/api/v2/cargosetu/trip`)
| Method | Endpoint | Permission | Description |
|---|---|---|---|
| GET | `/{tripId}/route?refresh=false` | `TRIP_READ` | Returns a normalized `TripRouteDTO` (source/destination/stops, `distanceMeters`, `durationSeconds`, `encodedPolyline`, `routeStatus`) |
| POST | `/{tripId}/route/geocode` body `{query}` | `TRIP_UPDATE` | Returns up to 5 Geocoding candidates, each flagged `approximate` for city-level results. **Saves nothing** |
| PUT | `/{tripId}/route/location` body `{endpoint: SOURCE\|DESTINATION\|STOP, tripStopId?, latitude, longitude, locationSource}` | `TRIP_UPDATE` | Staff confirmation of one point. Addresses are never overwritten |

`responseCode` / `routeStatus` values (the HTTP status is always 200, per the envelope convention):
- `200 READY`
- `422 MISSING_LOCATION` (includes `missingEndpoints`)
- `422 NO_ROUTE`
- `503 RATE_LIMITED`, `503 PROVIDER_UNAVAILABLE`, `503 NOT_CONFIGURED` (these include a `referenceId`, which is the `X-Request-Id` / log MDC `requestId`)
- `404`: the trip is missing or belongs to another company. The two cases look identical so the endpoint doesn't reveal which trips exist.

### Provider calls
- **`GoogleRoutesClient`** calls `computeRoutes` with DRIVE, `TRAFFIC_UNAWARE`, no alternatives, `METRIC` units and `regionCode=IN`.
  - Field mask: `routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline`, plus leg fields when stops are present.
  - Only stops with a confirmed coordinate are used, up to 25. The rest are reported as `stopsSkipped`.
- **`GoogleGeocodingClient`** is used for suggestions only.
- **Timeouts and retries:** connect 3 s and read 8 s (`app.maps.*`). There is one retry, and only for timeouts and 5xx errors. 4xx errors are never retried.
- **Server key:** `app.maps.server-api-key=${GOOGLE_MAPS_SERVER_API_KEY:}`. It is separate from the browser key. If it is missing, the endpoint returns `NOT_CONFIGURED`. The key is never logged or returned.

### Company boundary
`service/TripAccessPolicy` is now enforced in `TripService` (get, list, update and delete) and in the route
service. A trip's owner is `Trip.companyId`, falling back to `Shifting.companyId`. Staff id 1 (super admin)
sees everything. Trips with no owner at all stay visible, as they were before. `createTrip` now sets
`companyId`. `TripCapacityController` (capacity, stops) and `TripShipmentController` are **not yet**
scoped.

### Frontend (`code-setu-app-ui`)
- **`services/google-maps-loader.service.ts`** is the single loader for the Maps JS script, using the browser key from `env.js`. It reports rejected keys through `authFailed$`. `DriverLocationComponent` now uses it.
- **`component/trip-route/trip-route-section.component`** is the "Trip Route & Location" section in the Trip details panel.
  - It shows the loading, missing-location, no-route, error+retry and map-failure states.
  - Staff confirm a location by searching and picking a candidate, or by placing a pin on the map. Nothing is saved until they confirm.
  - It also lists the stops.
- **`component/trip-route/trip-route-map.component`** renders the map: A/B/stop markers, the decoded polyline, and fit-to-bounds.
- The Trip list has a new "View Route" action.


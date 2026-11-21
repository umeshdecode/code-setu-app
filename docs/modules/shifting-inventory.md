# Shifting Inventory Module

> Source root: `src/main/java/com/cargosetu/shifting/` (`entity`, `controller`, `service`, `repository`, `dtos`,
> `mapper`). One outside dependency worth flagging up front: `com.cargosetu.customer.entity.PackagingMaterial`
> (lives in the **customer** module's entity package, not `shifting`) and
> `com.cargosetu.customer.entity.ShiftItemDamage` (also in `customer.entity`, points *into* this module).

All claims below were verified by reading the source files directly on 2026-08-09. Anything not directly
observed in code is explicitly flagged **Unverified/Inferred**.

---

## 1. Overview

The Shifting Inventory module is the per-item packing/loading/unloading tracker for a single `Shifting`
(moving job — see [`customer-shifting.md`](./customer-shifting.md)). Each row is one physical item or box
being moved (e.g. "Sofa" in "Living Room"), and it carries three independent boolean flags —
`packed`, `loaded`, `unloaded` — that an operator flips as the item moves through the physical workflow.

Three entities make up the module:

- **`ShiftInventoryItem`** (`shift_inventory_item`) — the item record itself: room, name, category, a unique
  package number, condition/remarks, and the three status flags. Belongs to exactly one `Shifting`.
- **`ShiftInventoryPackaging`** (`shift_inventory_packaging`) — a line item recording how much of a given
  `PackagingMaterial` (e.g. "Bubble wrap", "Carton box") was used to pack one `ShiftInventoryItem`.
- **`ShiftInventoryPhoto`** (`shift_inventory_photo`) — a photo record (URL + type, e.g.
  `before_packing`/`after_packing`/`damage`) attached to one `ShiftInventoryItem`.

Only one entity in the module has a controller/service/exposed API: `ShiftInventoryItem`, via
`ShiftInventoryController` / `ShiftInventoryService`. `ShiftInventoryPackaging` and `ShiftInventoryPhoto`
have **no controllers and no dedicated services** — they are only ever created, read, or replaced as nested
collections inside a `ShiftInventoryItemDTO` payload (see §3 and §6). Their repositories
(`ShiftInventoryPackagingRepository`, `ShiftInventoryPhotoRepository`) exist but are **not referenced anywhere
in the codebase** outside their own file — confirmed by grepping the full `src/main/java` tree; they are
unused, dead scaffolding today.

---

## 2. API Controllers & Endpoints

**Controller:** `shifting/controller/ShiftInventoryController.java`
**Base mapping:** `BaseURL.BASE_URL` (`/api/v2/cargosetu`, `utils/BaseURL.java:4`) +
`UrlConstants.ShiftingUrlConstants.SHIFT_INVENTORY` (`/shift-inventory`,
`config/utils/UrlConstants.java:27`) → **`/api/v2/cargosetu/shift-inventory`**

| Method | Path | Handler | `@PreAuthorize` | Request | Response |
|---|---|---|---|---|---|
| POST | `/shift-inventory` | `createShiftInventoryItem` | `SHIFT_INVENTORY_CREATE` | `ShiftInventoryItemDTO` body | `GenericDataDTO`, `CREATED` (201) on success; 500 body on exception (caught in controller, `ResponseEntity.internalServerError()`) |
| GET | `/shift-inventory/{itemId}` | `getShiftInventoryItem` | `SHIFT_INVENTORY_READ` | path var `itemId` | `GenericDataDTO` with `ShiftInventoryItemDTO` in `data`; `NOT_FOUND` (404 code, HTTP 200) if missing |
| PUT | `/shift-inventory/{itemId}` | `updateShiftInventoryItem` | `SHIFT_INVENTORY_UPDATE` | path var `itemId` + `ShiftInventoryItemDTO` body | `GenericDataDTO` — full-record replace, see §3 |
| PATCH | `/shift-inventory/{itemId}/status` | `updateShiftInventoryItemStatus` | `SHIFT_INVENTORY_STATUS_UPDATE` | path var `itemId` + **query params** `packed`, `loaded`, `unloaded` (all `Boolean`, `required = false`) | `GenericDataDTO` with the updated `ShiftInventoryItemDTO` |

**No `GET` list/search endpoint** (e.g. "all inventory items for shift X") and **no `DELETE` endpoint** exist
on this controller — confirmed by reading the full 93-line controller file; there are exactly these four
handlers. A caller wanting all items for a shift has no dedicated endpoint to call (see §10 gaps).

### PATCH `/status` — exact semantics (read from `ShiftInventoryController.java:74-92` and
`ShiftInventoryService.updateItemStatus`, lines 176-201)

- All three query params are **optional** (`required = false`) and each is applied **independently**:
  `if (packed != null) item.setPacked(packed);` — same pattern for `loaded` and `unloaded`.
- A param that is **omitted** from the request leaves that flag **unchanged** on the entity (it does not
  reset to `false` or `null`).
- Any combination is accepted in a single call — e.g. `?packed=true&loaded=true&unloaded=true` all in one
  request is allowed, and so is flipping a flag back to `false` after it was `true` (e.g.
  `?loaded=false`) or setting `unloaded=true` while `packed`/`loaded` are still `false`.
- **There is no ordering/sequencing validation** — see §9 for the precise "no enforced order" finding.

Every handler in the controller wraps its call in `try { ... } catch (Exception e)` and on failure returns
`ResponseEntity.internalServerError().body(response)` with `response.setErrors(new Object[]{e.getMessage()})`
— i.e. it leaks the raw exception message to the client (same pattern flagged for `ShiftingController` in
`customer-shifting.md`).

---

## 3. Business / Service Logic

**Service:** `shifting/service/ShiftInventoryService.java` — a single concrete `@Service` class (no
interface, no `Impl` split — unlike most of the rest of the codebase's `XService`/`XServiceImpl` pattern per
`docs/services-and-known-issues.md`). Depends on `ShiftInventoryItemRepository`,
`ShiftInventoryItemMapper`, and `ShiftingRepository` (from `com.cargosetu.repo`) — **it does not inject or
use `ShiftInventoryPackagingRepository` or `ShiftInventoryPhotoRepository` at all**; those sub-entities are
persisted purely via JPA cascade off `ShiftInventoryItem` (see §6).

### `createShiftInventoryItem(ShiftInventoryItemDTO)` (lines 34-81)

1. Rejects with `VALIDATION_ERROR` (422) if `shiftId` is `null`.
2. Looks up the parent `Shifting` by `shiftId` via `shiftingRepository.findById(...)`; returns `NOT_FOUND`
   (404) if it doesn't exist.
3. Maps the DTO to a `ShiftInventoryItem` entity via `ShiftInventoryItemMapper.toEntity`, then sets
   `item.setShifting(...)` to the loaded parent.
4. Manually re-parents every nested `ShiftInventoryPhoto`/`ShiftInventoryPackaging` back to `item` (
   `photo.setInventoryItem(item)`, `pkg.setInventoryItem(item)`) — necessary because the MapStruct-generated
   nested mapping does not set the owning-side back-reference.
5. Defensively forces `packed`/`loaded`/`unloaded` to `false` if the mapper left them `null` (belt-and-braces;
   the entity column itself is `nullable = false` with a `false` default).
6. Saves via `shiftInventoryItemRepository.save(item)` — cascades to `ShiftInventoryPhoto` and
   `ShiftInventoryPackaging` rows (see §6). Returns `CREATED` (201) with the saved item mapped back to DTO.

### `getShiftInventoryItem(Long itemId)` (lines 83-101)

Plain `findById` → `NOT_FOUND` (404) or `SUCCESS` (200) wrapping the mapped DTO. No error/exception maps to
anything more specific than a generic 500.

### `updateShiftInventoryItem(Long itemId, ShiftInventoryItemDTO)` (lines 103-173)

This is a **full replace**, not a partial patch, and it does **not** use the mapper's generated
`updateFromDto` (which exists on `ShiftInventoryItemMapper` via `BaseUpdateMapper` but is never called here):

1. 404 if the item doesn't exist.
2. Maps the incoming DTO to a **new** transient entity (`updatedItem`), then forces its `id` to the path's
   `itemId`.
3. If `itemDTO.shiftId` is provided and differs from the existing item's shift, re-resolves and reassigns the
   parent `Shifting` (404 if the new shift doesn't exist); otherwise keeps the existing shift unchanged.
4. **Clears and fully replaces** the nested collections: `existingItem.getShiftInventoryPhotos().clear()`
   then re-adds everything from `updatedItem`'s photos (with `setInventoryItem(existingItem)` re-parented);
   identical pattern for `shiftInventoryPackagings`. Because the entity's `@OneToMany` is
   `orphanRemoval = true` (§6), this means **any photo or packaging row not present in the update payload is
   permanently deleted** on save — there is no merge/upsert-by-id semantics for the nested lists. A caller
   that PUTs an item with an empty/omitted `photos` array will delete all of that item's existing photos.
5. Copies the simple scalar fields (`roomName`, `itemName`, `category`, `packageNumber`, `remarks`,
   `conditionStatus`, `packed`, `loaded`, `unloaded`) from `updatedItem` onto `existingItem` one by one.
6. Saves and returns `SUCCESS` (200) with the mapped DTO.

### `updateItemStatus(Long itemId, Boolean packed, Boolean loaded, Boolean unloaded)` (lines 176-201)

See §2 for exact per-flag semantics. 404 if the item doesn't exist; otherwise applies each non-null flag
independently and saves. **No cross-field validation of any kind** — see §9.

---

## 4. DTOs / Request-Response Models

All four DTOs are plain `@Data` (Lombok) POJOs with no bean-validation annotations (`@NotNull`, `@NotBlank`,
etc.) anywhere — all "required" checks are enforced imperatively in the service (e.g. the `shiftId` null
check in `createShiftInventoryItem`), not declaratively.

| DTO | File | Fields | Notes |
|---|---|---|---|
| `ShiftInventoryItemDTO` | `shifting/dtos/ShiftInventoryItemDTO.java` | `id`, `shiftId`, `roomName`, `itemName`, `category`, `packageNumber`, `remarks`, `conditionStatus`, `packed`, `loaded`, `unloaded`, `photos: List<ShiftInventoryPhotoDTO>`, `packagingDetails: List<ShiftInventoryPackagingDTO>` | The request/response shape for all four controller endpoints. `photos`/`packagingDetails` are the nested nested-create/nested-replace vehicles described in §3. |
| `ShiftInventoryPackagingDTO` | `shifting/dtos/ShiftInventoryPackagingDTO.java` | `id`, `inventoryItemId`, `packagingMaterialId`, `quantityUsed`, `materialName` (read-only, populated from the joined `PackagingMaterial`), `unit` (read-only, same) | `materialName`/`unit` are only ever populated on the way out (`toDto`); nothing reads them back on `toEntity`. |
| `ShiftInventoryPhotoDTO` | `shifting/dtos/ShiftInventoryPhotoDTO.java` | `id`, `inventoryItemId`, `photoUrl`, `photoType` | `photoUrl` is a plain string the client supplies — see §8, there is no server-side upload endpoint in this module. |
| `PackagingMaterialDTO` | `shifting/dtos/PackagingMaterialDTO.java` | `id`, `materialName`, `unit`, `active` | Mirrors `customer.entity.PackagingMaterial`. Mapped via `shifting/mapper/PackagingMaterialMapper.java` but — confirmed by grep across `src/main/java` — **there is no `PackagingMaterialController` or `PackagingMaterialService` anywhere in the codebase.** This DTO is only ever populated as the nested `materialName`/`unit` fields inside `ShiftInventoryPackagingDTO`; there is no way to create/list/update packaging materials through the API. Rows must be inserted directly into `tblm_packaging_material` (e.g. by DB seed/migration) before they can be referenced by `packagingMaterialId`. |

### Mappers (MapStruct, `@Mapper(config = GlobalMapperConfig.class, ...)`, `shifting/mapper/`)

`GlobalMapperConfig` (`config/GlobalMapperConfig.java`) sets `componentModel = "spring"` and
`NullValuePropertyMappingStrategy.IGNORE` for all mappers project-wide.

| Mapper | Key `@Mapping`s |
|---|---|
| `ShiftInventoryItemMapper` (uses `ShiftInventoryPhotoMapper`, `ShiftInventoryPackagingMapper`) | `shifting.shiftingId` ↔ `shiftId`; `shiftInventoryPhotos` ↔ `photos`; `shiftInventoryPackagings` ↔ `packagingDetails` |
| `ShiftInventoryPackagingMapper` (uses `PackagingMaterialMapper`) | `inventoryItem.id` ↔ `inventoryItemId`; `packagingMaterial.materialId` ↔ `packagingMaterialId`; `packagingMaterial.materialName` → `materialName` (one-way, `toDto` only); `packagingMaterial.unit` → `unit` (one-way, `toDto` only). Also has an explicit `updateFromDto` (`@InheritConfiguration(name = "toEntity")`) — unused by the service today. |
| `ShiftInventoryPhotoMapper` | `inventoryItem.id` ↔ `inventoryItemId` |

All three extend `BaseUpdateMapper<D,E>` (`toEntity`/`toDto`/`updateFromDto`), but only `ShiftInventoryItemMapper.toEntity`/`toDto` and `ShiftInventoryPackagingMapper.updateFromDto` are actually invoked anywhere in the service layer (confirmed by reading `ShiftInventoryService.java` — it calls only `toEntity`/`toDto`).

---

## 5. Entities & Database Relationships

| Entity | Table | File |
|---|---|---|
| `ShiftInventoryItem` | `shift_inventory_item` | `shifting/entity/ShiftInventoryItem.java` |
| `ShiftInventoryPackaging` | `shift_inventory_packaging` | `shifting/entity/ShiftInventoryPackaging.java` |
| `ShiftInventoryPhoto` | `shift_inventory_photo` | `shifting/entity/ShiftInventoryPhoto.java` |

No `@Table`-level index/constraint annotations beyond what's listed below; tables are Hibernate
auto-DDL-created (`spring.jpa.hibernate.ddl-auto=update`), not Liquibase-managed — consistent with the
project-wide convention noted in `docs/services-and-known-issues.md`.

### `ShiftInventoryItem` (`shift_inventory_item`)

| Field | Column / Type | Constraints |
|---|---|---|
| `id` | PK, `IDENTITY` | — |
| `shifting` | `shift_id` (FK → `tblm_shifting.shifting_id`) | `@ManyToOne(fetch = LAZY)`, `nullable = false` — **many items belong to one `Shifting`** |
| `roomName` | `String` | `nullable = false` |
| `itemName` | `String` | `nullable = false` |
| `category` | `String` | nullable |
| `packageNumber` | `String` | `@Column(unique = true)` — **unique across the entire table**, not just per-shift (comment in source confirms intent: "Package number should be unique") |
| `remarks` | `String` | nullable |
| `conditionStatus` | `String` | `nullable = false` — free string, e.g. "Good", "Minor Dent", "Scratch" (comment in source; not an enum) |
| `packed` | `Boolean` | `nullable = false`, default `false` |
| `loaded` | `Boolean` | `nullable = false`, default `false` |
| `unloaded` | `Boolean` | `nullable = false`, default `false` |
| `shiftInventoryPhotos` | `List<ShiftInventoryPhoto>` | `@OneToMany(mappedBy = "inventoryItem", cascade = ALL, orphanRemoval = true)` — **one item has many photos** |
| `shiftInventoryPackagings` | `List<ShiftInventoryPackaging>` | `@OneToMany(mappedBy = "inventoryItem", cascade = ALL, orphanRemoval = true)` — **one item has many packaging-material line items** |

### `ShiftInventoryPackaging` (`shift_inventory_packaging`)

| Field | Column / Type | Constraints |
|---|---|---|
| `id` | PK, `IDENTITY` | — |
| `inventoryItem` | `inventory_item_id` (FK → `shift_inventory_item.id`) | `@ManyToOne(fetch = LAZY)`, `nullable = false` — **many packaging rows belong to one item** (owning side of the item's `@OneToMany` above) |
| `packagingMaterial` | `packaging_material_id` (FK → `tblm_packaging_material.material_id`) | `@ManyToOne(fetch = LAZY)`, `nullable = false` — many packaging-usage rows can reference the same `PackagingMaterial` |
| `quantityUsed` | `Double` | `nullable = false` |

### `ShiftInventoryPhoto` (`shift_inventory_photo`)

| Field | Column / Type | Constraints |
|---|---|---|
| `id` | PK, `IDENTITY` | — |
| `inventoryItem` | `inventory_item_id` (FK → `shift_inventory_item.id`) | `@ManyToOne(fetch = LAZY)`, `nullable = false` — **many photos belong to one item** |
| `photoUrl` | `String` | `nullable = false` |
| `photoType` | `String` | nullable — free string, e.g. `before_packing`/`after_packing`/`damage` (comment in source; not an enum) |

### Cardinality summary

- `Shifting` **1 : N** `ShiftInventoryItem` (unidirectional from the item side only — `Shifting` has no
  `@OneToMany` back to `ShiftInventoryItem`; confirmed by reading `customer/entity/Shifting.java` in full,
  it has no such field).
- `ShiftInventoryItem` **1 : N** `ShiftInventoryPhoto` (bidirectional, cascade `ALL` + `orphanRemoval`).
- `ShiftInventoryItem` **1 : N** `ShiftInventoryPackaging` (bidirectional, cascade `ALL` + `orphanRemoval`).
- `ShiftInventoryPackaging` **N : 1** `PackagingMaterial` (`com.cargosetu.customer.entity.PackagingMaterial`,
  table `tblm_packaging_material` — this entity lives in the **customer** module package, not `shifting`,
  despite being conceptually part of this module's data model).
- **External, inbound-only relation**: `com.cargosetu.customer.entity.ShiftItemDamage`
  (`customer/entity/ShiftItemDamage.java`) has `@ManyToOne @JoinColumn(name = "item_id")` pointing at
  `ShiftInventoryItem`, but `ShiftInventoryItem` has **no back-reference field to `ShiftItemDamage`** — it's
  unidirectional, item-agnostic from this module's point of view. `ShiftItemDamage` itself has **no
  controller, service, or repository anywhere in the codebase** (confirmed by grep) — it is entity-only, dead
  code today. Damage is instead recorded as free text on `ShiftInventoryItem.conditionStatus`/`remarks` in
  practice, per the current API surface.

---

## 6. Repositories

| Repository | File | Extends | Custom query methods |
|---|---|---|---|
| `ShiftInventoryItemRepository` | `shifting/repository/ShiftInventoryItemRepository.java` | `JpaRepository<ShiftInventoryItem, Long>` | **None** — empty interface body, inherits only standard `JpaRepository` CRUD (`findById`, `save`, `findAll`, `deleteById`, etc.) |
| `ShiftInventoryPackagingRepository` | `shifting/repository/ShiftInventoryPackagingRepository.java` | `JpaRepository<ShiftInventoryPackaging, Long>` | **None** — and, per §1, **never injected/used anywhere** in the service layer; packaging rows are only persisted transitively via `ShiftInventoryItem`'s cascade |
| `ShiftInventoryPhotoRepository` | `shifting/repository/ShiftInventoryPhotoRepository.java` | `JpaRepository<ShiftInventoryPhoto, Long>` | **None** — same as above, unused directly; photo rows are only persisted transitively via cascade |

No pagination, no `findByShiftingId(Long)`/`findByShifting_ShiftingId(...)`-style query exists — this is
consistent with §2's finding that there is no list-by-shift endpoint. Fetching all inventory items for a
given `Shifting` today would require adding a new repository method; it does not exist.

---

## 7. Security & Authentication

Permission constants (`utils/PermissionConstants.java:57-61`), all prefixed `PERM_` at runtime via
`PermissionConstants.PERM_PREFIX`:

| Constant | DB permission code | Used on |
|---|---|---|
| `SHIFT_INVENTORY_CREATE` | `SHIFT_INVENTORY_CREATE` | `POST /shift-inventory` |
| `SHIFT_INVENTORY_READ` | `SHIFT_INVENTORY_READ` | `GET /shift-inventory/{itemId}` |
| `SHIFT_INVENTORY_UPDATE` | `SHIFT_INVENTORY_UPDATE` | `PUT /shift-inventory/{itemId}` |
| `SHIFT_INVENTORY_STATUS_UPDATE` | `SHIFT_INVENTORY_STATUS_UPDATE` | `PATCH /shift-inventory/{itemId}/status` |

There is **no `SHIFT_INVENTORY_DELETE`** constant — consistent with there being no delete endpoint on the
controller. All four `@PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).SHIFT_INVENTORY_*)")`
annotations were confirmed by direct read of `ShiftInventoryController.java`.

**Critical caveat, project-wide, confirmed independently for this module**: `MethodSecurityConfig.java`
(`config/MethodSecurityConfig.java:7`) declares `@EnableMethodSecurity(prePostEnabled = false)`. This
disables Spring Security's `@PreAuthorize` processing globally, which means **every `@PreAuthorize` on
`ShiftInventoryController` is silently inert** — any authenticated (JWT-bearing) user can currently call any
of the four endpoints regardless of which permissions their role actually has. This mirrors the finding
already documented project-wide in `docs/services-and-known-issues.md` §4/§5.2; it is not specific to this
module but does apply fully to it.

No company/tenant scoping check exists anywhere in `ShiftInventoryService` — it does not check `companyId`
on the resolved `Shifting`, nor does it check that the requesting user's company matches the item's shift.
Any authenticated user who knows/guesses an `itemId` can read or modify any company's inventory item.
**Not verified against the DB whether `companyId` is even reachable from `ShiftInventoryItem`** — it isn't
column-mapped on the entity itself; it would need to be read via `item.getShifting().getCompanyId()`, which
the service never does.

---

## 8. Exception Handling

The controller catches `Exception` per-handler and returns `ResponseEntity.internalServerError()` (HTTP 500)
with `GenericDataDTO.errors = [e.getMessage()]` — i.e. the raw exception message is returned to the client
verbatim (same pattern as `ShiftingController`, see `customer-shifting.md`).

Inside the service, most failure paths are handled explicitly with `GenericDataDTO` + `CommonStatusCode`
(`VALIDATION_ERROR` 422, `NOT_FOUND` 404, `SUCCESS`/`CREATED` 200/201) rather than thrown exceptions — these
never reach the global handler; they're returned as HTTP 200 with the "real" status embedded in the JSON
body's `responseCode` field. Only genuinely unexpected exceptions (e.g. a DB constraint violation from the
unique `packageNumber`) fall into the service's own `catch (Exception e)` block, which returns
`INTERNAL_SERVER_ERROR` (500 *code*, but still wrapped as an HTTP 200 from the service — the controller's own
try/catch only fires if the *service itself* throws past its internal catch, which given the service's
blanket `catch (Exception e)` should be rare in practice).

No exception from this module is specific to it — there is no `ShiftInventoryException` or similar; the
project's shared `handler/GlobalExceptionHandler.java` (`@RestControllerAdvice`) would only ever be reached
by something the service's own broad try/catch didn't intercept (unlikely by construction). Per
`docs/services-and-known-issues.md`, that global handler always returns HTTP 400 for both its handled
exception types regardless of the logical error — not independently re-verified here since this module's own
try/catch blocks make it unlikely to be hit.

---

## 9. Configuration

No module-specific `application*.properties` keys, `@ConfigurationProperties`, or `@Value` injections exist
anywhere under `com.cargosetu.shifting` — confirmed by grep for `@Value`/`ConfigurationProperties` in the
package (no matches). The module relies entirely on the global JPA/Hibernate config
(`spring.jpa.hibernate.ddl-auto=update`) and the global `GlobalMapperConfig`/`MethodSecurityConfig` described
above. No module-specific Liquibase changelog exists (`Changelog/main.xml` only wires in
`changelog-master.xml` and `changelog-templete.xml` per `docs/services-and-known-issues.md` — neither touches
`shift_inventory_*` tables); the three tables are created purely by Hibernate auto-DDL from the `@Entity`
classes.

---

## 10. External Integrations

**Photo storage: there is none in this module.** `ShiftInventoryPhoto.photoUrl` is a plain `String` column
populated directly from whatever value the client sends in `ShiftInventoryPhotoDTO.photoUrl` — there is
**no multipart upload endpoint, no file-storage service call, and no validation that the URL points anywhere
real** in `ShiftInventoryController`, `ShiftInventoryService`, or any mapper. This is a different pattern
from the codebase's actual file-upload flows (e.g. `document/service/DocumentUploadService`,
`CompanyService`'s asset upload — both handle real multipart file persistence per
`docs/services-and-known-issues.md`); this module assumes some other client-side or out-of-band process
(e.g. direct upload to cloud storage from the mobile/web app) produces the URL before it's attached here.
**Unverified**: which actual storage backend (if any) the frontend uploads to before calling this API — not
discoverable from the backend source alone.

No other outbound integrations (no notification events published, no third-party SDK calls) exist in this
module — confirmed by reading `ShiftInventoryService.java` in full; it only touches its own repository, its
own mapper, and `ShiftingRepository`.

---

## 11. Important Business Rules & Validations

Verified directly from `ShiftInventoryService.java` and `ShiftInventoryController.java`:

1. **`shiftId` is required at create time** — `createShiftInventoryItem` returns `VALIDATION_ERROR` (422) if
   `itemDTO.getShiftId() == null`, and `NOT_FOUND` (404) if that `Shifting` doesn't exist. No such check
   exists for `roomName`/`itemName`/`conditionStatus` at the service layer despite those columns being
   `nullable = false` in the DB — an invalid create request would surface as a DB constraint failure caught
   by the service's generic `catch (Exception e)` → `INTERNAL_SERVER_ERROR`, not a clean validation message.
2. **`packageNumber` uniqueness is enforced only at the DB level** (`@Column(unique = true)`), not
   pre-checked in the service. A duplicate `packageNumber` will throw a `DataIntegrityViolationException`
   (or similar) at `save()` time, caught by the service's blanket `catch` and returned as a generic 500 —
   there is no dedicated "duplicate package number" (409 `CONFLICT`) response path.
3. **No enforced ordering between `packed` → `loaded` → `unloaded`.** Read directly from
   `updateItemStatus` (`ShiftInventoryService.java:176-201`): each flag is set independently and
   unconditionally when present in the request —
   ```java
   if (packed != null) item.setPacked(packed);
   if (loaded != null) item.setLoaded(loaded);
   if (unloaded != null) item.setUnloaded(unloaded);
   ```
   There is **no check that `packed == true` before allowing `loaded = true`**, and no check that
   `loaded == true` before allowing `unloaded = true`. A single PATCH call can set `unloaded=true` while
   `packed` and `loaded` are still `false`/unset, and this is accepted without error. **This confirms there
   is no state-machine/workflow enforcement of the packed → loaded → unloaded lifecycle anywhere in the
   backend** — any ordering shown in a UI (e.g. disabling "Mark loaded" until "packed" is checked) is a
   frontend-only convention, not a backend-enforced rule.
4. **Full-replace semantics on `PUT`, with destructive nested-collection replacement.** As detailed in §3,
   updating an item via `PUT` clears and fully replaces its `shiftInventoryPhotos` and
   `shiftInventoryPackagings` lists based on whatever is present in the request body — omitting a
   previously-saved photo/packaging entry from the update payload deletes it (`orphanRemoval = true`).
   There is no per-sub-item "add one photo" or "remove one photo" endpoint; the only way to add a photo is a
   full `PUT` of the parent item carrying the complete desired photo list.
5. **No tenant/company scoping** — see §7. Any authenticated caller can act on any company's inventory item
   by ID, and (per §7) `@PreAuthorize` is currently inert project-wide regardless.
6. **`conditionStatus` and `photoType` are free-text strings, not enums** — confirmed by the `// e.g. "..."`
   comments in the entity source (`ShiftInventoryItem.java:37`, `ShiftInventoryPhoto.java:22`); the backend
   does not restrict these to a fixed set of values (e.g. `"Good"`/`"Minor Dent"`/`"Scratch"` for condition,
   or `"before_packing"`/`"after_packing"`/`"damage"` for photo type are documented examples only, not an
   enforced enum or `CHECK` constraint).

---

## 12. Dependencies Between Modules

- **Hard dependency on `customer-shifting`** (see [`customer-shifting.md`](./customer-shifting.md)): every
  `ShiftInventoryItem` requires an existing `Shifting` (`com.cargosetu.customer.entity.Shifting`,
  `tblm_shifting`), resolved via `com.cargosetu.repo.ShiftingRepository` (injected directly into
  `ShiftInventoryService` — this module reaches across package boundaries into the top-level `repo` package
  rather than a `customer`-scoped one). There is no cascade delete or lifecycle coupling beyond the FK: the
  `Shifting` entity has no `@OneToMany` back to its inventory items, so deleting/completing a `Shifting` has
  no automatic effect on its `ShiftInventoryItem` rows (not independently tested — inferred from the
  one-directional mapping).
- **Dependency on the `customer` module's `PackagingMaterial` lookup table**
  (`com.cargosetu.customer.entity.PackagingMaterial`, `tblm_packaging_material`) via
  `ShiftInventoryPackaging.packagingMaterial`. Per `customer-shifting.md` §5, `PackagingMaterial` itself has
  no relationship back to `Shifting`/`Customer` — it's a standalone catalog table this module is the only
  confirmed consumer of (no controller manages it, per §4 above; rows must pre-exist for
  `packagingMaterialId` references to resolve).
- **Inbound-only dependency from `ShiftItemDamage`** (`customer.entity`, see §5) — dead code today (no
  controller/service), but if it were ever wired up it would read `ShiftInventoryItem` rows without this
  module needing any changes (unidirectional `@ManyToOne`).
- **No dependency in the other direction**: nothing in `shifting.*` is imported by `customer.*` except via
  the `ShiftItemDamage`/`PackagingMaterial` associations already listed — this module is a "leaf" consumer of
  the customer-shifting domain, not a dependency of it.
- **No dependency on the Trip module, notification engine, or document-upload services** — confirmed by
  reading `ShiftInventoryService.java`/`ShiftInventoryController.java` in full; no imports from
  `com.cargosetu.notification.*`, `com.cargosetu.document.*`, or `com.cargosetu.*.trip*` packages.

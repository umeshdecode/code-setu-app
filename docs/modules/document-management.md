# Document Management Module

## Overview

The Document Management module handles two distinct concerns that live in different parts of the codebase:

1. **Document type catalog + tracked uploads** (`com.cargosetu.document.*`) — a master list of document types (`DocumentMaster`, e.g. AADHAAR, PAN, DL, PASSPORT) and a record of files uploaded against a Staff or Customer for a given document type (`DocumentUpload`), with size/MIME validation, status tracking (`PENDING`/`APPROVED`/`REJECTED`/`EXPIRED`), and a persisted database row per upload.
2. **A separate, generic file-upload endpoint** (`com.cargosetu.controller.FileController`) that saves one or more files to a filesystem folder derived from a `Clientservice` lookup, with **no** `DocumentUpload` (or any other) database record created. It exists alongside the module above but is architecturally unrelated to it (no shared service/repository code) — see the comparison table in the next section.

This document covers both because the user-facing task ("upload a document") is served by either path depending on which controller the frontend calls.

Source packages / files read for this document:
- `src/main/java/com/cargosetu/document/model/DocumentMaster.java`
- `src/main/java/com/cargosetu/document/model/DocumentUpload.java`
- `src/main/java/com/cargosetu/document/model/DocumentStatus.java`
- `src/main/java/com/cargosetu/document/controller/DocumentController.java`
- `src/main/java/com/cargosetu/controller/FileController.java`
- `src/main/java/com/cargosetu/document/service/DocumentMasterService.java`
- `src/main/java/com/cargosetu/document/service/DocumentUploadService.java`
- `src/main/java/com/cargosetu/document/repository/DocumentMasterRepository.java`
- `src/main/java/com/cargosetu/document/repository/DocumentUploadRepository.java`
- `src/main/java/com/cargosetu/dto/DocumentMasterDTO.java`, `DocumentUploadDTO.java`, `BaseDocDTO.java`
- `src/main/java/com/cargosetu/mapper/DocumentMasterMapper.java`, `DocumentUploadMapper.java` (+ generated Impl classes under `build/generated/sources/annotationProcessor/java/main/com/cargosetu/mapper/`)
- `src/main/java/com/cargosetu/utils/utillity/FileUtility.java`
- `src/main/java/com/cargosetu/model/Clientservice.java`, `src/main/java/com/cargosetu/repo/ClientServiceRepo.java`
- `src/main/java/com/cargosetu/config/SecurityConfig.java`, `src/main/java/com/cargosetu/handler/GlobalExceptionHandler.java`, `CustomValidationException.java`
- `src/main/java/com/cargosetu/config/utils/UrlConstants.java`, `src/main/java/com/cargosetu/utils/BaseURL.java`
- `src/main/resources/application.properties`, `application-local.properties`, `application-prod.properties`

> Note on existing cross-module docs: `docs/entities.md` describes `DocumentUpload` with a `documentCode` field ("FK to DocumentMaster code"), lists `staffId` as **nullable**, and only three `DocumentStatus` values. Reading the actual entity (below) shows this is inaccurate on all three points — the FK is `document` → `DocumentMaster.documentId` (a numeric relation, not a code string), `staff_id`'s `@JoinColumn` is `nullable = false` (required, not optional), and `DocumentStatus` has four values including `EXPIRED`. This README reflects the verified source.

---

## API Controllers & Endpoints

Base URL prefix (from `BaseURL.java`): `/api/v2/cargosetu`

| Method | Full Path | Controller | Handler | Request | Response | Purpose |
|---|---|---|---|---|---|---|
| POST | `/api/v2/cargosetu/docs/uploads/document-master` | `DocumentController` | `saveMasterDocument` | JSON body: `DocumentMasterDTO` | `GenericDataDTO` (200, code `SUCCESS`/`FAIL` in body) | Create a document-type definition (e.g. register "PAN Card") |
| POST | `/api/v2/cargosetu/docs/uploads/doc-upload` | `DocumentController` | `uploadDocument` | `multipart/form-data`: part `file` (single `MultipartFile`) + part `data` (JSON string parsed into `DocumentUploadDTO`) | `ResponseEntity<GenericDataDTO>` — `200 OK` on success, `400 Bad Request` on validation failure | Upload one file for a Staff or Customer against a specific `documentId`, persisting a `DocumentUpload` row |
| POST | `/api/v2/cargosetu/document/upload` | `FileController` | `uploadDocument` | `multipart/form-data`: request param `file` (`MultipartFile[]`, one or more files) | JSON `HashMap` — `200 OK` `{"message": "..."}`, `417 Expectation Failed` `{"error": "..."}` on validation error, `500` on other exceptions | Generic bulk file save to a filesystem folder; **no DB record of the upload is created** |

Constants: `BaseURL.DOCS_UPLOADS = "/docs/uploads"`, `BaseURL.DOCUMENT = "/document"`; `UrlConstants.DOCUMENT_MASTER = "/document-master"`, `UrlConstants.UPLOAD_DOCUMENT = "/doc-upload"` (`src/main/java/com/cargosetu/config/utils/UrlConstants.java`, `src/main/java/com/cargosetu/utils/BaseURL.java`).

### How `DocumentController.uploadDocument` differs from `FileController.uploadDocument`

| Aspect | `DocumentController` (`/docs/uploads/doc-upload`) | `FileController` (`/document/upload`) |
|---|---|---|
| Files per request | Exactly one (`@RequestPart("file") MultipartFile`) | One or many (`@RequestParam MultipartFile[] file`) |
| Metadata payload | Requires a companion JSON `data` part deserialized to `DocumentUploadDTO` (staffId, documentId, customerId, expiryDate, status, remarks) | None — only the raw files |
| Validation | Two explicit checks: `DocumentUploadService.validateDocumentUpload` (documentId must be present/positive) then `validateFile` (null/empty/size/MIME type) | Only a null/empty-array check (`CustomValidationException` with HTTP 417 if no files) |
| Persistence | Saves file to disk **and** inserts a `DocumentUpload` row via `DocumentUploadRepository` linking staff/document/customer/status/etc. | Saves file(s) to disk only; **no entity, no repository write** — the upload is not tracked in the database at all |
| Destination folder | Fixed constant `DocumentUploadService.BASE_FILE_URL` (see Configuration/External Integrations below) | Looked up at request time from `ClientServiceRepo.findByName("Staff Document")`, using the `Clientservice.name` field's value as the folder path (see caveat below) |
| Response shape | `GenericDataDTO` (module-wide standard response envelope) | Ad-hoc `HashMap<String,Object>` (not using `GenericDataDTO`) |
| Error handling | Returns typed `ResponseEntity` with `GenericDataDTO` body and appropriate status inline in the method | Try/catch with `CustomValidationException` → 417, generic `Exception` → 500, both returned as a `HashMap` |

**Conclusion:** `FileController` is a lightweight, database-agnostic "dump files to a folder" endpoint, unrelated in code to the `DocumentMaster`/`DocumentUpload` tracking workflow. It appears to be an older/simpler or special-purpose ("Staff Document" bulk drop) upload path that bypasses the document-type/status/expiry tracking that `DocumentController` provides.

---

## Business/Service Logic

### `DocumentMasterService` (`document/service/DocumentMasterService.java`)
- `saveDocumentMaster(DocumentMasterDTO)`: maps DTO → `DocumentMaster` entity via `DocumentMasterMapper` (MapStruct, `BaseUpdateMapper` contract) and saves via `DocumentMasterRepository`. Returns `true` only if the saved entity has a generated `documentId > 0`. No duplicate-code check is performed in the service itself — uniqueness is enforced only at the DB level (`code` column has a `UNIQUE` constraint), so a duplicate `code` will raise a `DataIntegrityViolationException` (caught generically by `GlobalExceptionHandler`'s catch-all `Exception` handler, not the `CustomValidationException` handler).

### `DocumentUploadService` (`document/service/DocumentUploadService.java`)
- `validateDocumentUpload(DocumentUploadDTO requestDto)`: rejects if the DTO is `null`, or if `documentId` is `null`/`<= 0`. Does **not** validate `staffId` or `customerId` presence — the DTO could reference neither.
- `validateFile(MultipartFile file)`: rejects if `file` is `null`, `file.isEmpty()`, size `> 5 * 1024 * 1024` bytes (5 MB, constant `MAX_FILE_SIZE`), or `file.getContentType()` not in the allow-list (`allowedTypes`: `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, i.e. PDF/DOC/DOCX only — no images, despite `DocumentUpload.fileType` comment mentioning "pdf, jpg, png").
- `uploadDocument(MultipartFile file, DocumentUploadDTO dto)`:
  1. Derives `originalName` from `file.getOriginalFilename()` via `Paths.get(...).getFileName()`.
     - **Bug note:** the null-check is `if (file.getOriginalFilename() != null || !file.getOriginalFilename().isBlank())` — using `||` instead of `&&` means if the filename is `null`, the second operand (`!null.isBlank()`) still executes and throws `NullPointerException` rather than being short-circuited safely.
  2. Extracts the file extension from the name (after the last `.`); if there is no `.`, returns an early `FAIL` response ("invalid file extension type") — this check is redundant with `validateFile`'s MIME-type check but operates on the name, not the content type.
  3. Delegates actual disk write to `FileUtility.saveMultipartFile(new MultipartFile[]{file}, BASE_FILE_URL)` (static method) — see External Integrations for what this does.
  4. Populates `dto.fileName/filePath/fileSize/fileType` from the just-saved file, maps DTO → `DocumentUpload` entity via `DocumentUploadMapper`, and saves via `DocumentUploadRepository`.
  5. Any exception during this flow is wrapped and rethrown as an unchecked `RuntimeException("File upload failed", e)` — this is caught by `GlobalExceptionHandler`'s generic `Exception` handler (HTTP 400 per that handler's implementation, despite being framed as `INTERNAL_SERVER_ERROR` in the response body's `responseCode` — see Exception Handling below).

**Known gap (verified from the generated MapStruct impl, `DocumentUploadMapperImpl.java`):** the mapping from `customerId` (DTO) to the `customer` relation (entity) is commented out in `DocumentUploadMapper` (`//    @Mapping(source = "customerId", target = "customer.customerId")` and the corresponding `toDto` direction). The generated code confirms `customer` is **never set** on the entity and `customerId` is **never populated** on the DTO during mapping — meaning uploads submitted with a `customerId` will silently save with `customer = null` in the database, even though the DB column/FK exists and is nullable. A `mapCustomer(Long)` default method exists in the mapper interface but is unused (not referenced by any `@Mapping`).

---

## DTOs / Request-Response Models

| DTO | File | Fields | Notes |
|---|---|---|---|
| `DocumentMasterDTO` | `dto/DocumentMasterDTO.java` | `documentId`, `code`, `name`, `mandatory`, `expiryRequired`, `active` (defaults `true`) | Plain `@Getter @Setter`, 1:1 shape with `DocumentMaster` entity |
| `DocumentUploadDTO` | `dto/DocumentUploadDTO.java` | `staffDocumentId`, `staffId`, `documentId`, `fileName`, `filePath`, `fileType`, `fileSize`, `expiryDate`, `status` (`DocumentStatus`), `remarks`, `customerId` | `@Data`; flattens the entity's `staff`/`document`/`customer` relations to plain IDs for the API |
| `BaseDocDTO` | `dto/BaseDocDTO.java` | `eventName`, `documentFor`, `docType`, `filename`, `uniqueName`, `remarks`, `mvnoId` | **Verified NOT a shared parent of `DocumentMasterDTO`/`DocumentUploadDTO`** — neither class extends it, and a repo-wide grep found no class extending `BaseDocDTO` anywhere. It is a standalone, currently-unused/orphan DTO with a different field shape (looks intended for a different, more generic/multi-tenant "mvnoId"-aware document event flow that isn't wired to any controller or service in this codebase). |

`GenericDataDTO` (`utils/GenericDataDTO.java`) is the shared response envelope used by `DocumentController`: `responseMessege`, `responseCode` (from `CommonStatusCode`), `dataList`, `data`, `errors`, plus pagination fields (`totalRecords`, `pageRecords`, `currentPageNumber`, `totalPages`) that are unused by this module's endpoints.

`ValidationResponse` (`dto/ValidationResponse.java`): simple `{ isValid: boolean, message: String }` used internally by `DocumentUploadService`'s two validation methods; not returned directly to clients (its `message` is copied into `GenericDataDTO.responseMessege` on failure).

---

## Entities & Database Relationships

### `DocumentMaster` (`document/model/DocumentMaster.java`)
Table: `tblm_document_master`

| Column | Java field | Type | Constraints |
|---|---|---|---|
| `document_id` | `documentId` | `Long` | PK, identity |
| `code` | `code` | `String` | `NOT NULL`, `length=50`, `UNIQUE` (e.g. AADHAAR, PAN, DL, PASSPORT, RESUME per code comment) |
| `name` | `name` | `String` | `NOT NULL`, `length=100` |
| `mandatory` | `mandatory` | `Boolean` | `NOT NULL` |
| `expiryRequired` | `expiryRequired` | `Boolean` | `NOT NULL` |
| `active` | `active` | `Boolean` | `NOT NULL`, default `true` |

Also extends `AbstractModel` (`abstracts/AbstractModel.java`, `@MappedSuperclass`), inheriting audit columns: `is_active`, `is_deleted`, `created_by`, `created_by_name`, `created_at` (auto-set via `@PrePersist`), `updated_by_name`, `updated_by_id`, `updated_at`. Note `DocumentMaster` has its own `active` field distinct from the inherited `isActive` — both exist on the entity.

### `DocumentUpload` (`document/model/DocumentUpload.java`)
Table: `tblm_document_upload`. Does **not** extend `AbstractModel` (no audit columns, unlike `DocumentMaster`).

| Column | Java field | Type | Constraints |
|---|---|---|---|
| `doc_upload_id` | `staffDocumentId` | `Long` | PK, identity |
| `staff_id` | `staff` (`ManyToOne` → `Staff`, `com.cargosetu.model.Staff`) | FK | `@JoinColumn(nullable = false)` — **required**, lazy fetch |
| `document_id` | `document` (`ManyToOne` → `DocumentMaster`) | FK | `@JoinColumn(nullable = false)` — **required**, lazy fetch. **Verified: this is a numeric FK to `DocumentMaster.documentId`, not the `code` string** (confirmed both from the entity's `@JoinColumn(name = "document_id")` and the generated mapper, which maps `documentId` ↔ `document.documentId`) |
| `fileName` | `fileName` | `String` | `NOT NULL` |
| `filePath` | `filePath` | `String` | `NOT NULL` |
| `fileType` | `fileType` | `String` | `NOT NULL` — comment says "pdf, jpg, png" though current validation only allows pdf/doc/docx |
| `fileSize` | `fileSize` | `Long` | `NOT NULL` |
| `expiryDate` | `expiryDate` | `LocalDate` | nullable |
| `status` | `status` (`DocumentStatus` enum, `EnumType.STRING`) | `NOT NULL` |
| `remarks` | `remarks` | `String` | nullable |
| `customer_id` | `customer` (`ManyToOne` → `Customer`, `com.cargosetu.customer.entity.Customer`) | FK | `@JoinColumn(nullable = true)` — **optional**, lazy fetch |

So `DocumentUpload` has **two** owner-side associations: a mandatory `Staff` FK and an optional `Customer` FK on the same row — i.e., the schema supports a document being logically "for" a staff member always, with an optional additional link to a customer, rather than a clean either/or. (As noted above, the `customer` association is currently never populated by the mapper in practice, so in effect all uploads made through `DocumentController` today only ever set `staff`.)

### `DocumentStatus` (`document/model/DocumentStatus.java`)
Enum: `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`. Stored as `STRING` on `DocumentUpload.status`. No code in this module transitions status automatically (e.g., nothing flips `PENDING`→`EXPIRED` when `expiryDate` passes) — status is only ever set from whatever the client sends in `DocumentUploadDTO.status` at upload time.

---

## Repositories

| Repository | Extends | Custom query methods |
|---|---|---|
| `DocumentMasterRepository` (`document/repository/DocumentMasterRepository.java`) | `JpaRepository<DocumentMaster, Long>` | None — only inherited CRUD methods (`save`, `findById`, `findAll`, etc.) |
| `DocumentUploadRepository` (`document/repository/DocumentUploadRepository.java`) | `JpaRepository<DocumentUpload, Long>` | None — only inherited CRUD methods |

Neither repository declares any custom finder (e.g., no `findByStaff_StaffId`, no `findByCustomer_CustomerId`, no `findByStatus`) — there is currently no service/endpoint in this module to list/filter a staff's or customer's uploaded documents; only create (`save`) is exercised by the service layer.

`FileController`'s flow separately uses `ClientServiceRepo` (`repo/ClientServiceRepo.java`, extends `JpaRepository<Clientservice, Integer>`) with `findByName(String)` and `existsByNameAndValue(String,String)` — this repository belongs to the `Clientservice`/`tblmclientservice` table, unrelated to `DocumentMaster`/`DocumentUpload`.

---

## Security & Authentication

- Global filter chain (`config/SecurityConfig.java`) uses stateless JWT auth (`JwtAuthenticationFilter` added before `UsernamePasswordAuthenticationFilter`), CSRF disabled, form/basic/oauth2 login disabled.
- The permit-all allowlist covers `/auth/**`, static assets, Swagger/OpenAPI paths, `/api/demo-notifications/**`, `/public/**`, `/ws/**`, `/topic/**`. **Neither `/docs/uploads/**` (DocumentController) nor `/document/**` (FileController) appears in this allowlist**, so both fall under `.anyRequest().authenticated()` — a valid JWT is required for every endpoint in this module.
- No method-level `@PreAuthorize`/`hasRole`/`hasAuthority` annotations exist on `DocumentController` or `FileController` (verified by grep across `document/` package and `FileController.java`) — any authenticated user can call `document-master` (create a document type), `doc-upload`, and `document/upload`, regardless of role. There is no role check distinguishing staff-uploading-their-own-doc vs. an admin creating document-type masters.
- CORS (`corsConfigurationSource` bean) allows a fixed list of origins (localhost:4200, the deployed UI domains, `127.0.0.1:31201`) and only `GET/POST/PUT/DELETE`, headers `Authorization`/`Content-Type`, credentials allowed.

---

## Exception Handling

`GlobalExceptionHandler` (`handler/GlobalExceptionHandler.java`) is a `@RestControllerAdvice` with exactly two handlers, both returning `ApiResponseDto`:

| Exception | HTTP status returned | Notes |
|---|---|---|
| `CustomValidationException` | `400 Bad Request` (`ResponseEntity.badRequest()`) | Body uses the exception's own `errorCode` field, not necessarily 400 — e.g. `FileController` throws it with `HttpStatus.EXPECTATION_FAILED.value()` (417) as the `errorCode`, but the HTTP response itself is still hard-coded to 400 by `ResponseEntity.badRequest()`. **Note:** `FileController` itself already catches `CustomValidationException` locally and returns 417 directly (`ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)`), so the global handler's 400 behavior for this exception type is only reachable from other controllers that let it propagate. |
| Any other `Exception` | `400 Bad Request` | Body's `responseCode` is set to `CommonStatusCode.INTERNAL_SERVER_ERROR` (500) even though the actual HTTP response status is 400 — a mismatch between the payload's stated code and the real status. This is the path a `RuntimeException("File upload failed", e)` from `DocumentUploadService.uploadDocument` would take if it escaped the controller (the controller itself does not catch it, so it propagates here). |

`DocumentController.uploadDocument` itself does not use exceptions for its own validation flow — it uses the `ValidationResponse` return-value pattern from `DocumentUploadService` and returns `ResponseEntity.badRequest()` with a `GenericDataDTO` body directly, bypassing `GlobalExceptionHandler` for expected validation failures. Only unexpected errors (e.g., I/O failure while saving the file, or the NPE bug noted above) reach the global handler.

`FileController` uses its own local try/catch (not relying on the global handler for its primary paths): `CustomValidationException` → 417 with `{"error": ...}`; any other `Exception` → 500 with `{"error": ...}`. Since `FileController` catches broadly at the controller level, `GlobalExceptionHandler`'s catch-all is effectively unreachable for this controller's own explicit throws (it would still catch it, but the local catch fires first as it's inside the same method).

`FileNotCreatedException` (`exception/FileNotCreatedException.java`, extends `IOException`) is thrown by `FileUtility.saveFile`/`saveFileToServer` on I/O failure but is **not used** by the code paths this module's controllers actually call (`DocumentUploadService` calls the static `FileUtility.saveMultipartFile`, which throws a generic wrapped `RuntimeException("Could not store files", ex)` instead; `FileController` calls `FileUtility.saveMultipleFiles`, which declares `throws IOException` but does not itself throw `FileNotCreatedException`).

---

## Configuration

Multipart/file-size settings were checked across all three property files (`application.properties`, `application-local.properties`, `application-prod.properties`) — **no `spring.servlet.multipart.*` properties are configured anywhere in the codebase.** This means Spring Boot's defaults apply at the servlet/container level:
- `spring.servlet.multipart.max-file-size` defaults to **1MB**
- `spring.servlet.multipart.max-request-size` defaults to **10MB**

**This is a real, verifiable inconsistency**: `DocumentUploadService.MAX_FILE_SIZE` is set to 5MB and `validateFile` compares against `5 * 1024 * 1024` bytes, implying the application intends to accept files up to 5MB — but with no `spring.servlet.multipart.max-file-size` override, Spring's default 1MB container-level limit would reject (with a `MaxUploadSizeExceededException`, not a friendly `ValidationResponse`) any file between 1MB and 5MB before `DocumentUploadService.validateFile` ever runs. This exception is not specifically handled anywhere in this module and would fall through to `GlobalExceptionHandler`'s generic `Exception` handler.

No MIME-type allowlist is configured via Spring properties — the only enforced allow-list is the in-code `allowedTypes` `Set` in `DocumentUploadService` (PDF/DOC/DOCX). `FileController` has no MIME-type or extension validation at all — it accepts any file type.

`application.properties` also contains embedded secrets (Google OAuth client secret, Gmail SMTP password, Twilio auth token, prod DB password in `application-prod.properties`) — out of scope for this module's functionality but noted as observed while reading the config files for multipart settings; flagging as a general security concern rather than something specific to Document Management.

---

## External Integrations

No cloud storage (S3, GCS, Azure Blob, etc.) is used anywhere in this module — verified by reading the actual upload code paths, not inferred:

- **`DocumentController`'s `/doc-upload` path**: `DocumentUploadService.uploadDocument` calls `FileUtility.saveMultipartFile(MultipartFile[], String)` (`utils/utillity/FileUtility.java`, a `static` method), which writes to **`DocumentUploadService.BASE_FILE_URL`**, a hard-coded local filesystem path: `"C:\\Users\\Dell\\Desktop\\Personal\\uploads"`. This is clearly a developer-machine-specific path left in the code (not environment-configurable, not read from `application.properties`) — it will fail on any other machine/environment (e.g. the Linux/Docker-oriented prod deployment referenced in `application-prod.properties`) unless that literal path happens to exist and be writable. Files are saved with a randomized unique name: `UUID.randomUUID() + UUIDGenerator.generate() + "_" + originalName`, with a path-traversal guard (rejects names containing `..`, and re-checks the resolved path starts with the upload directory).
- **`FileController`'s `/document/upload` path**: calls `FileUtility.saveMultipleFiles(MultipartFile[], String)` (instance method, different from the static `saveMultipartFile` used above), which writes each file via the instance method `FileUtility.saveFile(argFile, path)`. That method resolves the destination directory from the `path` argument — which in `FileController.uploadDocuments` is **`clientService.getName()`**, i.e., whatever string is stored in the `name` column of the `tblmclientservice` row found by `clientServiceRepo.findByName("Staff Document")`. This is effectively "the folder path is a value configured in the `Clientservice` DB table," not a real filesystem-abstraction/cloud integration. `saveFile` prefixes the final filename with the current authenticated username (`getCurrentUser().getUsername() + "_" + originalName`, via `ApplicationUtils`, which `FileUtility` extends).
  - **Bug/risk note:** `clientServiceRepo.findByName("Staff Document").orElse(null)` — if no `Clientservice` row named "Staff Document" exists, `clientService` is `null` and the very next line, `clientService.getName()`, throws a `NullPointerException`. This is caught by `FileController`'s own generic `catch (Exception e)` and surfaces as a `500` with the NPE's (likely null/unhelpful) message — worth verifying that this seed row exists in every environment.
- Neither controller returns a public/servable URL for the uploaded file; `filePath` as stored is a local absolute path, not something a frontend could use as an `<img src>` or download link without another endpoint (none exists in this module) to stream the file back.

---

## Important Business Rules & Validations

1. **Document type uniqueness**: `DocumentMaster.code` is `UNIQUE` at the DB level (`tblm_document_master`); duplicate codes fail at save time, not via a pre-check in `DocumentMasterService`.
2. **Upload requires a valid document type**: `DocumentUploadService.validateDocumentUpload` requires `documentId` to be a positive number before proceeding; it does not verify the ID actually exists in `DocumentMasterRepository` (i.e., there's no explicit "document type not found" check — an invalid but positive ID would only fail later at the DB FK-constraint level when the `DocumentUpload` insert runs, if it fails at all, given the `document_id` `NOT NULL` FK).
3. **File size limit**: 5MB (`DocumentUploadService.MAX_FILE_SIZE`), enforced only in `validateFile` — see the Configuration section for the conflicting Spring default of 1MB that would trigger first.
4. **File type restriction**: only `application/pdf`, `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document` are accepted by `DocumentController`'s path (checked via `MultipartFile.getContentType()`, i.e., the client-supplied `Content-Type` header for that part — not sniffed from file bytes, so it can be spoofed by a client sending an arbitrary `Content-Type`).
5. **No such restriction on `FileController`**: any file type/size can be uploaded via `/document/upload` (only a non-empty-array check).
6. **Mandatory/expiry flags on `DocumentMaster`** (`mandatory`, `expiryRequired` booleans) are stored but **not enforced** anywhere in the read code — there is no service logic in this module that checks "is this mandatory document present for staff X" or "does this document type require an expiry date, and was one supplied." These fields appear to be intended as configuration for a downstream/other-module compliance check that was not found in the files covered here.
7. **`DocumentStatus` is client-supplied and not defaulted**: `DocumentUpload.status` is `NOT NULL` on the entity, but nothing in `DocumentUploadService`/`DocumentController` defaults it (e.g. to `PENDING`) if the client omits it in `DocumentUploadDTO` — an upload request without a `status` would likely fail at the DB `NOT NULL` constraint rather than being defaulted server-side.
8. **`customerId` is accepted in the DTO but silently dropped** — see the "Known gap" note under Business/Service Logic. This is a functional bug for any customer-document upload flow the frontend expects to work.

---

## Dependencies Between Modules

- **Staff / RBAC module** (`com.cargosetu.model.Staff`, referenced from `com.cargosetu.document.model.DocumentUpload.staff`): `DocumentUpload` has a mandatory FK to `Staff`, so every document upload row (as currently mappable) is anchored to a staff member. Any staff lifecycle change (e.g. staff deletion) would need to account for `tblm_document_upload` rows referencing that staff row (no `onDelete` cascade behavior is declared on the `@JoinColumn`, so default FK behavior — likely a DB-level constraint violation on delete — would apply, subject to the actual DB schema/migration, not verified here).
- **Customer / Customer-Shifting module** (`com.cargosetu.customer.entity.Customer`, referenced from `DocumentUpload.customer`): optional FK, intended to let a document also (or instead) be associated with a customer — but, as noted, this link is not currently populated by the mapper used in the only upload flow that exists (`DocumentController`), so in practice this cross-module dependency is present in the schema but dormant in the code path exercised today.
- **`Clientservice`/general config table** (`com.cargosetu.model.Clientservice`, `com.cargosetu.repo.ClientServiceRepo`): `FileController` depends on a specific seeded row (`name = "Staff Document"`) in this generic key-value-like service-config table to determine its upload folder — an indirect, fragile dependency outside the Document Management module's own package.
- **Security/Auth module** (`JwtAuthenticationFilter`, `com.cargosetu.config.SecurityConfig`): both controllers require a valid authenticated JWT (no public/permit-all carve-out); `FileUtility.saveFile`'s username-prefixing (used by `FileController`'s path) additionally depends on `ApplicationUtils.getCurrentUser()` resolving the current authenticated principal.
- **Mapper/DTO infrastructure** (`com.cargosetu.mapper.base.BaseUpdateMapper`, `com.cargosetu.config.GlobalMapperConfig`): both `DocumentMasterMapper` and `DocumentUploadMapper` are MapStruct interfaces sharing this repo-wide base contract and global mapper configuration, consistent with mapper patterns used by other modules (not specific to Document Management).

# Template / Quotation-PDF Module

> Source: `src/main/java/com/cargosetu/model/Template.java`, `controller/TemplateGenerationController.java`,
> `service/TemplateService.java`, `dto/QuotationDTO.java`, `dto/CompanyDTO.java`, `utils/utillity/PdfGenerator.java`,
> plus cross-module reads of `customer/entity/{Shifting,Customer,Charges,ExtraCharges}.java`,
> `repo/{ShiftingRepository,TemplateRepository,CompanyAssetRepository}.java`, `service/impl/CompanyServImpl.java`,
> `config/{MethodSecurityConfig,SecurityConfig}.java`, `handler/GlobalExceptionHandler.java`.

All claims below were verified by reading the source files directly on 2026-08-09; anything not directly
observed in code is explicitly flagged as **Unverified/Inferred**.

---

## 0. Read this first — there is no `Quotation` entity

This module does **not** implement a stateful "quotation" business object. There is no `Quotation` JPA entity,
no `QuotationRepository`, no quotation table, and nothing here is ever persisted as a quote. Grepping the
codebase for `class Quotation` / `@Entity` quotation tables returns nothing beyond the transient
`QuotationDTO` described in §4.

What actually exists is **on-demand PDF rendering**: given a `shiftingId`, `TemplateService` loads the live
`Shifting` + `Customer` + `Charges` rows, computes totals in Java, drops them into an HTML template stored in
the `Template` table, and converts the resulting HTML to a PDF byte stream that is streamed straight back in
the HTTP response. Nothing about "a quotation was generated," what its status is, whether the customer
accepted it, or what number it was issued as, is recorded anywhere. Every call recomputes everything from
scratch from current `Shifting`/`Charges` data — call it twice and you may get two different PDFs if the
underlying charges changed in between.

This is the single largest, most concrete gap between this codebase and a "real" quotation workflow (create →
send → track status → accept/reject → convert to invoice). See §10 for the explicit cross-reference to the
planning doc that names this gap.

---

## 1. Overview

- **Purpose**: generate a print-ready PDF (quotation or invoice) for a given `Shifting` (moving job), using
  company-branded HTML templates stored in the database or filesystem.
- **Not a workflow engine**: there is no state machine, no "quotation number" sequence, no acceptance/rejection
  flow, no expiry date, no versioning of a quote over time. Each request is a pure, stateless
  render-from-current-data operation.
- **Two output flows**, both implemented in `TemplateService` and exposed by `TemplateGenerationController`:
  1. **Quotation PDF** — `GET /templates/quotation/{shiftingId}/pdf` → `TemplateService.generateQuotationPdf()`.
  2. **Invoice PDF** — `GET /templates/{shiftingId}/invoice` → `TemplateService.generateInvoiceHtml()` (method
     name says "Html" but it returns a PDF byte array — see §2 note).
- **Template storage**: HTML templates themselves (the markup with Thymeleaf placeholders) live in the
  `Template` entity/table, uploaded via a third endpoint (`POST /templates`). A template is selected purely by
  its `type` string (`"QUOTATION"` or `"INVOICE"`) — there is no link from a `Shifting` or `Company` to a
  specific `Template` row; `TemplateRepository.findByType(type)` returns whichever single row of that type
  exists (see §6 for the resulting fragility).
- **Rendering engine**: Thymeleaf's `StringTemplateResolver` (renders an HTML string held in memory/DB, not a
  classpath template file) is used to fill placeholders, then the resulting HTML string is converted to PDF
  bytes by a `PdfGenerator` component — see §9 for exactly which library does that conversion (the answer is
  more interesting than it first looks, because the codebase currently contains **three different HTML→PDF
  code paths**, only one of which is actually wired up).

---

## 2. API Controllers & Endpoints

**File:** `src/main/java/com/cargosetu/controller/TemplateGenerationController.java`
**Base mapping:** `BaseURL.BASE_URL + BaseURL.TEMPLATES` = `/api/v2/cargosetu/templates`
(`BaseURL.java:4`, `BaseURL.java:16`).

| Method | Path | Handler | `@PreAuthorize` | Request | Response |
|---|---|---|---|---|---|
| POST | `/api/v2/cargosetu/templates` | `addTemplate` | `hasAuthority(PermissionConstants.COMPANY_CREATE)` | multipart: `file` part (the HTML file) + `data` part (JSON string deserialized as `List<Template>`, only element `[0]` is used) | `GenericDataDTO`, always `200 OK` at the HTTP level; body's `responseCode` is `CREATED`/`SUCCESS` on success or `INTERNAL_SERVER_ERROR` with `e.getMessage()` on failure (caught locally, not rethrown) |
| GET | `/api/v2/cargosetu/templates/quotation/{shiftingId}/pdf` | `quotationPdf` | `hasAuthority(PermissionConstants.COMPANY_CREATE)` | path var `shiftingId` (Long) | `application/pdf` bytes, `Content-Disposition: inline; filename="quotation-{shiftingId}.pdf"` on success |
| GET | `/api/v2/cargosetu/templates/{shiftingId}/invoice` | `getInvoiceHtml` | **none** — no `@PreAuthorize` annotation on this handler at all | path var `shiftingId` (Long) | `application/pdf` bytes (despite the method name), same `Content-Disposition` pattern, **filename is still `quotation-{shiftingId}.pdf`, not `invoice-...`** (`TemplateGenerationController.java:94`, copy-paste artifact) |

Verified line-by-line against `TemplateGenerationController.java:34-104`.

**Endpoint-level bugs/quirks worth flagging explicitly:**

- **The try/catch in `quotationPdf` does not actually protect the risky call.** The line
  `byte[] pdf = templateService.generateQuotationPdf(shiftingId);` (line 58) sits **outside** the `try` block
  that follows it (lines 59-71); the `try` only wraps the `ResponseEntity` construction, which essentially
  never throws. So any exception thrown inside `generateQuotationPdf` (shifting not found, company not found,
  no `QUOTATION` template row, template file unreadable) propagates straight out of the controller method
  (declared `throws Exception`) rather than being caught and turned into the intended `500` +
  `GenericDataDTO` error body. It instead falls through to `GlobalExceptionHandler`'s generic
  `@ExceptionHandler(Exception.class)`, which — per `handler/GlobalExceptionHandler.java:26-36` — returns
  `ResponseEntity.badRequest()` (**HTTP 400**, not 500) with an `ApiResponseDto` body, a different shape than
  the `GenericDataDTO` the controller's own catch block would have produced. The controller's local
  `catch (Exception e)` branch (lines 65-70) is effectively **dead code** for the realistic failure modes of
  this endpoint.
- **`getInvoiceHtml` has the opposite problem: it can silently return an empty/`null` PDF with a `200` status.**
  `generateInvoiceHtml()` in the service wraps its entire body in a `try { ... } catch (RuntimeException e) {
  e.printStackTrace(); } return null;` (see §3.2). So if the shifting/company/template lookups fail, the
  service swallows the exception, logs it to stdout only, and returns `null`. The controller then builds
  `ResponseEntity.ok()` with a `null` body and a PDF content-type header — the client receives a `200 OK` with
  an empty/absent body instead of any error indication.
- **`addTemplate` always returns HTTP 200** regardless of outcome — failures are only visible in the JSON
  `responseCode` field, consistent with the pattern already documented for `GlobalExceptionHandler` in
  `docs/services-and-known-issues.md` (§"Exception handling").
- **`@PreAuthorize` is globally inert in this codebase**, independent of anything specific to this module:
  `config/MethodSecurityConfig.java:7` declares `@EnableMethodSecurity(prePostEnabled = false)`, which disables
  Spring Security's method-level `@PreAuthorize`/`@PostAuthorize` processing entirely. This was confirmed by
  direct read and is also called out in `docs/services-and-known-issues.md`. Practically: the `COMPANY_CREATE`
  annotations on `addTemplate` and `quotationPdf` currently do nothing — any authenticated (JWT-bearing) user
  can call every endpoint in this controller, including the un-annotated `getInvoiceHtml`. See §7.

---

## 3. Business / Service Logic

**File:** `src/main/java/com/cargosetu/service/TemplateService.java` (301 lines, read in full).

Dependencies (constructor-injected via `@RequiredArgsConstructor`, plus two `@Autowired` fields):
`ShiftingRepository`, `CompanyRepository`, `CompanyAssetRepository` (field name `assetRepository`, **unused** —
asset lookups actually go through `CompanyService.getAssetAsBase64`, not this repository directly),
`TemplateRepository`, `CompanyService`, `ClientServiceRepo` (`@Autowired` field), `PdfGenerator`
(`@Autowired` field).

### 3.1 `createTemplate(MultipartFile file, Template template)` — template upload (lines 51-58)

1. Looks up a `Clientservice` row by name `"Template"` via `clientServiceRepo.findByName("Template").orElseThrow()`
   and uses its `.getName()` (i.e. the literal string `"Template"`) as the **base directory path** argument to
   `FileUtility.saveMultipartFile(new MultipartFile[]{file}, "Template")`.
   - `FileUtility.saveMultipartFile` (`utils/utillity/FileUtility.java:242-286`) resolves this against the
     process's current working directory (`Paths.get("Template").toAbsolutePath().normalize()`), creates the
     directory if missing, and writes the file under a random name:
     `UUID.randomUUID() + UUIDGenerator.generate() + "_" + originalFilename`.
   - This means uploaded template HTML files land in a relative `./Template/` folder on whatever host the app
     runs on — **not** an absolute, environment-configured upload path like `DocumentUploadService.BASE_FILE_URL`
     (`"C:\Users\Dell\Desktop\Personal\uploads"`, a hardcoded Windows dev-machine path used elsewhere in the
     codebase). `TemplateService.java:36` statically imports `DocumentUploadService.BASE_FILE_URL` but **never
     uses it** — a dead import.
2. If a file path was produced, sets it on the passed-in `Template` object (`template.setFilePath(...)`).
3. Saves the `Template` via `templateRepository.save(template)`. Whatever other fields (`name`, `type`,
   `htmlContent`, `isActive`, `companyId`, `isFavoriteTemplate`) were present on the deserialized JSON payload
   are persisted as-is — there is no server-side validation of `type` being one of `QUOTATION`/`INVOICE`, no
   uniqueness check, and no check for an existing active template of the same type before saving a new one.

### 3.2 `generateQuotationPdf(Long shiftingId)` — the core quotation flow (lines 60-101)

Step by step, exactly as written:

1. **Load data eagerly in one query**: `shiftingRepository.findShiftingWithCustomerAndCharges(shiftingId)`
   (`ShiftingRepository.java:24-26`) — a JPQL query
   `SELECT s FROM Shifting s JOIN FETCH s.customer JOIN FETCH s.charge WHERE s.shiftingId = :shiftingId`.
   This is the one repository method that eagerly loads `Shifting` + `Customer` + `Charges` together in a single
   round trip (note: `Charges.extraCharges` is separately `fetch = FetchType.EAGER` on the entity mapping itself
   — `Charges.java:61` — so `ExtraCharges` rows come along automatically without needing their own `JOIN FETCH`).
   Throws `RuntimeException("Shifting not found: " + shiftingId)` if absent.
2. Pulls `Customer customer = shifting.getCustomer()` and `Charges charges = shifting.getCharge()` off the
   loaded `Shifting`.
3. **Resolves the Company**: `companyRepository.findById(customer.getCompanyId())` — i.e. the company is derived
   from the **customer's** `companyId` column (`Customer.companyId`, `customer/entity/Customer.java:45-46`),
   *not* from `Shifting.companyId` (which also exists as a field, `Shifting.java:66`, but is not read here at
   all in this flow). Throws `RuntimeException("Company not found for customer")` if absent.
4. **Company branding assets**: calls `companyService.getAssetAsBase64(company.getCompanyId(), "LOGO")` and
   `...("SEAL")`. Per `CompanyServImpl.java:333-352`, this looks up a `CompanyAsset` row via
   `companyAssetRepository.findByCompanyIdAndAssetType(companyId, assetType)`, reads the file at its
   `filePath` off disk (`Files.readAllBytes`), base64-encodes it, and returns a full data URI
   (`"data:" + mime + ";base64," + b64`) — or `null` on any failure ("template handles null → shows placeholder
   box", per the source comment). Wraps both into a `CompanyDTO` via the static factory `CompanyDTO.from(company,
   logoB64, sealB64)` (`dto/CompanyDTO.java:89-114`), which also splits `companyName` into `companyNameFirst`
   (first word) / `companyNameRest` (remainder) for template layout purposes.
5. **Pricing math** — see §8 for the exact field list; summarized: `subTotal = computeSubTotal(charges)` (sums
   nine fixed `Charges` fields plus every `ExtraCharges.chargeValue`), `gstAmount = nullSafe(charges.getGstCharge())`
   (the **stored** GST amount is used directly — it is *not* recomputed from `subTotal * gstRate`), and
   `grandTotal = subTotal + gstAmount`.
6. Builds a `QuotationDTO` (see §4) with `quoteDate = LocalDate.now()`, the computed `subTotal`/`grandTotal`, and
   `tokenMoney = null` (never populated anywhere in this flow — there is no concept of an advance/token payment
   captured for a quotation).
7. **Template selection**: `templateRepository.findByType("QUOTATION").orElseThrow(...)` — the *only* selector
   is the literal type string. If more than one `Template` row has `type = "QUOTATION"`, `findByType` (a Spring
   Data derived query returning `Optional<Template>`, not a `List`) will throw
   `IncorrectResultSizeDataAccessException` at runtime if JPA returns more than one row — this is a real risk
   surface once a company-specific template feature is added, given `Template.companyId` already exists as a
   column but is **never used as a filter anywhere in `TemplateService`**.
8. **Loads the HTML source**: if `tmpl.getFilePath()` is set, reads it from disk via
   `Files.readString(Path.of(tmpl.getFilePath()))`; otherwise falls back to `tmpl.getHtmlContent()` (the `@Lob`
   column). Wraps read failures in `RuntimeException("Failed to read template file at: " + tmpl.getFilePath()`.
9. **Renders**: calls the private `renderHtml(...)` overload (see §3.4) to produce a filled HTML string via
   Thymeleaf.
10. **Converts to PDF**: `pdfGenerator.generatePdf(filledHtml)` — the injected `PdfGenerator` bean (see §9);
    returns the resulting `byte[]` directly to the controller.

### 3.3 `generateInvoiceHtml(Long shiftingId)` — the invoice flow (lines 244-298)

Nearly identical to §3.2 with these differences:

- The whole body is wrapped in `try { ... } catch (RuntimeException e) { e.printStackTrace(); } return null;` —
  **any failure anywhere in the flow is swallowed** and the method returns `null` (see the controller-level
  consequence noted in §2).
- Looks up the `Template` by `type = "INVOICE"` instead of `"QUOTATION"`.
- Builds an extra `variableMap` of ad-hoc Thymeleaf variables not present in the quotation flow:
  `invoiceNo` (constructed as `"INV-" + shiftingId + "-2026"` — **the year `2026` is hardcoded as a literal
  string**, not derived from `LocalDate.now()`, so every invoice number will read `...-2026` regardless of the
  actual current year once this code is still running past 2026), `invoiceDate` (today), `dueDate` (today + 7
  days, `LocalDate.now().plusDays(7)`, i.e. a fixed 7-day payment term with no per-company/per-customer
  configurability), and duplicated copies of `subTotal`/`grandTotal` as top-level Thymeleaf variables (in
  addition to the ones already on the `quotation` object).
- Calls the second `renderHtml` overload that also merges in this extra `variableMap`.
- Still produces PDF bytes via the same `pdfGenerator.generatePdf(filledHtml)` call — despite the method being
  named `generateInvoiceHtml`, it does not return HTML.

### 3.4 `renderHtml(...)` — Thymeleaf rendering (two overloads, lines 103-147)

Both overloads build a Thymeleaf `Context` and set the following variables before processing:

| Context variable | Type | Source |
|---|---|---|
| `company` | `CompanyDTO` | passed in directly |
| `customer` | `Map<String,Object>` | `getCustomerMap(customer)` — `firstName`, `lastName`, `mobileNo`, `workEmail`, `uid` (= `customer.getUuid()`) |
| `shifting` | `Map<String,Object>` | `getShiftingMap(shifting)` — `shiftingId`, `fromAddress`, `toAddress`, `fromCity` (= `shiftFromCity`), `toCity` (= `shiftToCity`), `shiftingStartDate`/`shiftReachedDate` (both `.toString()`'d, or `null`) |
| `charges` | `Map<String,Object>` | `getChargesMap(charges)` — see §8 for the full field list, includes `serviceRate`/`gstRate` (rate percentages) alongside the charge amounts |
| `quotation` | `QuotationDTO` | the object built in §3.2/§3.3 step 5-6 |
| `extras` | `List<ExtraCharges>` | `charges.getExtraCharges()` — the raw entity list, unmapped |
| *(invoice only)* `invoiceNo`, `invoiceDate`, `dueDate`, `subTotal`, `grandTotal` | mixed | the extra `variableMap` merged in via the second overload, only from `generateInvoiceHtml` |

The Thymeleaf engine is built fresh on every call by the private `buildStringEngine()` (lines 191-198): a
`StringTemplateResolver` (`TemplateMode.HTML`, `setCacheable(false)`) wrapped in a `SpringTemplateEngine` — i.e.
**no template caching**, and a brand-new `SpringTemplateEngine`/resolver instance is constructed per PDF
request rather than being a shared `@Bean`. This is functionally correct but has a per-request setup cost
worth noting for a high-traffic quotation-generation endpoint.

---

## 4. DTOs / Request-Response Models

### `QuotationDTO` (`src/main/java/com/cargosetu/dto/QuotationDTO.java`, full file — 16 lines)

```java
@Data
@Builder
public class QuotationDTO {
    private LocalDate quoteDate;
    private Double    subTotal;
    private Double    grandTotal;
    private Double    tokenMoney;
}
```

- **Confirmed transient / never persisted**: there is no `@Entity` annotation, no JPA repository anywhere in the
  codebase whose generic type is `QuotationDTO` (`grep`-verified — no `QuotationRepository` file exists, and no
  `.save(...)` call takes a `QuotationDTO`). It exists purely as a Thymeleaf view-model built fresh inside
  `TemplateService` on every PDF request and discarded immediately after rendering.
- `tokenMoney` is declared but never set to a non-null value anywhere in `TemplateService` — always `null` in
  both the quotation and invoice flows.
- No `quotationNumber`/`quoteId`/`status` field exists on this DTO at all, consistent with §0's point that there
  is no quotation identity or lifecycle in this codebase.

### `CompanyDTO` (`src/main/java/com/cargosetu/dto/CompanyDTO.java`)

Used here only via its static factory `CompanyDTO.from(Company c, String logoBase64, String sealBase64)`
(lines 89-114) to build the `company` Thymeleaf context variable. The DTO itself is the general-purpose company
read/write model used elsewhere (see `docs/modules/tenant-company.md` for its full field list); the fields this
module actually populates through `.from(...)` are: `companyName`, `companyNameFirst`, `companyNameRest`,
`address`, `city`, `state`, `contactNo1`, `contactNo2`, `companyEmail`, `whatsappNumber`, `panNumber`,
`gstNumber`, `branch`, `logoBase64`, `sealBase64`, `website`. Fields such as `country`, `mainBranch`,
`companyUUID`, `bankName`/`accountHolder`/`accountNo`/`ifscCode`, `landlineNo`, `tollFreeNumber`,
`tollFreeNumber`, and `assets` are **left at their defaults (null/empty string)** by this factory method — a
quotation/invoice template referencing e.g. `${company.bankName}` for payment instructions would always render
blank.

### `Template` request payload (upload endpoint)

`addTemplate` deserializes the `data` multipart part as `List<Template>` and only uses index `0` — i.e. the
client must wrap a single template object in a JSON array, an easy-to-miss API quirk
(`TemplateGenerationController.java:41-43`).

---

## 5. Entities & Database Relationships

### `Template` (`src/main/java/com/cargosetu/model/Template.java`, full file — 30 lines)

```java
@Entity
@Data
public class Template {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String type;          // QUOTATION, INVOICE etc — free string, no enum, no DB constraint
    @Lob
    private String htmlContent;   // inline HTML fallback when filePath is absent
    private Boolean isActive;     // present on the entity but never read/filtered on by TemplateService
    private String filePath;      // preferred source of HTML when set (see §3.2 step 8)
    private Long companyId;       // present on the entity but never used as a filter by TemplateService (see §3.2 step 7)
    private boolean isFavoriteTemplate = false; // present but not referenced anywhere in TemplateService
}
```

- No `@Table(name=...)` is specified, so the table name is JPA-provider-default (Hibernate's implicit naming
  strategy would produce `template`) — `docs/entities.md:302` already flags this table name as "(inferred)"
  rather than confirmed; this doc concurs, no explicit table name exists in source.
- No relationship annotations at all — `Template` is a standalone table with no FK to `Company`, despite having
  a `companyId` column. Multi-tenant template scoping (a company using its own branded quotation layout) is
  **schema-ready but not implemented** in the service layer: `companyId` and `isActive` are dead columns from
  `TemplateService`'s point of view.
- `isFavoriteTemplate` and `isActive` suggest a future "pick which of several templates per type/company is the
  active one" feature was planned, but today `findByType(type)` simply grabs the first/only row of that type
  with no `isActive = true` filter and no ordering — see the `IncorrectResultSizeDataAccessException` risk noted
  in §3.2 step 7.

### Related entities read by this module (owned by the customer-shifting module — see
`docs/modules/customer-shifting.md` for their full documentation)

- **`Shifting`** (`customer/entity/Shifting.java`, table `tblm_shifting`) — fields read here: `shiftingId`,
  `fromAddress`, `toAddress`, `shiftFromCity`, `shiftToCity`, `shiftingStartDate`, `shiftReachedDate`,
  `customer` (`@ManyToOne`, lazy), `charge` (`@OneToOne`, cascade `ALL`). `companyId` on `Shifting` exists
  (line 66) but is **not** the company used to resolve branding (see §3.2 step 3).
- **`Customer`** (`customer/entity/Customer.java`, table `tblm_customers`) — fields read: `firstName`,
  `lastName`, `mobileNo`, `workEmail`, `uuid`, `companyId` (this is the one actually used to look up `Company`).
- **`Charges`** (`customer/entity/Charges.java`, table `tblm_charges`) — see §8 for every field consumed by the
  pricing math. Has a `@OneToMany(cascade = ALL, orphanRemoval = true, fetch = EAGER)` to `ExtraCharges` and a
  `@OneToOne(mappedBy = "charge")` back-reference to `Shifting`.
- **`ExtraCharges`** (`customer/entity/ExtraCharges.java`, table `tblm_extra_charges`) — `chargeName`,
  `chargeValue` (summed into `subTotal`), `chargeRate` (present but **not used** in the subtotal calculation —
  only `chargeValue` is summed; `chargeRate` appears to be informational/unused by the math).
- **`Company`** (`model/Company.java`, table `tblmcompany`) — read-only here; resolved by `companyId`
  (`Integer`) via `CompanyRepository.findById`.
- **`CompanyAsset`** (`model/CompanyAsset.java`, table `tbl_company_assets`) — read indirectly through
  `CompanyService.getAssetAsBase64`, keyed by `(companyId, assetType)` where `assetType` is one of the free
  strings `"LOGO"`/`"SEAL"` (also `"SIGNATURE"`/`"STAMP"` per the field comment, though this module only ever
  requests `LOGO` and `SEAL`).

---

## 6. Repositories

| Repository | File | Methods used by this module |
|---|---|---|
| `TemplateRepository` | `repo/TemplateRepository.java` | `findByType(String type)` — the **only** query method on this repository; returns `Optional<Template>`, so more than one row of the same `type` is a latent runtime error (§3.2 step 7). No `save`-side uniqueness constraint enforces at most one row per `type`. |
| `ShiftingRepository` | `repo/ShiftingRepository.java` | `findShiftingWithCustomerAndCharges(Long shiftingId)` — the eager-fetch JPQL query described in §3.2 step 1. (The repository also has `findShifting` (native query, no fetch joins) and paging finders used elsewhere by the customer-shifting module, not by this one.) |
| `CompanyRepository` | (company/tenant module — see `docs/modules/tenant-company.md`) | `findById(Integer companyId)` |
| `CompanyAssetRepository` | `repo/CompanyAssetRepository.java` | Not called directly by `TemplateService` despite being injected as `assetRepository` — asset lookups go through `CompanyService.getAssetAsBase64`, which internally calls `companyAssetRepository.findByCompanyIdAndAssetType(companyId, assetType)`. |
| `ClientServiceRepo` | `repo/ClientServiceRepo.java` | `findByName("Template")` — used only inside `createTemplate` to derive the upload base-path string (§3.1); an odd reuse of a generic "client service" lookup table to hold what is effectively a hardcoded folder name. |

---

## 7. Security & Authentication

- `addTemplate` and `quotationPdf` are annotated `@PreAuthorize("hasAuthority(T(com.cargosetu.utils.PermissionConstants).COMPANY_CREATE)")`. `getInvoiceHtml` has **no** `@PreAuthorize` annotation at all
  (`TemplateGenerationController.java:88`, verified by direct read — no annotation line precedes the method).
- **However, none of this currently has any effect.** `config/MethodSecurityConfig.java` sets
  `@EnableMethodSecurity(prePostEnabled = false)`, which disables `@PreAuthorize` evaluation globally across
  the entire application, not just this controller. This is confirmed by direct read of that file and is also
  documented as a codebase-wide finding in `docs/services-and-known-issues.md`. The practical effect: **any
  request that passes the JWT authentication filter can call all three endpoints in this controller**,
  regardless of role or the `COMPANY_CREATE` permission the annotations imply. There is no additional
  company-scoping check inside `TemplateService` either (e.g. nothing verifies the calling user's company
  matches `customer.getCompanyId()`) — so, as written, an authenticated user from Company A can request a
  quotation PDF for a `shiftingId` belonging to Company B, and will receive Company B's branded PDF with
  Company B's logo/seal/contact info, purely by knowing or guessing the numeric `shiftingId`. This is a
  cross-tenant data exposure risk **specific to this module** worth calling out beyond the general
  `@PreAuthorize`-is-inert finding, since it doesn't require any additional permission bypass — the endpoint
  takes a bare numeric ID with no ownership check at all.
- Standard JWT authentication (`JwtAuthenticationFilter`) still applies unless the path is explicitly excluded
  in `SecurityConfig`/`shouldNotFilter` — `/templates/**` is not in the public/permit-all path lists observed
  in `SecurityConfig.java`, so an unauthenticated request should still be rejected before reaching the
  controller. (Not independently re-verified path-by-path in this pass beyond confirming `/templates/**` is
  absent from the `permitAll()` lines read — treat as **Inferred** from the general JWT filter behavior
  documented in `docs/services-and-known-issues.md`.)

---

## 8. Exception Handling

- **Service layer** throws plain `RuntimeException` with descriptive messages for every "not found" case
  (`"Shifting not found: {id}"`, `"Company not found for customer"`, `"No active QUOTATION template in DB"`,
  `"No active Invoice template "`, `"Failed to read template file at: {path}"`). None of these are custom
  exception types — no `TemplateNotFoundException`/`ShiftingNotFoundException` — so `GlobalExceptionHandler`
  can only ever catch them via its generic `@ExceptionHandler(Exception.class)` fallback, never anything more
  specific.
- **`GlobalExceptionHandler`** (`handler/GlobalExceptionHandler.java`) has exactly two handlers:
  `CustomValidationException` and generic `Exception`, and **both unconditionally return
  `ResponseEntity.badRequest()` (HTTP 400)** — even for the generic handler, which constructs a body carrying
  `CommonStatusCode.INTERNAL_SERVER_ERROR` in its JSON payload while the actual HTTP status stays 400. This is
  a codebase-wide issue (already noted in `docs/services-and-known-issues.md`), and it is this handler that
  actually processes failures from `quotationPdf` (see §2's note that the controller's own try/catch there is
  dead code for the realistic failure paths).
- **`generateInvoiceHtml` uniquely swallows its own exceptions** (`catch (RuntimeException e) {
  e.printStackTrace(); } return null;`) rather than letting them propagate to `GlobalExceptionHandler` at all —
  so invoice failures never even reach the standard error-handling path; they surface as a `200 OK` with an
  empty body (§2).
- **`addTemplate`** has its own local try/catch that never lets an exception propagate — it always returns
  `200 OK` with a `GenericDataDTO` whose `responseCode`/`responseMessege` reflect success or failure
  (`e.getMessage()` is returned directly to the client — a minor information-leak pattern also flagged
  elsewhere in the codebase's other controllers, e.g. `docs/modules/customer-shifting.md` §2.2's note about
  `ShiftingController`).

---

## 9. Configuration

- **No dedicated PDF-rendering configuration was found** in `src/main/resources/application*.properties` —
  grepped for `thymeleaf`, `pdf`, `playwright`, `font`, `upload` and found no matches related to this module.
  Thymeleaf's `StringTemplateResolver`/`SpringTemplateEngine` and the `PdfGenerator`/Playwright setup all run
  with library defaults; there is no configured font path, page-margin override (beyond the hardcoded values
  in `PdfGenerator`, see §9.1), or template cache size.
- **`PdfGenerator`** (`utils/utillity/PdfGenerator.java`, `@Component`, full file — 37 lines) hardcodes its PDF
  page options in code rather than via config:
  ```java
  page.pdf(new Page.PdfOptions()
      .setFormat("A4")
      .setPrintBackground(true)
      .setWidth("210mm")
      .setHeight(height + "px")   // dynamically computed from document.documentElement.scrollHeight
      .setMargin(new Margin().setTop("0mm").setBottom("0mm").setLeft("0mm").setRight("0mm")));
  ```
  It launches a fresh headless Chromium instance (`playwright.chromium().launch(...)`) **per call**, with
  `--no-sandbox`, and closes the browser at the end — there is no shared/pooled browser instance, so every
  quotation/invoice request pays the full cost of starting a browser process. This is a real
  performance/scalability concern for a production quotation-generation endpoint and is not configurable
  today (no toggle for browser reuse, headless pool size, or timeout).
- **Template upload path** is effectively hardcoded to a relative `./Template/` directory (§3.1) rather than
  being driven by any `application.properties` key — unlike `DocumentUploadService.BASE_FILE_URL`, which is at
  least a single `static final` constant (itself hardcoded to a Windows dev path, not externalized either).

---

## 10. External Integrations

**PDF/HTML libraries declared in `build.gradle`:**

```
implementation 'com.microsoft.playwright:playwright:1.49.0'
implementation 'org.xhtmlrenderer:flying-saucer-pdf:9.4.0'
implementation 'com.openhtmltopdf:openhtmltopdf-core:1.0.10'
implementation 'com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10'
implementation 'com.openhtmltopdf:openhtmltopdf-svg-support:1.0.10'
```

**Only one of these is actually wired into the live request path**, and it's worth correcting explicitly
because it's easy to assume otherwise from the imports alone:

- **`PdfGenerator` (`utils/utillity/PdfGenerator.java`) uses Microsoft Playwright (`com.microsoft.playwright`,
  v1.49.0)** — it launches a real headless Chromium browser, loads the rendered HTML via `page.setContent(...)`,
  waits for `WaitUntilState.NETWORKIDLE`, then calls `page.pdf(...)`. This is the class actually invoked by both
  `TemplateService.generateQuotationPdf` and `generateInvoiceHtml` via the injected `pdfGenerator` field.
- **`TemplateService` itself contains two additional, unused HTML→PDF code paths**:
  - `htmlToPdf(String html)` (marked `@Deprecated`, lines 200-209) — uses `org.xhtmlrenderer.pdf.ITextRenderer`
    (the **flying-saucer-pdf** library, v9.4.0 per `build.gradle`).
  - `htmlToPdfConverter(String html)` (lines 211-221) — uses `com.openhtmltopdf.pdfboxout.PdfRendererBuilder`
    with `BatikSVGDrawer` for SVG support (the **openhtmltopdf** family, v1.0.10).
  - **Neither of these two private methods is called from anywhere in the class** (verified: no call sites for
    `htmlToPdf(` or `htmlToPdfConverter(` exist outside their own definitions). They appear to be superseded
    implementations left in place, likely from an earlier iteration of this feature before the switch to
    Playwright. **This directly contradicts `docs/services-and-known-issues.md`'s current one-line summary**
    ("`PdfGenerator` (openhtmltopdf)") — that line should be treated as outdated; this document's finding,
    based on reading `PdfGenerator.java` directly, is that the live path is Playwright/Chromium, not
    openhtmltopdf.
- **Thymeleaf** (`org.thymeleaf.spring6.SpringTemplateEngine`, `StringTemplateResolver`) — used for placeholder
  substitution before the HTML reaches `PdfGenerator`; this is a standard Spring Boot Thymeleaf dependency, not
  a quotation-specific integration, and no explicit version override for it was found in `build.gradle` (so it
  tracks whatever version the `spring-boot-starter-thymeleaf` BOM pulls in — **Unverified/Inferred**, not
  independently confirmed by reading the full `build.gradle` dependency-resolution output).

**Practical implication of the Playwright dependency**: because `PdfGenerator` launches a real Chromium
browser process, the runtime environment must have Playwright's Chromium binary installed/downloaded (normally
via `mvn/gradle playwright install` or the Playwright CLI) — this is an operational dependency beyond a plain
JAR on the classpath, and no evidence of an explicit `playwright install` step was found in this pass (not
searched for CI/build scripts specifically — **Unverified**).

---

## 11. Important Business Rules & Validations

### 11.1 Pricing math — the exact fields summed (`TemplateService.computeSubTotal`, lines 223-238)

`subTotal` is the sum of these `Charges` fields (each passed through `nullSafe(Double v)` which maps `null` to
`0.0`, lines 240-242):

| # | Charges field | Column |
|---|---|---|
| 1 | `transportCharge` | `transport_charge` |
| 2 | `packingCharge` | `packing_charge` |
| 3 | `loadingCharge` | `loading_charge` |
| 4 | `unloadingCharge` | `unloading_charge` |
| 5 | `unpackingCharge` | `unpacking_charge` |
| 6 | `carTransportCharge` | `car_transport_charge` |
| 7 | `riskInsureCharge` | `risk_insure_charge` |
| 8 | `storageCharge` | `storage_charge` |
| 9 | `statisticalCharge` | `statistical_charge` |
| 10 | `serviceCharge` | `service_charge` |
| 11+ | every `ExtraCharges.chargeValue` in `charges.getExtraCharges()` | `tblm_extra_charges.charge_value` |

**Fields explicitly NOT included in `subTotal`**: `serviceRate` (a `Float` rate/percentage, presumably meant to
derive `serviceCharge` but not consulted here), `gstRate` (a `Float` percentage — also not used to compute
`gstCharge`; `gstCharge` is read as a pre-stored value, not derived), and `ExtraCharges.chargeRate` (present on
the entity but not summed or otherwise used in the math — only `chargeValue` is added).

Then:

```
gstAmount  = nullSafe(charges.getGstCharge())     // the *stored* GST amount, not subTotal * gstRate
grandTotal = subTotal + gstAmount
```

**This means GST is never recalculated from a rate at PDF-generation time** — whatever value was previously
saved into `Charges.gstCharge` (by whatever process created/edited the `Charges` row — outside this module's
scope) is trusted as-is and simply added on top of the freshly-summed `subTotal`. If `gstCharge` and `gstRate`
ever drift out of sync with `subTotal` (e.g. a charge line item was edited after GST was calculated), the PDF
will silently show an inconsistent GST amount relative to the recalculated subtotal — there is no
cross-validation between `gstCharge` and `gstRate * subTotal` anywhere in this flow.

### 11.2 Other business rules observed

- **No discounting, rounding, or currency-formatting logic exists in `TemplateService`** — all values are raw
  `Double` sums passed to the template as-is; any currency symbol, thousands separator, or decimal rounding
  must be handled entirely in the Thymeleaf HTML template itself (not reviewed here — template content is
  runtime data in the `Template` table, not source-controlled).
- **`tokenMoney`** (advance/token payment) is part of `QuotationDTO` but is never computed or populated —
  always `null` — so any template referencing `${quotation.tokenMoney}` renders a blank/`null` value.
  Confirmed: no assignment to `tokenMoney` exists anywhere except the `.tokenMoney(null)` builder call in both
  `generateQuotationPdf` and `generateInvoiceHtml`.
- **Invoice due date is a fixed, non-configurable 7-day term** (`LocalDate.now().plusDays(7)`,
  `TemplateService.java:287`) — no per-company or per-customer payment-terms configuration is consulted.
- **Invoice number generation is effectively broken beyond 2026**: `"INV-" + shiftingId + "-2026"` hardcodes the
  literal string `2026` rather than deriving it from `LocalDate.now().getYear()` — every invoice generated by
  this code, in any year, will carry the suffix `-2026`.
- **No idempotency / no persisted record of "an invoice/quotation was generated for this shifting"** — nothing
  prevents generating the same invoice number twice, and there is no way to look up "was a quotation ever sent
  for shifting #123" — this falls directly out of §0 (no persisted `Quotation`/`Invoice` entity exists).

---

## 12. Dependencies Between Modules

- **Hard dependency on the customer-shifting module** (`docs/modules/customer-shifting.md`) for `Shifting`,
  `Customer`, `Charges`, and `ExtraCharges` — this module reads all four entities but owns none of them, has no
  repository of its own for any of them, and performs no writes to any of them. If `ShiftingRepository.
  findShiftingWithCustomerAndCharges` or the `Shifting.customer`/`Shifting.charge` mappings ever change shape,
  this module's rendering breaks silently (wrong/missing data in the PDF) rather than at compile time in most
  cases, since field access happens through `Map<String,Object>` construction in `getCustomerMap`/
  `getShiftingMap`/`getChargesMap` rather than typed Thymeleaf model binding.
- **Hard dependency on the tenant/company module** (`docs/modules/tenant-company.md`) for `Company` (branding,
  contact info, GST/PAN numbers) and `CompanyAsset`/`CompanyService.getAssetAsBase64` (logo/seal images as
  base64 data URIs). Notably the company is resolved via `Customer.companyId`, not `Shifting.companyId` — see
  §3.2 step 3 and the cross-tenant risk noted in §7.
- **This module is exactly the gap that a future `Quotation` entity would need to fill.**
  `docs/WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md` explicitly calls this out twice:
  - Line 18-19: *"Explicitly not in this slice: quotation persistence (still just `TemplateService`'s transient
    PDF flow)..."* — confirming the plan's authors already identified this module as transient/non-persistent
    when scoping a separate (lead/WhatsApp) feature slice.
  - Lines 105-107, discussing webhook error-handling philosophy: *"...per spec §15.1 ('PDF generation failure
    leaves quotation state consistent and retryable') — same principle applies here..."* — implying the target
    product spec (referenced but not itself present in this repo) expects a `Quotation` to have persisted
    **state** that a PDF-generation failure could leave "consistent and retryable." That precondition does not
    hold today: there is no quotation state to leave consistent, because none is ever stored (§0). Any future
    work implementing a real `Quotation` entity/workflow (with a lifecycle, a persisted number, sent/accepted
    status, and links back to the `Shifting` this module already reads) would sit logically **in front of**
    this module — using it as the final "render the current numbers to PDF" step rather than replacing it.
  - Also see `docs/services-and-known-issues.md` lines 35 and 70, which independently flag `TemplateService` and
    `QuotationDTO` as the transient-only implementation and "the gap a real `Quotation` entity needs to fill" —
    this document's §0 and this section corroborate and expand on that existing note with the full mechanical
    detail of exactly how the transient flow works today.
- **No dependency on the notification module** — despite quotations/invoices being an obvious candidate for
  "email this PDF to the customer," `TemplateService` never publishes a `GenericNotificationEvent` or otherwise
  invokes anything under `notification/**`; the generated PDF bytes are only ever streamed back in the HTTP
  response, never emailed, stored, or attached to any downstream record.

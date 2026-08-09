# Module Documentation Index

This directory contains one comprehensive README per module of the `code-setu-app` backend (plus the
frontend, documented in the sibling `code-setu-app-ui` repo). Each module doc covers, for that module
alone: API controllers & endpoints, business/service logic, DTOs, entities & database relationships,
repositories, request/response models, security & authentication logic, exception handling,
configuration, external integrations, business rules & validations, and dependencies on/from other
modules — all verified directly against source, with unverified claims explicitly flagged as such.

For the broader picture, see also (one level up, in `docs/`):
- [`controllers.md`](../controllers.md), [`entities.md`](../entities.md), [`api-urls.md`](../api-urls.md) — flat cross-module reference tables
- [`services-and-known-issues.md`](../services-and-known-issues.md) — consolidated list of pre-existing security/architecture gaps that apply across modules, not owned by any one of them
- [`WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md`](../WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md) — forward-looking design for the not-yet-built Lead/WhatsApp/AI-extraction feature

## Module list

| Module | Doc | Covers |
|---|---|---|
| Auth & Security | [`auth-security.md`](./auth-security.md) | Login, JWT, OTP, refresh tokens, Google OAuth, method security |
| Tenant / Company | [`tenant-company.md`](./tenant-company.md) | Company (tenant root), Branch, CompanyAsset |
| Staff / RBAC | [`staff-rbac.md`](./staff-rbac.md) | Staff, Role, Permission, Department |
| Customer & Shifting | [`customer-shifting.md`](./customer-shifting.md) | Customer, Shifting (the core "job/quote" object), Charges, ExtraCharges, ShiftRoom, ShiftPackage |
| Shifting Inventory | [`shifting-inventory.md`](./shifting-inventory.md) | Per-item packed/loaded/unloaded tracking within a Shifting |
| Document Management | [`document-management.md`](./document-management.md) | DocumentMaster/DocumentUpload, plus the separate generic FileController |
| Fleet & Trip | [`fleet-trip.md`](./fleet-trip.md) | Trip execution, Driver, Vehicle, GPS location, assignments — the most mature module |
| Template & Quotation PDF | [`template-quotation.md`](./template-quotation.md) | HTML→PDF rendering for quotations/invoices (no persisted Quotation entity yet) |
| Inquiry (Lead Capture) | [`inquiry-lead-capture.md`](./inquiry-lead-capture.md) | The public contact-form endpoint — today's only "lead" mechanism |
| Notification Engine | [`notification-engine.md`](./notification-engine.md) | Event-driven Email/SMS/WhatsApp/Push dispatch, retry, audit |
| System Config & Plans | [`system-config-plans.md`](./system-config-plans.md) | Generic key/value config (`Clientservice`) + subscription `Plans` |
| Common / Cross-Cutting | [`common-cross-cutting.md`](./common-cross-cutting.md) | Shared DTO envelopes, exception handling, mapper config, URL constants, utilities |
| Frontend (Angular) | [`../../../code-setu-app-ui/docs/modules/frontend-angular.md`](../../../code-setu-app-ui/docs/modules/frontend-angular.md) *(sibling repo)* | Routing, auth flow, every feature module, services catalog |

## How the modules depend on each other

```
                        ┌─────────────────────────┐
                        │  Common / Cross-Cutting  │  (AbstractModel, exception handling,
                        │                          │   response envelopes, mapper config,
                        └────────────┬─────────────┘   URL constants — used by everything)
                                     │
                        ┌────────────▼─────────────┐
                        │      Auth & Security      │  (JWT, LoggedInUser — gates every
                        │                            │   authenticated endpoint below)
                        └────────────┬───────────────┘
                                     │ reads roles/permissions from
                        ┌────────────▼─────────────┐
                        │       Staff / RBAC         │
                        └────────────┬───────────────┘
                                     │ Staff→Company FK
                        ┌────────────▼─────────────┐
                        │     Tenant / Company       │  (tenant root — companyId/companyUuid
                        │                            │   referenced by nearly every module below,
                        └───┬────────────┬───────────┘   inconsistently typed — see
                            │            │                services-and-known-issues.md §3)
          ┌─────────────────▼──┐   ┌─────▼──────────────────┐
          │  Customer & Shifting│   │  System Config & Plans │  (Plans is platform-level,
          │  (the core domain   │   │  (mostly standalone)   │   not tenant-scoped)
          │   object)           │   └─────────────────────────┘
          └──┬────┬────┬────┬───┘
             │    │    │    │
   ┌─────────▼┐ ┌─▼────────▼─┐ ┌──▼──────────────┐ ┌▼──────────────────────┐
   │ Shifting  │ │ Fleet &    │ │ Template &       │ │ Document Management   │
   │ Inventory │ │ Trip       │ │ Quotation PDF    │ │ (attaches to Customer/│
   │(→Shifting)│ │(Trip→      │ │ (renders          │ │  Staff)               │
   │           │ │ Shifting)  │ │  Shifting+Charges) │ │                       │
   └───────────┘ └────────────┘ └──────────────────┘ └───────────────────────┘

   ┌────────────────────────┐        publishes GenericNotificationEvent into
   │ Inquiry (Lead Capture)  │───────────────────┐
   │ (standalone — no        │                   │
   │  tenant/Customer link)  │                   ▼
   └────────────────────────┘        ┌─────────────────────────┐
                                      │   Notification Engine    │  (consumed by Customer&Shifting,
   Customer & Shifting, Staff/RBAC ──▶│                          │   Staff/RBAC, Auth (OTP), and
   also publish into it              └─────────────────────────┘   Inquiry — see that module's
                                                                     "Dependencies" section for exact
                                                                     publishEvent call sites)

   Frontend (Angular, sibling repo) ──▶ calls essentially every backend module above via HTTP;
                                        see frontend-angular.md's "Dependencies on Backend Modules"
                                        section for the exact feature-to-module mapping.
```

Plain-language summary of the dependency edges, since the diagram above is necessarily approximate:

- **Everything** depends on **Common/Cross-Cutting** (base entity fields, response envelopes, exception
  handling, mapper config) and on **Auth & Security** (every non-public endpoint requires a valid JWT).
- **Auth & Security** depends on **Staff/RBAC** for the role/permission data it attaches to a token, and
  on **Tenant/Company** via `Staff.companyId`.
- **Tenant/Company** is the root every other tenant-scoped module points back to via a `companyId`
  (`Integer`) or, inconsistently, `companyUuid` (`String`) column — there is no enforced FK and the two
  styles coexist (documented per-module, consolidated in `../services-and-known-issues.md` §3).
- **Customer & Shifting** is the most depended-upon domain module: **Shifting Inventory**, **Fleet &
  Trip**, and **Template & Quotation PDF** all read from it directly (`Shifting`/`Customer`/`Charges`).
- **Notification Engine** is a sink, not a source — **Customer & Shifting**, **Staff/RBAC** (via OTP),
  and **Inquiry** all publish `GenericNotificationEvent`s into it, but it has no outbound dependency on
  any of them.
- **Inquiry (Lead Capture)** is deliberately standalone today (no tenant field, no `Customer` link) —
  see its module doc's forward-looking section for how the planned `Lead` entity in
  `../WHATSAPP_LEAD_AI_IMPLEMENTATION_PLAN.md` will supersede it.
- **System Config & Plans**' `Plans` half is platform-level (no tenant scoping at all); its
  `Clientservice` half is tenant-adjacent config consumed by **Template & Quotation PDF**, **Tenant/Company**
  (asset uploads), **Fleet & Trip** (trip documents), and **Document Management** — see that module's doc
  for the confirmed bug where the consumed field is wrong (`.getName()` instead of `.getValue()`).
- **Frontend (Angular)** depends on nearly every backend module via HTTP; its own doc maps each feature
  area to the backend module doc it corresponds to.

## Notable cross-module findings from this documentation pass

A few things surfaced by writing these module docs turned out to contradict earlier, less rigorous
documentation in this `docs/` folder and in the backend's own top-level `README.md`. Both have been
corrected in place (see their revision history / the strikethrough-style correction notes left inline),
but are worth restating here since they affect any future WhatsApp/notification work:

1. **`WhatsAppChannelSender` is a mock stub, not a real Twilio integration** — no SDK call, no retry, no
   phone normalization. Only Email and SMS channel senders are real today. See
   [`notification-engine.md`](./notification-engine.md) §12.
2. **Quotation/invoice PDF generation uses Microsoft Playwright** (headless Chromium), not
   openhtmltopdf/flying-saucer as previously documented — those libraries are on the classpath but dead
   code. See [`template-quotation.md`](./template-quotation.md).
3. **Method-level authorization is inert app-wide** (`MethodSecurityConfig.prePostEnabled = false`) —
   every module doc that lists `@PreAuthorize`-gated endpoints notes this same caveat; it's a single root
   cause, documented in full in [`auth-security.md`](./auth-security.md), not restated per-module.
4. Several modules have **no tenant-scoping enforcement at all** despite having a `companyId` column
   (Department, Driver/Vehicle/Trip family, Plans is intentionally platform-level) — the one confirmed
   correct example in the whole codebase is `CustomerService`'s company-scoped queries, documented in
   [`customer-shifting.md`](./customer-shifting.md).

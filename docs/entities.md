# Entities Documentation

> **ORM:** Spring Data JPA + Hibernate
> **Database:** MySQL 8.0
> **Migration:** Liquibase

---

## Table of Contents

### Core Module (`com.cargosetu.model`)
1. [Company](#1-company)
2. [Staff](#2-staff)
3. [Role](#3-role)
4. [Permission](#4-permission)
5. [Department](#5-department)
6. [Branch](#6-branch)
7. [Driver](#7-driver)
8. [Vehicle](#8-vehicle)
9. [VehicleLocation](#9-vehiclelocation)
10. [DriverVehicleAssignment](#10-drivervehicleassignment)
11. [Plans](#11-plans)
12. [Template](#12-template)
13. [Clientservice](#13-clientservice)
14. [CompanyAsset](#14-companyasset)
15. [CustomerInquiry](#15-customerinquiry)
16. [OtpVerification](#16-otpverification)
17. [RefreshToken](#17-refreshtoken)

### Customer Module (`com.cargosetu.customer.entity`)
18. [Customer](#18-customer)
19. [Shifting](#19-shifting)
20. [ShiftRoom](#20-shiftroom)
21. [ShiftPackage](#21-shiftpackage)
22. [Charges](#22-charges)
23. [ExtraCharges](#23-extracharges)
24. [PackagingMaterial](#24-packagingmaterial)
25. [ShiftItemDamage](#25-shiftitemdamage)

### Document Module (`com.cargosetu.document.model`)
26. [DocumentMaster](#26-documentmaster)
27. [DocumentUpload](#27-documentupload)

### Shifting Inventory Module (`com.cargosetu.shifting.entity`)
28. [ShiftInventoryItem](#28-shiftinventoryitem)
29. [ShiftInventoryPackaging](#29-shiftinventorypackaging)
30. [ShiftInventoryPhoto](#30-shiftinventoryphoto)

### Trip Module (`com.cargosetu.model`)
33. [Trip](#33-trip)
34. [TripDocument](#34-tripdocument)
35. [TripExpense](#35-tripexpense)
36. [TripStatusHistory](#36-tripstatushistory)
37. [TripTracking](#37-triptracking)

### Notification Module (`com.cargosetu.notification.model`)
31. [NotificationTemplate](#31-notificationtemplate)
32. [NotificationAudit](#32-notificationaudit)

---

## 1. Company

**Table:** `tblmcompany`
**File:** `src/main/java/com/cargosetu/model/Company.java`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `company_id` | Integer | PK, Auto-increment | Primary key |
| `company_name` | VARCHAR(150) | NOT NULL | Company display name |
| `company_email` | VARCHAR(150) | NOT NULL, UNIQUE | Primary company email |
| `whatsapp_number` | Long | NOT NULL | WhatsApp contact number |
| `contact_no_1` | Long | NOT NULL | Primary contact number |
| `contact_no_2` | Long | nullable | Secondary contact number |
| `landline_no` | String | nullable | Landline number |
| `toll_free_number` | String | nullable | Toll-free number |
| `pan_number` | VARCHAR(10) | NOT NULL, length=10 | PAN card number |
| `gst_number` | VARCHAR(15) | NOT NULL, length=15 | GST registration number |
| `address` | VARCHAR(300) | NOT NULL | Company address |
| `country` | VARCHAR(100) | NOT NULL | Country |
| `state` | VARCHAR(100) | NOT NULL | State |
| `city` | VARCHAR(100) | NOT NULL | City |
| `branch` | VARCHAR(100) | nullable | Branch name |
| `main_branch` | VARCHAR(100) | nullable | Main/head branch |
| `company_uuid` | String | nullable | UUID identifier |
| `created_by_id` | Long | nullable | Staff ID who created |

**Inherits from:** `AbstractModel` (audit fields: createdAt, updatedAt, isActive, etc.)

---

## 2. Staff

**Table:** `tblm_staff`
**File:** `src/main/java/com/cargosetu/model/Staff.java`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `staff_id` | Long | PK, Auto-increment | Primary key |
| `staff_uuid` | String | NOT NULL, UNIQUE | Unique UUID per staff member |
| `username` | String | NOT NULL | Login username |
| `firstName` | String | nullable | First name |
| `lastName` | String | nullable | Last name |
| `mobileNumber` | String | nullable | Primary mobile |
| `mobileCode` | String | nullable | Country code |
| `email` | String | nullable | Email address |
| `password` | String | nullable | Bcrypt-hashed password |
| `staff_type` | VARCHAR(100) | nullable | `SUPERADMIN`, `COMPANY_ADMIN`, `MANAGER`, `DRIVER`, `ACCOUNTANT` |
| `profile_image_url` | String | nullable | Profile picture URL |
| `alternate_mobile` | String | nullable | Alternate mobile number |
| `address_line` | String | nullable | Address |
| `city` | String | nullable | City |
| `state` | String | nullable | State |
| `pincode` | String | nullable | Pincode |
| `license_number` | String | nullable | Driving license number |
| `license_expiry_date` | LocalDate | nullable | License expiry |
| `last_login_at` | LocalDateTime | nullable | Last login timestamp |
| `failed_login_attempts` | Integer | default=0 | Failed login count |
| `password_changed_at` | LocalDateTime | nullable | When password was last changed |
| `previous_password` | String | nullable | Previous password hash |
| `department_id` | FK → Department | nullable | Assigned department |
| `company_id` | Integer | nullable | Owning company ID |

**Join Table:** `tblm_user_roles` → links Staff ↔ Role (ManyToMany)

---

## 3. Role

**Table:** `tblm_roles`
**File:** `src/main/java/com/cargosetu/model/Role.java`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `role_id` | Long | PK, Auto-increment | Primary key |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | Role code (e.g., `COMPANY_ADMIN`) |
| `role_name` | VARCHAR(100) | NOT NULL | Human-readable name |
| `description` | TEXT | nullable | Role description |
| `scope` | VARCHAR(50) | NOT NULL | Scope (`GLOBAL`, `COMPANY`, etc.) |

**Join Table:** `tblm_role_permissions` → links Role ↔ Permission (ManyToMany)

---

## 4. Permission

**Table:** `tblm_permissions` (inferred)
**File:** `src/main/java/com/cargosetu/model/Permission.java`

| Column | Type | Description |
|--------|------|-------------|
| `permission_id` | Long | PK |
| `code` | String | Permission code (e.g., `COMPANY_CREATE`) |
| `name` | String | Display name |
| `description` | String | What this permission allows |

---

## 5. Department

**Table:** `tblm_department`
**File:** `src/main/java/com/cargosetu/model/Department.java`
**Unique Constraint:** (`company_uuid`, `dept_code`)

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `departmentId` | Integer | PK, Auto-increment | Primary key |
| `department_uuid` | String | NOT NULL, immutable | UUID identifier |
| `company_uuid` | String | NOT NULL | Owning company UUID |
| `dept_code` | VARCHAR(50) | NOT NULL | Code: `MARKETING_EXECUTIVE`, `PACKAGING`, `ONSITE`, etc. |
| `name` | VARCHAR(100) | NOT NULL | Department name |
| `description` | String | nullable | Description |

---

## 6. Branch

**Table:** `branch`
**File:** `src/main/java/com/cargosetu/model/Branch.java`

| Column | Type | Description |
|--------|------|-------------|
| `branchId` | Long | PK |
| `branchName` | String | Branch name |
| `branchCode` | String | Unique branch code |
| `contactPerson` | String | Contact person name |
| `mobile` | String | Contact mobile |
| `email` | String | Branch email |
| `gstNumber` | String | GST number for branch |
| `addressLine1` | String | Address line 1 |
| `addressLine2` | String | Address line 2 |
| `city` | String | City |
| `state` | String | State |
| `country` | String | Country |
| `pincode` | String | Pincode |
| `headOffice` | Boolean | default=false |
| `active` | Boolean | default=true |
| `companyId` | Long | FK to Company |

---

## 7. Driver

**Table:** `driver` (default JPA naming)
**File:** `src/main/java/com/cargosetu/model/Driver.java`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | Long | PK, Auto-increment | Primary key |
| `name` | String | NOT NULL, 2–100 chars | Driver full name |
| `phoneNumber` | String | NOT NULL, 10–15 digits | Mobile number |
| `email` | String | NOT NULL, valid email | Email address |
| `licenseNumber` | String | NOT NULL, UNIQUE | Driving license number |
| `licenseExpiryDate` | LocalDate | NOT NULL, must be future | License expiry date |
| `experienceYears` | Integer | 0–60 | Years of experience |
| `address` | VARCHAR(255) | nullable | Home address |
| `status` | String | `ACTIVE`/`INACTIVE`/`SUSPENDED` | Current status |
| `joiningDate` | LocalDate | past or present | Date of joining |
| `createdAt` | LocalDateTime | auto, immutable | Creation timestamp |
| `updatedAt` | LocalDateTime | auto | Last update timestamp |

---

## 8. Vehicle

**Table:** `vehicles`
**File:** `src/main/java/com/cargosetu/model/Vehicle.java`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | Long | PK, Auto-increment | Primary key |
| `chassisNumber` | String | NOT NULL, UNIQUE | Chassis number |
| `engineNumber` | String | NOT NULL, UNIQUE | Engine number |
| `mileage` | Double | >= 0 | Mileage reading |
| `registrationNumber` | String | NOT NULL, UNIQUE, 5–20 chars | Vehicle registration plate |
| `vehicleType` | String | NOT NULL | `TRUCK`, `VAN`, `LCV`, etc. |
| `model` | String | NOT NULL | Vehicle model name |
| `manufacturer` | String | NOT NULL | Manufacturer brand |
| `capacityKg` | Integer | NOT NULL, >= 1 | Load capacity in kg |
| `status` | String | `AVAILABLE`/`ON_TRIP`/`MAINTENANCE`/`OUT_OF_SERVICE` | Current status |
| `insuranceExpiryDate` | LocalDate | NOT NULL, future | Insurance expiry |
| `pollutionExpiryDate` | LocalDate | NOT NULL, future | Pollution cert. expiry |
| `fuelType` | String | `DIESEL`/`PETROL`/`ELECTRIC`/`CNG` | Fuel type |
| `registrationDate` | LocalDate | past or present | Registration date |
| `currentLocation` | String | nullable | Last known location string |
| `createdAt` | LocalDateTime | auto, immutable | Creation timestamp |
| `updatedAt` | LocalDateTime | auto | Last update timestamp |

---

## 9. VehicleLocation

**Table:** `vehicle_location` (inferred)
**File:** `src/main/java/com/cargosetu/model/VehicleLocation.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `vehicleId` | Long | FK to Vehicle |
| `lat` | Double | GPS latitude |
| `lng` | Double | GPS longitude |
| `timestamp` | LocalDateTime | When location was recorded |

---

## 10. DriverVehicleAssignment

**Table:** `driver_vehicle_assignment` (inferred)
**File:** `src/main/java/com/cargosetu/model/DriverVehicleAssignment.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `driverId` | Long | FK to Driver |
| `vehicleId` | Long | FK to Vehicle |
| `assignedAt` | LocalDateTime | Assignment timestamp |
| `unassignedAt` | LocalDateTime | Unassignment timestamp |
| `description` | String | Reason/notes for unassignment |
| `active` | Boolean | Whether assignment is current |

---

## 11. Plans

**Table:** `tblmplans`
**File:** `src/main/java/com/cargosetu/model/Plans.java`

| Column | Type | Description |
|--------|------|-------------|
| `plan_id` | Integer | PK, Auto-increment |
| `plan_name` | String | Plan name |
| `plan_validity` | LocalDate | Validity/expiry date |
| `price` | Double | Plan price |
| `plan_category` | String | Category (e.g., Basic, Pro) |

**Inherits from:** `AbstractModel`

---

## 12. Template

**Table:** `template` (inferred)
**File:** `src/main/java/com/cargosetu/model/Template.java`

Stores HTML templates used to generate quotation PDFs and invoice documents for shifting orders.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `name` | String | Template name |
| `type` | String | Template type (e.g., `QUOTATION`, `INVOICE`) |
| `filePath` | String | Stored file path |
| `companyId` | Integer | Owning company |

---

## 13. Clientservice

**Table:** `client_service` (inferred)
**File:** `src/main/java/com/cargosetu/model/Clientservice.java`

Stores system configuration entries used by internal services (e.g., document upload path config).

| Column | Type | Description |
|--------|------|-------------|
| `id` | Integer | PK |
| `name` | String | Service/config name (e.g., `Staff Document`) |
| `value` | String | Configuration value |
| `description` | String | Description |

---

## 14. CompanyAsset

**Table:** `company_asset` (inferred)
**File:** `src/main/java/com/cargosetu/model/CompanyAsset.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `companyId` | Integer | FK to Company |
| `assetType` | String | Asset category |
| `filePath` | String | Stored file path |
| `uploadedAt` | LocalDateTime | Upload timestamp |

---

## 15. CustomerInquiry

**Table:** `customer_inquiry` (inferred)
**File:** `src/main/java/com/cargosetu/model/CustomerInquiry.java`

Stores inbound inquiries submitted from the public inquiry form.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `name` | String | Inquirer name |
| `email` | String | Contact email |
| `phone` | String | Contact phone |
| `message` | String | Inquiry message |
| `status` | InquiryStatus | `NEW`, `CONTACTED`, `RESOLVED`, `CLOSED` (verified in `InquiryStatus.java`) |
| `createdAt` | LocalDateTime | Submission timestamp |

---

## 16. OtpVerification

**Table:** `otp_verification` (inferred)
**File:** `src/main/java/com/cargosetu/model/OtpVerification.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `mobileNumber` | String | Mobile number OTP was sent to |
| `otpCode` | String | Generated OTP code |
| `expiresAt` | LocalDateTime | OTP expiry time |
| `verified` | Boolean | Whether OTP has been used |

---

## 17. RefreshToken

**Table:** `refresh_token` (inferred)
**File:** `src/main/java/com/cargosetu/model/RefreshToken.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `staffId` | Long | FK to Staff |
| `token` | String | JWT refresh token value |
| `expiryDate` | LocalDateTime | Token expiry |
| `revoked` | Boolean | Whether token is revoked |

---

## 18. Customer

**Table:** `tblm_customers`
**File:** `src/main/java/com/cargosetu/customer/entity/Customer.java`

| Column | Type | Description |
|--------|------|-------------|
| `customer_id` | Long | PK, Auto-increment |
| `firstname` | String | First name |
| `lastname` | String | Last name |
| `mobile_no` | String | Primary mobile |
| `alternate_mobile_no` | String | Alternate mobile |
| `email` | String | Personal email |
| `work_email` | String | Work email |
| `company_id` | Integer | Owning company ID |
| `uuid` | String | Unique UUID |
| `is_delete` | Boolean | Soft delete flag (default=false) |
| `createdbyid` | Long | Staff ID who created the record |

**Relations:**
- `shiftings` — OneToMany → `Shifting` (cascade ALL)
- `documentUploads` — OneToMany → `DocumentUpload`

---

## 19. Shifting

**Table:** `tblm_shifting`
**File:** `src/main/java/com/cargosetu/customer/entity/Shifting.java`

| Column | Type | Description |
|--------|------|-------------|
| `shifting_id` | Long | PK, Auto-increment |
| `shift_completed` | Boolean | Completion flag (default=false) |
| `shift_from_city` | String | Origin city |
| `shift_to_city` | String | Destination city |
| `from_address` | String | Full origin address |
| `to_address` | String | Full destination address |
| `shifting_type` | String | Type of shifting (House, Office, etc.) |
| `shifting_Date` | LocalDate | Shifting start date |
| `shift_reach_date` | LocalDate | Expected/actual delivery date |
| `shift_completed_by` | String | Name of who completed |
| `shift_complted_by_id` | Long | Staff ID who completed |
| `customer_id` | FK → Customer | ManyToOne |
| `charge_id` | FK → Charges | OneToOne (cascade ALL) |
| `shift_status` | String | Current status string |

---

## 20. ShiftRoom

**Table:** `shift_room` (inferred)
**File:** `src/main/java/com/cargosetu/customer/entity/ShiftRoom.java`

Tracks individual rooms included in a shifting order.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `shiftingId` | Long | FK to Shifting |
| `roomName` | String | Room name/type (Bedroom, Living Room, etc.) |
| `floor` | Integer | Floor number |

---

## 21. ShiftPackage

**Table:** `shift_package` (inferred)
**File:** `src/main/java/com/cargosetu/customer/entity/ShiftPackage.java`

Tracks packages within a shift.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `shiftingId` | Long | FK to Shifting |
| `packageType` | String | Package category |
| `quantity` | Integer | Number of packages |
| `weight` | Double | Weight in kg |

---

## 22. Charges

**Table:** `charges` (inferred, with `charge_id` as PK)
**File:** `src/main/java/com/cargosetu/customer/entity/Charges.java`

Stores the main charge/pricing for a shifting order.

| Column | Type | Description |
|--------|------|-------------|
| `charge_id` | Long | PK |
| `baseAmount` | Double | Base service charge |
| `packingCharge` | Double | Packing charge |
| `transportCharge` | Double | Transport charge |
| `labourCharge` | Double | Labour charge |
| `totalAmount` | Double | Total before extras |
| `taxAmount` | Double | GST/tax |
| `grandTotal` | Double | Final amount |

---

## 23. ExtraCharges

**Table:** `extra_charges` (inferred)
**File:** `src/main/java/com/cargosetu/customer/entity/ExtraCharges.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `chargeId` | Long | FK to Charges |
| `description` | String | Reason for extra charge |
| `amount` | Double | Extra charge amount |

---

## 24. PackagingMaterial

**Table:** `packaging_material` (inferred)
**File:** `src/main/java/com/cargosetu/customer/entity/PackagingMaterial.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `shiftingId` | Long | FK to Shifting |
| `materialName` | String | Name of material (bubble wrap, boxes, etc.) |
| `quantity` | Integer | Quantity used |
| `unitCost` | Double | Cost per unit |

---

## 25. ShiftItemDamage

**Table:** `shift_item_damage` (inferred)
**File:** `src/main/java/com/cargosetu/customer/entity/ShiftItemDamage.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `shiftingId` | Long | FK to Shifting |
| `itemName` | String | Damaged item name |
| `description` | String | Damage description |
| `reportedAt` | LocalDateTime | When damage was reported |
| `reportedBy` | Long | Staff ID who reported |

---

## 26. DocumentMaster

**Table:** `document_master` (inferred)
**File:** `src/main/java/com/cargosetu/document/model/DocumentMaster.java`

Defines which documents are required/accepted in the system.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `documentCode` | String | Unique code (e.g., `AADHAR`, `PAN`) |
| `documentName` | String | Display name |
| `required` | Boolean | Whether mandatory |
| `applicableTo` | String | Who needs it (STAFF, CUSTOMER) |

---

## 27. DocumentUpload

**Table:** `document_upload` (inferred)
**File:** `src/main/java/com/cargosetu/document/model/DocumentUpload.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `customerId` | Long | FK to Customer (nullable) |
| `staffId` | Long | FK to Staff (nullable) |
| `documentCode` | String | FK to DocumentMaster code |
| `filePath` | String | Stored file location |
| `uploadedAt` | LocalDateTime | Upload timestamp |
| `status` | DocumentStatus | `PENDING`, `APPROVED`, `REJECTED` |

---

## 28. ShiftInventoryItem

**Table:** `shift_inventory_item` (inferred)
**File:** `src/main/java/com/cargosetu/shifting/entity/ShiftInventoryItem.java`

Tracks individual items in a shift's inventory.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `shiftingId` | Long | FK to Shifting |
| `itemName` | String | Item name |
| `category` | String | Item category |
| `quantity` | Integer | Quantity |
| `isPacked` | Boolean | Packed flag |
| `isLoaded` | Boolean | Loaded onto vehicle flag |
| `isUnloaded` | Boolean | Unloaded at destination flag |

---

## 29. ShiftInventoryPackaging

**Table:** `shift_inventory_packaging` (inferred)
**File:** `src/main/java/com/cargosetu/shifting/entity/ShiftInventoryPackaging.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `inventoryItemId` | Long | FK to ShiftInventoryItem |
| `packagingType` | String | Box, Bubble wrap, etc. |
| `quantity` | Integer | Quantity used |

---

## 30. ShiftInventoryPhoto

**Table:** `shift_inventory_photo` (inferred)
**File:** `src/main/java/com/cargosetu/shifting/entity/ShiftInventoryPhoto.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `inventoryItemId` | Long | FK to ShiftInventoryItem |
| `photoUrl` | String | URL/path to photo |
| `capturedAt` | LocalDateTime | When photo was taken |
| `stage` | String | `BEFORE_PACK`, `AFTER_PACK`, `DELIVERED` |

---

## 31. NotificationTemplate

**Table:** `notification_template` (inferred)
**File:** `src/main/java/com/cargosetu/notification/model/NotificationTemplate.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `templateCode` | String | Unique code (e.g., `USER_REGISTERED`) |
| `channel` | NotificationChannel | `EMAIL`, `SMS`, `PUSH` |
| `subjectTemplate` | String | Email subject (Thymeleaf template) |
| `bodyTemplate` | TEXT | Message body template |
| `isActive` | Boolean | Whether template is enabled |

---

## 32. NotificationAudit

**Table:** `notification_audit` (inferred)
**File:** `src/main/java/com/cargosetu/notification/model/NotificationAudit.java`

Audit log for every notification sent by the system.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `templateCode` | String | Which template was used |
| `channel` | NotificationChannel | `EMAIL`, `SMS`, `PUSH` |
| `recipient` | String | Email/phone/device token |
| `status` | NotificationStatus | `SENT`, `FAILED`, `PENDING` |
| `sentAt` | LocalDateTime | When notification was dispatched |
| `errorMessage` | String | Error details if failed |

---

## 33. Trip

**Table:** `tbl_trip`
**File:** `src/main/java/com/cargosetu/model/Trip.java`

| Column | Type | Description |
|--------|------|-------------|
| `tripId` | Long | PK, Auto-increment |
| `tripNumber` | String | Human-readable trip reference number |
| `shifting` | FK → Shifting | ManyToOne — the shifting order this trip belongs to |
| `driver` | FK → Driver | ManyToOne — assigned driver |
| `vehicle` | FK → Vehicle | ManyToOne — assigned vehicle |
| `plannedStartTime` | LocalDateTime | Scheduled trip start |
| `actualStartTime` | LocalDateTime | Actual departure time |
| `actualEndTime` | LocalDateTime | Actual arrival/completion time |
| `plannedDistanceKm` | Double | Estimated distance in km |
| `actualDistanceKm` | Double | Actual distance covered |
| `status` | TripStatus (Enum) | Current trip status |
| `remarks` | String | Notes/remarks |
| `active` | Boolean | Whether trip record is active |
| `createdAt` | LocalDateTime | Creation timestamp |
| `updatedAt` | LocalDateTime | Last update timestamp |

---

## 34. TripDocument

**Table:** `trip_document`
**File:** `src/main/java/com/cargosetu/model/TripDocument.java`

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK, Auto-increment |
| `trip` | FK → Trip | ManyToOne — owning trip |
| `documentType` | String | Type of document (e.g., `LOADING_SLIP`, `DELIVERY_RECEIPT`) |
| `documentUrl` | String | Stored file URL/path |
| `fileName` | String | Original file name |

---

## 35. TripExpense

**Table:** `tbl_trip_expense`
**File:** `src/main/java/com/cargosetu/model/TripExpense.java`

| Column | Type | Description |
|--------|------|-------------|
| `expenseId` | Long | PK, Auto-increment |
| `trip` | FK → Trip | ManyToOne — owning trip |
| `expenseType` | ExpenseType (Enum) | Category of expense |
| `amount` | Double | Expense amount |
| `description` | String | Expense description |
| `billNumber` | String | Bill/invoice number |
| `receiptUrl` | String | Receipt file URL/path |
| `expenseDate` | LocalDate | Date expense was incurred |

---

## 36. TripStatusHistory

**Table:** `trip_status_history`
**File:** `src/main/java/com/cargosetu/model/TripStatusHistory.java`

Audit trail of every status change on a Trip.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK, Auto-increment |
| `trip` | FK → Trip | ManyToOne — which trip changed |
| `oldStatus` | String | Previous status value |
| `newStatus` | String | New status value |
| `changedBy` | String | Username of who made the change |
| `changedAt` | LocalDateTime | When the status change occurred |

---

## 37. TripTracking

**Table:** `TripTracking` (default JPA naming)
**File:** `src/main/java/com/cargosetu/model/TripTracking.java`

Real-time GPS tracking points for a trip.

| Column | Type | Description |
|--------|------|-------------|
| `id` | Long | PK |
| `trip` | FK → Trip | ManyToOne — owning trip |
| `latitude` | Double | GPS latitude |
| `longitude` | Double | GPS longitude |
| `location` | String | Human-readable location label |
| `trackedAt` | LocalDateTime | When the location was recorded |

---

## Enumerations

| Enum | Values | Used In |
|------|--------|---------|
| `NotificationChannel` | `EMAIL`, `SMS`, `PUSH` | NotificationTemplate, NotificationAudit |
| `NotificationStatus` | `SENT`, `FAILED`, `PENDING` | NotificationAudit |
| `DocumentStatus` | `PENDING`, `APPROVED`, `REJECTED` | DocumentUpload |
| `InquiryStatus` | `NEW`, `CONTACTED`, `RESOLVED`, `CLOSED` | CustomerInquiry |
| `TripStatus` | Enum values in `TripStatus.java` | Trip |
| `ExpenseType` | Enum values in `ExpenseType.java` | TripExpense |

---

## Inheritance

All core entities in `com.cargosetu.model` extend `AbstractModel` which provides:

| Field | Type | Description |
|-------|------|-------------|
| `createdAt` | LocalDateTime | Auto-set on create |
| `updatedAt` | LocalDateTime | Auto-set on update |
| `isActive` | Boolean | Soft-delete/active flag |

# Controllers Documentation

> **Base URL:** `/api/v2/cargosetu`
> **Server Port:** `31201`
> **Tech Stack:** Spring Boot 3.5.x · Spring Security · JWT Auth

---

## Table of Contents

1. [AuthController](#1-authcontroller)
2. [OtpAuthController](#2-otpauthcontroller)
3. [BaseApiController](#3-baseapicontroller)
4. [CompanyController](#4-companycontroller)
5. [StaffController](#5-staffcontroller)
6. [DepartmentController](#6-departmentcontroller)
7. [BranchController](#7-branchcontroller)
8. [RoleController](#8-rolecontroller)
9. [PermissionController](#9-permissioncontroller)
10. [DriverController](#10-drivercontroller)
11. [VehicleController](#11-vehiclecontroller)
12. [VehicleLocationController](#12-vehiclelocationcontroller)
13. [DriverVehicleAssignmentController](#13-drivervehicleassignmentcontroller)
14. [CustomerController](#14-customercontroller)
15. [ShiftingController](#15-shiftingcontroller)
16. [PlanController](#16-plancontroller)
17. [FileController](#17-filecontroller)
18. [DocumentController](#18-documentcontroller)
19. [TemplateGenerationController](#19-templategenerationcontroller)
20. [ClientServiceController](#20-clientservicecontroller)
21. [InquiryController](#21-inquirycontroller)
22. [ShiftInventoryController](#22-shiftinventorycontroller)
23. [TripController](#23-tripcontroller)
24. [TripDocumentController](#24-tripdocumentcontroller)
25. [TripExpenseController](#25-tripexpensecontroller)
26. [TripStatusHistoryController](#26-tripstatushistorycontroller)
27. [NotificationAdminController](#27-notificationadmincontroller)
28. [DemoNotificationController](#28-demonotificationcontroller)
29. [ViewController](#29-viewcontroller)
30. [DemoController](#30-democontroller)

---

## 1. AuthController

**File:** `src/main/java/com/cargosetu/controller/AuthController.java`
**Base Mapping:** `/auth`
**Auth Required:** No

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/login` | Login with username + password; returns JWT access & refresh tokens |
| POST | `/auth/user/register` | Register a new staff/user account |

**Login Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Login Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

**Register Request Body:** `StaffDTO` (see Entities doc for fields)

---

## 2. OtpAuthController

**File:** `src/main/java/com/cargosetu/controller/OtpAuthController.java`
**Base Mapping:** `/auth/otp`
**Auth Required:** No

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/otp/send` | Send OTP to mobile number via SMS |
| POST | `/auth/otp/verify` | Verify OTP and return JWT tokens |

**Send OTP Request Body:**
```json
{ "mobileNumber": "string" }
```

**Verify OTP Request Body:**
```json
{
  "mobileNumber": "string",
  "otpCode": "string"
}
```

---

## 3. CompanyController

**File:** `src/main/java/com/cargosetu/controller/CompanyController.java`
**Base Mapping:** `/api/v2/cargosetu/company`
**Auth Required:** Yes
**Content-Type:** `multipart/form-data` for create; `application/json` for update

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/company` | `COMPANY_CREATE` | Create new company (multipart) |
| PUT | `/api/v2/cargosetu/company` | `COMPANY_UPDATE_DELETE` | Update existing company |
| GET | `/api/v2/cargosetu/company/{uuid}` | `COMPANY_READ` | Get company by UUID |
| GET | `/api/v2/cargosetu/company` | `COMPANY_READ` | Get all companies |
| DELETE | `/api/v2/cargosetu/company/{id}` | `COMPANY_UPDATE_DELETE` | Delete company by ID |
| GET | `/api/v2/cargosetu/company/currentCompany` | `COMPANY_READ` | Get logged-in user's company |
| POST | `/api/v2/cargosetu/company/assets` | `COMPANY_CREATE` | Upload company assets (multipart) |

---

## 4. StaffController

**File:** `src/main/java/com/cargosetu/controller/StaffController.java`
**Base Mapping:** `/api/v2/cargosetu/staff`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/api/v2/cargosetu/staff/{id}` | `STAFF_READ` or `COMPANY_ADMIN` | Get staff by ID |
| POST | `/api/v2/cargosetu/staff` | `STAFF_CREATE` | Create new staff member |
| DELETE | `/api/v2/cargosetu/staff/{staffId}` | — | Delete staff by ID |
| GET | `/api/v2/cargosetu/staff?page=0&size=5` | `STAFF_READ` or `COMPANY_ADMIN` | Get all staff (paginated) |

**Query Params (GET all):**
- `page` (default: `0`) — page number
- `size` (default: `5`) — page size

---

## 5. DepartmentController

**File:** `src/main/java/com/cargosetu/controller/DepartmentController.java`
**Base Mapping:** `/api/v2/cargosetu/department`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/cargosetu/department` | Get all departments |
| POST | `/api/v2/cargosetu/department` | Create department |
| GET | `/api/v2/cargosetu/department/{id}` | Get department by ID |
| PUT | `/api/v2/cargosetu/department/{id}` | Update department |
| DELETE | `/api/v2/cargosetu/department/{id}` | Delete department |

---

## 6. BranchController

**File:** `src/main/java/com/cargosetu/controller/BranchController.java`
**Base Mapping:** `/api/v2/cargosetu/branches`
**Auth Required:** Yes
**Permission Required:** `COMPANY_CREATE` (all endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/cargosetu/branches` | Create new branch |
| PUT | `/api/v2/cargosetu/branches/{branchId}` | Update branch |
| GET | `/api/v2/cargosetu/branches/{branchId}` | Get branch by ID |
| DELETE | `/api/v2/cargosetu/branches/{branchId}` | Delete branch |

---

## 7. RoleController

**File:** `src/main/java/com/cargosetu/controller/RoleController.java`
**Base Mapping:** `/api/v2/cargosetu/role`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/api/v2/cargosetu/role` | — | Get all roles |
| GET | `/api/v2/cargosetu/role/{id}` | — | Get role by ID |
| POST | `/api/v2/cargosetu/role` | `STAFF_CREATE` | Create new role |
| PUT | `/api/v2/cargosetu/role` | — | Update role |
| DELETE | `/api/v2/cargosetu/role/{id}` | — | Delete role |

---

## 8. PermissionController

**File:** `src/main/java/com/cargosetu/controller/PermissionController.java`
**Base Mapping:** `/api/v2/cargosetu/permission`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/cargosetu/permission` | Get all permissions |
| GET | `/api/v2/cargosetu/permission/{id}` | Get permission by ID |
| POST | `/api/v2/cargosetu/permission` | Create permission |
| PUT | `/api/v2/cargosetu/permission/{id}` | Update permission |
| DELETE | `/api/v2/cargosetu/permission/{id}` | Delete permission |

---

## 9. DriverController

**File:** `src/main/java/com/cargosetu/controller/DriverController.java`
**Base Mapping:** `/api/v2/cargosetu/drivers`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/cargosetu/drivers` | Create driver |
| GET | `/api/v2/cargosetu/drivers/{id}` | Get driver by ID |
| GET | `/api/v2/cargosetu/drivers` | Get all drivers |
| PUT | `/api/v2/cargosetu/drivers/{id}` | Update driver |
| DELETE | `/api/v2/cargosetu/drivers/{id}` | Delete driver |

---

## 10. VehicleController

**File:** `src/main/java/com/cargosetu/controller/VehicleController.java`
**Base Mapping:** `/vehicle`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/vehicle` | Create vehicle |
| GET | `/vehicle/{id}` | Get vehicle by ID |
| GET | `/vehicle` | Get all vehicles |
| PUT | `/vehicle/{id}` | Update vehicle |
| DELETE | `/vehicle/{id}` | Delete vehicle |

---

## 11. VehicleLocationController

**File:** `src/main/java/com/cargosetu/controller/VehicleLocationController.java`
**Base Mapping:** `/api/v2/cargosetu/vehicle/location`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/cargosetu/vehicle/location` | Update/save vehicle GPS location |
| GET | `/api/v2/cargosetu/vehicle/{vehicleId}/location` | Get latest location for a vehicle |

**Update Location Request Body:**
```json
{
  "vehicleId": 1,
  "lat": 18.5204,
  "lng": 73.8567
}
```

---

## 12. DriverVehicleAssignmentController

**File:** `src/main/java/com/cargosetu/controller/DriverVehicleAssignmentController.java`
**Base Mapping:** `/api/v2/cargosetu/assignments`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/cargosetu/assignments?driverId=&vehicleId=` | Assign driver to vehicle |
| GET | `/api/v2/cargosetu/assignments` | Get all driver-vehicle assignments |
| PUT | `/api/v2/cargosetu/assignments/unassign/{id}` | Unassign a driver from vehicle |

**Query Params (POST):**
- `driverId` — Driver ID (Long)
- `vehicleId` — Vehicle ID (Long)

---

## 13. CustomerController

**File:** `src/main/java/com/cargosetu/customer/controller/CustomerController.java`
**Base Mapping:** `/api/v2/cargosetu/customer`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/customer` | `CUSTOMER_CREATE` | Create customer |
| PUT | `/api/v2/cargosetu/customer` | `CUSTOMER_UPDATE` | Update customer |
| DELETE | `/api/v2/cargosetu/customer` | `CUSTOMER_DELETE` | Delete customer |
| GET | `/api/v2/cargosetu/customer/{uuid}` | `CUSTOMER_VIEW` | Get customer by UUID |
| GET | `/api/v2/cargosetu/customer?pageNumber=&pageSize=` | `CUSTOMER_VIEW` | Get all customers (paginated) |

---

## 14. ShiftingController

**File:** `src/main/java/com/cargosetu/customer/controller/ShiftingController.java`
**Base Mapping:** `/api/v2/cargosetu/shifts`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/shifts` | `SHIFT_CREATE` | Create shifting order |
| GET | `/api/v2/cargosetu/shifts/{shiftId}` | `SHIFT_READ` | Get shifting by ID |
| PUT | `/api/v2/cargosetu/shifts/{shiftId}` | `SHIFT_UPDATE` | Update shifting |
| GET | `/api/v2/cargosetu/shifts/shifting-types` | `CUSTOMER_VIEW` | Get all shifting type options |

**Shifting Types returned:** House, Office, Factory, Mall, Shop, Warehouse, Goods, Govt., Medical Item, Electronic Item, Liquid Items, Industrial Items, Others

---

## 15. PlanController

**File:** `src/main/java/com/cargosetu/controller/PlanController.java`
**Base Mapping:** `/api/v2/cargosetu/plan`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/plan` | `PLAN_CREATE` | Create plan |
| PUT | `/api/v2/cargosetu/plan/{id}` | `PLAN_UPDATE_DELETE` | Update plan |
| GET | `/api/v2/cargosetu/plan/{id}` | `PLAN_READ` | Get plan by ID |
| GET | `/api/v2/cargosetu/plan` | `PLAN_READ` | Get all plans |
| DELETE | `/api/v2/cargosetu/plan/{id}` | `PLAN_UPDATE_DELETE` | Delete plan |
| GET | `/api/v2/cargosetu/plan/public` | `PLAN_READ` | Get publicly visible plans |

---

## 16. FileController

**File:** `src/main/java/com/cargosetu/controller/FileController.java`
**Base Mapping:** `/api/v2/cargosetu/document`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/cargosetu/document/upload` | Upload one or more staff documents (multipart/form-data) |

**Request Param:** `file` — `MultipartFile[]`

---

## 17. DocumentController

**File:** `src/main/java/com/cargosetu/document/controller/DocumentController.java`
**Base Mapping:** `/api/v2/cargosetu/docs/uploads`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v2/cargosetu/docs/uploads/document-master` | Save document master definition |
| POST | `/api/v2/cargosetu/docs/uploads/doc-upload` | Upload a document file (multipart/form-data) |

---

## 18. TemplateGenerationController

**File:** `src/main/java/com/cargosetu/controller/TemplateGenerationController.java`
**Base Mapping:** `/api/v2/cargosetu/templates`
**Auth Required:** Yes · `COMPANY_CREATE`

| Method | Endpoint | Response | Description |
|--------|----------|----------|-------------|
| POST | `/api/v2/cargosetu/templates` | JSON | Upload new HTML template (multipart/form-data) |
| GET | `/api/v2/cargosetu/templates/quotation/{shiftingId}/pdf` | JSON string | Generate quotation PDF for shifting |
| GET | `/api/v2/cargosetu/templates/{shiftingId}/invoice` | `text/html` | Get invoice as filled HTML |

---

## 19. ClientServiceController

**File:** `src/main/java/com/cargosetu/controller/ClientServiceController.java`
**Base Mapping:** `/api/v2/cargosetu/system/configuration`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/cargosetu/system/configuration` | Get all client service configurations |
| POST | `/api/v2/cargosetu/system/configuration` | Create client service entry |
| GET | `/api/v2/cargosetu/system/configuration/{id}` | Get client service by ID |
| PUT | `/api/v2/cargosetu/system/configuration/{id}` | Update client service |

---

## 20. InquiryController

**File:** `src/main/java/com/cargosetu/controller/InquiryController.java`
**Base Mapping:** `public/api/inquiries`
**Auth Required:** No (public endpoint)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `public/api/inquiries` | Submit a customer inquiry |

---

## 21. ShiftInventoryController

**File:** `src/main/java/com/cargosetu/shifting/controller/ShiftInventoryController.java`
**Base Mapping:** `/api/v2/cargosetu/shift-inventory`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/shift-inventory` | `SHIFT_INVENTORY_CREATE` | Create inventory item for a shift |
| GET | `/api/v2/cargosetu/shift-inventory/{itemId}` | `SHIFT_INVENTORY_READ` | Get inventory item by ID |
| PUT | `/api/v2/cargosetu/shift-inventory/{itemId}` | `SHIFT_INVENTORY_UPDATE` | Update inventory item |
| PATCH | `/api/v2/cargosetu/shift-inventory/{itemId}/status?packed=&loaded=&unloaded=` | `SHIFT_INVENTORY_STATUS_UPDATE` | Update item status flags |

**PATCH Query Params:**
- `packed` (boolean, optional)
- `loaded` (boolean, optional)
- `unloaded` (boolean, optional)

---

## 22. NotificationAdminController

**File:** `src/main/java/com/cargosetu/notification/controller/NotificationAdminController.java`
**Base Mapping:** `/api/admin/notifications`
**Auth Required:** Yes (admin)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/notifications/templates` | Get all notification templates |
| POST | `/api/admin/notifications/templates` | Create notification template |
| PUT | `/api/admin/notifications/templates/{id}` | Update notification template |
| GET | `/api/admin/notifications/audits` | Get full notification audit log |
| POST | `/api/admin/notifications/send-test?templateCode=&recipient=&channel=` | Dispatch test notification |

**send-test Query Params:**
- `templateCode` — template code string
- `recipient` — email/phone
- `channel` — `EMAIL`, `SMS`, or `PUSH`
- **Body:** `Map<String, Object>` — template variables

---

## 23. TripController

**File:** `src/main/java/com/cargosetu/controller/TripController.java`
**Base Mapping:** `/api/v2/cargosetu/trip`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/trip` | `TRIP_CREATE` | Create a new trip |
| GET | `/api/v2/cargosetu/trip` | `TRIP_READ` | Get all trips |
| GET | `/api/v2/cargosetu/trip/{tripId}` | `TRIP_READ` | Get trip by ID |
| PUT | `/api/v2/cargosetu/trip/{tripId}` | `TRIP_UPDATE` | Update trip |
| DELETE | `/api/v2/cargosetu/trip/{tripId}` | `TRIP_DELETE` | Delete trip |

---

## 24. TripDocumentController

**File:** `src/main/java/com/cargosetu/controller/TripDocumentController.java`
**Base Mapping:** `/api/v2/cargosetu/trip-document`
**Auth Required:** Yes
**Content-Type:** `multipart/form-data` for upload endpoints

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/trip-document` | `TRIP_DOCUMENT_CREATE` | Upload trip document (multipart) |
| GET | `/api/v2/cargosetu/trip-document` | `TRIP_DOCUMENT_READ` | Get all trip documents |
| GET | `/api/v2/cargosetu/trip-document/{id}` | `TRIP_DOCUMENT_READ` | Get trip document by ID |
| GET | `/api/v2/cargosetu/trip-document/by-trip/{tripId}` | `TRIP_DOCUMENT_READ` | Get all documents for a trip |
| PUT | `/api/v2/cargosetu/trip-document/{id}` | `TRIP_DOCUMENT_UPDATE` | Update trip document (multipart) |
| DELETE | `/api/v2/cargosetu/trip-document/{id}` | `TRIP_DOCUMENT_DELETE` | Delete trip document |

**POST/PUT Request Params (multipart):**
- `file` — `MultipartFile` (optional on PUT)
- `tripId` — Long
- `documentType` — String (optional)

---

## 25. TripExpenseController

**File:** `src/main/java/com/cargosetu/controller/TripExpenseController.java`
**Base Mapping:** `/api/v2/cargosetu/trip-expense`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/trip-expense` | `TRIP_EXPENSE_CREATE` | Create trip expense |
| GET | `/api/v2/cargosetu/trip-expense` | `TRIP_EXPENSE_READ` | Get all trip expenses |
| GET | `/api/v2/cargosetu/trip-expense/{id}` | `TRIP_EXPENSE_READ` | Get trip expense by ID |
| GET | `/api/v2/cargosetu/trip-expense/by-trip/{tripId}` | `TRIP_EXPENSE_READ` | Get all expenses for a trip |
| PUT | `/api/v2/cargosetu/trip-expense/{id}` | `TRIP_EXPENSE_UPDATE` | Update trip expense |
| DELETE | `/api/v2/cargosetu/trip-expense/{id}` | `TRIP_EXPENSE_DELETE` | Delete trip expense |

---

## 26. TripStatusHistoryController

**File:** `src/main/java/com/cargosetu/controller/TripStatusHistoryController.java`
**Base Mapping:** `/api/v2/cargosetu/trip-status-history`
**Auth Required:** Yes

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| POST | `/api/v2/cargosetu/trip-status-history` | `TRIP_STATUS_HISTORY_CREATE` | Create status history record |
| GET | `/api/v2/cargosetu/trip-status-history` | `TRIP_STATUS_HISTORY_READ` | Get all status history records |
| GET | `/api/v2/cargosetu/trip-status-history/{id}` | `TRIP_STATUS_HISTORY_READ` | Get record by ID |
| GET | `/api/v2/cargosetu/trip-status-history/by-trip/{tripId}` | `TRIP_STATUS_HISTORY_READ` | Get all status history for a trip |
| PUT | `/api/v2/cargosetu/trip-status-history/{id}` | `TRIP_STATUS_HISTORY_UPDATE` | Update status history record |
| DELETE | `/api/v2/cargosetu/trip-status-history/{id}` | `TRIP_STATUS_HISTORY_DELETE` | Delete status history record |

---

## 27. NotificationAdminController

**File:** `src/main/java/com/cargosetu/notification/controller/NotificationAdminController.java`
**Base Mapping:** `/api/admin/notifications`
**Auth Required:** Yes (admin)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/notifications/templates` | Get all notification templates |
| POST | `/api/admin/notifications/templates` | Create notification template |
| PUT | `/api/admin/notifications/templates/{id}` | Update notification template |
| GET | `/api/admin/notifications/audits` | Get full notification audit log |
| POST | `/api/admin/notifications/send-test?templateCode=&recipient=&channel=` | Dispatch test notification |

**send-test Query Params:**
- `templateCode` — template code string
- `recipient` — email/phone
- `channel` — `EMAIL`, `SMS`, or `PUSH`
- **Body:** `Map<String, Object>` — template variables

---

## 28. DemoNotificationController

**File:** `src/main/java/com/cargosetu/notification/controller/DemoNotificationController.java`
**Base Mapping:** `/api/demo-notifications`
**Auth Required:** No (demo endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/demo-notifications/register?email=&fullName=&username=&shouldRollback=` | Demo: trigger user-registration notification |
| POST | `/api/demo-notifications/order?orderId=&email=&phone=&name=&amount=` | Demo: trigger order notification (SMS + Email) |
| GET | `/api/demo-notifications/email-logs` | Get email notification audit logs |
| GET | `/api/demo-notifications/sms-logs` | Get SMS notification audit logs |
| GET | `/api/demo-notifications/push-logs` | Get push notification audit logs |

---

## 29. ViewController

**File:** `src/main/java/com/cargosetu/controller/ViewController.java`
**Base Mapping:** `/pages`
**Auth Required:** No
**Type:** Spring MVC `@Controller` (returns view names, not JSON)

| Method | Endpoint | Returns | Description |
|--------|----------|---------|-------------|
| GET | `/pages/login` | `login` (Thymeleaf view) | Render the login HTML page |

---

## 30. BaseApiController

**File:** `src/main/java/com/cargosetu/controller/BaseApiController.java`
**Base Mapping:** `/api/v2/cargosetu`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v2/cargosetu/refreshtoken` | Exchange an existing Bearer token for a new JWT access + refresh token pair |

**Header Required:** `Authorization: Bearer <existing-token>`

**Response:**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

---

## 31. DemoController

**File:** `src/main/java/com/cargosetu/demo/DemoController.java`
**Base Mapping:** `/demo`
**Auth Required:** Yes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/demo` | Returns a greeting with the currently logged-in user's username |

# API URLs Reference

> **Server:** `http://localhost:31201`
> **Base Path:** `/api/v2/cargosetu`
> **Auth:** JWT Bearer token required on all endpoints unless marked **[PUBLIC]**

---

## Quick Reference — All Endpoints

| Module | Method | Full URL | Permission |
|--------|--------|----------|------------|
| **Auth** | POST | `/auth/login` | PUBLIC |
| **Auth** | POST | `/auth/user/register` | PUBLIC |
| **Auth** | POST | `/auth/otp/send` | PUBLIC |
| **Auth** | POST | `/auth/otp/verify` | PUBLIC |
| **Company** | POST | `/api/v2/cargosetu/company` | COMPANY_CREATE |
| **Company** | PUT | `/api/v2/cargosetu/company` | COMPANY_UPDATE_DELETE |
| **Company** | GET | `/api/v2/cargosetu/company` | COMPANY_READ |
| **Company** | GET | `/api/v2/cargosetu/company/{uuid}` | COMPANY_READ |
| **Company** | GET | `/api/v2/cargosetu/company/currentCompany` | COMPANY_READ |
| **Company** | DELETE | `/api/v2/cargosetu/company/{id}` | COMPANY_UPDATE_DELETE |
| **Company** | POST | `/api/v2/cargosetu/company/assets` | COMPANY_CREATE |
| **Staff** | POST | `/api/v2/cargosetu/staff` | STAFF_CREATE |
| **Staff** | GET | `/api/v2/cargosetu/staff` | STAFF_READ |
| **Staff** | GET | `/api/v2/cargosetu/staff/{id}` | STAFF_READ |
| **Staff** | DELETE | `/api/v2/cargosetu/staff/{staffId}` | — |
| **Department** | POST | `/api/v2/cargosetu/department` | — |
| **Department** | GET | `/api/v2/cargosetu/department` | — |
| **Department** | GET | `/api/v2/cargosetu/department/{id}` | — |
| **Department** | PUT | `/api/v2/cargosetu/department/{id}` | — |
| **Department** | DELETE | `/api/v2/cargosetu/department/{id}` | — |
| **Branch** | POST | `/api/v2/cargosetu/branches` | COMPANY_CREATE |
| **Branch** | GET | `/api/v2/cargosetu/branches/{branchId}` | COMPANY_CREATE |
| **Branch** | PUT | `/api/v2/cargosetu/branches/{branchId}` | COMPANY_CREATE |
| **Branch** | DELETE | `/api/v2/cargosetu/branches/{branchId}` | COMPANY_CREATE |
| **Role** | GET | `/api/v2/cargosetu/role` | — |
| **Role** | GET | `/api/v2/cargosetu/role/{id}` | — |
| **Role** | POST | `/api/v2/cargosetu/role` | STAFF_CREATE |
| **Role** | PUT | `/api/v2/cargosetu/role` | — |
| **Role** | DELETE | `/api/v2/cargosetu/role/{id}` | — |
| **Permission** | GET | `/api/v2/cargosetu/permission` | — |
| **Permission** | GET | `/api/v2/cargosetu/permission/{id}` | — |
| **Permission** | POST | `/api/v2/cargosetu/permission` | — |
| **Permission** | PUT | `/api/v2/cargosetu/permission/{id}` | — |
| **Permission** | DELETE | `/api/v2/cargosetu/permission/{id}` | — |
| **Driver** | POST | `/api/v2/cargosetu/drivers` | — |
| **Driver** | GET | `/api/v2/cargosetu/drivers` | — |
| **Driver** | GET | `/api/v2/cargosetu/drivers/{id}` | — |
| **Driver** | PUT | `/api/v2/cargosetu/drivers/{id}` | — |
| **Driver** | DELETE | `/api/v2/cargosetu/drivers/{id}` | — |
| **Vehicle** | POST | `/vehicle` | — |
| **Vehicle** | GET | `/vehicle` | — |
| **Vehicle** | GET | `/vehicle/{id}` | — |
| **Vehicle** | PUT | `/vehicle/{id}` | — |
| **Vehicle** | DELETE | `/vehicle/{id}` | — |
| **Vehicle Location** | POST | `/api/v2/cargosetu/vehicle/location` | — |
| **Vehicle Location** | GET | `/api/v2/cargosetu/vehicle/{vehicleId}/location` | — |
| **Assignments** | POST | `/api/v2/cargosetu/assignments` | — |
| **Assignments** | GET | `/api/v2/cargosetu/assignments` | — |
| **Assignments** | PUT | `/api/v2/cargosetu/assignments/unassign/{id}` | — |
| **Customer** | POST | `/api/v2/cargosetu/customer` | CUSTOMER_CREATE |
| **Customer** | GET | `/api/v2/cargosetu/customer` | CUSTOMER_VIEW |
| **Customer** | GET | `/api/v2/cargosetu/customer/{uuid}` | CUSTOMER_VIEW |
| **Customer** | PUT | `/api/v2/cargosetu/customer` | CUSTOMER_UPDATE |
| **Customer** | DELETE | `/api/v2/cargosetu/customer` | CUSTOMER_DELETE |
| **Shifting** | POST | `/api/v2/cargosetu/shifts` | SHIFT_CREATE |
| **Shifting** | GET | `/api/v2/cargosetu/shifts/{shiftId}` | SHIFT_READ |
| **Shifting** | PUT | `/api/v2/cargosetu/shifts/{shiftId}` | SHIFT_UPDATE |
| **Shifting** | GET | `/api/v2/cargosetu/shifts/shifting-types` | CUSTOMER_VIEW |
| **Plan** | POST | `/api/v2/cargosetu/plan` | PLAN_CREATE |
| **Plan** | GET | `/api/v2/cargosetu/plan` | PLAN_READ |
| **Plan** | GET | `/api/v2/cargosetu/plan/{id}` | PLAN_READ |
| **Plan** | GET | `/api/v2/cargosetu/plan/public` | PLAN_READ |
| **Plan** | PUT | `/api/v2/cargosetu/plan/{id}` | PLAN_UPDATE_DELETE |
| **Plan** | DELETE | `/api/v2/cargosetu/plan/{id}` | PLAN_UPDATE_DELETE |
| **File Upload** | POST | `/api/v2/cargosetu/document/upload` | — |
| **Documents** | POST | `/api/v2/cargosetu/docs/uploads/document-master` | — |
| **Documents** | POST | `/api/v2/cargosetu/docs/uploads/doc-upload` | — |
| **Templates** | POST | `/api/v2/cargosetu/templates` | COMPANY_CREATE |
| **Templates** | GET | `/api/v2/cargosetu/templates/quotation/{shiftingId}/pdf` | COMPANY_CREATE |
| **Templates** | GET | `/api/v2/cargosetu/templates/{shiftingId}/invoice` | — |
| **System Config** | GET | `/api/v2/cargosetu/system/configuration` | — |
| **System Config** | POST | `/api/v2/cargosetu/system/configuration` | — |
| **System Config** | GET | `/api/v2/cargosetu/system/configuration/{id}` | — |
| **System Config** | PUT | `/api/v2/cargosetu/system/configuration/{id}` | — |
| **Inquiry** | POST | `public/api/inquiries` | PUBLIC |
| **Shift Inventory** | POST | `/api/v2/cargosetu/shift-inventory` | SHIFT_INVENTORY_CREATE |
| **Shift Inventory** | GET | `/api/v2/cargosetu/shift-inventory/{itemId}` | SHIFT_INVENTORY_READ |
| **Shift Inventory** | PUT | `/api/v2/cargosetu/shift-inventory/{itemId}` | SHIFT_INVENTORY_UPDATE |
| **Shift Inventory** | PATCH | `/api/v2/cargosetu/shift-inventory/{itemId}/status` | SHIFT_INVENTORY_STATUS_UPDATE |
| **Trip** | POST | `/api/v2/cargosetu/trip` | TRIP_CREATE |
| **Trip** | GET | `/api/v2/cargosetu/trip` | TRIP_READ |
| **Trip** | GET | `/api/v2/cargosetu/trip/{tripId}` | TRIP_READ |
| **Trip** | PUT | `/api/v2/cargosetu/trip/{tripId}` | TRIP_UPDATE |
| **Trip** | DELETE | `/api/v2/cargosetu/trip/{tripId}` | TRIP_DELETE |
| **Trip Document** | POST | `/api/v2/cargosetu/trip-document` | TRIP_DOCUMENT_CREATE |
| **Trip Document** | GET | `/api/v2/cargosetu/trip-document` | TRIP_DOCUMENT_READ |
| **Trip Document** | GET | `/api/v2/cargosetu/trip-document/{id}` | TRIP_DOCUMENT_READ |
| **Trip Document** | GET | `/api/v2/cargosetu/trip-document/by-trip/{tripId}` | TRIP_DOCUMENT_READ |
| **Trip Document** | PUT | `/api/v2/cargosetu/trip-document/{id}` | TRIP_DOCUMENT_UPDATE |
| **Trip Document** | DELETE | `/api/v2/cargosetu/trip-document/{id}` | TRIP_DOCUMENT_DELETE |
| **Trip Expense** | POST | `/api/v2/cargosetu/trip-expense` | TRIP_EXPENSE_CREATE |
| **Trip Expense** | GET | `/api/v2/cargosetu/trip-expense` | TRIP_EXPENSE_READ |
| **Trip Expense** | GET | `/api/v2/cargosetu/trip-expense/{id}` | TRIP_EXPENSE_READ |
| **Trip Expense** | GET | `/api/v2/cargosetu/trip-expense/by-trip/{tripId}` | TRIP_EXPENSE_READ |
| **Trip Expense** | PUT | `/api/v2/cargosetu/trip-expense/{id}` | TRIP_EXPENSE_UPDATE |
| **Trip Expense** | DELETE | `/api/v2/cargosetu/trip-expense/{id}` | TRIP_EXPENSE_DELETE |
| **Trip Status History** | POST | `/api/v2/cargosetu/trip-status-history` | TRIP_STATUS_HISTORY_CREATE |
| **Trip Status History** | GET | `/api/v2/cargosetu/trip-status-history` | TRIP_STATUS_HISTORY_READ |
| **Trip Status History** | GET | `/api/v2/cargosetu/trip-status-history/{id}` | TRIP_STATUS_HISTORY_READ |
| **Trip Status History** | GET | `/api/v2/cargosetu/trip-status-history/by-trip/{tripId}` | TRIP_STATUS_HISTORY_READ |
| **Trip Status History** | PUT | `/api/v2/cargosetu/trip-status-history/{id}` | TRIP_STATUS_HISTORY_UPDATE |
| **Trip Status History** | DELETE | `/api/v2/cargosetu/trip-status-history/{id}` | TRIP_STATUS_HISTORY_DELETE |
| **Base API** | GET | `/api/v2/cargosetu/refreshtoken` | JWT required |
| **View** | GET | `/pages/login` | PUBLIC (returns HTML) |
| **Demo** | GET | `/demo` | JWT required |
| **Notification Admin** | GET | `/api/admin/notifications/templates` | Admin |
| **Notification Admin** | POST | `/api/admin/notifications/templates` | Admin |
| **Notification Admin** | PUT | `/api/admin/notifications/templates/{id}` | Admin |
| **Notification Admin** | GET | `/api/admin/notifications/audits` | Admin |
| **Notification Admin** | POST | `/api/admin/notifications/send-test` | Admin |
| **Demo Notifications** | POST | `/api/demo-notifications/register` | PUBLIC |
| **Demo Notifications** | POST | `/api/demo-notifications/order` | PUBLIC |
| **Demo Notifications** | GET | `/api/demo-notifications/email-logs` | PUBLIC |
| **Demo Notifications** | GET | `/api/demo-notifications/sms-logs` | PUBLIC |
| **Demo Notifications** | GET | `/api/demo-notifications/push-logs` | PUBLIC |

---

## Grouped by Module

### Authentication

**Base:** `/auth`

```
POST   /auth/login                     Login with username + password
POST   /auth/user/register             Register a new staff account
POST   /auth/otp/send                  Send OTP to mobile number
POST   /auth/otp/verify                Verify OTP and get JWT tokens
```

---

### Company Management

**Base:** `/api/v2/cargosetu/company`

```
POST   /api/v2/cargosetu/company                   Create company (multipart/form-data)
PUT    /api/v2/cargosetu/company                   Update company
GET    /api/v2/cargosetu/company                   Get all companies
GET    /api/v2/cargosetu/company/{uuid}            Get company by UUID
GET    /api/v2/cargosetu/company/currentCompany    Get current logged-in user's company
DELETE /api/v2/cargosetu/company/{id}              Delete company
POST   /api/v2/cargosetu/company/assets            Upload company assets (multipart/form-data)
```

---

### Staff Management

**Base:** `/api/v2/cargosetu/staff`

```
POST   /api/v2/cargosetu/staff                     Create staff member
GET    /api/v2/cargosetu/staff?page=0&size=5       Get all staff (paginated)
GET    /api/v2/cargosetu/staff/{id}                Get staff by ID
DELETE /api/v2/cargosetu/staff/{staffId}           Delete staff
```

---

### Department Management

**Base:** `/api/v2/cargosetu/department`

```
GET    /api/v2/cargosetu/department                Get all departments
POST   /api/v2/cargosetu/department                Create department
GET    /api/v2/cargosetu/department/{id}           Get department by ID
PUT    /api/v2/cargosetu/department/{id}           Update department
DELETE /api/v2/cargosetu/department/{id}           Delete department
```

---

### Branch Management

**Base:** `/api/v2/cargosetu/branches`

```
POST   /api/v2/cargosetu/branches                  Create branch
GET    /api/v2/cargosetu/branches/{branchId}       Get branch by ID
PUT    /api/v2/cargosetu/branches/{branchId}       Update branch
DELETE /api/v2/cargosetu/branches/{branchId}       Delete branch
```

---

### Role & Permission Management

**Roles Base:** `/api/v2/cargosetu/role`

```
GET    /api/v2/cargosetu/role                      Get all roles
GET    /api/v2/cargosetu/role/{id}                 Get role by ID
POST   /api/v2/cargosetu/role                      Create role
PUT    /api/v2/cargosetu/role                      Update role
DELETE /api/v2/cargosetu/role/{id}                 Delete role
```

**Permissions Base:** `/api/v2/cargosetu/permission`

```
GET    /api/v2/cargosetu/permission                Get all permissions
GET    /api/v2/cargosetu/permission/{id}           Get permission by ID
POST   /api/v2/cargosetu/permission                Create permission
PUT    /api/v2/cargosetu/permission/{id}           Update permission
DELETE /api/v2/cargosetu/permission/{id}           Delete permission
```

---

### Driver Management

**Base:** `/api/v2/cargosetu/drivers`

```
POST   /api/v2/cargosetu/drivers                   Create driver
GET    /api/v2/cargosetu/drivers                   Get all drivers
GET    /api/v2/cargosetu/drivers/{id}              Get driver by ID
PUT    /api/v2/cargosetu/drivers/{id}              Update driver
DELETE /api/v2/cargosetu/drivers/{id}              Delete driver
```

---

### Vehicle Management

**Base:** `/vehicle`

```
POST   /vehicle                                    Create vehicle
GET    /vehicle                                    Get all vehicles
GET    /vehicle/{id}                               Get vehicle by ID
PUT    /vehicle/{id}                               Update vehicle
DELETE /vehicle/{id}                               Delete vehicle
```

**Location Tracking Base:** `/api/v2/cargosetu/vehicle`

```
POST   /api/v2/cargosetu/vehicle/location                  Save/update vehicle GPS location
GET    /api/v2/cargosetu/vehicle/{vehicleId}/location      Get latest location for vehicle
```

---

### Driver-Vehicle Assignments

**Base:** `/api/v2/cargosetu/assignments`

```
POST   /api/v2/cargosetu/assignments?driverId={id}&vehicleId={id}
                                                   Assign driver to vehicle
GET    /api/v2/cargosetu/assignments               Get all assignments
PUT    /api/v2/cargosetu/assignments/unassign/{id} Unassign driver from vehicle
```

---

### Customer Management

**Base:** `/api/v2/cargosetu/customer`

```
POST   /api/v2/cargosetu/customer                           Create customer
GET    /api/v2/cargosetu/customer?pageNumber=0&pageSize=5   Get all customers (paginated)
GET    /api/v2/cargosetu/customer/{uuid}                    Get customer by UUID
PUT    /api/v2/cargosetu/customer                           Update customer
DELETE /api/v2/cargosetu/customer                           Soft-delete customer
```

---

### Shifting Orders

**Base:** `/api/v2/cargosetu/shifts`

```
POST   /api/v2/cargosetu/shifts                    Create shifting order
GET    /api/v2/cargosetu/shifts/{shiftId}          Get shifting by ID
PUT    /api/v2/cargosetu/shifts/{shiftId}          Update shifting
GET    /api/v2/cargosetu/shifts/shifting-types     Get all available shifting types
```

**Shifting Types:** `House Shifting`, `Office Shifting`, `Factory Shifting`, `Mall Shifting`, `Shop Shifting`, `Warehouse Shifting`, `Goods Shifting`, `Govt. Related Shifting`, `Medical Item Shifting`, `Electronic Item Shift`, `Liquid Items Shift`, `Industrial Items Shift`, `Others`

---

### Subscription Plans

**Base:** `/api/v2/cargosetu/plan`

```
POST   /api/v2/cargosetu/plan                      Create plan
GET    /api/v2/cargosetu/plan                      Get all plans
GET    /api/v2/cargosetu/plan/{id}                 Get plan by ID
GET    /api/v2/cargosetu/plan/public               Get public-facing plans (no auth)
PUT    /api/v2/cargosetu/plan/{id}                 Update plan
DELETE /api/v2/cargosetu/plan/{id}                 Delete plan
```

---

### File & Document Management

**File Upload Base:** `/api/v2/cargosetu/document`

```
POST   /api/v2/cargosetu/document/upload           Upload staff document(s)
                                                   Content-Type: multipart/form-data
                                                   Param: file (MultipartFile[])
```

**Document CRUD Base:** `/api/v2/cargosetu/docs/uploads`

```
POST   /api/v2/cargosetu/docs/uploads/document-master   Create document master definition
POST   /api/v2/cargosetu/docs/uploads/doc-upload        Upload document file
                                                         Content-Type: multipart/form-data
                                                         Parts: file + data (JSON)
```

---

### Template Generation

**Base:** `/api/v2/cargosetu/templates`

```
POST   /api/v2/cargosetu/templates                              Upload HTML template
                                                                Content-Type: multipart/form-data
                                                                Parts: file + data (JSON Template)
GET    /api/v2/cargosetu/templates/quotation/{shiftingId}/pdf   Generate quotation PDF string
GET    /api/v2/cargosetu/templates/{shiftingId}/invoice         Get invoice as filled HTML
```

---

### System Configuration

**Base:** `/api/v2/cargosetu/system/configuration`

```
GET    /api/v2/cargosetu/system/configuration              Get all system config entries
POST   /api/v2/cargosetu/system/configuration              Create config entry
GET    /api/v2/cargosetu/system/configuration/{id}         Get config by ID
PUT    /api/v2/cargosetu/system/configuration/{id}         Update config entry
```

---

### Public Inquiry

```
POST   public/api/inquiries                        Submit a customer inquiry (no auth required)
```

---

### Shift Inventory

**Base:** `/api/v2/cargosetu/shift-inventory`

```
POST   /api/v2/cargosetu/shift-inventory                          Create inventory item
GET    /api/v2/cargosetu/shift-inventory/{itemId}                 Get inventory item by ID
PUT    /api/v2/cargosetu/shift-inventory/{itemId}                 Update inventory item
PATCH  /api/v2/cargosetu/shift-inventory/{itemId}/status          Update item status flags
       Query params: packed={bool}&loaded={bool}&unloaded={bool}
```

---

### Trip Management

**Trip Base:** `/api/v2/cargosetu/trip`

```
POST   /api/v2/cargosetu/trip                      Create a new trip
GET    /api/v2/cargosetu/trip                      Get all trips
GET    /api/v2/cargosetu/trip/{tripId}             Get trip by ID
PUT    /api/v2/cargosetu/trip/{tripId}             Update trip
DELETE /api/v2/cargosetu/trip/{tripId}             Delete trip
```

**Trip Documents Base:** `/api/v2/cargosetu/trip-document`

```
POST   /api/v2/cargosetu/trip-document                       Upload trip document (multipart)
GET    /api/v2/cargosetu/trip-document                       Get all trip documents
GET    /api/v2/cargosetu/trip-document/{id}                  Get trip document by ID
GET    /api/v2/cargosetu/trip-document/by-trip/{tripId}      Get all documents for a trip
PUT    /api/v2/cargosetu/trip-document/{id}                  Update trip document (multipart)
DELETE /api/v2/cargosetu/trip-document/{id}                  Delete trip document
```

**Trip Expenses Base:** `/api/v2/cargosetu/trip-expense`

```
POST   /api/v2/cargosetu/trip-expense                        Create trip expense
GET    /api/v2/cargosetu/trip-expense                        Get all trip expenses
GET    /api/v2/cargosetu/trip-expense/{id}                   Get trip expense by ID
GET    /api/v2/cargosetu/trip-expense/by-trip/{tripId}       Get all expenses for a trip
PUT    /api/v2/cargosetu/trip-expense/{id}                   Update trip expense
DELETE /api/v2/cargosetu/trip-expense/{id}                   Delete trip expense
```

**Trip Status History Base:** `/api/v2/cargosetu/trip-status-history`

```
POST   /api/v2/cargosetu/trip-status-history                 Create status history record
GET    /api/v2/cargosetu/trip-status-history                 Get all status history records
GET    /api/v2/cargosetu/trip-status-history/{id}            Get record by ID
GET    /api/v2/cargosetu/trip-status-history/by-trip/{tripId}  Get all history for a trip
PUT    /api/v2/cargosetu/trip-status-history/{id}            Update status history record
DELETE /api/v2/cargosetu/trip-status-history/{id}            Delete status history record
```

---

### Utility / Misc

```
GET    /api/v2/cargosetu/refreshtoken   Refresh JWT token (Header: Authorization: Bearer <token>)
GET    /pages/login                     Render login HTML page (no auth, returns Thymeleaf view)
GET    /demo                            Hello world — returns current logged-in username
```

---

### Notification Administration

**Base:** `/api/admin/notifications`

```
GET    /api/admin/notifications/templates                         Get all notification templates
POST   /api/admin/notifications/templates                         Create notification template
PUT    /api/admin/notifications/templates/{id}                    Update notification template
GET    /api/admin/notifications/audits                            Get notification audit logs
POST   /api/admin/notifications/send-test                         Dispatch test notification
       Query params: templateCode, recipient, channel (EMAIL|SMS|PUSH)
       Body: { "variable": "value" }
```

---

### Demo Endpoints

**Base:** `/api/demo-notifications`

```
POST   /api/demo-notifications/register             Trigger registration notification
       Params: email, fullName, username, shouldRollback (default=false)

POST   /api/demo-notifications/order                Trigger order notification (SMS + Email)
       Params: orderId, email, phone, name, amount

GET    /api/demo-notifications/email-logs           Email notification audit records
GET    /api/demo-notifications/sms-logs             SMS notification audit records
GET    /api/demo-notifications/push-logs            Push notification audit records
```

---

## URL Constants Reference

### `BaseURL.java`

| Constant | Value |
|----------|-------|
| `BASE_URL` | `/api/v2/cargosetu` |
| `ROLES` | `/role` |
| `PERMISSION` | `/permission` |
| `DEPARTMENT` | `/department` |
| `COMPANY` | `/company` |
| `DOCUMENT` | `/document` |
| `SYSTEM_CONFIG` | `/system/configuration` |
| `DOCS_UPLOADS` | `/docs/uploads` |
| `VEHICLE` | `/vehicle` |
| `LOCATION` | `/location` |
| `DRIVER` | `/drivers` |
| `ASSIGNMENTS` | `/assignments` |
| `TEMPLATES` | `/templates` |
| `BRANCHES` | `/branches` |
| `PUBLIC_PLAN` | `public` |
| `INQUERY` | `/api/inquiries` |

### `UrlConstants.java`

| Constant | Value |
|----------|-------|
| `AUTH` | `/auth` |
| `LOGIN_URL` | `/login` |
| `REGISTER_USER` | `/user/register` |
| `OTP` | `/otp` |
| `LOGOUT_URL` | `/logout` |
| `DOCUMENT_MASTER` | `/document-master` |
| `UPLOAD_DOCUMENT` | `/doc-upload` |
| `PLAN` | `/plan` |
| `PUBLIC` | `/public` |
| `SERVER_ALIVE` | `/isAlive` |
| `StaffUrlConstants.STAFF` | `/staff` |
| `CustomerUrlConstants.CUSTOMER` | `/customer` |
| `CustomerUrlConstants.SHIFTS` | `/shifts` |
| `ShiftingUrlConstants.SHIFT_INVENTORY` | `/shift-inventory` |
| `TripUrlConstants.TRIP` | `/trip` |
| `TripUrlConstants.TRIP_STATUS_HISTORY` | `/trip-status-history` |
| `TripUrlConstants.TRIP_EXPENSE` | `/trip-expense` |
| `TripUrlConstants.TRIP_DOCUMENT` | `/trip-document` |

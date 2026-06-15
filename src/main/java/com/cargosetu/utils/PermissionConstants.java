package com.cargosetu.utils;

/**
 * Permission constants used for Spring Security authorization.
 *
 * @param PERM_PREFIX
 *        Mandatory prefix for all permission authorities.
 *        This prefix is required by Spring Security and MUST be used
 *        for every permission constant defined in this class.
 *
 *        IMPORTANT RULES FOR DEVELOPERS:
 *        1. Do NOT store the prefix (PERM_) in the database.
 *        2. Database permission codes must be plain (e.g. ROLE_CREATE, STAFF_READ).
 *        3. Always build permission constants using PERM_PREFIX + <DB_PERMISSION_CODE>.
 *        4. Controllers must use these constants in @PreAuthorize annotations.
 *        5. Never hardcode permission strings directly in controllers.
 *
 *        Example:
 *        public static final String ROLE_CREATE = PERM_PREFIX + "ROLE_CREATE";
 */
public class PermissionConstants {
    public static final String PERM_PREFIX = "PERM_";

    // role permissions
    public static final String ROLE_CREATE = PERM_PREFIX + "ROLE_CREATE";
    public static final String ROLE_READ  = PERM_PREFIX + "ROLE_READ";
    public static final String ROLE_UPDATE_DELETE  = PERM_PREFIX + "ROLE_UPDATE_DELETE";

    // staff permissions
    public static final String STAFF_CREATE  = PERM_PREFIX + "STAFF_CREATE";
    public static final String STAFF_READ  = PERM_PREFIX + "STAFF_READ";
    public static final String STAFF_UPDATE_DELETE  = PERM_PREFIX + "STAFF_UPDATE_DELETE";

    // company permissions
    public static final String COMPANY_READ  = PERM_PREFIX + "COMPANY_READ";
    public static final String COMPANY_CREATE  = PERM_PREFIX + "COMPANY_CREATE";
    public static final String COMPANY_DELETE = PERM_PREFIX + "COMPANY_DELETE";
    public static final String COMPANY_UPDATE  = PERM_PREFIX + "COMPANY_UPDATE";
    public static final String COMPANY_UPDATE_DELETE = PERM_PREFIX + "COMPANY_UPDATE_DELETE";

    // plan permissions
    public static final String PLAN_READ  = PERM_PREFIX + "PLAN_READ";
    public static final String PLAN_CREATE  = PERM_PREFIX + "PLAN_CREATE";
    public static final String PLAN_UPDATE_DELETE = PERM_PREFIX + "PLAN_UPDATE_DELETE";

    // customer permissions
    public static final String CUSTOMER_CREATE = PERM_PREFIX + "CUSTOMER_CREATE";
    public static final String CUSTOMER_DELETE = PERM_PREFIX + "CUSTOMER_DELETE";
    public static final String CUSTOMER_UPDATE = PERM_PREFIX + "CUSTOMER_UPDATE";
    public static final String CUSTOMER_VIEW = PERM_PREFIX + "CUSTOMER_VIEW";

    // shifting permissions
    public static final String SHIFT_CREATE = PERM_PREFIX + "SHIFT_CREATE";
    public static final String SHIFT_READ = PERM_PREFIX + "SHIFT_READ";
    public static final String SHIFT_UPDATE = PERM_PREFIX + "SHIFT_UPDATE";

    // shift inventory permissions
    public static final String SHIFT_INVENTORY_CREATE = PERM_PREFIX + "SHIFT_INVENTORY_CREATE";
    public static final String SHIFT_INVENTORY_READ = PERM_PREFIX + "SHIFT_INVENTORY_READ";
    public static final String SHIFT_INVENTORY_UPDATE = PERM_PREFIX + "SHIFT_INVENTORY_UPDATE";
    public static final String SHIFT_INVENTORY_STATUS_UPDATE = PERM_PREFIX + "SHIFT_INVENTORY_STATUS_UPDATE";

    // trip permissions
    public static final String TRIP_CREATE = PERM_PREFIX + "TRIP_CREATE";
    public static final String TRIP_READ = PERM_PREFIX + "TRIP_READ";
    public static final String TRIP_UPDATE = PERM_PREFIX + "TRIP_UPDATE";
    public static final String TRIP_DELETE = PERM_PREFIX + "TRIP_DELETE";
}

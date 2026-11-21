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

    // trip status history permissions
    public static final String TRIP_STATUS_HISTORY_CREATE = PERM_PREFIX + "TRIP_STATUS_HISTORY_CREATE";
    public static final String TRIP_STATUS_HISTORY_READ = PERM_PREFIX + "TRIP_STATUS_HISTORY_READ";
    public static final String TRIP_STATUS_HISTORY_UPDATE = PERM_PREFIX + "TRIP_STATUS_HISTORY_UPDATE";
    public static final String TRIP_STATUS_HISTORY_DELETE = PERM_PREFIX + "TRIP_STATUS_HISTORY_DELETE";

    // trip expense permissions
    public static final String TRIP_EXPENSE_CREATE = PERM_PREFIX + "TRIP_EXPENSE_CREATE";
    public static final String TRIP_EXPENSE_READ = PERM_PREFIX + "TRIP_EXPENSE_READ";
    public static final String TRIP_EXPENSE_UPDATE = PERM_PREFIX + "TRIP_EXPENSE_UPDATE";
    public static final String TRIP_EXPENSE_DELETE = PERM_PREFIX + "TRIP_EXPENSE_DELETE";

    // department permissions
    public static final String DEPARTMENT_CREATE = PERM_PREFIX + "DEPARTMENT_CREATE";
    public static final String DEPARTMENT_READ   = PERM_PREFIX + "DEPARTMENT_READ";
    public static final String DEPARTMENT_UPDATE = PERM_PREFIX + "DEPARTMENT_UPDATE";
    public static final String DEPARTMENT_DELETE = PERM_PREFIX + "DEPARTMENT_DELETE";

    // permission management permissions
    public static final String PERMISSION_CREATE = PERM_PREFIX + "PERMISSION_CREATE";
    public static final String PERMISSION_READ   = PERM_PREFIX + "PERMISSION_READ";
    public static final String PERMISSION_UPDATE = PERM_PREFIX + "PERMISSION_UPDATE";
    public static final String PERMISSION_DELETE = PERM_PREFIX + "PERMISSION_DELETE";

    // Marketplace Permissions
    public static final String MARKETPLACE_LOAD_CREATE = PERM_PREFIX + "MARKETPLACE_LOAD_CREATE";
    public static final String MARKETPLACE_LOAD_READ = PERM_PREFIX + "MARKETPLACE_LOAD_READ";
    public static final String MARKETPLACE_LOAD_UPDATE = PERM_PREFIX + "MARKETPLACE_LOAD_UPDATE";
    public static final String MARKETPLACE_TRUCK_CREATE = PERM_PREFIX + "MARKETPLACE_TRUCK_CREATE";
    public static final String MARKETPLACE_TRUCK_READ = PERM_PREFIX + "MARKETPLACE_TRUCK_READ";
    public static final String MARKETPLACE_TRUCK_UPDATE = PERM_PREFIX + "MARKETPLACE_TRUCK_UPDATE";
    public static final String MARKETPLACE_OFFER_CREATE = PERM_PREFIX + "MARKETPLACE_OFFER_CREATE";
    public static final String MARKETPLACE_OFFER_READ = PERM_PREFIX + "MARKETPLACE_OFFER_READ";
    public static final String MARKETPLACE_BOOKING_CREATE = PERM_PREFIX + "MARKETPLACE_BOOKING_CREATE";
    public static final String MARKETPLACE_BOOKING_READ = PERM_PREFIX + "MARKETPLACE_BOOKING_READ";
    public static final String MARKETPLACE_BOOKING_CANCEL = PERM_PREFIX + "MARKETPLACE_BOOKING_CANCEL";
    public static final String MARKETPLACE_BOOKING_ASSIGN = PERM_PREFIX + "MARKETPLACE_BOOKING_ASSIGN";
    public static final String MARKETPLACE_TRIP_READ = PERM_PREFIX + "MARKETPLACE_TRIP_READ";
    public static final String MARKETPLACE_TRIP_UPDATE = PERM_PREFIX + "MARKETPLACE_TRIP_UPDATE";
    public static final String MARKETPLACE_PAYMENT_CREATE = PERM_PREFIX + "MARKETPLACE_PAYMENT_CREATE";
    public static final String MARKETPLACE_PAYMENT_READ = PERM_PREFIX + "MARKETPLACE_PAYMENT_READ";
    public static final String MARKETPLACE_WALLET_READ = PERM_PREFIX + "MARKETPLACE_WALLET_READ";
    public static final String MARKETPLACE_CHAT_CREATE = PERM_PREFIX + "MARKETPLACE_CHAT_CREATE";
    public static final String MARKETPLACE_CHAT_READ = PERM_PREFIX + "MARKETPLACE_CHAT_READ";
    public static final String MARKETPLACE_DISPUTE_CREATE = PERM_PREFIX + "MARKETPLACE_DISPUTE_CREATE";
    public static final String MARKETPLACE_DISPUTE_READ = PERM_PREFIX + "MARKETPLACE_DISPUTE_READ";
    public static final String MARKETPLACE_DISPUTE_RESOLVE = PERM_PREFIX + "MARKETPLACE_DISPUTE_RESOLVE";
    public static final String MARKETPLACE_RATING_CREATE = PERM_PREFIX + "MARKETPLACE_RATING_CREATE";
    public static final String MARKETPLACE_RATING_READ = PERM_PREFIX + "MARKETPLACE_RATING_READ";
    public static final String MARKETPLACE_MATCH_READ = PERM_PREFIX + "MARKETPLACE_MATCH_READ";
    public static final String MARKETPLACE_MATCH_CREATE = PERM_PREFIX + "MARKETPLACE_MATCH_CREATE";
    public static final String MARKETPLACE_CHAT_UPDATE = PERM_PREFIX + "MARKETPLACE_CHAT_UPDATE";
    public static final String MARKETPLACE_DISPUTE_UPDATE = PERM_PREFIX + "MARKETPLACE_DISPUTE_UPDATE";
    public static final String MARKETPLACE_ADMIN_MODERATE = PERM_PREFIX + "MARKETPLACE_ADMIN_MODERATE";
    public static final String MARKETPLACE_ADMIN_SETTLE = PERM_PREFIX + "MARKETPLACE_ADMIN_SETTLE";
    public static final String MARKETPLACE_ADMIN_CONFIG = PERM_PREFIX + "MARKETPLACE_ADMIN_CONFIG";
}

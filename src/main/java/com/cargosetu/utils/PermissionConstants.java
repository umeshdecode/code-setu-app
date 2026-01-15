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

}

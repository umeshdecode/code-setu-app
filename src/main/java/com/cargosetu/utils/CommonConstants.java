package com.cargosetu.utils;

public class CommonConstants {
    public static final String SUPERADMIN = "SUPERADMIN";
    public static final String STAFF = "STAFF";
    public static final String RESPONSE_MESSAGE = "Message";
    public static final String ERROR_TAG = "Error";
    public static final String AUTHORIZATION_TOKEN_PREFIX = "Bearer ";
    public static final long EXPIRATION_TIME = 864_000_000;
    public static final String SYSTEM_DEFAULT = "SYSTEM-DEFAULT";
    public interface RolesConstants{
        public static final String SUPERADMIN = "SUPERADMIN";
        public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
    }
    public interface PlanConstant{
        public static final String PLAN_CREATED =
                "Plan created successfully.";

        public static final String PLAN_UPDATED =
                "Plan updated successfully.";

        public static final String PLAN_FETCHED =
                "Plan fetched successfully.";

        public static final String PLAN_LIST_FETCHED =
                "Plans fetched successfully.";

        public static final String PLAN_DELETED =
                "Plan deleted successfully.";

        public static final String PLAN_NOT_FOUND =
                "Plan not found.";
    }
}

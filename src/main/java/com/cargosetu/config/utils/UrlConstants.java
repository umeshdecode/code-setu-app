package com.cargosetu.config.utils;

public class UrlConstants {
    public static final String LOGIN_URL = "/login";
    public static final String REGISTER_USER = "/user/register";
    public static final String LOGOUT_URL = "/logout";
    public static final String AUTH = "/auth";
    public static final String pages = "/pages";
    public static final String DOCUMENT_MASTER = "/document-master";
    public static final String UPLOAD_DOCUMENT = "/doc-upload";
    public static final String PLAN = "/plan";

    public interface StaffUrlConstants {
        public static final String STAFF = "/staff";
    }

    public interface CustomerUrlConstants {
        public static final String CUSTOMER = "/customer";
        public static final String SHIFTS = "/shifts";
    }

    public interface ShiftingUrlConstants {
        public static final String SHIFT_INVENTORY = "/shift-inventory";
    }
}

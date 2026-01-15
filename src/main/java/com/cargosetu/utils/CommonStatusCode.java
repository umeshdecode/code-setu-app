package com.cargosetu.utils;
public final class CommonStatusCode {

    private CommonStatusCode() {}

    public static final Integer SUCCESS = 200;
    public static final Integer CREATED = 201;
    public static final Integer FAIL = 400;
    public static final Integer UNAUTHORIZED = 401;
    public static final Integer FORBIDDEN = 403;
    public static final Integer NOT_FOUND = 404;
    public static final Integer CONFLICT = 409;
    public static final Integer VALIDATION_ERROR = 422;
    public static final Integer INTERNAL_SERVER_ERROR = 500;
    public static final Integer SERVICE_UNAVAILABLE = 503;  // Service Down
    public static final String MSG_SERVICE_UNAVAILABLE = "Service temporarily unavailable";

    public static final String MSG_SUCCESS = "Request processed successfully";
    public static final String MSG_CREATED = " created successfully";
    public static final String MSG_UPDATED = " updated successfully";
    public static final String MSG_DELETED = " deleted successfully";

    public static final String MSG_BAD_REQUEST = "Invalid request data";
    public static final String MSG_UNAUTHORIZED = "Unauthorized access";
    public static final String MSG_FORBIDDEN = "Access denied";
    public static final String MSG_NOT_FOUND = "Resource not found";
    public static final String MSG_CONFLICT = "Duplicate record found";
    public static final String MSG_VALIDATION_FAILED = "Validation failed";

    public static final String MSG_INTERNAL_SERVER_ERROR = "Something went wrong. Please try again later";


    public static final String STATUS = "status";
    public static final String MESSAGE = "message";
    public static final String DATA = "data";
    public static final String DATA_LIST = "dataList";
    public static final String ERROR = "error";
    public static final String TIMESTAMP = "timestamp";
}


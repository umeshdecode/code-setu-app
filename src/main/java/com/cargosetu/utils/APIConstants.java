package com.cargosetu.utils;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

public class APIConstants {

    public static Integer SUCCESS = HttpStatus.OK.value();
    public static Integer NO_CONTENT_FOUND = HttpStatus.NO_CONTENT.value();
    public static Integer FAIL = HttpStatus.BAD_REQUEST.value();
    public static Integer INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR.value();
    public static Integer FORBIDDEN = HttpStatus.FORBIDDEN.value();
    public static Integer NOT_FOUND = HttpStatus.NOT_FOUND.value();
    public static final String ERROR_TAG = "ERROR";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final Integer NULL_VALUE=HttpStatus.NOT_FOUND.value();
    public static final String PASS_FAIL = "PASSWORD IS FAIL";
    public static final String MESSAGE="msg";
    public static final String BATCH_PAYMENT_NOT_ASSIGNED="Not Assigned";
    public static final String BATCH_PAYMENT_ASSIGNED="Assigned";
    public static final String BATCH_PAYMENT_REASSIGNED="ReAssigned";
    public static final Integer EXPECTATION_FAILED=HttpStatus.EXPECTATION_FAILED.value();

    //OTPConstants
    public static List<String> OTP_GENERATION_TYPE = Arrays.asList( "ALWAYS_NEW", "REUSE");
    public static final String TYPE = "type";
    public static final String TYPE_FETCH = "fetch";
    public static final String TYPE_CREATE = "create";
    public static final String TYPE_UPDATE = "update";
    public static final String TYPE_DELETE = "delete";

    public static final String NOT_PUT_IN_QUEUE = "NotPutInQueue";
    public static final String VALIDATION_REASON = "ValidationReason";


    public static final String BASIC_STRING_MSG = "BasicStringMsg";
    public static final String BASIC_NUMERIC_MSG = "BasicNumericMsg";

    public static final String VALIDATION_REASON_BASIC_STRING_MESSAGE="Null, blank value and 'string' are not allowed";

    public static final String VALIDATION_REASON_BASIC_NUMERIC_MESSAGE="Null and 0 are not allowed";

    public static final String BLANK_STRING = "string";

    public static final String TYPE_VALIDATE = "validate";

    public static final String INVALID_RESELLER_MSG="Reseller is invalid";

    public static final Integer INVALID_RESELLER_CODE=402;

    public static final String QUOTA_USED = "quotaUsed";
}

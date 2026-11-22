package com.cargosetu.marketplace.util;

import com.cargosetu.utils.CommonStatusCode;
import com.cargosetu.utils.GenericDataDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Builds marketplace HTTP responses whose HTTP status always matches the
 * {@code responseCode} the service put in the body, so clients that only look
 * at the HTTP status never mistake a business failure for a success.
 */
public final class MarketplaceResponses {

    private MarketplaceResponses() {}

    public static ResponseEntity<GenericDataDTO> of(GenericDataDTO response) {
        if (response == null) {
            return error();
        }
        int code = response.getResponseCode() != null ? response.getResponseCode() : CommonStatusCode.SUCCESS;
        if (HttpStatus.resolve(code) == null) {
            code = CommonStatusCode.INTERNAL_SERVER_ERROR;
        }
        // A business-rule refusal ("you can't bid on your own listing") must not look like an
        // authentication failure: the mobile client refreshes the token and logs the user out on
        // HTTP 403. The body keeps responseCode 403; the HTTP status becomes 409.
        if (code == CommonStatusCode.FORBIDDEN) {
            code = CommonStatusCode.CONFLICT;
        }
        return ResponseEntity.status(code).body(response);
    }

    /** Generic 500 that never leaks exception details to the client. */
    public static ResponseEntity<GenericDataDTO> error() {
        return ResponseEntity.status(CommonStatusCode.INTERNAL_SERVER_ERROR)
                .body(new GenericDataDTO(CommonStatusCode.INTERNAL_SERVER_ERROR,
                        CommonStatusCode.MSG_INTERNAL_SERVER_ERROR, null, null));
    }

    /**
     * Marks the current transaction rollback-only. Services catch exceptions to build an
     * error response; without this the partial writes made before the failure would commit.
     */
    public static void rollback() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception ignored) {
            // no active transaction
        }
    }

    public static GenericDataDTO fail(Integer code, String message) {
        GenericDataDTO dto = new GenericDataDTO();
        dto.setResponseCode(code);
        dto.setResponseMessege(message);
        return dto;
    }
}

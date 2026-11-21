package com.cargosetu.client.exception;

public class VehicleCatalogueClientException extends RuntimeException {

    private final Integer statusCode;

    public VehicleCatalogueClientException(String message) {
        super(message);
        this.statusCode = null;
    }

    public VehicleCatalogueClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public VehicleCatalogueClientException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}

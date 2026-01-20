package com.cargosetu.exception;

public class CustomValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    Integer errCode;
    public CustomValidationException(Integer errCode, String strErrMessage, Throwable err) {
        super(strErrMessage,err);
        this.errCode=errCode;
    }
    public Integer getErrCode() {
        return errCode;
    }
    public void setErrCode(Integer errCode) {
        this.errCode = errCode;
    }
}

package com.asterion.order.application.exception;

public class MerchantServiceException extends RuntimeException {

    public MerchantServiceException(String message) {
        super(message);
    }

    public MerchantServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
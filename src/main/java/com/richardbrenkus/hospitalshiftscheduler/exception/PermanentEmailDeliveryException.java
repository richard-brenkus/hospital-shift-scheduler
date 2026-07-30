package com.richardbrenkus.hospitalshiftscheduler.exception;

public class PermanentEmailDeliveryException extends RuntimeException {

    public PermanentEmailDeliveryException(String message) {
        super(message);
    }

    public PermanentEmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

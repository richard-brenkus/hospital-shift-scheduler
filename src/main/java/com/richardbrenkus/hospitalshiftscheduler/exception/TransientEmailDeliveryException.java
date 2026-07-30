package com.richardbrenkus.hospitalshiftscheduler.exception;

public class TransientEmailDeliveryException extends RuntimeException {

    public TransientEmailDeliveryException(String message) {
        super(message);
    }

    public TransientEmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

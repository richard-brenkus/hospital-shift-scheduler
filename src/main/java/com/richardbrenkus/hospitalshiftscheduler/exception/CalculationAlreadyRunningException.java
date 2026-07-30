package com.richardbrenkus.hospitalshiftscheduler.exception;

public class CalculationAlreadyRunningException extends RuntimeException {

    public CalculationAlreadyRunningException() {
        super("Another schedule calculation is already running.");
    }

    public CalculationAlreadyRunningException(String message) {
        super(message);
    }
}

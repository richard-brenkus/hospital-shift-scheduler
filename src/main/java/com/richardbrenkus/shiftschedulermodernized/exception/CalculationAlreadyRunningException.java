package com.richardbrenkus.shiftschedulermodernized.exception;

public class CalculationAlreadyRunningException extends RuntimeException {

    public CalculationAlreadyRunningException() {
        super("Another schedule calculation is already running.");
    }

    public CalculationAlreadyRunningException(String message) {
        super(message);
    }
}

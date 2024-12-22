package com.scitequest.martin;

public class MeasurementException extends Exception {

    public MeasurementException(String string) {
        super(string);
    }

    public MeasurementException(Exception e) {
        super(e);
    }
}

package com.murali.transactionmanager.model;

/**
 * Contains return code that will be returned by this service's APIs
 */
public enum ReturnCode {
    SUCCESS(201), OLD_TRANSACTION(204), BAD_REQUEST(400);

    private int value;

    private ReturnCode(int code) {
        this.value = code;
    }

    public int getValue() {
        return this.value;
    }
}

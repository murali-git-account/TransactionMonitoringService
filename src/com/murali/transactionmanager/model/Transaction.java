package com.murali.transactionmanager.model;

import org.apache.commons.lang.Validate;

/**
 * Contains attributes of a transaction
 */
public class Transaction {
    private double amount;
    private long timestamp;

    public Transaction(double amount, long timestamp) {
        Validate.isTrue(timestamp > 0, "timestamp is null");

        this.amount = amount;
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public double getAmount() {
        return this.amount;
    }

    @Override
    public String toString() {
        return "Amount: " + amount + " timestamp: " + timestamp;
    }
}

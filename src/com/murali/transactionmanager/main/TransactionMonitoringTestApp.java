package com.murali.transactionmanager.main;

import java.time.Clock;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.store.TransactionRecorder;
import com.murali.transactionmanager.store.impl.InMemoryTransactionRecorder;

/**
 * Test Application
 */
public class TransactionMonitoringTestApp {
    private static final TransactionRecorder transactionRecorder = new InMemoryTransactionRecorder();

    public static void main(String args[]) {
        postATransaction(10.3);
        postATransaction(9.7);
        // Invoke API 2
        System.out
                .println("Statistics: " + transactionRecorder.getStatistics());
    }

    private static void postATransaction(double amount) {
        // Invoke API 1
        transactionRecorder
                .record(new Transaction(amount, Clock.systemUTC().millis()));
    }
}

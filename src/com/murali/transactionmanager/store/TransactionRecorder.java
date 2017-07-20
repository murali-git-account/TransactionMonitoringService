package com.murali.transactionmanager.store;

import java.util.Map;

import com.murali.transactionmanager.model.ReturnCode;
import com.murali.transactionmanager.model.Transaction;

/**
 * Provides operations to record {@link Transaction}s and return statistics
 * about them
 */
public interface TransactionRecorder {
    /**
     * Records the transaction
     * 
     * @param transaction
     * @return ReturnCode
     */
    ReturnCode record(Transaction transaction);

    /**
     * Returns statistics about all transactions recorded within a time window
     */
    Map<String, Number> getStatistics();
}

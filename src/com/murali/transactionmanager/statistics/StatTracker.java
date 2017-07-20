package com.murali.transactionmanager.statistics;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.model.Statistic;

/**
 * This contains the aggregates stats of based on all {@link Transaction}s
 * recorded until the moment of time
 */
public interface StatTracker {
    /**
     * Return the name of the stat tracked by this generator
     * 
     * @return name
     */
    String getStatName();

    /**
     * Returns the actual statistic
     */
    Statistic getStatistics();

    /**
     * Updates statistics for the new transactions
     * 
     * @param transaction
     */
    void recordTransaction(Transaction transaction);

    /**
     * Aggregates (or folds) the stats value from the given {@link Statistic}
     * onto itself
     */
    void mergeStatistic(Statistic statistic);
}

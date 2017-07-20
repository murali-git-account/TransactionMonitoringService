package com.murali.transactionmanager.statistics.impl;

import org.apache.commons.lang.Validate;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.StatTracker;
import com.murali.transactionmanager.statistics.model.Statistic;

/**
 * This records {@link Transaction}s and keeps track of the count statistic
 */
public class CountStatTracker implements StatTracker {
    private static final double COUNTER_ONE = 1.0;
    public static final String STAT_NAME = "count";
    private Statistic statistic = new Statistic(STAT_NAME, 0.0);

    /**
     * Return the name of the stat tracked by this generator
     * 
     * @return name
     */
    @Override
    public final String getStatName() {
        return STAT_NAME;
    }

    /**
     * Returns the actual statistic
     */
    @Override
    public Statistic getStatistics() {
        return statistic;
    }

    @Override
    public synchronized void recordTransaction(Transaction transaction) {
        Validate.notNull(transaction);
        statistic.add(COUNTER_ONE);
    }

    @Override
    public synchronized void mergeStatistic(Statistic statistic) {
        Validate.notNull(statistic);
        Validate.isTrue(STAT_NAME.equals(statistic.getUnit()));
        this.statistic.add(statistic.getValue());
    }
}

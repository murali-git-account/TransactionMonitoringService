package com.murali.transactionmanager.statistics.impl;

import org.apache.commons.lang.Validate;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.StatTracker;
import com.murali.transactionmanager.statistics.model.Statistic;

/**
 * This generator listens to {@link Transaction} and generate the minimum of
 * transaction amounts
 */
public class MinAmountStatTracker implements StatTracker {
    public static final String STAT_NAME = "min";
    private Statistic statistic;

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
        updateValue(transaction.getAmount());
    }

    @Override
    public synchronized void mergeStatistic(Statistic statistic) {
        Validate.notNull(statistic);
        Validate.isTrue(STAT_NAME.equals(statistic.getUnit()));
        updateValue(statistic.getValue());
    }

    private synchronized void updateValue(double value) {
        if (this.statistic == null) {
            this.statistic = new Statistic(STAT_NAME, value);
        } else if (value < statistic.getValue()) {
            this.statistic.setValue(value);
        }
    }
}

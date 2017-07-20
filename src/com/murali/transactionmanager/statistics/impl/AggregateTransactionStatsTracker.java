package com.murali.transactionmanager.statistics.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang.Validate;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.StatTracker;

public class AggregateTransactionStatsTracker {
    public static final String AVG_STAT_NAME = "avg";

    private long timestampSeconds;

    private CountStatTracker countStatTracker;
    private MaxAmountStatTracker maxAmountStatTracker;
    private MinAmountStatTracker minAmountStatTracker;
    private SumOfAmountsStatTracker sumOfAmountsStatTracker;
    private List<StatTracker> transactionListeners;

    public AggregateTransactionStatsTracker(long timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
        this.countStatTracker = new CountStatTracker();
        this.maxAmountStatTracker = new MaxAmountStatTracker();
        this.minAmountStatTracker = new MinAmountStatTracker();
        this.sumOfAmountsStatTracker = new SumOfAmountsStatTracker();

        transactionListeners = new ArrayList<>();
        transactionListeners.add(countStatTracker);
        transactionListeners.add(maxAmountStatTracker);
        transactionListeners.add(minAmountStatTracker);
        transactionListeners.add(sumOfAmountsStatTracker);
    }

    private CountStatTracker getCountStatTracker() {
        return this.countStatTracker;
    }

    private MaxAmountStatTracker getMaxAmountStatTracker() {
        return this.maxAmountStatTracker;
    }

    private MinAmountStatTracker getMinAmountStatTracker() {
        return this.minAmountStatTracker;
    }

    private SumOfAmountsStatTracker getSumOfAmountsStatTracker() {
        return this.sumOfAmountsStatTracker;
    }

    public synchronized void recordTransaction(Transaction transaction) {
        transactionListeners.stream()
                .forEach(listener -> listener.recordTransaction(transaction));
    }

    public synchronized void mergeStatistic(
            AggregateTransactionStatsTracker otherStatsTracker) {
        Validate.notNull(otherStatsTracker);

        mergeIfNotNull(otherStatsTracker,
                AggregateTransactionStatsTracker::getCountStatTracker);
        mergeIfNotNull(otherStatsTracker,
                AggregateTransactionStatsTracker::getSumOfAmountsStatTracker);
        mergeIfNotNull(otherStatsTracker,
                AggregateTransactionStatsTracker::getMaxAmountStatTracker);
        mergeIfNotNull(otherStatsTracker,
                AggregateTransactionStatsTracker::getMinAmountStatTracker);
    }

    public long getTimestampInSeconds() {
        return this.timestampSeconds;
    }

    public Map<String, Number> getStats() {
        Map<String, Number> stats = new HashMap<>();

        stats.put(CountStatTracker.STAT_NAME,
                (long) countStatTracker.getStatistics().getValue());

        stats.put(MaxAmountStatTracker.STAT_NAME,
                getOrDefault(maxAmountStatTracker));

        stats.put(MinAmountStatTracker.STAT_NAME,
                getOrDefault(minAmountStatTracker));

        stats.put(SumOfAmountsStatTracker.STAT_NAME, Double
                .valueOf(sumOfAmountsStatTracker.getStatistics().getValue()));

        double avg = 0.0;
        if (countStatTracker.getStatistics().getValue() > 0) {
            avg = Double.valueOf(sumOfAmountsStatTracker.getStatistics()
                    .getValue()
                    / Double.valueOf(
                            countStatTracker.getStatistics().getValue()));
        }
        stats.put(AVG_STAT_NAME, Double.valueOf(avg));

        return stats;
    }

    private Double getOrDefault(StatTracker tracker) {
        return (tracker.getStatistics() == null) ? null
                : tracker.getStatistics().getValue();
    }

    private void mergeIfNotNull(
            AggregateTransactionStatsTracker otherAggregateTransactionStatsTracker,
            Function<AggregateTransactionStatsTracker, StatTracker> trackerSupplier) {
        Validate.notNull(otherAggregateTransactionStatsTracker);
        StatTracker statTracker = trackerSupplier
                .apply(otherAggregateTransactionStatsTracker);
        Validate.notNull(statTracker);

        if (statTracker.getStatistics() != null) {
            trackerSupplier.apply(this)
                    .mergeStatistic(statTracker.getStatistics());
        } else {
            System.out.println("Failed to merge stat");
        }
    }
}

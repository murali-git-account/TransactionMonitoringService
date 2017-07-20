package com.murali.transactionmanager.store.impl;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

import org.apache.commons.lang.Validate;

import com.murali.transactionmanager.model.ReturnCode;
import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.impl.AggregateTransactionStatsTracker;
import com.murali.transactionmanager.store.TransactionRecorder;

/**
 * In-memory transaction records that provide the backend implementation for the
 * 2 Rest APIs
 */
public class InMemoryTransactionRecorder implements TransactionRecorder {
    private static final int MILLISEC_IN_A_SECOND = 1000;
    private final static long RECORDING_INTERVAL_IN_SECS = 60;
    public final static long RECORDING_INTERVAL_IN_MILLIS = RECORDING_INTERVAL_IN_SECS
            * MILLISEC_IN_A_SECOND;

    private final List<Transaction> transactions = new ArrayList<>();
    private final StampedLock transactionLocks = new StampedLock();
    Map<Long, AggregateTransactionStatsTracker> aggregateTransactionStatsBySecondOffset = new HashMap<>();

    protected Supplier<Long> currentTimeSuppiler;

    public InMemoryTransactionRecorder() {
        currentTimeSuppiler = () -> Clock.systemUTC().millis();
    }

    protected void setCurrentTimeSupplier(Supplier<Long> timeSupplier) {
        this.currentTimeSuppiler = timeSupplier;
    }

    /**
     * Registers the transaction
     * 
     * @param transaction
     * @return
     */
    @Override
    public ReturnCode record(Transaction transaction) {
        Validate.notNull(transaction, "Transaction is null");

        long timeElapsedSinceTransactionInMillis = currentTimeSuppiler.get()
                - transaction.getTimestamp();

        if (timeElapsedSinceTransactionInMillis > RECORDING_INTERVAL_IN_MILLIS) {
            return ReturnCode.OLD_TRANSACTION;
        }

        addTransaction(transaction);
        return ReturnCode.SUCCESS;
    }

    /**
     * Returns statistics about all transactions recorded within a time window
     */
    @Override
    public Map<String, Number> getStatistics() {

        AggregateTransactionStatsTracker aggregateTrackerForCurrentRecordingInterval = new AggregateTransactionStatsTracker(
                currentTimeSuppiler.get() / MILLISEC_IN_A_SECOND);

        aggregateTransactionStatsBySecondOffset.entrySet().forEach(entry -> {
            aggregateTrackerForCurrentRecordingInterval
                    .mergeStatistic(entry.getValue());
        });
        return aggregateTrackerForCurrentRecordingInterval.getStats();
    }

    private synchronized void addTransaction(Transaction transaction) {
        long writeLock = transactionLocks.writeLock();

        // Records transactions
        transactions.add(transaction);

        // Generate statistics for the second within the last 60, when this
        // transaction occurred
        // 'aggregateTransactionStatsBySecondOffset' map uses (mod 60) hash
        // function to record stats of all transactions that happened
        // during a particular second within the last 60 seconds.

        long newTransactionTimeInSeconds = (transaction.getTimestamp()
                / MILLISEC_IN_A_SECOND);
        long newTransactionTimeStampOffsetSeconds = newTransactionTimeInSeconds
                % RECORDING_INTERVAL_IN_SECS;
        try {
            AggregateTransactionStatsTracker existingTransactionStatsTracker = aggregateTransactionStatsBySecondOffset
                    .get(newTransactionTimeInSeconds);

            if (existingTransactionStatsTracker == null) {
                // This is the first transaction for this seconds. Create a new
                // stats instance
                existingTransactionStatsTracker = new AggregateTransactionStatsTracker(
                        newTransactionTimeInSeconds);
                existingTransactionStatsTracker.recordTransaction(transaction);
                aggregateTransactionStatsBySecondOffset.put(
                        newTransactionTimeInSeconds,
                        existingTransactionStatsTracker);
            } else if (existingTransactionStatsTracker
                    .getTimestampInSeconds() == newTransactionTimeInSeconds) {
                // Transactions have already been recorded at the same time (in
                // second). So, update the stats based on this new transaction
                existingTransactionStatsTracker.recordTransaction(transaction);
            } else if (existingTransactionStatsTracker
                    .getTimestampInSeconds() < newTransactionTimeInSeconds) {
                // The current stats tracker has been recording stats for a
                // time (in seconds) before the current 60 seconds windows.
                // So, replace it with a new tracker for this new
                // transactionTime

                aggregateTransactionStatsBySecondOffset
                        .remove(newTransactionTimeStampOffsetSeconds);
                AggregateTransactionStatsTracker newStatistics = new AggregateTransactionStatsTracker(
                        newTransactionTimeInSeconds);
                newStatistics.recordTransaction(transaction);
                aggregateTransactionStatsBySecondOffset.put(
                        newTransactionTimeStampOffsetSeconds, newStatistics);
            }
        } finally {
            transactionLocks.unlock(writeLock);
        }
    }
}

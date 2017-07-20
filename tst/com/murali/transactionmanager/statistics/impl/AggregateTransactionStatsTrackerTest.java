package com.murali.transactionmanager.statistics.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.murali.transactionmanager.model.Transaction;

/**
 * Unit tests for {@link AggregateTransactionStatsTracker}
 */
public class AggregateTransactionStatsTrackerTest {
    private ExecutorService executorService;

    @Before
    public void setup() {
        executorService = Executors.newFixedThreadPool(30);
    }

    @Test
    public void testStatsWhenNoTransactionWasRecorded() {
        invokeAndVerifyAmount(0);
    }

    @Test
    public void testVerifyStatsAfterOneTransaction() {
        invokeAndVerifyAmount(1);
    }

    @Test
    public void testVerifyStatsAfter1000Transactions()
            throws InterruptedException {
        invokeAndVerifyAmount(1000);
    }

    private void invokeAndVerifyAmount(int numberOfRecordings) {
        long timestamp = Clock.systemUTC().millis();

        AggregateTransactionStatsTracker aggregateTransactionStatsTracker = new AggregateTransactionStatsTracker(
                timestamp);

        try {
            recordTransaction(aggregateTransactionStatsTracker,
                    numberOfRecordings);
        } catch (InterruptedException e) {
            fail("Test was interrupted due to exception: " + e);
        }

        verifyStats(aggregateTransactionStatsTracker.getStats(),
                numberOfRecordings);
    }

    private void verifyStats(Map<String, Number> stats,
            long numberOfRecordings) {
        long actualCount = stats.get(CountStatTracker.STAT_NAME).longValue();
        double actualSum = stats.get(SumOfAmountsStatTracker.STAT_NAME)
                .doubleValue();

        assertEquals(numberOfRecordings, actualCount);
        assertEquals(generateSumOfNNumber(numberOfRecordings), actualSum, 0.0);


        if (numberOfRecordings == 0) {
            assertNull(stats.get(MaxAmountStatTracker.STAT_NAME));
            assertNull(stats.get(MinAmountStatTracker.STAT_NAME));
            assertEquals(0,
                    stats.get(AggregateTransactionStatsTracker.AVG_STAT_NAME)
                            .doubleValue(),
                    0.0);
        } else {
            assertEquals(actualSum / actualCount,
                    stats.get(AggregateTransactionStatsTracker.AVG_STAT_NAME)
                            .doubleValue(),
                    0.0);
            assertEquals(numberOfRecordings,
                    stats.get(MaxAmountStatTracker.STAT_NAME).doubleValue(),
                    0.0);
            assertEquals(1,
                    stats.get(MinAmountStatTracker.STAT_NAME).doubleValue(),
                    0.0);
        }
    }

    private void recordTransaction(
            AggregateTransactionStatsTracker aggregateTransactionStatsTracker,
            int numberOfRecordings) throws InterruptedException {
        for (int i = 0; i < numberOfRecordings; i++) {
            double amount = i + 1;
            executorService.submit(() -> aggregateTransactionStatsTracker
                    .recordTransaction(new Transaction(amount,
                            System.currentTimeMillis())));
        }
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }

    private long generateSumOfNNumber(long n) {
        return (n * (n + 1)) / 2;
    }
}

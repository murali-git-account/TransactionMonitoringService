package com.murali.transactionmanager.statistics.impl;

import static com.murali.transactionmanager.test.TestData.VALID_TEST_TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.model.Statistic;

/**
 * Test suite for (@link MinAmountStatTracker}
 */
public class MinAmountStatTrackerTest {
    private MinAmountStatTracker statTracker;
    private ExecutorService executorService;

    @Before
    public void setup() {
        statTracker = new MinAmountStatTracker();
        executorService = Executors.newFixedThreadPool(30);
    }

    @Test
    public void testCountWhenNoTransactionWasRecorded() {
        invokeAndVerifyAmount(0, 0);
    }

    @Test
    public void testVerifyCountAfterOneTransaction() {
        invokeAndVerifyAmount(1, 1);
    }

    @Test
    public void testVerifyCountAfter1000Transactions() {
        invokeAndVerifyAmount(1000, 1);
    }

    @Test
    public void testVerifyMergeStatisticsLowerAmount() {
        statTracker.recordTransaction(VALID_TEST_TRANSACTION);
        statTracker.mergeStatistic(
                new Statistic(MinAmountStatTracker.STAT_NAME, -1));

        Statistic actualStatistic = statTracker.getStatistics();
        assertEquals(MinAmountStatTracker.STAT_NAME, actualStatistic.getUnit());
        assertEquals(-1, actualStatistic.getValue(), 0.0);
    }

    @Test
    public void testVerifyMergeStatisticsHigherAmount() {
        statTracker.recordTransaction(VALID_TEST_TRANSACTION);
        statTracker.mergeStatistic(
                new Statistic(MinAmountStatTracker.STAT_NAME, 100));

        Statistic actualStatistic = statTracker.getStatistics();
        assertEquals(MinAmountStatTracker.STAT_NAME, actualStatistic.getUnit());
        assertEquals(VALID_TEST_TRANSACTION.getAmount(),
                actualStatistic.getValue(), 0.0);
    }

    private void invokeAndVerifyAmount(int numberOfRecordings,
            long expectedAmount) {
        try {
            recordTransaction(numberOfRecordings);
        } catch (InterruptedException e) {
            fail("Test was interrupted due to exception: " + e);
        }

        Statistic actualStatistic = statTracker.getStatistics();

        if (numberOfRecordings == 0) {
            // TODO: Need to clarify requirements; for now returning if there is
            // no data
            assertNull(actualStatistic);
        } else {
            assertEquals(MinAmountStatTracker.STAT_NAME,
                    actualStatistic.getUnit());
            assertEquals(expectedAmount, actualStatistic.getValue(), 0.0);
        }
    }

    private void recordTransaction(int numberOfRecordings)
            throws InterruptedException {
        for (int i = 0; i < numberOfRecordings; i++) {
            double amount = i + 1;
            executorService.submit(() -> statTracker.recordTransaction(
                    new Transaction(amount, System.currentTimeMillis())));
        }
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }
}
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
 * Test suite for (@link MaxAmountStatTracker}
 */
public class MaxAmountStatTrackerTest {
    private MaxAmountStatTracker statTracker;
    private ExecutorService executorService;

    @Before
    public void setup() {
        statTracker = new MaxAmountStatTracker();
        executorService = Executors.newFixedThreadPool(30);
    }

    @Test
    public void testMaxAmountWhenNoTransactionWasRecorded() {
        invokeAndVerifyAmount(0, 0);
    }

    @Test
    public void testVerifyMaxAmountAfterOneTransaction() {
        invokeAndVerifyAmount(1, 1);
    }

    @Test
    public void testVerifyMaxAmountAfter1000Transactions() {
        invokeAndVerifyAmount(1000, 1000);
    }

    @Test
    public void testVerifyMergeStatisticsLowerAmount() {
        statTracker.recordTransaction(VALID_TEST_TRANSACTION);
        statTracker.mergeStatistic(
                new Statistic(MaxAmountStatTracker.STAT_NAME, -1));

        Statistic actualStatistic = statTracker.getStatistics();
        assertEquals(MaxAmountStatTracker.STAT_NAME, actualStatistic.getUnit());
        assertEquals(VALID_TEST_TRANSACTION.getAmount(),
                actualStatistic.getValue(), 0.0);
    }
    
    @Test
    public void testVerifyMergeStatisticsHigherAmount() {
        statTracker.recordTransaction(VALID_TEST_TRANSACTION);
        statTracker.mergeStatistic(
                new Statistic(MaxAmountStatTracker.STAT_NAME, 100));

        Statistic actualStatistic = statTracker.getStatistics();
        assertEquals(MaxAmountStatTracker.STAT_NAME, actualStatistic.getUnit());
        assertEquals(100,
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
            assertEquals(MaxAmountStatTracker.STAT_NAME,
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

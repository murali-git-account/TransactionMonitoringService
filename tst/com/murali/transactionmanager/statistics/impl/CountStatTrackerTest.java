package com.murali.transactionmanager.statistics.impl;

import static com.murali.transactionmanager.test.TestData.VALID_TEST_TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.murali.transactionmanager.statistics.model.Statistic;

/**
 * Test suite for (@link CountStatTrackerTest}
 */
public class CountStatTrackerTest {
    private CountStatTracker statTracker;
    private ExecutorService executorService;

    @Before
    public void setup() {
        statTracker = new CountStatTracker();
        executorService = Executors.newFixedThreadPool(30);
    }

    @Test
    public void testCountWhenNoTransactionWasRecorded() {
        invokeAndVerifyCount(0, 0);
    }

    @Test
    public void testVerifyCountAfterOneTransaction() {
        invokeAndVerifyCount(1, 1);
    }

    @Test
    public void testVerifyCountAfter1000Transactions() {
        invokeAndVerifyCount(1000, 1000);
    }

    @Test
    public void testVerifyMergeStatistics() {
        statTracker.recordTransaction(VALID_TEST_TRANSACTION);
        statTracker.mergeStatistic(statTracker.getStatistics());

        Statistic actualStatistic = statTracker.getStatistics();
        assertEquals(CountStatTracker.STAT_NAME, actualStatistic.getUnit());
        assertEquals(2, actualStatistic.getValue(), 0.0);
    }

    private void invokeAndVerifyCount(int numberOfRecordings,
            long expectedCount) {
        try {
            recordTransaction(numberOfRecordings);
        } catch (InterruptedException e) {
            fail("Test was interrupted due to exception: " + e);
        }

        Statistic actualStatistic = statTracker.getStatistics();

        assertEquals(CountStatTracker.STAT_NAME, actualStatistic.getUnit());
        assertEquals(expectedCount, actualStatistic.getValue(), 0.0);
    }

    private void recordTransaction(int numberOfRecordings)
            throws InterruptedException {
        for (int i = 0; i < numberOfRecordings; i++) {
            executorService.submit(() -> statTracker
                    .recordTransaction(VALID_TEST_TRANSACTION));
        }
        executorService.awaitTermination(2, TimeUnit.SECONDS);
    }
}

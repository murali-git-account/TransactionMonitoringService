package com.murali.transactionmanager.statistics.impl;

import static com.murali.transactionmanager.test.TestData.VALID_TEST_TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.model.Statistic;

/**
 * Test suite for (@link SumOfAmountsStatTrackerTest}
 */
public class SumOfAmountsStatTrackerTest {
    private SumOfAmountsStatTracker statTracker;
    private ExecutorService executorService;

    @Before
    public void setup() {
        statTracker = new SumOfAmountsStatTracker();
        executorService = Executors.newFixedThreadPool(30);
    }

    @Test
    public void testSumWhenNoTransactionWasRecorded() {
        invokeAndVerifyAmount(0, 0);
    }

    @Test
    public void testVerifySumAfterOneTransaction() {
        invokeAndVerifyAmount(1, 1);
    }

    @Test
    public void testVerifySumAfter1000Transactions()
            throws InterruptedException {
        invokeAndVerifyAmount(5, generateSumOfNNumber(5));
    }

    @Test
    public void testVerifyMergeStatistics() {
        statTracker.recordTransaction(VALID_TEST_TRANSACTION);
        statTracker.mergeStatistic(statTracker.getStatistics());

        Statistic actualStatistic = statTracker.getStatistics();
        assertEquals(SumOfAmountsStatTracker.STAT_NAME,
                actualStatistic.getUnit());
        assertEquals(VALID_TEST_TRANSACTION.getAmount() * 2,
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
        assertEquals(SumOfAmountsStatTracker.STAT_NAME,
                actualStatistic.getUnit());
        assertEquals(expectedAmount, actualStatistic.getValue(), 0.0);
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

    private long generateSumOfNNumber(long n) {
        return (n * (n + 1)) / 2;
    }
}
package com.murali.transactionmanager.store.impl;

import static com.murali.transactionmanager.test.TestData.VALID_TEST_TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.murali.transactionmanager.model.ReturnCode;
import com.murali.transactionmanager.model.Transaction;
import com.murali.transactionmanager.statistics.impl.AggregateTransactionStatsTracker;
import com.murali.transactionmanager.statistics.impl.CountStatTracker;
import com.murali.transactionmanager.statistics.impl.MaxAmountStatTracker;
import com.murali.transactionmanager.statistics.impl.MinAmountStatTracker;
import com.murali.transactionmanager.statistics.impl.SumOfAmountsStatTracker;

/**
 * Unit tests for {@link InMemoryTransactionRecorder}
 */
public class InMemoryTransactionRecorderTest {

    private static final double ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON = 0.01;

    @Mock
    private Supplier<Long> mockCurrentTimeSupplier;

    private InMemoryTransactionRecorder transactionRecorder;
    private ExecutorService executorService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        transactionRecorder = new InMemoryTransactionRecorder();
        transactionRecorder.setCurrentTimeSupplier(mockCurrentTimeSupplier);
        executorService = Executors.newFixedThreadPool(30);
    }

    @Test
    public void testWhenTransactionIsWithinRecordingInterval() {
        when(mockCurrentTimeSupplier.get()).thenReturn(VALID_TEST_TRANSACTION
                .getTimestamp()
                + InMemoryTransactionRecorder.RECORDING_INTERVAL_IN_MILLIS / 2);
        assertEquals(ReturnCode.SUCCESS,
                transactionRecorder.record(VALID_TEST_TRANSACTION));
        verify(mockCurrentTimeSupplier, times(1)).get();
    }

    @Test
    public void testWhenTransactionIsRightAtTheRecordingInterval() {
        when(mockCurrentTimeSupplier.get())
                .thenReturn(VALID_TEST_TRANSACTION.getTimestamp());
        assertEquals(ReturnCode.SUCCESS,
                transactionRecorder.record(VALID_TEST_TRANSACTION));
        verify(mockCurrentTimeSupplier, times(1)).get();
    }

    @Test
    public void testWhenTransactionIsBeyondRecordingInterval() {
        when(mockCurrentTimeSupplier.get()).thenReturn(VALID_TEST_TRANSACTION
                .getTimestamp()
                + InMemoryTransactionRecorder.RECORDING_INTERVAL_IN_MILLIS * 2);
        assertEquals(ReturnCode.OLD_TRANSACTION,
                transactionRecorder.record(VALID_TEST_TRANSACTION));
        verify(mockCurrentTimeSupplier, times(1)).get();
    }

    @Test
    public void testMultipleTransactionRecordingWithNegativeValues()
            throws InterruptedException {
        long timestampInMillis = System.currentTimeMillis();
        transactionRecorder.setCurrentTimeSupplier(System::currentTimeMillis);

        transactionRecorder.record(new Transaction(-10.8, timestampInMillis));
        transactionRecorder.record(new Transaction(1.2, timestampInMillis));

        Map<String, Number> stats = transactionRecorder.getStatistics();

        assertEquals(2, stats.get(CountStatTracker.STAT_NAME).longValue());
        assertEquals(-9.6,
                stats.get(SumOfAmountsStatTracker.STAT_NAME).doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
        assertEquals(1.2,
                stats.get(MaxAmountStatTracker.STAT_NAME).doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
        assertEquals(-10.8,
                stats.get(MinAmountStatTracker.STAT_NAME).doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
        assertEquals(-4.8,
                stats.get(AggregateTransactionStatsTracker.AVG_STAT_NAME)
                        .doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
    }

    @Test
    public void testMultipleTransactionRecordingWithSameTimestamp()
            throws InterruptedException {
        long timestampInMillis = System.currentTimeMillis();
        transactionRecorder.setCurrentTimeSupplier(System::currentTimeMillis);

        recordTransaction(100, timestampInMillis, 0);

        verifyStats(transactionRecorder.getStatistics(), 100);
    }

    @Test
    public void testMultipleTransactionRecordingAcrossSeconds()
            throws InterruptedException {
        long timestampInMillis = System.currentTimeMillis();
        transactionRecorder.setCurrentTimeSupplier(System::currentTimeMillis);

        recordTransaction(100, timestampInMillis, 1000);

        double expectedSum = generateSumOfNNumber(60);
        verifyStats(60, expectedSum, expectedSum / 60.0, 60, 1);
    }

    @Test
    public void testWhenNoTransactionIsRecorded() throws InterruptedException {
        transactionRecorder.setCurrentTimeSupplier(System::currentTimeMillis);
        verifyStats(transactionRecorder.getStatistics(), 0);
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
            assertEquals(0.0,
                    stats.get(AggregateTransactionStatsTracker.AVG_STAT_NAME)
                            .doubleValue(),
                    0.0);
        } else {
            assertEquals(numberOfRecordings,
                    stats.get(MaxAmountStatTracker.STAT_NAME).doubleValue(),
                    0.0);
            assertEquals(1,
                    stats.get(MinAmountStatTracker.STAT_NAME).doubleValue(),
                    0.0);
            assertEquals(actualSum / actualCount,
                    stats.get(AggregateTransactionStatsTracker.AVG_STAT_NAME)
                            .doubleValue(),
                    0.0);
        }
    }

    private void recordTransaction(int numberOfRecordings,
            long timestampInMillis, int timestampDecrementMillis)
            throws InterruptedException {

        for (int i = 0; i < numberOfRecordings; i++) {
            double amount = i + 1;
            AtomicLong newTimestamp = new AtomicLong(
                    timestampInMillis - (i * timestampDecrementMillis));
            executorService.submit(() -> transactionRecorder
                    .record(new Transaction(amount, newTimestamp.longValue())));
        }
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void verifyStats(long count, double sum, double avg, double max,
            double min) {
        Map<String, Number> stats = transactionRecorder.getStatistics();

        assertEquals(count, stats.get(CountStatTracker.STAT_NAME).longValue());
        assertEquals(sum,
                stats.get(SumOfAmountsStatTracker.STAT_NAME).doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
        assertEquals(max,
                stats.get(MaxAmountStatTracker.STAT_NAME).doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
        assertEquals(min,
                stats.get(MinAmountStatTracker.STAT_NAME).doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
        assertEquals(avg,
                stats.get(AggregateTransactionStatsTracker.AVG_STAT_NAME)
                        .doubleValue(),
                ACCEPTABLE_DELTA_IN_DOUBLE_COMPARISON);
    }

    private double generateSumOfNNumber(long n) {
        return Double.valueOf((n * (n + 1)) / 2.0);
    }
}

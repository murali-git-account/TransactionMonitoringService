package com.murali.transactionmanager.statistics.model;

import java.util.concurrent.locks.StampedLock;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Base class for all statistic captured by the monitor
 */
public class Statistic {
    private final String unit;
    private double value;

    private StampedLock valueUpdateLock = new StampedLock();

    public Statistic(String unit, double value) {
        Validate.isTrue(StringUtils.isNotBlank(unit));
        Validate.notNull(value);

        this.unit = unit;
        this.value = value;
    }

    public final String getUnit() {
        return this.unit;
    }

    public final double getValue() {
        long tryOptimisticRead = valueUpdateLock.tryOptimisticRead();

        if (!valueUpdateLock.validate(tryOptimisticRead)) {
            tryOptimisticRead = valueUpdateLock.readLock();
            try {
                return this.getValue();
            } finally {
                valueUpdateLock.unlock(tryOptimisticRead);
            }
        } else {

            return this.value;
        }
    }

    public void setValue(double value) {
        long writeLock = valueUpdateLock.writeLock();

        try {
            this.value = value;
        } finally {
            valueUpdateLock.unlock(writeLock);
        }
    }

    public void add(double value) {
        long writeLock = valueUpdateLock.writeLock();

        try {
            this.value += value;
        } finally {
            valueUpdateLock.unlock(writeLock);
        }
    }

    public void add(Statistic statistic) {
        Validate.notNull(statistic);
        if (statistic.getUnit().equals(statistic.getUnit())) {
            long writeLock = valueUpdateLock.writeLock();

            try {
                this.value += value;
            } finally {
                valueUpdateLock.unlock(writeLock);
            }
        } else {
            throw new IllegalArgumentException(
                    "Cannot add Statistic with unit " + statistic.getUnit()
                            + " to a statistic with unit " + this.getUnit());
        }
    }
}

package com.scitequest.martin.utils;

import java.util.DoubleSummaryStatistics;
import java.util.stream.Collector;

/** https://codereview.stackexchange.com/a/125418 . */
public final class DoubleStatistics extends DoubleSummaryStatistics {

    private double sumOfSquare = 0.0d;
    /** Low order bits of sum. */
    private double sumOfSquareCompensation;
    /** Used to compute right sum for non-finite inputs. */
    private double simpleSumOfSquare;

    @Override
    public void accept(double value) {
        super.accept(value);
        double squareValue = value * value;
        simpleSumOfSquare += squareValue;
        sumOfSquareWithCompensation(squareValue);
    }

    public DoubleStatistics combine(DoubleStatistics other) {
        super.combine(other);
        simpleSumOfSquare += other.simpleSumOfSquare;
        sumOfSquareWithCompensation(other.sumOfSquare);
        sumOfSquareWithCompensation(other.sumOfSquareCompensation);
        return this;
    }

    private void sumOfSquareWithCompensation(double value) {
        double tmp = value - sumOfSquareCompensation;
        // Little wolf of rounding error
        double velvel = sumOfSquare + tmp;
        sumOfSquareCompensation = (velvel - sumOfSquare) - tmp;
        sumOfSquare = velvel;
    }

    public double getSumOfSquare() {
        double tmp = sumOfSquare + sumOfSquareCompensation;
        if (Double.isNaN(tmp) && Double.isInfinite(simpleSumOfSquare)) {
            return simpleSumOfSquare;
        }
        return tmp;
    }

    public double getStandardDeviation() {
        long count = getCount();
        if (count > 0) {
            return Math.sqrt((getSumOfSquare() - count * Math.pow(getAverage(), 2)) / (count));
        }
        return 0.0;
    }

    public double getSampleStandardDeviation() {
        long count = getCount();
        if (count > 0.0) {
            return Math.sqrt((getSumOfSquare() - count * Math.pow(getAverage(), 2))
                    / (count - 1.0));
        }
        return 0.0;
    }

    public static Collector<Double, ?, DoubleStatistics> collector() {
        return Collector.of(DoubleStatistics::new, DoubleStatistics::accept,
                DoubleStatistics::combine);
    }
}

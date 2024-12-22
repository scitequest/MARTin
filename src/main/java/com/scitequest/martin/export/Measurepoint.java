package com.scitequest.martin.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scitequest.martin.utils.DoubleStatistics;

/** Values and metadata of a single measured measurement circle. */
public final class Measurepoint implements Comparable<Measurepoint> {

    /** The spotfield this datapoint pertains to. */
    private final int spot;
    /** The row of the grid. */
    private final int row;
    /** The column of the grid. */
    private final int col;

    /** The minimum value that was measured within the area. */
    private final double min;
    /** The maximum value that was measured within the area. */
    private final double max;
    /** The mean of the measurement area. */
    private final double mean;
    /** The standard deviation of all measurement values. */
    @JsonProperty("std_deviation")
    private final double stdDev;

    private Measurepoint(int spot, int row, int col,
            double min, double max, double mean, double stdDev) {
        this.spot = spot;
        this.row = row;
        this.col = col;
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stdDev = stdDev;
    }

    /**
     * Create a new Datapoint.
     *
     * The values are checked for consistency. This is now described: spotfield,
     * row, column, diameter must be positive. {@code min <= mean <= max}.
     *
     * @param spot   the spotfield
     * @param row    the row in the spotfield
     * @param col    the column in the spotfield
     * @param min    the minimum value measured
     * @param max    the maximum value measured
     * @param mean   the mean measured
     * @param stdDev the standard deviation
     * @return a new MeasurePoint
     * @throws IllegalArgumentException if any check fails
     */
    @JsonCreator
    public static Measurepoint of(
            @JsonProperty("spot") int spot,
            @JsonProperty("row") int row,
            @JsonProperty("col") int col,
            @JsonProperty("min") double min,
            @JsonProperty("max") double max,
            @JsonProperty("mean") double mean,
            @JsonProperty("std_deviation") double stdDev)
            throws IllegalArgumentException {
        if (spot < 0) {
            throw new IllegalArgumentException("Spotfield cannot be less than zero");
        }
        if (row < 0 || col < 0) {
            throw new IllegalArgumentException("Row and column cannot be less than zero");
        }
        if (min > max) {
            throw new IllegalArgumentException(
                    String.format("Minimum (%f) must be smaller than maximum (%f)", min, max));
        }
        if (mean < min || mean > max) {
            throw new IllegalArgumentException("Mean must in the interval [min; max]");
        }
        return new Measurepoint(spot, row, col, min, max, mean, stdDev);
    }

    public static Measurepoint tryFrom(int spot, int row, int col, DoubleStatistics stats) {
        if (stats.getCount() == 0) {
            throw new IllegalArgumentException("Measured pixel count is 0");
        }
        return Measurepoint.of(spot, row, col,
                stats.getMin(), stats.getMax(), stats.getAverage(),
                stats.getSampleStandardDeviation());
    }

    /**
     * Get the spotfield.
     *
     * @return the spotfield
     */
    public int getSpot() {
        return spot;
    }

    /**
     * Get the row.
     *
     * @return the row
     */
    public int getRow() {
        return row;
    }

    /**
     * Get the column.
     *
     * @return the column
     */
    public int getCol() {
        return col;
    }

    /**
     * Get the minimum.
     *
     * @return the minimum
     */
    public double getMin() {
        return min;
    }

    /**
     * Get the maximum.
     *
     * @return the maximum
     */
    public double getMax() {
        return max;
    }

    /**
     * Get the mean.
     *
     * @return the mean
     */
    public double getMean() {
        return mean;
    }

    /**
     * Get the standard deviation.
     *
     * @return the standard deviation
     */
    public double getStdDev() {
        return stdDev;
    }

    @Override
    public String toString() {
        return "Datapoint [spot=" + spot + ", row=" + row + ", col=" + col
                + ", min=" + min + ", max=" + max
                + ", mean=" + mean + ", stdDev=" + stdDev + "]";
    }

    @Override
    public int compareTo(Measurepoint other) {
        int ret = Integer.compare(this.spot, other.spot);
        if (ret != 0) {
            return ret;
        }
        ret = Integer.compare(this.row, other.row);
        if (ret != 0) {
            return ret;
        }
        return Integer.compare(this.col, other.col);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + spot;
        result = prime * result + row;
        result = prime * result + col;
        long temp;
        temp = Double.doubleToLongBits(min);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(max);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mean);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(stdDev);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @SuppressWarnings("checkstyle:NeedBraces")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Measurepoint other = (Measurepoint) obj;
        if (spot != other.spot)
            return false;
        if (row != other.row)
            return false;
        if (col != other.col)
            return false;
        if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min))
            return false;
        if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max))
            return false;
        if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
            return false;
        if (Double.doubleToLongBits(stdDev) != Double.doubleToLongBits(other.stdDev))
            return false;
        return true;
    }

    @SuppressWarnings("checkstyle:NeedBraces")
    public boolean equalsEpsilon(Object obj, double eps) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Measurepoint other = (Measurepoint) obj;
        if (spot != other.spot)
            return false;
        if (row != other.row)
            return false;
        if (col != other.col)
            return false;
        if (!equalsEpsilon(min, other.min, eps))
            return false;
        if (!equalsEpsilon(max, other.max, eps))
            return false;
        if (!equalsEpsilon(mean, other.mean, eps))
            return false;
        if (!equalsEpsilon(stdDev, other.stdDev, eps))
            return false;
        return true;
    }

    private static boolean equalsEpsilon(double x, double y, double eps) {
        return Math.abs(x - y) < eps;
    }
}

package com.scitequest.martin.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class DatapointStatistics {

    @JsonProperty("row")
    private final int row;
    @JsonProperty("col")
    private final int column;
    @JsonProperty("adjusted_average")
    private final double rawAvg;
    @JsonProperty("adjusted_average_std_deviation")
    private final double stdDevRawAvg;
    @JsonProperty("relative_adjusted_average_std_deviation")
    private final double relStdDevRawAvg;
    @JsonProperty("normalized_average")
    private final double rawAvgNormalized;
    @JsonProperty("normalized_average_std_deviation")
    private final double stdDevNormAvg;
    @JsonProperty("relative_normalized_average_std_deviation")
    private final double relStdDevNormAvg;

    private DatapointStatistics(int row, int column,
            double rawAvg, double stdDevRawAvg, double relStdDevRawAvg,
            double rawAvgNormalized, double stdDevNormAvg, double relStdDevNormAvg) {
        this.row = row;
        this.column = column;
        this.rawAvg = rawAvg;
        this.stdDevRawAvg = stdDevRawAvg;
        this.relStdDevRawAvg = relStdDevRawAvg;
        this.rawAvgNormalized = rawAvgNormalized;
        this.stdDevNormAvg = stdDevNormAvg;
        this.relStdDevNormAvg = relStdDevNormAvg;
    }

    @JsonCreator
    public static DatapointStatistics of(
            @JsonProperty("row") int row,
            @JsonProperty("col") int column,
            @JsonProperty("adjusted_average") double rawAvg,
            @JsonProperty("adjusted_average_std_deviation") double stdDevRawAvg,
            @JsonProperty("relative_adjusted_average_std_deviation") double relStdDevRawAvg,
            @JsonProperty("normalized_average") double rawAvgNormalized,
            @JsonProperty("normalized_average_std_deviation") double stdDevNormAvg,
            @JsonProperty("relative_normalized_average_std_deviation") double relStdDevNormAvg) {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException(String.format(
                "Row and column cannot be less than zero. Provided values: row = %d, column = %d", 
                row, column));
        }
        return new DatapointStatistics(row, column,
                rawAvg, stdDevRawAvg, relStdDevRawAvg,
                rawAvgNormalized, stdDevNormAvg, relStdDevNormAvg);
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public double getRawAvg() {
        return rawAvg;
    }

    public double getRawAvgNormalized() {
        return rawAvgNormalized;
    }

    public double getStdDevRawAvg() {
        return stdDevRawAvg;
    }

    public double getRelStdDevRawAvg() {
        return relStdDevRawAvg;
    }

    public double getStdDevNormAvg() {
        return stdDevNormAvg;
    }

    public double getRelStdDevNormAvg() {
        return relStdDevNormAvg;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + row;
        result = prime * result + column;
        long temp;
        temp = Double.doubleToLongBits(rawAvg);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(rawAvgNormalized);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(stdDevRawAvg);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(relStdDevRawAvg);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(stdDevNormAvg);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(relStdDevNormAvg);
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
        DatapointStatistics other = (DatapointStatistics) obj;
        if (row != other.row)
            return false;
        if (column != other.column)
            return false;
        if (Double.doubleToLongBits(rawAvg) != Double.doubleToLongBits(other.rawAvg))
            return false;
        if (Double.doubleToLongBits(rawAvgNormalized) != Double.doubleToLongBits(other.rawAvgNormalized))
            return false;
        if (Double.doubleToLongBits(stdDevRawAvg) != Double.doubleToLongBits(other.stdDevRawAvg))
            return false;
        if (Double.doubleToLongBits(relStdDevRawAvg) != Double.doubleToLongBits(other.relStdDevRawAvg))
            return false;
        if (Double.doubleToLongBits(stdDevNormAvg) != Double.doubleToLongBits(other.stdDevNormAvg))
            return false;
        if (Double.doubleToLongBits(relStdDevNormAvg) != Double.doubleToLongBits(other.relStdDevNormAvg))
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
        DatapointStatistics other = (DatapointStatistics) obj;
        if (row != other.row)
            return false;
        if (column != other.column)
            return false;
        if (!equalsEpsilon(rawAvg, other.rawAvg, eps))
            return false;
        if (!equalsEpsilon(rawAvgNormalized, other.rawAvgNormalized, eps))
            return false;
        if (!equalsEpsilon(stdDevRawAvg, other.stdDevRawAvg, eps))
            return false;
        if (!equalsEpsilon(relStdDevRawAvg, other.relStdDevRawAvg, eps))
            return false;
        if (!equalsEpsilon(stdDevNormAvg, other.stdDevNormAvg, eps))
            return false;
        if (!equalsEpsilon(relStdDevNormAvg, other.relStdDevNormAvg, eps))
            return false;
        return true;
    }

    private static boolean equalsEpsilon(double x, double y, double eps) {
        return Math.abs(x - y) < eps;
    }
}

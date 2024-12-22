package com.scitequest.martin.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@JsonPropertyOrder({ "measurePoint", "meanMinusMin", "normalizedMean" })
public final class Datapoint {

    /** Raw measurements taken of the image. */
    @JsonUnwrapped
    private final Measurepoint measurePoint;
    /** Mean value subtracted by minimumValue of Dataset. */
    @JsonProperty("mean_minus_min")
    private final double meanMinusMin;
    /** Internally normalized mean value of spot relative to its set. */
    @JsonProperty("normalized_mean")
    private final double normalizedMean;

    private Datapoint(Measurepoint measurePoint, double meanMinusMin, double iNormalized) {
        this.measurePoint = measurePoint;
        this.meanMinusMin = meanMinusMin;
        this.normalizedMean = iNormalized;
    }

    /**
     * @param measurePoint The raw values measured.
     * @param meanMinusMin Adjusted value. Minimum of set is set to zero each other
     *                     Datapoint has to be shifted by the same ammount.
     *                     Represents the dynamic range.
     * @param iNormalized  Normalized mean value relative to set of this Datapoint
     *
     * @return a new instance of DataPoint.
     */
    @JsonCreator
    public static Datapoint of(
            @JsonProperty("measure_point") Measurepoint measurePoint,
            @JsonProperty("mean_minus_min") double meanMinusMin,
            @JsonProperty("normalized_mean") double iNormalized) {
        return new Datapoint(measurePoint, meanMinusMin, iNormalized);
    }

    /**
     * Get the original measurepoint.
     *
     * @return the measurepoint
     */
    public Measurepoint getMeasurePoint() {
        return measurePoint;
    }

    /**
     * Get the mean minus min value.
     *
     * @return the mean minus min value
     */
    public double getMeanMinusMin() {
        return meanMinusMin;
    }

    /**
     * Get the normalized mean.
     *
     * @return the normalized mean
     */
    public double getNormalizedMean() {
        return normalizedMean;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((measurePoint == null) ? 0 : measurePoint.hashCode());
        long temp;
        temp = Double.doubleToLongBits(meanMinusMin);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(normalizedMean);
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
        Datapoint other = (Datapoint) obj;
        if (measurePoint == null) {
            if (other.measurePoint != null)
                return false;
        } else if (!measurePoint.equals(other.measurePoint))
            return false;
        if (Double.doubleToLongBits(meanMinusMin) != Double.doubleToLongBits(other.meanMinusMin))
            return false;
        if (Double.doubleToLongBits(normalizedMean) != Double.doubleToLongBits(other.normalizedMean))
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
        Datapoint other = (Datapoint) obj;
        if (measurePoint == null || other.measurePoint == null)
            return false;
        if (!measurePoint.equalsEpsilon(other.measurePoint, eps))
            return false;
        if (!equalsEpsilon(meanMinusMin, other.meanMinusMin, eps))
            return false;
        if (!equalsEpsilon(normalizedMean, other.normalizedMean, eps))
            return false;
        return true;
    }

    private static boolean equalsEpsilon(double x, double y, double eps) {
        return Math.abs(x - y) < eps;
    }
}

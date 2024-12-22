package com.scitequest.martin.export;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scitequest.martin.Point;

public class Circle implements Geometry {
    public final Point position;
    public final int diameter;

    private Circle(Point position, int diameter) {
        this.position = position;
        this.diameter = diameter;
    }

    @JsonCreator
    public static Circle of(
            @JsonProperty("position") Point position,
            @JsonProperty("diameter") int diameter) {
        if (diameter < 0) {
            throw new IllegalArgumentException("Diameter must be positive");
        }
        return new Circle(position, diameter);
    }

    @Override
    public Geometry moveTo(double x, double y) {
        return Circle.of(Point.of(x, y), diameter);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((position == null) ? 0 : position.hashCode());
        result = prime * result + diameter;
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
        Circle other = (Circle) obj;
        if (position == null) {
            if (other.position != null)
                return false;
        } else if (!position.equals(other.position))
            return false;
        if (diameter != other.diameter)
            return false;
        return true;
    }
}

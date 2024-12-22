package com.scitequest.martin.export;

import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scitequest.martin.Point;

public class Polygon implements Geometry {
    public final List<Point> coordinates;

    private Polygon(List<Point> coordinates) {
        this.coordinates = coordinates;
    }

    @JsonCreator
    public static Polygon ofPolygon(@JsonProperty("coordinates") List<Point> coordinates) {
        return new Polygon(Collections.unmodifiableList(coordinates));
    }

    public static Polygon ofRectangle(Point p0, Point p1, Point p2, Point p3) {
        List<Point> coords = List.of(p0, p1, p2, p3);
        return new Polygon(coords);
    }

    public Rectangle2D.Double calculateBounds() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Point p : coordinates) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public Geometry moveTo(double x, double y) {
        List<Point> coords = List.copyOf(coordinates);
        Rectangle2D.Double bounds = calculateBounds();

        for (int i = 0; i < coords.size(); i++) {
            Point p = coords.get(i);
            Point newPos = Point.of(p.x - bounds.x + x, p.y - bounds.y + y);
            coords.set(i, newPos);
        }

        return Polygon.ofPolygon(coords);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((coordinates == null) ? 0 : coordinates.hashCode());
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
        Polygon other = (Polygon) obj;
        if (coordinates == null) {
            if (other.coordinates != null)
                return false;
        } else if (!coordinates.equals(other.coordinates))
            return false;
        return true;
    }
}

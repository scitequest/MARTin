package com.scitequest.martin;

import com.scitequest.martin.settings.MaskSettings.MeasureShape;

/**
 * This class summarizes four coordinates into a rectangular polygon. Instances
 * of this class correspond to the elements of a slide.
 */
public final class PolyShape {
    /**
     * Each coordinate corresponds to one of the four corners of our shape. The
     * number of coordinates is variable, rectangles and lines are for example both
     * possible. The coordinates should be sorted clockwise.
     */
    private Coordinates[] corners;
    /**
     * PolyShapes are not always supposed to be visible/interactable.
     */
    private boolean enabled;
    /**
     * The level of a polygon depicts its position on a Z-scale. The higher the
     * level, the more the polygon is in the foreground of the image. Zero means the
     * whole image, one is the slide itself.
     */
    private int level;
    /**
     * Geometric shape of this PolyShape.
     * Not sure if we need this though.
     */
    private MeasureShape shape;

    /**
     * This generates a polyShape object given any number of points.
     *
     * @param pCorners the x and y values of each corner of our polygon.
     * @param level    which Z-index this polyshape is on {@see level}
     * @param shape    the shape this poly shape describes
     */
    PolyShape(Point[] pCorners, int level, MeasureShape shape) {
        corners = new Coordinates[pCorners.length];
        for (int i = 0; i < corners.length; i++) {
            corners[i] = new Coordinates(pCorners[i]);
        }
        this.shape = shape;
        this.enabled = true;
        this.level = level;
    }

    public PolyShape getCopy() {
        Point[] copyCorners = new Point[corners.length];
        Point cornerPoint;
        for (int i = 0; i < copyCorners.length; i++) {
            cornerPoint = corners[i].getPoint();
            copyCorners[i] = Point.of(cornerPoint.x, cornerPoint.y);
        }
        // Not sure if needed, do enums behave like objects?
        MeasureShape copyShape = MeasureShape.valueOf(shape.name());
        return new PolyShape(copyCorners, level, copyShape);
    }

    public PolyShape averageCoords(PolyShape other) {
        if (!(this.getNPoints() == other.getNPoints())) {
            return null;
        }
        Point[] avgPoints = new Point[corners.length];
        for (int i = 0; i < corners.length; i++) {
            avgPoints[i] = corners[i].getPoint().midpoint(other.getCornersAsPoints()[i]);
        }
        // Not sure if needed, do enums behave like objects?
        MeasureShape copyShape = MeasureShape.valueOf(shape.name());
        return new PolyShape(avgPoints, level, copyShape);
    }

    /**
     * Sets all coordinates given x and y positions.
     *
     * @param x array containing x values.
     * @param y array containing y values.
     */
    public void setCoordinates(double[] x, double[] y) {
        for (int i = 0; i < corners.length; i++) {
            corners[i].setPoint(x[i], y[i]);
        }
        /*
         * We could check if the length of the arrays and numbers of corners are the
         * same and return an error message if they don't.
         */
    }

    /**
     * Set all coordinates given instances of Point (containing x and y).
     *
     * @param p an array containing all points of the Polygon.
     */
    public void setCoordinates(Point[] p) {
        for (int i = 0; i < corners.length; i++) {
            corners[i].setPoint(p[i]);
        }
    }

    /**
     * Moves the whole poly shape to a new center without changing its shape.
     *
     * @param newCenter new center point of this polyShape
     */
    public void moveToCenter(Point newCenter) {
        Point oldCenter = getShapeCenter();
        movePoly(newCenter.x - oldCenter.x, newCenter.y - oldCenter.y);
    }

    /**
     * This method calls the shape-specific method used for checking if a mouse
     * click was inside the bounds of any given polyShape.
     *
     * @param cX     X-coordinate of the click.
     * @param cY     Y-coordinate of the click.
     * @param radius radius of the polyShape, only used for circles.
     * @return the polyShape if clicked, null if clicked on the background.
     */
    public PolyShape isWithinBounds(double cX, double cY, double radius) {
        if (shape == MeasureShape.CIRCLE) {
            return isClickWithinPerimeter(cX, cY, radius);
        } else {
            return isClickInsidePoly(cX, cY);
        }
    }

    public boolean polysIntersect(double radius, PolyShape otherShape) {
        Point closestPoint = otherShape.getShapeCenter();
        double minDistance = Double.MAX_VALUE;
        double distance = 0;

        if (shape != MeasureShape.CIRCLE) {
            for (Point point : otherShape.getCornersAsPoints()) {
                distance = this.getShapeCenter().distanceTo(point);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestPoint = point.getCopy();
                }
            }
        } else {
            /*
             * This is only true if both shapes are circles
             * of the same radius.
             * We may want to change polyShape to either calculate
             * or store its radius.
             * For MARTin itself this is probably not neccessary.
             */
            radius = radius * 2 - 1;
        }

        return isWithinBounds(closestPoint.x, closestPoint.y, radius) != null;
    }

    /**
     * This method checks if a mouse click was within the boundaries of the
     * polyShape.
     *
     * @param cX x-value of the mouse click.
     * @param cY y-value of the mouse click.
     * @return true or false depending if the mouse click was inside the polygon.
     */
    public PolyShape isClickInsidePoly(double cX, double cY) {
        if (enabled) {
            // Special cases
            for (int i = 0; i < corners.length; i++) {
                if (corners[i].getY() == cY) {
                    // Click exactly on corner
                    if (corners[i].getX() == cX) {
                        return this;
                    }
                    // Click exactly on line
                    if (corners[(i + 1) % 4].getY() == cY) {
                        if (corners[i].getX() <= cX ^ corners[(i + 1) % 4].getX() <= cX) {
                            return this;
                        }
                    }
                }
            }

            int iCount = 0;
            double a;

            for (int i = 0; i < corners.length; i++) {
                // A XOR B on y-axis -> one above and one below the click.
                if (corners[i].getY() < cY ^ corners[(i + 1) % 4].getY() < cY) {
                    // Are both A and B in front of our click?
                    if (corners[i].getX() >= cX && corners[(i + 1) % 4].getX() >= cX) {
                        // Our click is to the left of a line.
                        iCount++;
                    } else {
                        // Is A in front of the click?
                        if (corners[i].getX() >= cX) {
                            a = (corners[i].getY() - corners[(i + 1) % 4].getY())
                                    / (corners[i].getX() - corners[(i + 1) % 4].getX());

                            a = Math.abs(a);

                            if (a != 0 && (cX - corners[(i + 1) % 4].getX()) * a < Math
                                    .abs(cY - corners[(i + 1) % 4].getY())) {
                                iCount++;
                            }
                        } else {
                            // Is B in front of the click?
                            if (corners[(i + 1) % 4].getX() >= cX) {
                                a = (corners[(i + 1) % 4].getY() - corners[i].getY())
                                        / (corners[(i + 1) % 4].getX() - corners[i].getX());

                                a = Math.abs(a);

                                if (a != 0 && (cX - corners[i].getX()) * a < Math.abs(cY - corners[i].getY())) {
                                    iCount++;
                                }
                            }
                        }
                    }
                }
            }
            if (iCount == 1) {
                return this;
            }
        }
        return null;
    }

    /**
     * In contrast to isClickInsidePoly this method checks if a click was within a
     * set radius around the first coordinate of a polyShape (index 0). This Method
     * is supposed to be used with point shaped polyShapes (i.E. circles), hence the
     * use of only the first coordinate (since they only have one).
     *
     * @param cX     x-position of click.
     * @param cY     y-position of click.
     * @param radius search radius around first coordinate (effectively treating
     *               them as circles).
     * @return reference of clicked polyShape, if there was none null is returned.
     */
    public PolyShape isClickWithinPerimeter(double cX, double cY, double radius) {
        Point centerPoint = getShapeCenter();
        if (enabled) {
            if (centerPoint.x - radius <= cX && centerPoint.x + radius >= cX) {
                if (centerPoint.y - radius <= cY && centerPoint.y + radius >= cY) {
                    return this;
                }
            }
        }
        return null;
    }

    /**
     * This method invokes Coordinate.calculateOrbit() for all coordinates.
     *
     * @param rCenter the rotational center of the slide.
     */
    public void calculateOrbits(Point rCenter) {
        for (int i = 0; i < corners.length; i++) {
            corners[i].calculateOrbit(rCenter);
        }
    }

    /**
     * This method allows us to move a polygon linearly. If all elements of a slide
     * are moved the same invoking calculateOrbits is unnecessary.
     *
     * @param deltaX change of Position on the X-Axis.
     * @param deltaY change of Position on the Y-Axis.
     */
    public void movePoly(double deltaX, double deltaY) {
        for (int i = 0; i < corners.length; i++) {
            corners[i].setPoint(corners[i].getX() + deltaX, corners[i].getY() + deltaY);
        }
    }

    /**
     * This method invokes Coordinate.setAngle() for all coordinates and adds the
     * change to the saved angle value.
     *
     * @param angle   the change in angle.
     * @param rCenter the rotational center of the slide.
     */
    public void setAngles(double angle, Point rCenter) {
        for (int i = 0; i < corners.length; i++) {
            corners[i].setAngle(angle + corners[i].getAngle(), rCenter);
        }
    }

    /**
     * Returns the coordinates of the polygon in order to make them drawable, the
     * return-format mirrors that of graphics.drawPolygon().
     *
     * @return polygon coordinates as a two-dimensional array. 0 contains the
     *         x-values, 1 contains the y-values.
     */
    public double[][] getPolyCoordinates() {
        int dim = 2; // dim as in the number of dimensions our coordinate system has.
        double[][] ret = new double[dim][corners.length];

        for (int i = 0; i < ret[0].length; i++) {
            ret[0][i] = corners[i].getX();
            ret[1][i] = corners[i].getY();
        }
        return ret;
    }

    /**
     * This method returns the center point between all coordinates of the
     * polyShape. The center of any Polygon is the average between all its points
     * for x and y.
     *
     * @return the center Point.
     */
    public Point getShapeCenter() {
        double x = 0;
        double y = 0;

        for (Coordinates corner : corners) {
            x = x + corner.getX();
            y = y + corner.getY();
        }
        x /= corners.length;
        y /= corners.length;
        return Point.of(x, y);
    }

    /**
     * This method calculates the distances between connected points of the current
     * instance of PolyShape. Sorted in the same order as the points themselves are.
     *
     * @return double array containing distances.
     */
    public double[] calculateVectorLengths() {
        double distances[] = new double[corners.length];
        double deltaX;
        double deltaY;
        for (int i = 0; i < distances.length; i++) {
            deltaX = corners[i].getX() + corners[(i + 1) % 4].getX();
            deltaY = corners[i].getY() + corners[(i + 1) % 4].getY();
            distances[i] = Math.sqrt((deltaX * deltaX + deltaY * deltaY));
        }
        return distances;
    }

    /**
     * Returns a PolyShape that generated by adding the distances between each
     * corner of sShape to their corresponding corner to this shape.
     * Effectively resulting in a shrunken down PolyShape.
     *
     * @param sShape by which this shape is to be shrunken down.
     * @return shrunken down PolyShape or null if shape of this PolsShape and sShape
     *         is
     *         unequal.
     */
    public PolyShape shrinkByShape(PolyShape sShape) {
        if (this.getNPoints() != sShape.getNPoints()) {
            return null;
        }

        Point[] deltaDist = new Point[sShape.getNPoints()];
        double[][] sShapeCoords = sShape.getPolyCoordinates();
        Point sShapeCenter = sShape.getShapeCenter();
        double cX;
        double cY;
        for (int i = 0; i < deltaDist.length; i++) {
            cX = sShapeCenter.x - sShapeCoords[0][i];
            cY = sShapeCenter.y - sShapeCoords[1][i];
            deltaDist[i] = Point.of(cX, cY);
        }
        if (sShape.getShape() == MeasureShape.DIAMOND) {
            Point[] boundRectDeltas = new Point[sShape.getNPoints()];
            int h = deltaDist.length - 1;
            for (int c = 0; c < deltaDist.length; c++) {
                boundRectDeltas[c] = Point.of(deltaDist[c].x + deltaDist[h].x, deltaDist[c].y + deltaDist[h].y);
                h = (h + 1) % deltaDist.length;
            }
            deltaDist = boundRectDeltas;
        }

        Point[] shrunkenShapePoints = new Point[sShape.getNPoints()];
        for (int i = 0; i < shrunkenShapePoints.length; i++) {

            cX = this.corners[i].getX() + deltaDist[i].x;
            cY = this.corners[i].getY() + deltaDist[i].y;
            shrunkenShapePoints[i] = Point.of(cX, cY);
        }

        return new PolyShape(shrunkenShapePoints, 0, this.getShape());
    }

    /**
     * Returns the value of the attribute enabled.
     *
     * @return enabled: true or false depending if the polygon is active or not.
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Allows the user to alter the attribute enabled.
     *
     * @param enabled is the polygon visible and interactible.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the level of the polyShape.
     *
     * @return level of the polyShape.
     */
    public int getLevel() {
        return level;
    }

    public MeasureShape getShape() {
        return shape;
    }

    public void setShape(MeasureShape shape) {
        this.shape = shape;
    }

    public int getNPoints() {
        return corners.length;
    }

    public Point[] getCornersAsPoints() {
        Point[] pointCorners = new Point[corners.length];
        for (int c = 0; c < corners.length; c++) {
            pointCorners[c] = corners[c].getPoint();
        }
        return pointCorners;
    }
    public String getPolyStatus() {
        String centerCoord = String.format("(%f , %f)", getShapeCenter().x, getShapeCenter().y);
        String ret = String.format("Polygon released. Shape: %s, Center Coordinate: %s", shape, centerCoord);
        
        return ret;
    }

    @Override
    public String toString() {
        return "PolyShape [corners=" + corners[0].getX() + ", " + corners[0].getY()
                + ", enabled=" + enabled
                + ", level=" + level
                + ", shape=" + shape + "]";
    }
}

package com.scitequest.martin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.scitequest.martin.settings.MaskExt;
import com.scitequest.martin.settings.MaskSettings.MeasureShape;
import com.scitequest.martin.settings.Settings;
import com.scitequest.martin.view.AlphabeticNumberEditor;
import com.scitequest.martin.view.Drawable;

public final class SlideMask {
    private Settings settings;

    /**
     * Specifies the interactible elements of SlideMask.
     */
    public enum Element {
        /**
         * Default case, none selected.
         */
        NONE,
        /**
         * Whole slideMask.
         */
        SLIDE,
        /**
         * Any of the delRects.
         */
        DEL_RECT,
        /**
         * Any of the measureCircles.
         */
        MEASURE_CIRCLE
    }

    /** The absolute x coordinate of the slide system. */
    private int absoluteX;
    /** The absolute y coordinate of the slide system. */
    private int absoluteY;
    /** The absolute rotation the slide currently has. */
    private double absoluteRotation;

    /** The x and y location of the rotational center. */
    private Point rCenter;
    /** Coordinates of the slide itself. */
    private PolyShape slide;
    /** vertical lines dennoting the columns of the superGrid. */
    private ArrayList<PolyShape> verticalDivideLines;
    /** Horizontal lines dennoting the rows of the superGrid. */
    private ArrayList<PolyShape> horizontalDivideLines;
    /** List containing all SpotFields. */
    private ArrayList<PolyGrid> spotFields;
    /** List containing all MeasureFields. */
    private ArrayList<PolyGrid> measureFields;
    /**
     * PolyGrid containing the center positions for all spotField column labelling.
     */
    private ArrayList<PolyGrid> colLabelGrids;
    /** PolyGrid containing the center positions for all spotField row labelling. */
    private ArrayList<PolyGrid> rowLabelGrids;
    /**
     * Grid of rectangles that are used for noise calculation above the topmost
     * spotfields.
     */
    private ArrayList<PolyGrid> topNoiseRects;
    /**
     * Grid of rectangles that are used for noise calculation between the rows of
     * the supergrid.
     */
    private ArrayList<PolyGrid> inBetweenNoiseRects;
    /**
     * Grid of rectangles that are used for noise calculation below the bottommost
     * spotfields.
     */
    private ArrayList<PolyGrid> bottomNoiseRects;

    private ArrayList<PolyShape> grabbedShapes;

    /**
     * Number of corners in a rectangle.
     */
    public static final int RECTANGULAR = 4;
    /**
     * Number of coordinates needed to describe a straight line.
     */
    public static final int LINE = 2;
    /**
     * Number of coordinates of a single point.
     */
    public static final int POINT = 1;
    /**
     * The number of rows each BackgroundRect field is divided in.
     */
    public static final int DEL_RECT_ROWS = 1;
    /**
     * The number of columns each BackgroundRect field is divided in.
     */
    public static final int DEL_RECT_COLS = 4;
    /** Ratio between the rects and the space they inhabit, this allows for gaps. */
    public static final double RECT_RATIO = 0.8;
    /**
     * Standard x-position of the upper-left corner of each new slide.
     */
    public static final double ZERO_POSITION_X = 0;

    /**
     * Standard y-position of the upper-left corner of each new slide.
     */
    public static final double ZERO_POSITION_Y = 0;

    /**
     * Standard value for polyGrids without an inset.
     */
    public static final double NO_INSET = 0;

    /**
     * Minimum Height of noise rects, MARTin won't generate anything smaller.
     */
    public static final double MINIMUM_NOISE_RECT_HEIGHT = 5;

    private MaskExt maskParameters;

    /**
     * Constructor of SlideMask.
     * Creates a slideMask in zero position by a given set of settings.
     *
     * @param settings Settings the new slideMask will be based on.
     */
    SlideMask(Settings settings) {
        this.settings = settings;
        absoluteX = 0;
        absoluteY = 0;
        absoluteRotation = 0;
        grabbedShapes = new ArrayList<PolyShape>();
        this.maskParameters = settings.getMaskSettings().getLastUsedMask();

        this.rCenter = Point.of(maskParameters.getSlideWidth()
                / 2.0, maskParameters.getSlideHeight() / 2.0);

        setupCoordinates();
    }

    /**
     * Sets all parameters to the ones of a previously created slideMask and then
     * calculates the coordinates for all elements.
     */
    public void setupCoordinates() {
        int slideWidth = maskParameters.getSlideWidth();
        int slideHeight = maskParameters.getSlideHeight();
        int xInset = maskParameters.getxInset();
        int yInset = maskParameters.getyInset();
        int spotFieldWidth = maskParameters.getSpotFieldWidth();
        int spotFieldHeight = maskParameters.getSpotFieldHeight();
        int superGridCols = maskParameters.getSuperGridCols();
        int superGridRows = maskParameters.getSuperGridRows();
        List<Integer> horizontalSpacing = maskParameters.getHorizontalSpacing();
        List<Integer> verticalSpacing = maskParameters.getVerticalSpacing();
        MeasureShape shape = maskParameters.getShape();
        int measureFieldWidth = maskParameters.getMeasureFieldWidth();
        int measureFieldHeight = maskParameters.getMeasureFieldHeight();
        int spotFieldNColumns = maskParameters.getSpotFieldNColumns();
        int spotFieldNRows = maskParameters.getSpotFieldNRows();
        int spotfieldLastMeasurepointIndex = settings.getMaskSettings().getLastMeasurePointIndex();
        /*
         * Setup of the slide outline.
         */
        Point[] slidePoints = new Point[4];
        // Upper left
        slidePoints[0] = Point.of(ZERO_POSITION_X, ZERO_POSITION_Y);
        // Upper right
        slidePoints[1] = Point.of(slideWidth + ZERO_POSITION_X, ZERO_POSITION_Y);
        // Lower right
        slidePoints[2] = Point.of(slideWidth + ZERO_POSITION_X, slideHeight + ZERO_POSITION_Y);
        // Lower left
        slidePoints[3] = Point.of(ZERO_POSITION_X, slideHeight + ZERO_POSITION_Y);
        this.slide = new PolyShape(slidePoints, 1, MeasureShape.RECTANGLE);

        // Setup of vertical divide lines if there are any.
        this.verticalDivideLines = new ArrayList<PolyShape>();
        int spacingInset = 0;
        Point[] verticalDivideLinePoints = new Point[2];

        for (int i = 0; i < verticalSpacing.size(); i++) {
            double xPos = ZERO_POSITION_X + xInset + spacingInset + spotFieldWidth * (i + 1)
                    + verticalSpacing.get(i) / 2.0;
            // Top point
            verticalDivideLinePoints[0] = Point.of(xPos, ZERO_POSITION_Y);
            // Bottom point
            verticalDivideLinePoints[1] = Point.of(xPos, slideHeight + ZERO_POSITION_Y);

            verticalDivideLines.add(new PolyShape(verticalDivideLinePoints, 2, MeasureShape.LINE));
            spacingInset += verticalSpacing.get(i);
        }
        // Setup of horizontal divide lines if there are any.
        this.horizontalDivideLines = new ArrayList<PolyShape>();
        spacingInset = 0;
        Point[] horizontalDivideLinePoints = new Point[2];

        for (int i = 0; i < horizontalSpacing.size(); i++) {
            double yPos = ZERO_POSITION_Y + yInset + spotFieldHeight * (i + 1) + spacingInset
                    + horizontalSpacing.get(i) / 2.0;
            // Left point
            horizontalDivideLinePoints[0] = Point.of(ZERO_POSITION_X, yPos);
            // Right point
            horizontalDivideLinePoints[1] = Point.of(ZERO_POSITION_X + slideWidth, yPos);

            horizontalDivideLines.add(new PolyShape(horizontalDivideLinePoints, 2, MeasureShape.LINE));
            spacingInset += horizontalSpacing.get(i);
        }
        // Setup of spotFields.
        double spotRectWidth = spotFieldWidth / (double) spotFieldNColumns;
        double spotRectHeight = spotFieldHeight / (double) spotFieldNRows;

        double yPos = yInset;
        this.spotFields = new ArrayList<PolyGrid>();
        for (int superRow = 0; superRow < superGridRows; superRow++) {
            double xPos = xInset;
            for (int superCol = 0; superCol < superGridCols; superCol++) {
                PolyGrid grid = new PolyGrid(spotFieldNColumns, spotFieldNRows,
                        MeasureShape.RECTANGLE, 2, Point.of(xPos, yPos),
                        spotFieldWidth, spotFieldHeight,
                        PolyGrid.NO_INSET, PolyGrid.NO_INSET,
                        spotRectWidth, spotRectHeight);

                grid.setEnabledZone(spotfieldLastMeasurepointIndex);

                spotFields.add(grid);
                if (superCol < (superGridCols - 1)) {
                    xPos += spotFieldWidth + verticalSpacing.get(superCol);
                }
            }
            if (superRow < (superGridRows - 1)) {
                yPos += spotFieldHeight + horizontalSpacing.get(superRow);
            }
        }
        // Setup of measureFields.
        measureFields = new ArrayList<PolyGrid>();

        // Inset relative to spotField cells. This is used to center the measureFields
        // within their cells.
        double shapeInsetX = (spotRectWidth - measureFieldWidth) / 2;
        double shapeInsetY = (spotRectHeight - measureFieldHeight) / 2;
        PolyGrid measureGrid;

        for (int sField = 0; sField < spotFields.size(); sField++) {
            measureGrid = new PolyGrid(spotFieldNColumns, spotFieldNRows,
                    shape, 3, spotFields.get(sField).getPos(),
                    spotFields.get(sField).getWidth(), spotFields.get(sField).getHeight(),
                    shapeInsetX, shapeInsetY, measureFieldWidth, measureFieldHeight);

            measureGrid.setEnabledZone(spotfieldLastMeasurepointIndex);
            measureFields.add(measureGrid);
        }
        this.colLabelGrids = new ArrayList<PolyGrid>();
        this.rowLabelGrids = new ArrayList<PolyGrid>();
        for (int sField = 0; sField < spotFields.size(); sField++) {

            Point colGridPoint = Point.of(spotFields.get(sField).getPos().x,
                    spotFields.get(sField).getPos().y - spotRectHeight);

            PolyGrid colGrid = new PolyGrid(spotFieldNColumns, 1,
                    MeasureShape.POINT, 2, colGridPoint,
                    spotFields.get(sField).getWidth(), spotRectHeight,
                    spotRectWidth / 2, spotRectHeight / 2, 0, 0);

            colLabelGrids.add(colGrid);

            Point rowGridPoint = Point.of(spotFields.get(sField).getPos().x - spotRectWidth,
                    spotFields.get(sField).getPos().y);

            PolyGrid rowGrid = new PolyGrid(1, spotFieldNRows,
                    MeasureShape.POINT, 2, rowGridPoint,
                    spotRectWidth, spotFields.get(sField).getHeight(),
                    spotRectWidth / 2, spotRectHeight / 2, 0, 0);

            rowLabelGrids.add(rowGrid);
        }
        // Setup of delRects.
        double noiseRectWidth = spotFields.get(0).getWidth() / DEL_RECT_COLS;

        topNoiseRects = new ArrayList<PolyGrid>();
        // Inset as in the space between top of spotfield and bottom of rect.
        double topRectInset = yInset * ((1 - RECT_RATIO) / 2);
        double topRectHeight = yInset * RECT_RATIO;
        double topPosY = spotFields.get(0).getPos().y - topRectInset - topRectHeight;

        bottomNoiseRects = new ArrayList<PolyGrid>();
        // Inset as in the space between bottom of spotfield and top of rect.
        double sumHSpace = horizontalSpacing.stream().reduce(0, (a, b) -> a + b);

        double bottomInset = slideHeight - (yInset + spotFieldHeight * superGridRows + sumHSpace);
        double bottomRectInset = bottomInset * ((1 - RECT_RATIO) / 2);

        double bottomRectHeight = bottomInset * RECT_RATIO;
        double bottomPosY = spotFields.get(spotFields.size() - 1).getPos().y
                + spotFieldHeight + bottomRectInset;

        for (int sCol = 0; sCol < superGridCols; sCol++) {

            if (topRectHeight >= MINIMUM_NOISE_RECT_HEIGHT) {
                Point topPoint = Point.of(spotFields.get(sCol).getPos().x, topPosY);
                PolyGrid upperGrid = new PolyGrid(DEL_RECT_COLS, DEL_RECT_ROWS,
                        MeasureShape.RECTANGLE, 4, topPoint,
                        spotFieldWidth, topRectHeight,
                        NO_INSET, NO_INSET, noiseRectWidth, topRectHeight);
                topNoiseRects.add(upperGrid);
            }

            if (bottomRectHeight >= MINIMUM_NOISE_RECT_HEIGHT) {
                Point bottomPoint = Point.of(spotFields.get(sCol).getPos().x,
                        bottomPosY);
                PolyGrid lowerGrid = new PolyGrid(DEL_RECT_COLS, DEL_RECT_ROWS,
                        MeasureShape.RECTANGLE, 4, bottomPoint,
                        spotFieldWidth, topRectHeight,
                        NO_INSET, NO_INSET, noiseRectWidth, bottomRectHeight);
                bottomNoiseRects.add(lowerGrid);
            }
        }

        inBetweenNoiseRects = new ArrayList<PolyGrid>();

        for (int sRow = 0; sRow < superGridRows - 1; sRow++) {
            double rectHeight = horizontalSpacing.get(sRow) * RECT_RATIO;
            if (rectHeight >= MINIMUM_NOISE_RECT_HEIGHT) {
                double rectInset = horizontalSpacing.get(sRow) * ((1 - RECT_RATIO) / 2);
                double rectYPos = spotFields.get((sRow + 1) * superGridCols).getPos().y
                        - rectInset - rectHeight;

                for (int sCol = 0; sCol < superGridCols; sCol++) {
                    Point rectPoint = Point.of(spotFields.get(sCol).getPos().x, rectYPos);
                    PolyGrid inBetweenGrid = new PolyGrid(DEL_RECT_COLS, DEL_RECT_ROWS,
                            MeasureShape.RECTANGLE, 4, rectPoint,
                            spotFieldWidth, rectHeight,
                            NO_INSET, NO_INSET, noiseRectWidth, rectHeight);
                    inBetweenNoiseRects.add(inBetweenGrid);
                }
            }
        }

        calculateSlideMaskOrbits();

    }

    /**
     * This method leads to the orbit calculation of all coordinates of an instance
     * of SlideMask.
     */
    private void calculateSlideMaskOrbits() {

        slide.calculateOrbits(rCenter);

        for (PolyShape vDivideLine : verticalDivideLines) {
            vDivideLine.calculateOrbits(rCenter);
        }
        for (PolyShape hDivideLine : horizontalDivideLines) {
            hDivideLine.calculateOrbits(rCenter);
        }
        for (PolyGrid sField : spotFields) {
            sField.calculateGridOrbits(rCenter);
        }
        for (PolyGrid mField : measureFields) {
            mField.calculateGridOrbits(rCenter);
        }
        for (PolyGrid cLabels : colLabelGrids) {
            cLabels.calculateGridOrbits(rCenter);
        }
        for (PolyGrid rLabels : rowLabelGrids) {
            rLabels.calculateGridOrbits(rCenter);
        }
        for (PolyGrid tNoiseRects : topNoiseRects) {
            tNoiseRects.calculateGridOrbits(rCenter);
        }
        for (PolyGrid ibNoiseRects : inBetweenNoiseRects) {
            ibNoiseRects.calculateGridOrbits(rCenter);
        }
        for (PolyGrid bNoiseRects : bottomNoiseRects) {
            bNoiseRects.calculateGridOrbits(rCenter);
        }
    }

    /**
     * This method repositions a slide after resetting, effectively allowing the
     * user to fit slide-parameters in place.
     */
    public void repositionSlide() {
        // zero position for old rCenter
        rCenter = Point.of(rCenter.x - absoluteX, rCenter.y - absoluteY);

        setupCoordinates(); // new setup without changing the rCenter "stabilizes" the rotation

        // Moves the slide to previous position after resetting
        if (absoluteX != 0 || absoluteY != 0) {
            moveSlide(absoluteX, absoluteY, true);
        }
        if (absoluteRotation != 0) {
            rotateSlide(absoluteRotation, 0, 0, 0, 0, true);
        }
        int slideWidth = maskParameters.getSlideWidth();
        int slideHeight = maskParameters.getSlideHeight();
        rCenter = Point.of(slideWidth / 2 + absoluteX, slideHeight / 2 + absoluteY);
        calculateSlideMaskOrbits();

    }

    /**
     * This method checks if a mouse click is within or outside a rectangular
     * polygon. Here it is used to determine if a click was inside one of the
     * delRects (and which one), or if it was inside of the slide mask.
     *
     * All grabbed elements are temporarily stored in grabbedShapes.
     *
     * @param x the x-position of the mouse click.
     * @param y the y-position of the mouse click.
     *
     * @return returns an index of the type of clicked polygon. 0 = none, 1 = slide,
     *         2 = delRect.
     */
    public Element isOnRectPolygon(int x, int y) {
        PolyShape checkedPoly;

        if (settings.getMeasurementSettings().isSubtractBackground()) {
            for (int i = 0; i < DEL_RECT_COLS; i++) {
                for (PolyGrid tRects : topNoiseRects) {
                    checkedPoly = tRects.getGridElement(0, i).isClickInsidePoly(x, y);
                    if (checkedPoly != null) {
                        grabbedShapes.add(checkedPoly);
                    }
                }
                for (PolyGrid ibRects : inBetweenNoiseRects) {
                    checkedPoly = ibRects.getGridElement(0, i).isClickInsidePoly(x, y);
                    if (checkedPoly != null) {
                        grabbedShapes.add(checkedPoly);
                    }
                }
                for (PolyGrid bRects : bottomNoiseRects) {
                    checkedPoly = bRects.getGridElement(0, i).isClickInsidePoly(x, y);
                    if (checkedPoly != null) {
                        grabbedShapes.add(checkedPoly);
                    }
                }
            }
        }
        if (grabbedShapes.isEmpty()) {
            checkedPoly = slide.isClickInsidePoly(x, y);
            if (checkedPoly != null) {
                grabbedShapes.add(checkedPoly);
                return Element.SLIDE;
            } else {
                return Element.NONE;
            }
        } else {
            return Element.DEL_RECT;
        }
    }

    /**
     * This method checks if a click was on a measure circle and and stores all
     * clicked circles.
     *
     * @param x x-position of click.
     * @param y y-position of click.
     * @return true if a measureCircle was clicked, info is used in logfile.
     */
    public boolean isOnMeasureField(int x, int y) {
        boolean ret = false;

        for (PolyGrid mFieldGrid : measureFields) {
            PolyShape[][] mGrid = mFieldGrid.getGrid();
            for (int row = 0; row < mGrid.length; row++) {
                for (int col = 0; col < mGrid[0].length; col++) {
                    if (mGrid[row][col].isEnabled()) {
                        if (mGrid[row][col].isWithinBounds(x, y, mFieldGrid.getShapeWidth() / 2) != null) {
                            grabbedShapes.add(mGrid[row][col]);
                            ret = true;
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * If the mouse-click is released then clear the grabbedShapes list.
     */
    public void releaseGrip() {
        grabbedShapes.clear();
    }

    /**
     * This method checks which of the elements of grabbedShapes are to be moved and
     * calls for the corresponding method. If only the slide was grabbed moveSlide
     * is invoked.
     *
     * @param x          the mouse x coordinate
     * @param y          the mouse y coordinate
     * @param lastX      the previous mouse x coordinate
     * @param lastY      the previouse mouse y coordinate
     * @param distanceX  a distance multiplier in x direction
     * @param distanceY  a distance multiplier in y direction
     * @param reposition whether to reposition the slide if required
     * @return true if any element was moved
     */
    public boolean moveGrabbedElement(int x, int y, int lastX, int lastY,
            double distanceX, double distanceY, boolean reposition) {
        if (grabbedShapes.isEmpty()) {
            return false;
        }

        double deltaX = (x - lastX) * distanceX;
        double deltaY = (y - lastY) * distanceY;

        ArrayList<PolyShape> move = new ArrayList<PolyShape>();
        PolyShape poly;
        int maxLevel = -1;

        for (int i = 0; i < grabbedShapes.size(); i++) {
            poly = grabbedShapes.get(i);
            if (maxLevel < poly.getLevel()) {
                move.clear();
                maxLevel = poly.getLevel();
            }
            move.add(grabbedShapes.get(i));
        }
        if (maxLevel == 1) {
            moveSlide(deltaX, deltaY, reposition);
        } else if (maxLevel > 1) {
            for (int i = 0; i < move.size(); i++) {
                moveElement(move.get(i), deltaX, deltaY);
            }
        }
        return true;
    }

    /**
     * Moves all elements of the slide-mask linearily. Note that this does not
     * require a recalculation of the orbits, since the rotational center is moved
     * the same.
     *
     * @param deltaX     how many pixels to move the slide in x direction
     * @param deltaY     how many pixels to move the slide in y direction
     * @param reposition whether to also change the absolute coordinates of the
     *                   slide
     */
    public void moveSlide(double deltaX, double deltaY, boolean reposition) {

        if (!reposition) {
            absoluteX += deltaX;
            absoluteY += deltaY;
        }

        rCenter = Point.of(rCenter.x + deltaX, rCenter.y + deltaY);

        slide.movePoly(deltaX, deltaY);
        for (PolyShape vDivideLine : verticalDivideLines) {
            vDivideLine.movePoly(deltaX, deltaY);
        }
        for (PolyShape hDivideLine : horizontalDivideLines) {
            hDivideLine.movePoly(deltaX, deltaY);
        }
        for (PolyGrid sField : spotFields) {
            sField.moveGrid(deltaX, deltaY);
        }
        for (PolyGrid mField : measureFields) {
            mField.moveGrid(deltaX, deltaY);
        }
        for (PolyGrid cLabels : colLabelGrids) {
            cLabels.moveGrid(deltaX, deltaY);
        }
        for (PolyGrid rLabels : rowLabelGrids) {
            rLabels.moveGrid(deltaX, deltaY);
        }
        for (PolyGrid tNoiseRects : topNoiseRects) {
            tNoiseRects.moveGrid(deltaX, deltaY);
        }
        for (PolyGrid ibNoiseRects : inBetweenNoiseRects) {
            ibNoiseRects.moveGrid(deltaX, deltaY);
        }
        for (PolyGrid bNoiseRects : bottomNoiseRects) {
            bNoiseRects.moveGrid(deltaX, deltaY);
        }
    }

    /**
     * Moves a selected element by a given number of pixels in x and y direction.
     *
     * @param element Element to be moved.
     * @param deltaX  Horizontal pixels the element will be moved. Negative numbers
     *                will move to the left, positive to the right.
     * @param deltaY  Vertical pixels the element will be moved. Negative numbers
     *                will move up, positive down.
     */
    public void moveElement(PolyShape element, double deltaX, double deltaY) {
        element.movePoly(deltaX, deltaY);
        element.calculateOrbits(rCenter);
    }

    /**
     * boolean reset as in a complete reset of the whole slide {@code ->} not
     * triggered by resetRotation digRotation as in digitalRotation (in contrast to
     * analog).
     *
     * @param digRotation if set, the rotation to set the slide to
     * @param nX          the x coordinate of the mouse
     * @param nY          the y coordinate of the mouse
     * @param oX          the previous x coordinate of the mouse
     * @param oY          the previous y coordinate of the mouse
     * @param reposition  whether to reposition the slide
     */
    public void rotateSlide(double digRotation,
            int nX, int nY, int oX, int oY,
            boolean reposition) {
        int direction = -1;
        // standard value that results in a 1° spin
        double lever = 1;

        // "Strength" of rotation is determined by the distance between rotational
        // center and rotational lever. The rotational lever is formed out of the
        // coordinates of our mouse drag. This allows for faster rotations as well as
        // more accurate rotations (sub 1°). The main idea of this algorithm is derived
        // from circleArranger.
        if (digRotation != 0) {
            lever = digRotation; // digital rotation only allows positive integers
            direction = 1;
        } else {
            // rotation was initiated via mouse drag -> direction is relative to the
            // rotational center
            int deltaX = nX - oX; // right = +1, left = -1
            int deltaY = nY - oY; // up = -1, down = +1

            if (oX >= rCenter.x && oY <= rCenter.y) {
                // first quadrant
                direction = deltaX + deltaY >= 0 ? -1 : 1;
            } else if (oX >= rCenter.x && oY >= rCenter.y) {
                // fourth quadrant
                direction = deltaX * (-1) + deltaY >= 0 ? -1 : 1;
            } else if (oX <= rCenter.x && oY >= rCenter.y) {
                // third quadrant
                direction = deltaX + deltaY >= 0 ? 1 : -1;
            } else {
                // second quadrant
                direction = deltaX * (-1) + deltaY >= 0 ? 1 : -1;
            }
            // values are set relative to the rotational center -> conversion of coordinates
            // into distances
            nX = nX - (int) rCenter.x;
            nY = nY - (int) rCenter.y;

            oX = oX - (int) rCenter.x;
            oY = oY - (int) rCenter.y;

            lever = Math.atan2(nY, nX) - Math.atan2(oY, oX); // lever = change in angle
            lever = Math.toDegrees(Math.abs(lever));

        }
        if (!reposition) {
            absoluteRotation = (360 + absoluteRotation + direction * lever) % 360;
        }
        double dRotation = direction * lever;

        slide.setAngles(dRotation, rCenter);
        for (PolyShape vDivideLine : verticalDivideLines) {
            vDivideLine.setAngles(dRotation, rCenter);
        }
        for (PolyShape hDivideLine : horizontalDivideLines) {
            hDivideLine.setAngles(dRotation, rCenter);
        }
        for (PolyGrid sField : spotFields) {
            sField.rotateGridElements(dRotation, rCenter);
        }
        for (PolyGrid mField : measureFields) {
            mField.rotateGridElements(dRotation, rCenter);
        }
        for (PolyGrid cLabels : colLabelGrids) {
            cLabels.rotateGridElements(dRotation, rCenter);
        }
        for (PolyGrid rLabels : rowLabelGrids) {
            rLabels.rotateGridElements(dRotation, rCenter);
        }
        for (PolyGrid tNoiseRects : topNoiseRects) {
            tNoiseRects.rotateGridElements(dRotation, rCenter);
        }
        for (PolyGrid ibNoiseRects : inBetweenNoiseRects) {
            ibNoiseRects.rotateGridElements(dRotation, rCenter);
        }
        for (PolyGrid bNoiseRects : bottomNoiseRects) {
            bNoiseRects.rotateGridElements(dRotation, rCenter);
        }
    }

    public MaskExt getMaskParameters() {
        return maskParameters;
    }

    public void setMaskParameters(MaskExt maskParameters) {
        this.maskParameters = maskParameters;
    }

    ArrayList<Point[][]> getSpotFieldCentroids() {
        ArrayList<Point[][]> ret = new ArrayList<Point[][]>();
        for (PolyGrid sField : spotFields) {
            ret.add(sField.getCentroids());
        }
        return ret;
    }

    ArrayList<Point[][]> getMeasureFieldCentroids() {
        ArrayList<Point[][]> ret = new ArrayList<Point[][]>();
        for (PolyGrid mField : measureFields) {
            ret.add(mField.getCentroids());
        }
        return ret;
    }

    /**
     * This method returns the x and y values of the slide polygon as a
     * two-dimensional array.
     *
     * @return the x and y values of the slide polygon
     * @see PolyShape#getPolyCoordinates()
     */
    public double[][] getSlideCoordinates() {
        return slide.getPolyCoordinates();
    }

    /**
     * This method returns the x and y values of the vertical divide Lines as a
     * ArrayList containing a two-dimensional array.
     *
     * @return the x and y values of all vertical divide lines
     * @see PolyShape#getPolyCoordinates()
     */
    public ArrayList<double[][]> getVerticalDivideLineCoordinates() {
        ArrayList<double[][]> ret = new ArrayList<double[][]>();
        for (PolyShape vLine : verticalDivideLines) {
            ret.add(vLine.getPolyCoordinates());
        }

        return ret;
    }

    /**
     * This method returns the x and y values of the horizontal divide Lines as a
     * ArrayList containing a two-dimensional array.
     *
     * @return the x and y values of all horizontal divide lines
     * @see PolyShape#getPolyCoordinates()
     */
    public ArrayList<double[][]> getHorizontalDivideLineCoordinates() {
        ArrayList<double[][]> ret = new ArrayList<double[][]>();
        for (PolyShape hLine : horizontalDivideLines) {
            ret.add(hLine.getPolyCoordinates());
        }

        return ret;
    }

    /**
     * This method returns the coordinates of all spotFields as a three-dimensional
     * list of doubles.
     *
     * @return Double coordinates of all spotFields.
     */
    public ArrayList<double[][][]> getSpotFieldCoordinates() {
        ArrayList<double[][][]> ret = new ArrayList<double[][][]>();
        for (PolyGrid sField : spotFields) {
            ret.add(sField.getGridCoordinates());
        }
        return ret;
    }

    /**
     * This method returns the coordinates of all backgroundRectangles as a
     * two-dimensional list of doubles.
     *
     * @return Double coordinates of all backgroundRectangles.
     */
    public ArrayList<double[][]> getBackgroundRectCoordinates() {
        ArrayList<double[][]> ret = new ArrayList<double[][]>();

        ArrayList<PolyGrid> noiseRects = new ArrayList<PolyGrid>();
        noiseRects.addAll(topNoiseRects);
        noiseRects.addAll(inBetweenNoiseRects);
        noiseRects.addAll(bottomNoiseRects);

        for (PolyGrid nRect : noiseRects) {
            double[][][] gridCoordinates = nRect.getGridCoordinates();
            for (double[][] gCoordinates : gridCoordinates) {
                ret.add(gCoordinates);
            }
        }
        return ret;
    }

    /**
     * This returns the position of all column lables as a list of Points.
     * This is currently used for the annotated images.
     *
     * @return Point-coordinates of all column labels.
     */
    public ArrayList<Point[]> getColLabelPositions() {
        ArrayList<Point[]> ret = new ArrayList<Point[]>();
        for (PolyGrid cLabelGrid : colLabelGrids) {
            ret.add(cLabelGrid.getCentroids()[0]);
        }
        return ret;
    }

    /**
     * This returns the position of all row lables as a list of Points.
     * This is currently used for the annotated images.
     *
     * @return Point-coordinates of all row labels.
     */
    public ArrayList<Point[]> getRowLabelPositions() {
        ArrayList<Point[]> ret = new ArrayList<Point[]>();
        for (PolyGrid rLabelGrid : rowLabelGrids) {
            Point[][] centroids = rLabelGrid.getCentroids();
            Point[] centroidCol = new Point[centroids.length];
            for (int i = 0; i < centroids.length; i++) {
                centroidCol[i] = centroids[i][0];
            }
            ret.add(centroidCol);
        }
        return ret;
    }

    public double getAbsoluteRotation() {
        return absoluteRotation;
    }

    public int getAbsoluteX() {
        return absoluteX;
    }

    public int getAbsoluteY() {
        return absoluteY;
    }

    public Point getrCenter() {
        return rCenter;
    }

    public void setrCenter(Point rCenter) {
        this.rCenter = rCenter;
    }

    /**
     * Draws the slide outline.
     *
     * @param stilus   Interface that allows drawing operations to be used on an
     *                 imagePlus.
     * @param polygonX X-coordinates of the polygon that will be drawn.
     * @param polygonY X-coordinates of the polygon that will be drawn.
     */
    private void drawSlide(Drawable stilus, int[] polygonX, int[] polygonY) {
        stilus.setColor(settings.getDisplaySettings().getSlideColor());
        double[][] slideCoordinates = getSlideCoordinates();
        for (int i = 0; i < polygonX.length; i++) {
            polygonX[i] = (int) Math.round(slideCoordinates[0][i]);
            polygonY[i] = (int) Math.round(slideCoordinates[1][i]);
        }
        stilus.drawPolygon(polygonX.clone(), polygonY.clone());
    }

    /**
     * Draws the individual spotFields
     *
     * @param stilus   Interface that allows drawing operations to be used on an
     *                 imagePlus.
     * @param polygonX X-coordinates of the polygon that will be drawn.
     * @param polygonY X-coordinates of the polygon that will be drawn.
     * @param lastPos  Last spotField position that is visible and therefore should
     *                 be drawn.
     */
    private void drawSpotFields(Drawable stilus, int[] polygonX, int[] polygonY, int lastPos) {
        ArrayList<double[][][]> spotField = getSpotFieldCoordinates();
        stilus.setColor(settings.getDisplaySettings().getSpotfieldColor());
        for (double[][][] sField : spotField) {
            for (int j = 0; j <= lastPos; j++) {
                for (int i = 0; i < polygonX.length; i++) {
                    polygonX[i] = (int) Math.round(sField[j][0][i]);
                    polygonY[i] = (int) Math.round(sField[j][1][i]);
                }
                stilus.drawPolygon(polygonX.clone(), polygonY.clone());
            }
        }
    }

    /**
     * Draws correctly rotated spotField annotations, which are numbers or letters
     * for each row and column.
     *
     * @param stilus  Interface that allows drawing operations to be used on an
     *                imagePlus.
     * @param lastPos Last spotField position that is visible. This restricts which
     *                annotations shall be visible.
     */
    private void drawSpotFieldAnnotation(Drawable stilus, int lastPos) {
        ArrayList<Point[]> colLabelPos = getColLabelPositions();
        ArrayList<Point[]> rowLabelPos = getRowLabelPositions();

        // Y-coordinates in drawString mark the bottom of the text box, and
        // there seems to be no option to change that. So we just cheat a little.
        double yInset = maskParameters.getSpotFieldHeight()
                / 2.0
                / maskParameters.getSpotFieldNRows();
        stilus.setColor(Color.BLACK);

        for (Point[] cLabelPos : colLabelPos) {
            for (int col = 0; col < cLabelPos.length; col++) {
                stilus.drawString("" + (col + 1), (int) cLabelPos[col].x,
                        (int) cLabelPos[col].y + (int) yInset);
            }
        }
        for (Point[] rLabelPos : rowLabelPos) {
            for (int row = 0; row < rLabelPos.length; row++) {
                if (row <= lastPos / maskParameters.getSpotFieldNColumns()) {
                    stilus.drawString(AlphabeticNumberEditor.toAlphabeticNumber(row),
                            (int) rLabelPos[row].x,
                            (int) rLabelPos[row].y + (int) yInset);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Draws all divide lines of a slide mask, both horizontally and vertically - if
     * applicable.
     *
     * @param stilus Interface that allows drawing operations to be used on an
     *               imagePlus.
     */
    private void drawDivideLine(Drawable stilus) {
        stilus.setColor(settings.getDisplaySettings().getHalflineColor());
        int[] polyLineY = { 0, 0 };
        int[] polyLineX = { 0, 0 };

        ArrayList<double[][]> verticalDivideLineCoords = getVerticalDivideLineCoordinates();

        for (double[][] vDivCoords : verticalDivideLineCoords) {
            for (int i = 0; i < polyLineX.length; i++) {
                polyLineX[i] = (int) Math.round(vDivCoords[0][i]);
                polyLineY[i] = (int) Math.round(vDivCoords[1][i]);
            }
            stilus.drawPolygon(polyLineX.clone(), polyLineY.clone());
        }

        ArrayList<double[][]> horizontalDivideLineCoords = getHorizontalDivideLineCoordinates();

        for (double[][] hDivCoords : horizontalDivideLineCoords) {
            for (int i = 0; i < polyLineX.length; i++) {
                polyLineX[i] = (int) Math.round(hDivCoords[0][i]);
                polyLineY[i] = (int) Math.round(hDivCoords[1][i]);
            }
            stilus.drawPolygon(polyLineX.clone(), polyLineY.clone());
        }

    }

    /**
     * Draws all deletion rectangles if enabled.
     *
     * @param stilus   Interface that allows drawing operations to be used on an
     *                 imagePlus.
     * @param polygonX X-coordinates of the polygon that will be drawn.
     * @param polygonY X-coordinates of the polygon that will be drawn.
     */
    private void drawDelRects(Drawable stilus, int[] polygonX, int[] polygonY) {
        stilus.setColor(settings.getDisplaySettings().getDeletionRectangleColor());
        ArrayList<double[][]> delRects = getBackgroundRectCoordinates();

        for (double[][] delRect : delRects) {
            if (delRect != null) {
                for (int i = 0; i < polygonX.length; i++) {
                    polygonX[i] = (int) Math.round(delRect[0][i]);
                    polygonY[i] = (int) Math.round(delRect[1][i]);
                }
                stilus.drawPolygon(polygonX.clone(), polygonY.clone());
            }
        }
    }

    /**
     * Draws all measureFields.
     *
     * @param stilus  Interface that allows drawing operations to be used on an
     *                imagePlus.
     * @param lastPos Last spotField position that is visible and therefore should
     *                be drawn.
     */
    private void drawMeasureFields(Drawable stilus, int lastPos) {
        stilus.setColor(settings.getDisplaySettings().getMeasureCircleColor());
        for (PolyGrid measureField : measureFields) {
            int runIdx = 0;
            PolyShape[][] shapeGrid = measureField.getGrid();
            for (int row = 0; row < shapeGrid.length; row++) {
                for (PolyShape cell : shapeGrid[row]) {
                    if (runIdx > lastPos) {
                        row = shapeGrid.length;
                        break;
                    }
                    double[][] coordinates = cell.getPolyCoordinates();
                    if (measureField.getShape() == MeasureShape.CIRCLE) {
                        int x = (int) (cell.getShapeCenter().x - measureField.getShapeWidth() / 2);
                        int y = (int) (cell.getShapeCenter().y - measureField.getShapeHeight() / 2);
                        stilus.drawOval(x, y,
                                (int) (measureField.getShapeWidth()),
                                (int) (measureField.getShapeHeight()));
                    } else {
                        /*
                         * Have to cast here, not sure if we need double precision at all.
                         * If we reduce precision later we can remove this.
                         */
                        int[] xCoords = new int[coordinates[0].length];
                        int[] yCoords = new int[coordinates[1].length];
                        for (int coords = 0; coords < coordinates[0].length; coords++) {
                            xCoords[coords] = (int) coordinates[0][coords];
                            yCoords[coords] = (int) coordinates[1][coords];
                        }
                        stilus.drawPolygon(xCoords, yCoords);
                    }
                    runIdx++;
                }
            }
        }
    }

    /**
     * A slide mask is drawn onto an imageProcessor or an instance of Graphics.
     *
     * @param stilus the pen and surface to use for drawing
     * @param dO     the options what and how things should be drawn
     */
    public void drawElements(Drawable stilus, DrawOptions dO) {
        // We added these two lists to shorten the code of some of the draw methods,
        // practically this does not serve any other purpose and can be done within the
        // draw methods as well.
        int[] polygonX = { 0, 0, 0, 0 };
        int[] polygonY = { 0, 0, 0, 0 };

        int lastIdx = settings.getMaskSettings().getLastMeasurePointIndex();

        if (dO.isDrawSlide()) {
            drawSlide(stilus, polygonX, polygonY);
        }
        if (dO.isDrawSpotFields()) {
            drawSpotFields(stilus, polygonX, polygonY, lastIdx);
        }
        if (dO.isDrawSpotFieldAnnotation()) {
            drawSpotFieldAnnotation(stilus, lastIdx);
        }
        if (dO.isDrawDivideLine()) {
            drawDivideLine(stilus);
        }
        if (dO.isDrawDelRects()) {
            drawDelRects(stilus, polygonX, polygonY);
        }
        if (dO.isDrawMeasureCircles()) {
            drawMeasureFields(stilus, lastIdx);
        }
    }

    /**
     * Draws a set of ovals on a given imagePlus
     *
     * @param stilus    drawable imagePlus
     * @param positions centroids of ovals that are to be drawn
     * @param hWidth    width of ovals
     * @param hHeight   height of ovals
     */
    public void drawSearchFields(Drawable stilus, ArrayList<Point> positions,
            double hWidth, double hHeight) {
        stilus.setColor(new Color(0, 0, 0));
        for (Point pos : positions) {
            stilus.drawOval((int) (pos.x - hWidth), (int) (pos.y - hHeight), (int) hWidth * 2, (int) hHeight * 2);
        }
    }

    public ArrayList<PolyGrid> getSpotFields() {
        return spotFields;
    }

    public ArrayList<PolyGrid> getMeasureFields() {
        return measureFields;
    }

    public ArrayList<String> getGrabbedElementsStatus(){
        
        ArrayList<String> stringList = new ArrayList<String>();
        for (PolyShape shape : grabbedShapes) {
            stringList.add(shape.getPolyStatus());
        }
        return stringList;
    }
}

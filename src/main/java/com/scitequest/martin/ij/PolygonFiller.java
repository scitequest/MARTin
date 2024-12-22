package com.scitequest.martin.ij;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import com.scitequest.martin.export.Polygon;
import com.scitequest.martin.utils.DoubleStatistics;

/**
 * This class was adapted from the ImageJ repository on 2024-12-21.
 *
 * This class is used to provide the measuring algorithm for a polygon shape.
 *
 * The file was located at:
 * https://github.com/imagej/ImageJ/blob/master/ij/process/PolygonFiller.java
 */
public class PolygonFiller {
    int edges; // number of edges
    int activeEdges; // number of active edges

    // the polygon
    float[] xf, yf; // floating-point coordinates, may be given instead of integer 'x'
    double xOffset, yOffset; // offset that has to be added to xf, yf
    int n; // number of coordinates (polygon vertices)

    // edge table
    double[] ex; // x coordinates
    int[] ey1; // upper y coordinates
    int[] ey2; // lower y coordinates
    double[] eslope; // inverse slopes (1/m)
    int yMin, yMax; // lowest and highest of all ey1, ey2;

    // sorted edge table (indexes into edge table) (currently not used)
    int[] sedge;

    // active edge table (indexes into edge table)
    int[] aedge;

    /** Constructs a PolygonFiller. */
    public PolygonFiller() {
    }

    /**
     * Specifies the polygon to be filled in case of float coordinates.
     * In this case, multiple polygons separated by one set of NaN coordinates each.
     */
    public void setPolygon(float[] xf, float[] yf, int n, double xOffset, double yOffset) {
        this.xf = xf;
        this.yf = yf;
        this.n = n;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    void allocateArrays(int n) {
        if (ex == null || n > ex.length) {
            ex = new double[n];
            ey1 = new int[n];
            ey2 = new int[n];
            sedge = new int[n];
            aedge = new int[n];
            eslope = new double[n];
        }
    }

    /**
     * Generates the edge table for all non-horizontal lines:
     * ey1, ey2: min & max y value
     * eslope: inverse slope dx/dy
     * ex: x value at ey1, corrected for half-pixel shift between outline&pixel
     * coordinates
     * sedge: list of sorted edges is prepared (not sorted yet)
     */
    void buildEdgeTable() {
        yMin = Integer.MAX_VALUE;
        yMax = Integer.MIN_VALUE;
        edges = 0;
        int polyStart = 0; // index where the polygon has started (i.e., 0 unless we have multiple ploygons
                           // separated by NaN)
        for (int i = 0; i < n; i++) {
            int iplus1 = i == n - 1 ? polyStart : i + 1;

            if (Float.isNaN(xf[iplus1])) // after the last point, close the polygon
                iplus1 = polyStart;
            if (Float.isNaN(xf[i])) { // when a new polygon follows, remember the start point for closing it
                polyStart = i + 1;
                continue;
            }
            double y1f = yf[i] + yOffset;
            double y2f = yf[iplus1] + yOffset;
            double x1f = xf[i] + xOffset;
            double x2f = xf[iplus1] + xOffset;
            int y1 = (int) Math.round(y1f);
            int y2 = (int) Math.round(y2f);
            // IJ.log("x, y="+xf[i]+","+yf[i]+"+
            // offs="+xOffset+","+yOffset+"->"+x1f+","+y1f+" int="+y1);
            if (y1 == y2 || (y1 <= 0 && y2 <= 0))
                continue; // ignore horizontal lines or lines that don't reach the first row of pixels
            if (y1 > y2) { // swap ends to ensure y1<y2
                int tmp = y1;
                y1 = y2;
                y2 = tmp;
                double ftmp = y1f;
                y1f = y2f;
                y2f = ftmp;
                ftmp = x1f;
                x1f = x2f;
                x2f = ftmp;
            }
            double slope = (x2f - x1f) / (y2f - y1f);
            ex[edges] = x1f + (y1 - y1f + 0.5) * slope + 1e-8; // x at the y1 pixel coordinate
            ey1[edges] = y1;
            ey2[edges] = y2;
            eslope[edges] = slope;
            if (y1 < yMin)
                yMin = y1;
            if (y2 > yMax)
                yMax = y2;

            edges++;
        }
        for (int i = 0; i < edges; i++)
            sedge[i] = i;
        activeEdges = 0;
    }

    /**
     * Measures the area enclosed by the polygon.
     *
     * @param img     the image to measure
     * @param poly    the polygon overlay
     * @param cornerX the upper left x coordinate of the polygon boundary
     * @param cornerY the upper left y coordinate of the polygon boundary
     * @param width   the polygon boundary width
     * @param height  the polygon boundary height
     * @return the measured statistics
     */
    public DoubleStatistics measure(BufferedImage img, Polygon poly,
            int cornerX, int cornerY, int width, int height) {
        DoubleStatistics stats = new DoubleStatistics();

        final Raster raster = img.getRaster();
        // Create a buffer for getting pixel values
        final int[] pixel = new int[1];

        allocateArrays(n);
        buildEdgeTable();
        // printEdges();
        int x1, x2;
        int yStart = yMin > 0 ? yMin : 0;
        if (yMin != 0)
            shiftXValuesAndActivate(yStart);
        // IJ.log("yMin="+yMin+" yStart="+yStart+" nActive="+activeEdges);
        for (int y = yStart; y < Math.min(height, yMax + 1); y++) {
            removeInactiveEdges(y);
            activateEdges(y);
            for (int i = 0; i < activeEdges; i += 2) {
                x1 = (int) (ex[aedge[i]] + 0.5);
                if (x1 < 0)
                    x1 = 0;
                if (x1 > width)
                    x1 = width;
                x2 = (int) (ex[aedge[i + 1]] + 0.5);
                if (x2 < 0)
                    x2 = 0;
                if (x2 > width)
                    x2 = width;
                for (int x = x1; x < x2; x++) {
                    raster.getPixel(x + cornerX, y + cornerY, pixel);
                    stats.accept(pixel[0]);
                }
            }
            updateXCoordinates();
        }

        return stats;
    }

    /**
     * Shifts the x coordinates of all edges according to their slopes
     * as required for starting at the given y value and prepares the
     * list of active edges as it would have resulted from procesing
     * the previous lines
     */
    void shiftXValuesAndActivate(int yStart) {
        for (int i = 0; i < edges; i++) {
            int index = sedge[i];
            if (ey1[index] < yStart && ey2[index] >= yStart) {
                ex[index] += eslope[index] * (yStart - ey1[index]);
                aedge[activeEdges++] = index;
            }
        }
        sortActiveEdges();
    }

    /**
     * Updates the x coordinates in the active edges list and sorts the list if
     * necessary.
     */
    void updateXCoordinates() {
        int index;
        double x1 = -Double.MAX_VALUE, x2;
        boolean sorted = true;
        for (int i = 0; i < activeEdges; i++) {
            index = aedge[i];
            x2 = ex[index] + eslope[index];
            ex[index] = x2;
            if (x2 < x1)
                sorted = false;
            x1 = x2;
        }
        if (!sorted)
            sortActiveEdges();
    }

    /** Sorts the active edges list by x coordinate using a selection sort. */
    void sortActiveEdges() {
        int min, tmp;
        for (int i = 0; i < activeEdges; i++) {
            min = i;
            for (int j = i; j < activeEdges; j++)
                if (ex[aedge[j]] < ex[aedge[min]])
                    min = j;
            tmp = aedge[min];
            aedge[min] = aedge[i];
            aedge[i] = tmp;
        }
    }

    /** Removes edges from the active edge table that are no longer needed. */
    void removeInactiveEdges(int y) {
        int i = 0;
        while (i < activeEdges) {
            int index = aedge[i];
            if (y < ey1[index] || y >= ey2[index]) {
                for (int j = i; j < activeEdges - 1; j++)
                    aedge[j] = aedge[j + 1];
                activeEdges--;
            } else
                i++;
        }
    }

    /** Adds edges to the active edge table. */
    void activateEdges(int y) {
        for (int i = 0; i < edges; i++) {
            int edge = sedge[i];
            if (y == ey1[edge]) {
                int index = 0;
                while (index < activeEdges && ex[edge] > ex[aedge[index]])
                    index++;
                for (int j = activeEdges - 1; j >= index; j--)
                    aedge[j + 1] = aedge[j];
                aedge[index] = edge;
                activeEdges++;
            }
        }
    }
}

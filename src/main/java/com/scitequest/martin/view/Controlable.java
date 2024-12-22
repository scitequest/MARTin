package com.scitequest.martin.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import com.scitequest.martin.DrawOptions;
import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.DataStatistics;
import com.scitequest.martin.export.JsonParseException;
import com.scitequest.martin.export.Metadata;
import com.scitequest.martin.export.Parameters;
import com.scitequest.martin.settings.MaskExt;
import com.scitequest.martin.settings.ProjectExt;

/**
 * Specifies which operations a controller must support.
 *
 * This is to decouple the view from a specific controler.
 */
public interface Controlable {

    /** Specifies how the plugin is run. */
    enum RunType {
        /** When run as a plugin as per the menu from ImageJ. */
        PLUGIN,
        /** When run headless. */
        HEADLESS,
        /** When run as standalone application. */
        STANDALONE,
    }

    /**
     * Get how the plugin is currently run.
     *
     * @return the type which the plugin is run
     */
    RunType getRunType();

    /** An actor requested to perform the measurement. */
    void measure();

    /**
     * User requested to validate the specified measurement folder.
     *
     * @param folder the folder which contains all data to be verified
     * @return which files have passed the check as results object
     */
    IntegrityCheckResult checkIntegrity(Path folder);

    /** An actor requested to perform the circle fit algorithm. */
    void measureFieldFit();

    /**
     * Move slide to the specified, absolute coordinates.
     *
     * @param x the x coordinate to move to
     * @param y the y coordinate to move to
     */
    void moveSlide(int x, int y);

    /**
     * An actor requested to rotate the slide.
     *
     * @param angle degrees the slide is to be rotated from its current position.
     */
    void rotateSlide(double angle);

    /**
     * Rotate the slide via mouse input.
     *
     * @param x     current mouse x coordinate
     * @param y     current mouse y coordinate
     * @param lastX previous mouse x coordinate
     * @param lastY previous mouse y coordinate
     */
    void rotateSlideViaMouse(int x, int y, int lastX, int lastY);

    /**
     * Returns the current absolute slide rotation.
     *
     * @return the absolute slide rotation
     */
    double getSlideRotation();

    /**
     * Returns the current absolute x coordinate of the slide.
     *
     * @return the slide x coordinate
     */
    int getSlidexPosition();

    /**
     * Returns the current absolute y coordinate of the slide.
     *
     * @return the slide y coordinate
     */
    int getSlideyPosition();

    /**
     * Updates the currently clicked measure circles in the slide.
     *
     * Note: coordinates are offscreen coordinates.
     *
     * @param x the mouse x coordinate
     * @param y the mouse y coordinate
     */
    void updateClickedMeasureCircles(int x, int y);

    /**
     * Updates the currently clicked rectangular polygons.
     *
     * Note coordinates are offscreen coordinates.
     *
     * @param x the mouse x coordinate
     * @param y the mouse y coordinate
     */
    void updateClickedRectPolygons(int x, int y);

    /**
     * Move the currently grabbed element.
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
    boolean moveGrabbedElement(int x, int y, int lastX, int lastY,
            int distanceX, int distanceY, boolean reposition);

    /** Signal that the mouse click has been released. */
    void releaseMouseGrip();

    /**
     * Load projects from a file.
     *
     * @param path the path to the project file to be imported
     * @throws IOException        if an error happened during file I/O
     * @throws JsonParseException if the file could not be parsed or contained
     *                            invalid data
     * @return the imported project
     */
    ProjectExt importProject(Path path) throws IOException, JsonParseException;

    /**
     * Save projects to a file.
     *
     * @param proj the project which should be exported
     * @param path the path to the project file to be exported
     */
    void exportProject(ProjectExt proj, Path path);

    /**
     * Load a slide mask file.
     *
     * @param path the path to the mask file to be imported
     * @throws IOException        if an error happened during file I/O
     * @throws JsonParseException if the file could not be parsed or contained
     *                            invalid data
     * @return the imported mask
     */
    MaskExt importMask(Path path) throws IOException, JsonParseException;

    /**
     * Save a slide mask file.
     *
     * @param mask the mask which should be exported
     * @param path the path to the project file to be exported
     */
    void exportMask(MaskExt mask, Path path);

    /**
     * Draws all slide elements and text parts using the draw options.
     *
     * @param stilus      the pen to use
     * @param drawOptions what should be drawn
     */
    void drawElements(Drawable stilus, DrawOptions drawOptions);

    /** Reinitialize a slide after slide parameters were changed. */
    void repositionSlide();

    /** Called when the user requests to open a new image. */
    void openImage();

    /** Called when the displayed image was closed. */
    void imageClosed();

    void toggleFilter();

    /**
     * True if an image is currently loaded and displayed.
     *
     * @return whether an image is currently loaded
     */
    boolean isImageLoaded();

    /**
     * Test if the adaptive filtering is active.
     *
     * @return true, if the filter is enabled
     */
    boolean isFilterEnabled();

    BufferedImage generateGridImage();

    void optionsGuiClosed();

    /**
     * Handles opening and closing of exportGui.
     *
     * @param exportGuiOpened if true exportGui is opened if false it is closed.
     */
    void exportGuiOpen(boolean exportGuiOpened);

    /** Update the views. */
    void update();

    /** Called when the plugin is disposing. */
    void exit();

    void export(Metadata exportMetadata, Parameters parameters,
            Data data, DataStatistics dataStatistics);

    void setActiveMaskSettings(MaskExt activeMaskSettings);
}

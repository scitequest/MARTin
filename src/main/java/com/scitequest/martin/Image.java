package com.scitequest.martin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import com.scitequest.martin.export.Geometry;
import com.scitequest.martin.utils.DoubleStatistics;

public interface Image {

    /**
     * Gets the underlying image implementation.
     *
     * @return the raw image representation
     */
    BufferedImage getWrapped();

    /**
     * Get the file path from which this image was read.
     *
     * @return a path to the file
     */
    Path getFilePath();

    /**
     * Return the maximum value a pixel can be.
     *
     * For instance, for a grayscale byte image, the maximum value would be 255.
     *
     * @return the maximum pixel value the image can represent
     */
    int getMaxDataValue();

    /**
     * Clones this image.
     *
     * As a result, changes made to the image below no longer affect the original
     * image.
     *
     * @return a cloned image
     */
    Image duplicate();

    /**
     * Inverts the pixel values of this image.
     *
     * No thresholding is performed.
     */
    void invert();

    /**
     * Subtracts the value from all pixels.
     *
     * The new value is clamped to 0.
     *
     * @param value the value to subtract from the image
     */
    void subtract(int value);

    /**
     * Applies a local contrast filter to the image.
     *
     * @param blockRadiusX       block size in pixels in x direction
     * @param blockRadiusY       block size in pixels in x direction
     * @param standardDeviations factor for how many standard deviations the
     *                           contrast should be stretched
     */
    void normalizeLocalContrast(int blockRadiusX, int blockRadiusY, double standardDeviations);

    /**
     * Measures the image with the within the specified geometry.
     *
     * @param geometry the shape in which to measure
     * @return the measurement statistics
     * @throws MeasurementException if any error occured during measurement such as
     *                              if the shape is outside of the image's bounds
     */
    DoubleStatistics measure(Geometry geometry) throws MeasurementException;

    /**
     * Save this image as file
     *
     * @param filePath the path to the new image file
     * @throws IOException error should the file not be saved
     */
    void save(Path filePath) throws IOException;
}

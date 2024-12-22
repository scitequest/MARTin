package com.scitequest.martin;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.scitequest.martin.export.Circle;
import com.scitequest.martin.export.Geometry;
import com.scitequest.martin.export.Polygon;
import com.scitequest.martin.ij.PolygonFiller;
import com.scitequest.martin.utils.DoubleStatistics;
import com.scitequest.martin.utils.LocalContrastNormalizer;

public final class ImageImpl implements Image {

    private static final int U16_MAX = 65535;
    private static final int U8_MAX = 255;

    private final BufferedImage image;
    private final Path filePath;

    private ImageImpl(final BufferedImage image, final Path filePath) {
        this.image = image;
        this.filePath = filePath;
    }

    public static final Image read(final Path filePath) throws IOException {
        final BufferedImage img = ImageIO.read(filePath.toFile());
        if (img == null) {
            throw new IOException("The image file cannot be read");
        }
        if (!isSupportedImage(img)) {
            String msg = String.format("Image type %d is not supported", img.getType());
            throw new IOException(msg);
        }
        return new ImageImpl(img, filePath);
    }

    public static boolean isSupportedImage(BufferedImage img) {
        int type = img.getType();
        if (type == BufferedImage.TYPE_BYTE_GRAY || type == BufferedImage.TYPE_USHORT_GRAY) {
            return true;
        }
        return false;
    }

    @Override
    public BufferedImage getWrapped() {
        return image;
    }

    @Override
    public Path getFilePath() {
        return filePath;
    }

    @Override
    public int getMaxDataValue() {
        int type = image.getType();
        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            return U8_MAX;
        }
        if (type == BufferedImage.TYPE_USHORT_GRAY) {
            return U16_MAX;
        }
        throw new IllegalStateException("Max data value not defined for this image type");
    }

    @Override
    public Image duplicate() {
        ColorModel cm = image.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        BufferedImage copy = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
        return new ImageImpl(copy, filePath);
    }

    @Override
    public void invert() {
        final WritableRaster raster = image.getRaster();

        if (image.getType() == BufferedImage.TYPE_USHORT_GRAY) {
            final short[] data = ((DataBufferUShort) raster.getDataBuffer()).getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (short) (U16_MAX - (data[i] & 0xFFFF));
            }
            raster.setDataElements(0, 0, image.getWidth(), image.getHeight(), data);
            return;
        } else if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            final byte[] data = ((DataBufferByte) raster.getDataBuffer()).getData();
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (U8_MAX - (data[i] & 0xFF));
            }
            raster.setDataElements(0, 0, image.getWidth(), image.getHeight(), data);
            return;
        }

        throw new UnsupportedOperationException(
                String.format("Invert not implemented for image type '%s'", image.getType()));
    }

    @Override
    public void subtract(int value) {
        DataBuffer buf = image.getRaster().getDataBuffer();
        for (int i = 0; i < buf.getSize(); i++) {
            int val = buf.getElem(i) - value;
            buf.setElem(i, Math.max(0, val));
        }
    }

    @Override
    public void normalizeLocalContrast(
            int blockRadiusX, int blockRadiusY, double standardDeviations) {
        LocalContrastNormalizer.filter(this, blockRadiusX, blockRadiusY, standardDeviations);
    }

    private boolean verifyRectangleWithinImage(
            final int cornerX, final int cornerY, final int width, final int height) {
        final Raster raster = image.getRaster();

        if (cornerX < 0 || cornerX + width >= raster.getWidth() ||
                cornerY < 0 || cornerY + height >= raster.getHeight()) {
            return true;
        }

        return false;
    }

    /**
     * Method to measure within a circle.
     *
     * Algorithm was taken of ImageJ.
     *
     * @param circle the circle to measure
     * @return the measured statistics
     * @throws MeasurementException
     */
    private DoubleStatistics measureCircle(final Circle circle) throws MeasurementException {
        int cornerX = (int) (circle.position.x - circle.diameter / 2.0);
        int cornerY = (int) (circle.position.y - circle.diameter / 2.0);
        int diameter = (int) Math.ceil(circle.diameter);
        final DoubleStatistics stats = new DoubleStatistics();

        if (verifyRectangleWithinImage(cornerX, cornerY, diameter, diameter)) {
            throw new MeasurementException("Circle to measure is out of bounds");
        }

        final Raster raster = image.getRaster();
        // Create a buffer for getting pixel values
        final int[] pixel = new int[1];

        // Analyze pixels within circle
        double radius = diameter / 2.0;
        double radius_sqrt = radius * radius;
        radius -= 0.5;
        double xx, yy;

        for (int y = 0; y <= diameter; y++) {
            for (int x = 0; x <= diameter; x++) {
                xx = x - radius;
                yy = y - radius;
                if ((xx * xx / radius_sqrt + yy * yy / radius_sqrt) <= 1.0) {
                    raster.getPixel(x + cornerX, y + cornerY, pixel);
                    stats.accept(pixel[0]);
                }
            }
        }

        return stats;
    }

    private DoubleStatistics measurePolygon(final Polygon poly) throws MeasurementException {
        Rectangle2D.Double bounds = poly.calculateBounds();

        int cornerX = (int) Math.floor(bounds.x);
        int cornerY = (int) Math.floor(bounds.y);
        int width = (int) Math.ceil(bounds.x + bounds.width) - cornerX;
        int height = (int) Math.ceil(bounds.y + bounds.height) - cornerY;
        double xOffset = bounds.x - cornerX;
        double yOffset = bounds.y - cornerY;

        if (verifyRectangleWithinImage(cornerX, cornerY, width, height)) {
            throw new MeasurementException("Polygon to measure is out of bounds");
        }

        int n = poly.coordinates.size();
        // Initialize IJ poly arrays
        float[] xpf = new float[n];
        float[] ypf = new float[n];
        for (int i = 0; i < n; i++) {
            Point p = poly.coordinates.get(i);
            xpf[i] = (float) (p.x) - (float) bounds.x;
            ypf[i] = (float) (p.y) - (float) bounds.y;
        }

        PolygonFiller pf = new PolygonFiller();
        pf.setPolygon(xpf, ypf, n, xOffset, yOffset);

        return pf.measure(image, poly, cornerX, cornerY, width, height);
    }

    @Override
    public DoubleStatistics measure(final Geometry spot) throws MeasurementException {
        DoubleStatistics stats;
        if (spot instanceof Circle) {
            stats = measureCircle((Circle) spot);
        } else {
            stats = measurePolygon((Polygon) spot);
        }

        if (stats.getCount() == 0) {
            throw new MeasurementException("The measured geometry did not measure at least one pixel");
        }

        return stats;
    }

    @Override
    public void save(final Path filePath) throws IOException {
        ImageIO.write(image, "tiff", filePath.toFile());
    }
}

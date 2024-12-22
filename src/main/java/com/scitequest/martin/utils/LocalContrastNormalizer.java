package com.scitequest.martin.utils;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import com.scitequest.martin.Image;

public final class LocalContrastNormalizer {
    private static class IntegralImage {
        private final double[][] sum;
        private final double[][] sumSquares;
        private final int width;
        private final int height;

        public IntegralImage(WritableRaster raster) {
            this.width = raster.getWidth();
            this.height = raster.getHeight();
            this.sum = new double[height + 1][width + 1];
            this.sumSquares = new double[height + 1][width + 1];

            // Calculate integral images
            for (int y = 0; y < height; y++) {
                double rowSum = 0;
                double rowSumSq = 0;
                for (int x = 0; x < width; x++) {
                    double value = raster.getSampleDouble(x, y, 0);
                    rowSum += value;
                    rowSumSq += value * value;

                    sum[y + 1][x + 1] = rowSum + sum[y][x + 1];
                    sumSquares[y + 1][x + 1] = rowSumSq + sumSquares[y][x + 1];
                }
            }
        }

        public double getSum(int x1, int y1, int x2, int y2) {
            return sum[y2 + 1][x2 + 1] - sum[y2 + 1][x1] - sum[y1][x2 + 1] + sum[y1][x1];
        }

        public double getSumSquares(int x1, int y1, int x2, int y2) {
            return sumSquares[y2 + 1][x2 + 1] - sumSquares[y2 + 1][x1] -
                    sumSquares[y1][x2 + 1] + sumSquares[y1][x1];
        }
    }

    private LocalContrastNormalizer() {
    }

    public static void filter(
            Image image,
            int blockRadiusX, int blockRadiusY, double standardDeviations) {
        if (blockRadiusX < 0 || blockRadiusY < 0) {
            throw new IllegalArgumentException("Block radius must be non-negative");
        }
        if (standardDeviations <= 0) {
            throw new IllegalArgumentException("Standard deviations must be positive");
        }

        BufferedImage img = image.getWrapped();
        final WritableRaster raster = img.getRaster();
        final int width = img.getWidth();
        final int height = img.getHeight();
        double dataMax = image.getMaxDataValue();

        // Create integral images
        IntegralImage integral = new IntegralImage(raster);

        // Process each pixel
        for (int y = 0; y < height; y++) {
            int yMin = Math.max(0, y - blockRadiusY);
            int yMax = Math.min(height - 1, y + blockRadiusY);

            for (int x = 0; x < width; x++) {
                int xMin = Math.max(0, x - blockRadiusX);
                int xMax = Math.min(width - 1, x + blockRadiusX);

                // Get block statistics using integral image
                long blockSize = (long) (xMax - xMin + 1) * (yMax - yMin + 1);
                double blockSum = integral.getSum(xMin, yMin, xMax, yMax);
                double blockSumSq = integral.getSumSquares(xMin, yMin, xMax, yMax);

                double mean = blockSum / blockSize;
                double variance = blockSumSq / (blockSize - 1)
                        - (blockSum * blockSum) / (blockSize * blockSize - blockSize);
                double stdDev = variance < 0 ? 0.0 : Math.sqrt(variance);

                // Get current pixel value
                double value = raster.getSampleDouble(x, y, 0);

                // Kernel (modified to also stretch image min/max)
                double effectiveStdDev = standardDeviations * stdDev;
                value = (dataMax * (value - mean + effectiveStdDev))
                        / (2 * effectiveStdDev);

                // Clamp values to valid range and update the image
                double clamp = Math.min(dataMax, Math.max(0, value));
                raster.setSample(x, y, 0, clamp);
            }
        }
    }
}

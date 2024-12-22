package com.scitequest.martin;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.Datapoint;
import com.scitequest.martin.export.JsonParseException;
import com.scitequest.martin.export.Measurepoint;
import com.scitequest.martin.settings.Settings;
import com.scitequest.martin.values.Kcna2FmB6Expected;
import com.scitequest.martin.values.Kcna2Fp31sExpected;
import com.scitequest.martin.values.SyniSm67;

public final class IT {

    /**
     * Enables the output of measurement values.
     *
     * Set this to {@code true} if you want information about measurements or tests.
     */
    private static final boolean debug = false;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path settingsPath;

    @Before
    public void setUp() throws IOException {
        settingsPath = folder.newFile().toPath();
        Settings.defaultSettings().save(settingsPath);
    }

    @After
    public void tearDown() {
        settingsPath = null;
    }

    @Test
    public void kcna2FmB6() throws IOException, SecurityException, JsonParseException, MeasurementException {
        String imagePath = "src/test/resources/img/BS6 - 60sec - B - 1.tif";
        Control control = Control.headless(ImageImpl.read(Path.of(imagePath)), settingsPath);

        control.setActiveMask(Paths.get("src/test/resources/kcna2_fm_b6_mask.json"));
        control.repositionSlide();
        control.moveSlide(21, 43);
        control.rotateSlide(0.842);
        control.measureFieldFit();

        Data data = control.doMeasure();
        validateImage(data,
                Kcna2FmB6Expected.lMins, Kcna2FmB6Expected.lMaxs,
                Kcna2FmB6Expected.lMeans, Kcna2FmB6Expected.lStdDevs,
                Kcna2FmB6Expected.rMins, Kcna2FmB6Expected.rMaxs,
                Kcna2FmB6Expected.rMeans, Kcna2FmB6Expected.rStdDevs,
                800, 1400, 1200,
                150., 50., 75., 50.);
    }

    @Test
    public void kcna2Fp31s() throws IOException, SecurityException, JsonParseException, MeasurementException {
        String imagePath = "src/test/resources/img/KCNA2 - fp - 31s - 30sec - B - 4.tif";
        Control control = Control.headless(ImageImpl.read(Path.of(imagePath)), settingsPath);

        control.setActiveMask(Paths.get("src/test/resources/kcna2_fp_31s_mask.json"));
        control.repositionSlide();
        control.moveSlide(80, 24);
        control.rotateSlide(0.129);
        control.measureFieldFit();

        Data data = control.doMeasure();
        validateImage(data,
                Kcna2Fp31sExpected.lMins, Kcna2Fp31sExpected.lMaxs,
                Kcna2Fp31sExpected.lMeans, Kcna2Fp31sExpected.lStdDevs,
                Kcna2Fp31sExpected.rMins, Kcna2Fp31sExpected.rMaxs,
                Kcna2Fp31sExpected.rMeans, Kcna2Fp31sExpected.rStdDevs,
                6000., 18000., 15000.,
                2005., 650., 600., 550.);
    }

    @Test
    public void syniSm67() throws IOException, SecurityException, JsonParseException, MeasurementException {
        String imagePath = "src/test/resources/img/SYNI - SM67 - 60sec - A - 2.tif";
        Control control = Control.headless(ImageImpl.read(Path.of(imagePath)), settingsPath);

        control.setActiveMask(Paths.get("src/test/resources/syni_sm67_mask.json"));
        control.repositionSlide();
        control.moveSlide(32, 54);
        control.rotateSlide(0.78);
        control.measureFieldFit();

        Data data = control.doMeasure();
        validateImage(data,
                SyniSm67.lMins, SyniSm67.lMaxs,
                SyniSm67.lMeans, SyniSm67.lStdDevs,
                SyniSm67.rMins, SyniSm67.rMaxs,
                SyniSm67.rMeans, SyniSm67.rStdDevs,
                2000, 4200, 2400,
                360., 100., 200., 150.);
    }

    public void validateImage(Data actual,
            double[] lMins, double[] lMaxs, double[] lMeans, double[] lStdDevs,
            double[] rMins, double[] rMaxs, double[] rMeans, double[] rStdDevs,
            double epsilon, double epsilonMinValue, double epsilonMaxValue,
            double maxMeanDeltaMin, double maxMeanDeltaMax,
            double maxMeanDeltaMean, double maxMeanDeltaStdDev)
            throws FileNotFoundException {
        List<Datapoint> lMeasures = actual.getValues().stream()
                .filter(datapoint -> datapoint.getMeasurePoint().getSpot() == 0)
                .collect(Collectors.toList());

        List<Datapoint> rMeasures = actual.getValues().stream()
                .filter(datapoint -> datapoint.getMeasurePoint().getSpot() == 1)
                .collect(Collectors.toList());

        List<Double> minDeltas = new ArrayList<>();
        List<Double> maxDeltas = new ArrayList<>();
        List<Double> meanDeltas = new ArrayList<>();
        List<Double> stdDevDeltas = new ArrayList<>();

        for (int i = 0; i < lMins.length; i++) {
            Measurepoint measurePoint = lMeasures.get(i).getMeasurePoint();

            debugPrint("actual: " + measurePoint);
            debugPrint("expected: min=" + lMins[i] + ", max=" + lMaxs[i]
                    + ", mean=" + lMeans[i] + ", stdDev=" + lStdDevs[i]);

            assertAcceptableValue(measurePoint.getMin(), lMins[i], epsilonMinValue);
            assertAcceptableValue(measurePoint.getMax(), lMaxs[i], epsilonMaxValue);
            assertAcceptableValue(measurePoint.getMean(), lMeans[i], epsilon);
            assertAcceptableValue(measurePoint.getStdDev(), lStdDevs[i], epsilon);

            minDeltas.add(Math.abs(measurePoint.getMin() - lMins[i]));
            maxDeltas.add(Math.abs(measurePoint.getMax() - lMaxs[i]));
            meanDeltas.add(Math.abs(measurePoint.getMean() - lMeans[i]));
            stdDevDeltas.add(Math.abs(measurePoint.getStdDev() - lStdDevs[i]));
        }

        for (int i = 0; i < rMins.length; i++) {
            Measurepoint measurePoint = rMeasures.get(i).getMeasurePoint();

            debugPrint("actual: " + measurePoint);
            debugPrint("expected: min=" + rMins[i] + ", max=" + rMaxs[i]
                    + ", mean=" + rMeans[i] + ", stdDev=" + rStdDevs[i]);

            assertAcceptableValue(measurePoint.getMin(), rMins[i], epsilonMinValue);
            assertAcceptableValue(measurePoint.getMax(), rMaxs[i], epsilonMaxValue);
            assertAcceptableValue(measurePoint.getMean(), rMeans[i], epsilon);
            assertAcceptableValue(measurePoint.getStdDev(), rStdDevs[i], epsilon);

            minDeltas.add(Math.abs(measurePoint.getMin() - rMins[i]));
            maxDeltas.add(Math.abs(measurePoint.getMax() - rMaxs[i]));
            meanDeltas.add(Math.abs(measurePoint.getMean() - rMeans[i]));
            stdDevDeltas.add(Math.abs(measurePoint.getStdDev() - rStdDevs[i]));
        }

        DoubleSummaryStatistics minStats = getStats(minDeltas);
        DoubleSummaryStatistics maxStats = getStats(maxDeltas);
        DoubleSummaryStatistics meanStats = getStats(meanDeltas);
        DoubleSummaryStatistics stdDevStats = getStats(stdDevDeltas);

        debugPrint("Min stats: " + minStats);
        debugPrint("Max stats: " + maxStats);
        debugPrint("Mean stats: " + meanStats);
        debugPrint("StdDev stats: " + stdDevStats);

        assertTrue(minStats.getAverage() <= maxMeanDeltaMin);
        assertTrue(maxStats.getAverage() <= maxMeanDeltaMax);
        assertTrue(meanStats.getAverage() <= maxMeanDeltaMean);
        assertTrue(stdDevStats.getAverage() <= maxMeanDeltaStdDev);
    }

    private DoubleSummaryStatistics getStats(List<Double> list) {
        return list.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
    }

    private void assertAcceptableValue(double a, double b, double epsilon) {
        assertTrue(Math.abs(a - b) < epsilon);
    }

    private void debugPrint(String x) {
        if (debug) {
            System.out.println(x);
        }
    }
}

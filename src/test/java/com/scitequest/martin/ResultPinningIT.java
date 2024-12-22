package com.scitequest.martin;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.DataStatistics;
import com.scitequest.martin.export.JsonParseException;
import com.scitequest.martin.settings.Settings;

/**
 * This test compares test results with a stored JSON file in order to check
 * wether they match.
 */
public class ResultPinningIT {

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
    public void syniSm67() throws IOException, SecurityException, JsonParseException, MeasurementException {
        String imagePath = "src/test/resources/img/SYNI - SM67 - 60sec - A - 2.tif";
        Control control = Control.headless(ImageImpl.read(Path.of(imagePath)), settingsPath);

        control.setActiveMask(Paths.get("src/test/resources/syni_sm67_mask.json"));
        control.repositionSlide();
        control.moveSlide(32, 54);
        control.rotateSlide(0.78);

        Data data = control.doMeasure();

        // JSON
        Path jsonControlPath = Path.of("src/test/resources/syni_sm67.json");
        Data expectedData = Const.mapper.readValue(
                Files.readString(jsonControlPath), Data.class);
        // Repin the JSON file
        // Files.writeString(jsonControlPath, Const.mapper.writeValueAsString(data));
        assertEquals(data, expectedData);

        // TSV
        Path tsvControlPath = Path.of("src/test/resources/syni_sm67.tsv");
        String actualTsv = data.asTsv();
        String expectedTsv = Files.readString(tsvControlPath).replace("\r\n", "\n");
        // Repin the TSV file
        // Files.writeString(tsvControlPath, actualTsv.toString());
        assertEquals(expectedTsv, actualTsv);
    }

    @Test
    public void syniSm67StatisticsNoBackgroundSubtraction()
            throws IOException, SecurityException, JsonParseException, MeasurementException {
        Settings settings = Settings.load(settingsPath);
        // Disable background subtraction
        settings.getMeasurementSettings().setSubtractBackground(false);
        settings.store();
        settings.save();

        String imagePath = "src/test/resources/img/SYNI - SM67 - 60sec - A - 2.tif";
        Control control = Control.headless(ImageImpl.read(Path.of(imagePath)), settingsPath);

        control.setActiveMask(Paths.get("src/test/resources/syni_sm67_mask.json"));
        control.repositionSlide();
        control.moveSlide(32, 54);
        control.rotateSlide(0.78);

        Data data = control.doMeasure();
        DataStatistics dataStatistics = DataStatistics.analyze(data);

        // JSON
        Path jsonControlPath = Path.of("src/test/resources/syni_sm67_stats.json");
        DataStatistics expectedDataStatistics = Const.mapper.readValue(
                Files.readString(jsonControlPath), DataStatistics.class);
        // Repin the JSON file
        // Files.writeString(jsonControlPath,
        // Const.mapper.writeValueAsString(dataStatistics));
        assertEquals(dataStatistics, expectedDataStatistics);

        // TSV
        Path tsvControlPath = Path.of("src/test/resources/syni_sm67_stats.tsv");
        String actualTsv = dataStatistics.asTsv();
        String expectedTsv = Files.readString(tsvControlPath).replace("\r\n", "\n");
        // Repin the TSV file
        // Files.writeString(tsvControlPath, actualTsv.toString());
        assertEquals(expectedTsv, actualTsv);
    }
}

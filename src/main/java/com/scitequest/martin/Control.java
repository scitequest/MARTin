package com.scitequest.martin;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.swing.JOptionPane;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.formdev.flatlaf.FlatLaf;
import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.DataStatistics;
import com.scitequest.martin.export.Geometry;
import com.scitequest.martin.export.JsonParseException;
import com.scitequest.martin.export.Measurepoint;
import com.scitequest.martin.export.Metadata;
import com.scitequest.martin.export.Parameters;
import com.scitequest.martin.export.Polygon;
import com.scitequest.martin.settings.ExportSettings;
import com.scitequest.martin.settings.MaskExt;
import com.scitequest.martin.settings.MaskSettings;
import com.scitequest.martin.settings.ProjectExt;
import com.scitequest.martin.settings.ProjectSettings;
import com.scitequest.martin.settings.Settings;
import com.scitequest.martin.utils.DoubleStatistics;
import com.scitequest.martin.utils.SystemUtils;
import com.scitequest.martin.view.Controlable;
import com.scitequest.martin.view.Drawable;
import com.scitequest.martin.view.Gui;
import com.scitequest.martin.view.GuiUtils;
import com.scitequest.martin.view.IntegrityCheckResult;
import com.scitequest.martin.view.IntegrityCheckResult.IntegrityCheckContext;
import com.scitequest.martin.view.IntegrityCheckResult.IntegrityCheckError;
import com.scitequest.martin.view.ProcessorPen;
import com.scitequest.martin.view.SettingsGui;
import com.scitequest.martin.view.View;

/**
 * This class is the central node between all classes of the project. Control
 * assures proper coordination between a theoretical slideMask, user output and
 * user input.
 */
public final class Control implements Controlable {

    /** Logger specific to the our base package. */
    private static final Logger MARTIN_LOGGER = Logger.getLogger("com.scitequest.martin");

    /** The logger for this class. */
    private static final Logger log = Logger.getLogger("com.scitequest.martin.Control");

    /** Stores how the plugin is currently run. */
    private final RunType runType;

    /**
     * The image(es) to measure.
     *
     * Warning: this must be handled fully read-only to provide measurement
     * consistency. Always {@code duplicate()} before mutating the object!
     */
    private Optional<Image> image = Optional.empty();

    /**
     * View is either empty (during tests) or contains a GUI (when used by an actual
     * user).
     */
    private Optional<View> view = Optional.empty();

    /**
     * This stores all preferences the user sets. This is in regards
     * to slide parameters, save paths and data export.
     */
    private final Settings settings;

    /**
     * This class contains all coordinates of a slide mask. The user can interact
     * with a graphical depiction of said coodinates.
     */
    private SlideMask slide;

    /**
     * Whether the display currently uses adaptive filtering.
     */
    private boolean isFilterEnabled = false;

    /**
     * Create the control instance.
     *
     * @param runType  how the control is run
     * @param img      the image if running in headless as no UI is open
     * @param settings the settings
     */
    private Control(RunType runType, Optional<Image> img, Settings settings) {
        log.info(String.format("Starting MARTin %s (Git commit %s) in %s mode and locale %s",
                Const.VERSION, Const.GIT_INFO, runType, SystemUtils.getLocale()));

        this.runType = runType;
        this.image = img;
        this.settings = settings;
        this.slide = new SlideMask(settings);

        switch (runType) {
            case HEADLESS:
                break;
            case STANDALONE:
                this.view = Optional.of(new Gui(this, this.settings,
                        Const.VERSION, Const.GIT_INFO));
                break;
            default:
                String msg = "Non-existant runType was selected";
                log.severe(msg);
                throw new IllegalStateException("unreachable");
        }

        log.config("Initialized new instance of Control");
    }

    /**
     * Create the control running in headless mode.
     *
     * In headless mode no GUI is created.
     *
     * @param img          the image to process
     * @param settingsPath the path to the settings file to load
     * @return the new control
     * @throws IOException        if the settings or log file could not be
     *                            read/saved, or the version information could not
     *                            be obtained from the properties.
     * @throws SecurityException  if the logging file handler could not be created
     *                            because the caller has not
     *                            LoggingPermission("control")
     * @throws JsonParseException if the settings provided could not be parsed
     */
    public static Control headless(Image img, Path settingsPath)
            throws SecurityException, IOException, JsonParseException {
        setupLogging();
        Settings settings = Settings.loadOrDefault(settingsPath);
        return new Control(RunType.HEADLESS, Optional.of(img), settings);
    }

    /**
     * Create the control running in standalone mode.
     *
     * If there is no image supplied the program will start without a loaded image.
     *
     */
    public static void standalone() {
        try {
            setupLogging();
        } catch (SecurityException | IOException e) {
            System.err.println("Failed to initialize MARTin logging: " + e);
            String msg = String.format(Const.bundle.getString("control.martinStartupErrorLogging.text"), e);
            GuiUtils.showErrorDialog((Frame) null, msg,
                    Const.bundle.getString("control.martinStartupErrorLogging.title"));
            return;
        }
        // We have to dispose ImageJ on failure to fully exist.
        getSettingsCheckedWithGui().ifPresent(settings -> {
            // Initialize look and feel
            FlatLaf.registerCustomDefaultsSource("themes");
            String themeClassName = settings.getDisplaySettings().getTheme().getClassName();
            SettingsGui.setLookAndFeel(themeClassName);
            // Create new control instance that runs the application
            new Control(RunType.STANDALONE, Optional.empty(), settings);
        });
    }

    private static void setupLogging() throws SecurityException, IOException {
        // Only setup our logging if it hasn't been already
        boolean isLoggingAlreadyConfigured = MARTIN_LOGGER.getHandlers().length != 0;
        if (!isLoggingAlreadyConfigured) {
            // Set the format string for simple formatters
            System.setProperty("java.util.logging.SimpleFormatter.format", Const.LOG_FORMAT);
            // Prevents that the parent handler also logs messages to the console
            MARTIN_LOGGER.setUseParentHandlers(false);
            // Log all levels
            MARTIN_LOGGER.setLevel(Level.ALL);
            // Create a new handler that logs the messages to a file
            Path logdirPath = Paths.get(Const.PROJECT_DIRECTORIES.dataLocalDir);
            Files.createDirectories(logdirPath);
            Path logfilePath = logdirPath.resolve("martin.log");
            Handler fh = new FileHandler(logfilePath.toString());
            // Use simple formatting instead of XML
            fh.setFormatter(new SimpleFormatter());
            // Adds the file handler to the logger
            MARTIN_LOGGER.addHandler(fh);
        }
    }

    /**
     * Get the settings. If the settings on the filesystem are of an old version, we
     * ask the user if he wants to reset them and store the settings again.
     *
     * @return the obtained settings
     */
    private static Optional<Settings> getSettingsCheckedWithGui() {
        Path settingsPath = Paths.get(Const.PROJECT_DIRECTORIES.preferenceDir)
                .resolve("settings.json");
        try {
            return Optional.of(Settings.loadOrDefault(settingsPath));
        } catch (IOException loadEx) {
            log.log(Level.SEVERE, "Could not load existing settings from the filesystem");
            String msg = String.format(Const.bundle.getString("control.martinStartupErrorLoadExistingSettings.text"),
                    loadEx);
            GuiUtils.showErrorDialog((Frame) null, msg,
                    Const.bundle.getString("control.martinStartupErrorLoadExistingSettings.title"));
            return Optional.empty();
        } catch (JsonParseException parseEx) {
            log.log(Level.WARNING, "Could not parse existing settings file", parseEx);
            // Open dialog if settings could not be parsed and prompt to reset them
            String msg = Const.bundle.getString("control.martinStartupErrorParsing.text");
            if (JOptionPane.showConfirmDialog(null, msg,
                    Const.bundle.getString("control.martinStartupErrorParsing.title"),
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
                log.info("Resetting settings on user request");
                Settings settings = Settings.defaultSettings();
                try {
                    settings.save(settingsPath);
                } catch (IOException saveEx) {
                    log.log(Level.SEVERE, "Could not save previously reset settings");
                    GuiUtils.showErrorDialog((Frame) null,
                            Const.bundle.getString("control.martinStartupErrorSettings.text"),
                            Const.bundle.getString("control.martinStartupErrorSettings.title"));
                    return Optional.empty();
                }
                return Optional.of(settings);
            }
            return Optional.empty();
        }
    }

    /**
     * Sanity check to ensure that a valid image is open.
     *
     * @throws IllegalStateException if no image is open
     */
    private Image ensureImageOpen() throws IllegalStateException {
        return this.image.orElseThrow(() -> {
            String msg = "No current image despite one being required";
            log.severe(msg);
            return new IllegalStateException(msg);
        });
    }

    @Override
    public RunType getRunType() {
        return this.runType;
    }

    /**
     * Calculates the delta values between the slide masks current position and its
     * new position. These values are then used to call moveSlide of the class
     * SlideMask.
     */
    @Override
    public void moveSlide(int x, int y) {
        int deltaX = x - slide.getAbsoluteX();
        int deltaY = y - slide.getAbsoluteY();
        log.config("Manually moved slide in direction: x = " + deltaX + " y = " + deltaY);
        slide.moveSlide(deltaX, deltaY, false);
        update();
    }

    /** Digital rotation by {@code rotationSpinner}. */
    @Override
    public void rotateSlide(double angle) {
        slide.rotateSlide(angle - slide.getAbsoluteRotation(), 0, 0, 0, 0, false);
        update(); // gCanvas.repaint();
    }

    /** Analog rotation of slide mask via click-and-drag. */
    @Override
    public void rotateSlideViaMouse(int x, int y, int lastX, int lastY) {
        slide.rotateSlide(0, x, y, lastX, lastY, false);
        update();
    }

    @Override
    public double getSlideRotation() {
        return slide.getAbsoluteRotation();
    }

    @Override
    public int getSlidexPosition() {
        return slide.getAbsoluteX();
    }

    @Override
    public int getSlideyPosition() {
        return slide.getAbsoluteY();
    }

    /**
     * Checks if a click was made on any of the measure fields. By releasing the
     * mouse this effect is reverted via the method releaseMouseGrip.
     */
    @Override
    public void updateClickedMeasureCircles(int x, int y) {
        boolean clickedOnCircle = slide.isOnMeasureField(x, y);
        if (clickedOnCircle) {
            log.config("User clicked on measureCircle");
        }
    }

    /**
     * Checks if a click was made on any of the rectangular polygons, these include
     * the whole slide and the delRects. By releasing the mouse this effect is
     * reverted via the method releaseMouseGrip.
     */
    @Override
    public void updateClickedRectPolygons(int x, int y) {
        SlideMask.Element selection = slide.isOnRectPolygon(x, y);
        String msg = String.format("User grabbed %s with position x = %d, y = %d, angle = %f",
                selection, slide.getAbsoluteX(), slide.getAbsoluteY(), slide.getAbsoluteRotation());
        log.config(msg);
    }

    /**
     * If any element is grabbed this will cause it to be moved by the given values.
     * If the whole slide was grabbed all elements will be moved instead.
     */
    @Override
    public boolean moveGrabbedElement(int x, int y, int lastX, int lastY,
            int distanceX, int distanceY, boolean reposition) {
        boolean hasMoved = slide.moveGrabbedElement(x, y, lastX, lastY,
                distanceX, distanceY, reposition);
        update();
        return hasMoved;
    }

    /** This releases all grabbed elements. */
    @Override
    public void releaseMouseGrip() {
        String msg = String.format("User released grabbed elements. Current position of slide:"
                + " x = %d, y = %d, angle = %f",
                slide.getAbsoluteX(), slide.getAbsoluteY(), slide.getAbsoluteRotation());
        log.config(msg);
        slide.releaseGrip();
    }

    /**
     * Initiates the fitting algorithm. This effectively positions all measureFields
     * to the highest signal in their immediate neighbourhood i.E. within their
     * respective spotField cell.
     */
    @Override
    public void measureFieldFit() {
        log.config("Initiated fitting algorithm");
        // Guard against empty image
        ensureImageOpen();

        // Copies the image and makes sure its LUT is the same as the original image.
        Image img = this.image.get().duplicate();
        if (settings.getMeasurementSettings().isInvertLut()) {
            img.invert();
        }

        // Effectively defines the bounds the measureFields are allowed to be moved in
        // this algorithm.
        ArrayList<PolyGrid> spotFields = slide.getSpotFields();

        ArrayList<PolyGrid> measureFields = slide.getMeasureFields();

        // Since the shape of the measureFields is uniform for an individual slideMask
        // we just copy the first measureFields parameters here to use it for all
        // measureField positions further on.
        PolyShape measureGridElement = measureFields.get(0).getGridElement(0, 0);
        double radius = measureFields.get(0).getShapeWidth() / 2;

        double[][] measureElementCoords = measureGridElement.getPolyCoordinates();

        // This way of doing things takes element rotation into consideration
        // It only has to be done once, since all elements are rotated the same.
        double maxX = 0;
        double maxY = 0;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        for (int i = 0; i < measureElementCoords[0].length; i++) {
            maxX = Math.max(measureElementCoords[0][i], maxX);
            maxY = Math.max(measureElementCoords[1][i], maxY);
            minX = Math.min(measureElementCoords[0][i], minX);
            minY = Math.min(measureElementCoords[1][i], minY);
        }
        // These values are for measureField positioning (center to top left corner)
        double hWidth = (maxX - minX) / 2;
        double hHeight = (maxY - minY) / 2;
        // These values are used for field population
        double widthRatio = spotFields.get(0).getShapeWidth()
                / measureFields.get(0).getShapeWidth();
        double heightRatio = spotFields.get(0).getShapeHeight()
                / measureFields.get(0).getShapeHeight();

        int lastIdx = settings.getMaskSettings().getLastMeasurePointIndex();
        int nCols = spotFields.get(0).getColumns();

        // For all spotfields for each row and col search the highest value position of
        // the corresponding measureField.
        for (int field = 0; field < measureFields.size(); field++) {
            for (int row = 0; row < spotFields.get(field).getRows(); row++) {
                for (int col = 0; col < nCols; col++) {
                    if (row * nCols + col > lastIdx) {
                        row = spotFields.get(field).getRows();
                        break;
                    }
                    // Boundaries the fitting takes place.
                    PolyShape searchPerimeter = spotFields.get(field).getGridElement(row, col)
                            .shrinkByShape(measureFields.get(field).getGridElement(row, col));

                    SearchArea searchArea = SearchArea.of(this, img, searchPerimeter,
                            measureGridElement, radius,
                            hWidth, hHeight, widthRatio, heightRatio);

                    // Calculates the center of the highest value position
                    Point maxPos = searchArea.searchForAbsoluteMax();
                    // Relocates the measureField to the highest value position
                    measureFields.get(field).getGridElement(row, col).moveToCenter(maxPos);
                }
            }
            measureFields.get(field).calculateGridOrbits(slide.getrCenter());
        }
        update();
    }

    /**
     * Subtracts each pixel value of a given image by the mean pixel value of given
     * set of deletionRectangles.
     *
     * @param img                The image the noise reduction will take place on.
     * @param deletionRectangles A set of rectangles which are positioned on the
     *                           background of an image.
     */
    private static double measureBackgroundNoise(Image img, List<Polygon> deletionRectangles) {
        log.config("Getting background noise.");

        double backgroundNoiseSum = 0.0;
        int validBackgroundRectangles = 0;

        // Measure each deletion rectangle
        for (Polygon poly : deletionRectangles) {
            var coords = poly.coordinates;
            // To match previous versions, the background rectangle coordinates are floored
            List<Point> pointsFloored = new ArrayList<>(coords.size());
            for (Point p : poly.coordinates) {
                Point pFloored = Point.of((int) p.x, (int) p.y);
                pointsFloored.add(pFloored);
            }
            // Try to measure the backround rectangle
            try {
                DoubleStatistics stats = img.measure(Polygon.ofPolygon(pointsFloored));
                backgroundNoiseSum += stats.getAverage();
                validBackgroundRectangles++;
            } catch (MeasurementException e) {
                log.config(String.format("deletion rectangle is out of bounds: %s", poly));
            }
        }

        if (validBackgroundRectangles == 0) {
            log.warning(
                    "No noise could be subtracted because no background subtraction rectangle was within image bounds");
            return 0.0;
        }

        double backgroundNoise = backgroundNoiseSum / validBackgroundRectangles;
        log.config("backgroundNoise to be subtracted = " + backgroundNoise);
        return backgroundNoise;
    }

    /**
     * Recursively search an XML DOM for a node with the "name" attribute
     * "DateTimeOriginal".
     *
     * If found it extracts the value.
     *
     * @param node the starting node
     * @return if found, the datetime as a string
     */
    private static Optional<String> findDateTimeOriginal(Node node) {
        Node valueAttr = node.getAttributes().getNamedItem("name");
        if (valueAttr != null && valueAttr.getNodeValue().equals("DateTimeOriginal")) {
            return Optional.of(node
                    .getFirstChild()
                    .getFirstChild()
                    .getAttributes()
                    .getNamedItem("value")
                    .getNodeValue());
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Optional<String> dateTimeOriginal = findDateTimeOriginal(children.item(i));
            if (dateTimeOriginal.isPresent()) {
                return dateTimeOriginal;
            }
        }

        return Optional.empty();
    }

    /**
     * Attempt to get the datetime from a image file.
     *
     * NOTE: ATM only TIFF is supported. Other formats MAY be supportable in the
     * future but it could very well be complicated.
     *
     * @param file the image file to extract the created timestamp from
     * @return if found, a timestamp when the image was created
     */
    public static Optional<LocalDateTime> getDateTimeOriginalFromFile(File file) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                // Pick the first available ImageReader
                ImageReader reader = readers.next();
                // Attach source to the reader
                reader.setInput(iis, true);
                // Read metadata of first image
                IIOMetadata metadata = reader.getImageMetadata(0);
                // Read TIFF format
                String tiffMetadataFormatName = "javax_imageio_tiff_image_1.0";
                // Get the metadata as XML DOM tree ...
                Node node = metadata.getAsTree(tiffMetadataFormatName);
                // Recursively search for "DateTimeOriginal"
                // If found parse the string to a datetime object based on the system timezone
                Optional<LocalDateTime> datetime = findDateTimeOriginal(node).flatMap(s -> {
                    var formatter = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
                    try {
                        return Optional.of(LocalDateTime.parse(s, formatter));
                    } catch (DateTimeParseException e) {
                        log.warning("Could not parse datetime '" + s + "'");
                        return Optional.empty();
                    }
                });
                return datetime;
            }
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Writes a JSON object to the file specified by path.
     *
     * @param path              the path of the file to create
     * @param obj               the JSON object
     * @param jsonWriterFactory the factory to create JSON writers
     * @return true, if there was an error
     */
    @Deprecated
    private boolean writeJsonFileDeprecated(Path path, JsonObject obj,
            JsonWriterFactory jsonWriterFactory) {
        log.config(String.format("Writing JSON file '%s'", path));
        try (BufferedWriter writer = Files.newBufferedWriter(path,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                JsonWriter jsonWriter = jsonWriterFactory.createWriter(writer)) {
            jsonWriter.write(obj);
            return false;
        } catch (IOException e) {
            String msg = String.format(Const.bundle.getString("control.exportJSONFileError.text"), path);
            log.log(Level.SEVERE, msg, e);
            this.view.ifPresent(
                    v -> v.showErrorDialog(msg, Const.bundle.getString("control.exportJSONFileError.title")));
            return true;
        }
    }

    /**
     * Writes a JSON object to the file specified by path.
     *
     * @param path the path of the file to create
     * @param obj  the JSON object
     */
    private boolean writeJsonFile(Path path, Object obj) {
        log.config(String.format("Writing JSON file '%s'", path));
        try {
            Files.writeString(path, Const.mapper.writeValueAsString(obj), StandardCharsets.UTF_8);
            return false;
        } catch (IOException e) {
            String msg = String.format("Could not export JSON file '%s'", path);
            log.log(Level.SEVERE, msg, e);
            this.view.ifPresent(v -> v.showErrorDialog(msg, "Unable to export file"));
            return true;
        }
    }

    private boolean writeTsvFile(Path path, String content) {
        log.config(String.format("Writing TSV file '%s'", path));
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return false;
        } catch (IOException e) {
            String msg = String.format(Const.bundle.getString("control.exportTSVFileError.text"), path);
            log.log(Level.SEVERE, msg, e);
            this.view
                    .ifPresent(v -> v.showErrorDialog(msg, Const.bundle.getString("control.exportTSVFileError.title")));
            return true;
        }
    }

    private boolean writeImageFile(Path path, BufferedImage img) {
        log.config(String.format("Writing image file '%s'", path));
        try {
            ImageIO.write(img, "tif", path.toFile());
            return false;
        } catch (IOException e) {
            String msg = String.format(Const.bundle.getString("control.exportImageFileError.text"), path);
            log.log(Level.SEVERE, msg, e);
            this.view.ifPresent(
                    v -> v.showErrorDialog(msg, Const.bundle.getString("control.exportImageFileError.title")));
            return true;
        }
    }

    /**
     * Exports all enabled files of a measurements into a given directory.
     * Files can be enabled and disabled via the options menu.
     *
     * @param exportDir      Directory in which the export will be conducted.
     * @param metadata       Metadata of the measurement.
     * @param parameters     Positional metadata of the measurement.
     * @param data           Raw measure data for each position of the measurement.
     * @param dataStatistics Aggregated measure data across all spotfields of a
     *                       measurement.
     * @return true if the export has been successful.
     */
    public boolean exportIntoFolder(Path exportDir,
            Metadata metadata, Parameters parameters,
            Data data, DataStatistics dataStatistics) {
        ExportSettings exportSettings = settings.getExportSettings();
        // Write the actual data files
        var jsonConfig = Map.of(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(jsonConfig);

        // Write the metadata
        Path metadataPath = exportDir.resolve("metadata.json");
        if (writeJsonFileDeprecated(metadataPath, metadata.asJson(), jsonWriterFactory)) {
            return false;
        }
        Path parametersPath = exportDir.resolve("parameters.json");
        if (writeJsonFile(parametersPath, parameters)) {
            return false;
        }

        // Write the data
        if (exportSettings.isExportJSON()) {
            Path jsonDataPath = exportDir.resolve("data.json");
            if (writeJsonFile(jsonDataPath, data)) {
                return false;
            }
        }
        if (exportSettings.isExportTSV()) {
            Path tsvDataPath = exportDir.resolve("data.tsv");
            if (writeTsvFile(tsvDataPath, data.asTsv())) {
                return false;
            }
        }

        // Write the statistics
        if (exportSettings.isExportJSON()) {
            Path jsonDataStatisticsPath = exportDir.resolve("data_statistics.json");
            if (writeJsonFile(jsonDataStatisticsPath, dataStatistics)) {
                return false;
            }
        }
        if (exportSettings.isExportTSV()) {
            Path tsvDataStatisticsPath = exportDir.resolve("data_statistics.tsv");
            if (writeTsvFile(tsvDataStatisticsPath, dataStatistics.asTsv())) {
                return false;
            }
        }

        if (exportSettings.isSaveAnnotatedImage()) {
            if (writeImageFile(exportDir.resolve("annotated_image.tiff"), generateGridImage())) {
                return false;
            }
        }
        if (exportSettings.isSaveWholeImage()) {
            Path imagePath = image.get().getFilePath();
            Optional<String> extension = Optional.ofNullable(imagePath.getFileName())
                    .map(f -> f.toString())
                    .filter(f -> f.contains("."))
                    .map(f -> f.substring(f.lastIndexOf(".")));
            Path storedImageFileName = exportDir.resolve("image" + extension.orElse(""));
            try {
                Files.copy(imagePath, storedImageFileName);
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Initiates the measurement with the current position of all elements of the
     * slideMask and the values set for metadata.
     *
     * @param metadata       Metadata of the measurement.
     * @param parameters     Positional metadata of the measurement.
     * @param data           Raw measure data for each position of the measurement.
     * @param dataStatistics Aggregated measure data across all spotfields of a
     *                       measurement.
     */
    public void export(Metadata metadata, Parameters parameters,
            Data data, DataStatistics dataStatistics) {
        log.info(String.format("Initiating export of measurements with metadata %s",
                metadata.asJson()));

        // Query export directory from user if it set in the settings
        Optional<Path> baseExportDirectory = settings.getExportSettings().getExportDirectory()
                .or(() -> view.get().chooseDirectory("Specify Export Directory"));
        if (baseExportDirectory.isEmpty()) {
            String msg = Const.bundle.getString("control.exportCanceled.text");
            log.info(msg);
            view.get().showInfoDialog(msg, Const.bundle.getString("control.exportCanceled.title"));
            return;
        }

        // Create directory for the output files
        DateTimeFormatter localTimeFormatter = new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral('-')
                .appendValue(MINUTE_OF_HOUR, 2)
                .appendLiteral('-')
                .appendValue(SECOND_OF_MINUTE, 2)
                .toFormatter();
        String projectName = metadata.getProject()
                .map(proj -> proj.getNameAsKebapCase())
                .orElse("none");
        String sampleId = metadata.getPatient().getIdAsKebapCase();
        Path exportDir = baseExportDirectory.get()
                .resolve(projectName)
                .resolve(metadata.getDatetime().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .resolve(metadata.getDatetime().format(localTimeFormatter) + "-" + sampleId);
        log.config(String.format("Using export directory '%s'", exportDir));
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            String msg = String.format(Const.bundle.getString("control.measureExportError.text"), exportDir);
            log.log(Level.SEVERE, msg, e);
            this.view.ifPresent(v -> v.showErrorDialog(msg, null));
            return;
        }

        if (exportIntoFolder(exportDir, metadata, parameters, data, dataStatistics)) {
            log.info("Export completed successfully");
        } else {
            log.warning("Export failed");
        }
    }

    /**
     * Measures and returns image statistics for a given image.
     *
     * Also does some data analysis, namely:
     * shifting the minimum of each spotField to zero
     * as well as internal nomalization of each spotField.
     *
     * @param img             image to be measured
     * @param backgroundNoise
     * @param parameters      measurement parameters
     * @return measurement data of image.
     * @throws MeasurementException
     */
    private static Data measureValues(Image img, Parameters parameters)
            throws MeasurementException {
        List<Geometry> spots = parameters.getSpots();
        int maxSpotsPerSpotfield = spots.size() / parameters.getNumberOfSpotfields();

        List<Measurepoint> values = new ArrayList<>();
        for (int spot = 0; spot < parameters.getNumberOfSpotfields(); spot++) {
            for (int i = 0; i < parameters.getSpotsPerSpotfield(); i++) {
                int row = i / parameters.getColumnsPerSpotfield();
                int col = i % parameters.getColumnsPerSpotfield();
                int absIdx = spot * maxSpotsPerSpotfield + i;

                try {
                    DoubleStatistics stats = img.measure(spots.get(absIdx));
                    values.add(Measurepoint.tryFrom(spot, row, col, stats));
                } catch (IllegalArgumentException e) {
                    // Rethrow as measurement exception, will be catched again below
                    throw new MeasurementException(e);
                } catch (MeasurementException e) {
                    String msg = String.format(
                            "Encountered measurement error: %s (spot=%d, row=%d, col%d, spot=%s)",
                            e, spot, row, col, spot);
                    log.log(Level.WARNING, msg);
                    throw e;
                }
            }
        }

        return Data.fromMeasurepoints(values);

    }

    /**
     * Actual implementation of the measurement.
     *
     * This method must be independent of GUI or other interactions and must be able
     * to run headless.
     *
     * @param img        the image to measure
     * @param parameters the parameters that specify what and how the measurement
     *                   should be done
     * @return the measured data
     * @throws MeasurementException
     */
    public static Data doMeasure(Image img, Parameters parameters) throws MeasurementException {
        log.config("Initiating measurement with parameters");
        if (parameters.isInvertLut()) {
            img.invert();
        }
        if (parameters.isSubtractBackground()) {
            double noise = measureBackgroundNoise(img, parameters.getBackgroundRectangles());
            // Value floored for consistency with prior versions
            img.subtract((int) noise);
        }
        return measureValues(img, parameters);
    }

    /**
     * Measure but get values from the class instance.
     *
     * Only used for tests, hence a protected wrapper
     *
     * @return the measured data
     * @throws MeasurementException
     */
    protected Data doMeasure() throws MeasurementException {
        Parameters parameters = Parameters.fromSettingsAndSlide(settings, slide);
        return doMeasure(image.get().duplicate(), parameters);
    }

    /**
     * Measure and export to specified folder but get values from the class
     * instance.
     *
     * Only used for tests, hence a protected wrapper
     *
     * @param exportFolder the folder to export the data to
     * @throws MeasurementException
     */
    protected void doMeasureAndExport(Path exportFolder, Metadata metadata) throws MeasurementException {
        Parameters parameters = Parameters.fromSettingsAndSlide(settings, slide);
        Data data = doMeasure(image.get(), parameters);
        exportIntoFolder(exportFolder, metadata, parameters,
                data, DataStatistics.analyze(data));
    }

    /**
     * Initiates measuring process.
     */
    @Override
    public void measure() {
        Image img = ensureImageOpen().duplicate();

        // Collect metadata
        ZonedDateTime datetime = ZonedDateTime.now();
        // Try to get a assay date from the measured image
        File imageFile = image.get().getFilePath().toFile();
        Optional<LocalDateTime> assayDate = getDateTimeOriginalFromFile(imageFile);

        // Run the measurement
        Parameters parameters = Parameters.fromSettingsAndSlide(settings, slide);
        Data data;
        try {
            data = doMeasure(img, parameters);
        } catch (MeasurementException e) {
            log.log(Level.SEVERE, e.toString());
            view.ifPresent(v -> v.showErrorDialog(
                    Const.bundle.getString("control.measurementFailed.text"),
                    Const.bundle.getString("control.measurementFailed.title")));
            return;
        }
        DataStatistics dataStatistics = DataStatistics.analyze(data);

        // Get the metadata from the user and export if requested
        log.config("Asking for metadata input");
        this.view.get().openExportGui(datetime, assayDate, parameters, data, dataStatistics);
    }

    /**
     * Exports an instance of project to a given path.
     */
    @Override
    public void exportProject(ProjectExt proj, Path path) {
        try {
            ProjectSettings.exportProject(proj, path);
        } catch (IOException e) {
            String msg = Const.bundle.getString("control.projectExportError.text");
            log.log(Level.SEVERE, msg, e);
            view.ifPresent(v -> v.showErrorDialog(
                    String.format("%s: %s", msg, e.getMessage()), null));
            return;
        }

        log.info(String.format("Projects file '%s' has been saved", path));
    }

    /**
     * Find the first image file in a given directory. On failure to open the
     * directory, empty is returned.
     *
     * A file is an image file if it starts with "image."
     *
     * @param dir the directory to find an image file in
     * @return the path of the image file if found
     */
    private static Optional<Path> getImageFileInDirectory(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(file -> isRegularReadableFile(file))
                    .filter(file -> file.getFileName().toString().startsWith("image."))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Initiates an integrity check. This checks if a given measurement can be
     * reproduced
     * by its the original image and positional data.
     */
    @Override
    public IntegrityCheckResult checkIntegrity(Path folder) {
        Path parametersPath = folder.resolve("parameters.json");
        Path imagePath = getImageFileInDirectory(folder)
                .orElse(folder.resolve("image"));
        IntegrityCheckContext ctx = new IntegrityCheckContext(folder, imagePath);

        // Guards against missing files strongly required for integrity checks
        if (!isRegularReadableFile(parametersPath)) {
            return IntegrityCheckResult.ofError(
                    IntegrityCheckError.INVALID_MEASUREMENT_DIRECTORY, ctx);
        }
        if (!isRegularReadableFile(imagePath)) {
            return IntegrityCheckResult.ofError(IntegrityCheckError.MISSING_IMAGE, ctx);
        }

        // Open the image to check the integrity against
        Image img;
        try {
            img = ImageImpl.read(imagePath);
        } catch (IOException e) {
            return IntegrityCheckResult.ofError(IntegrityCheckError.IMAGE_OPEN_FAILED, ctx);
        }
        try {
            // Read the paramaters used to take the image
            Parameters parameters = Const.mapper.readValue(
                    Files.readString(parametersPath, StandardCharsets.UTF_8), Parameters.class);
            // Check the integrity with the image and parameters
            return IntegrityCheckResult.ofCompleted(checkIntegrity(folder, img, parameters), ctx);
        } catch (MeasurementException e) {
            return IntegrityCheckResult.ofError(IntegrityCheckError.MEASUREMENT_EXCEPTION, ctx);
        } catch (IOException | IllegalArgumentException e) {
            return IntegrityCheckResult.ofError(IntegrityCheckError.IO_EXCEPTION, ctx);
        }
    }

    /**
     * This compares a the measurement files (data and data_statistics) of a given
     * measurement with a reproduced measurement, using the positional data and a
     * copy of the original image.
     *
     * @param folder     Directory of a singular measurement.
     * @param img        Original image.
     * @param parameters Positional metadata of original measurement.
     * @return Positive or negative integrity-check-result for each measurement
     *         file.
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws MeasurementException
     */
    private HashMap<Path, Boolean> checkIntegrity(Path folder, Image img, Parameters parameters)
            throws IOException, IllegalArgumentException, MeasurementException {
        // Remeasure image based on the parameters
        Data data = doMeasure(img, parameters);
        DataStatistics dataStatistics = DataStatistics.analyze(data);

        // Check the existing files for similarity
        double eps = Const.INTEGRITY_CHECK_EPSILON;
        HashMap<Path, Boolean> results = new HashMap<>();

        Path storedJsonDataPath = folder.resolve("data.json");
        Path storedJsonDataStatisticsPath = folder.resolve("data_statistics.json");
        Path storedTsvDataPath = folder.resolve("data.tsv");
        Path storedTsvDataStatisticsPath = folder.resolve("data_statistics.tsv");

        if (isRegularReadableFile(storedJsonDataPath)) {
            Data storedJsonData = Const.mapper.readValue(
                    Files.readString(storedJsonDataPath, StandardCharsets.UTF_8),
                    Data.class);
            results.put(storedJsonDataPath, data.equalsEpsilon(storedJsonData, eps));
        }
        if (isRegularReadableFile(storedJsonDataStatisticsPath)) {
            DataStatistics storedJsonDataStatistics = Const.mapper.readValue(
                    Files.readString(storedJsonDataStatisticsPath, StandardCharsets.UTF_8),
                    DataStatistics.class);
            results.put(storedJsonDataStatisticsPath,
                    dataStatistics.equalsEpsilon(storedJsonDataStatistics, eps));
        }
        if (isRegularReadableFile(storedTsvDataPath)) {
            Data storedTsvData = Data.fromTsv(
                    Files.readString(storedTsvDataPath));
            results.put(storedTsvDataPath, data.equalsEpsilon(storedTsvData, eps));
        }
        if (isRegularReadableFile(storedTsvDataStatisticsPath)) {
            DataStatistics storedTsvDataStatistics = DataStatistics.fromTsv(
                    Files.readString(storedTsvDataStatisticsPath));
            results.put(storedTsvDataStatisticsPath,
                    dataStatistics.equalsEpsilon(storedTsvDataStatistics, eps));
        }

        return results;
    }

    private static boolean isRegularReadableFile(Path path) {
        return Files.isRegularFile(path) && Files.isReadable(path);
    }

    @Override
    public ProjectExt importProject(Path path) throws IOException, JsonParseException {
        return settings.getProjectSettings().importProject(path);
    }

    /**
     * Exports a slideMask to a given directory.
     */
    @Override
    public void exportMask(MaskExt mask, Path path) {
        try {
            MaskSettings.exportMask(mask, path);
        } catch (IOException e) {
            String msg = Const.bundle.getString("control.slideExportError.text");
            log.log(Level.SEVERE, msg, e);
            view.ifPresent(v -> v.showErrorDialog(
                    String.format("%s: %s", msg, e.getMessage()), null));
            return;
        }

        log.info(String.format("Slide file '%s' has been saved", path));
    }

    @Override
    public MaskExt importMask(Path path) throws IOException, JsonParseException {
        return settings.getMaskSettings().importMask(path);
    }

    /**
     * The purpose of this method is to generate a gridded image which can be
     * saved
     * by the user.
     *
     * @return current slideMask selection cropped out
     */
    public BufferedImage generateGridImage() {
        log.config("Initiate generating gridded image.");
        Image img = ensureImageOpen();

        DrawOptions drawOptions = new DrawOptions(true,
                settings.getDisplaySettings().isShowSpotfieldGrids(),
                true, true, false,
                settings.getDisplaySettings().isShowMeasureCircles());

        // Calculate slide bounds
        double[][] slideCoordinates = slide.getSlideCoordinates();
        List<Point> slideCoords = new ArrayList<>(slideCoordinates[0].length);
        for (int i = 0; i < slideCoordinates[0].length; i++) {
            double x = slideCoordinates[0][i];
            double y = slideCoordinates[1][i];
            slideCoords.add(Point.of(x, y));
        }
        Polygon slideArea = Polygon.ofPolygon(slideCoords);
        Rectangle2D.Double slideBounds = slideArea.calculateBounds();

        // Create a new BufferedImage for the cropped area
        int padding = 50;
        int cornerX = (int) (slideBounds.x - padding);
        int cornerY = (int) (slideBounds.y - padding);
        int width = (int) (slideBounds.width + 2.0 * padding);
        int height = (int) (slideBounds.height + 2.0 * padding);
        BufferedImage croppedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Draw the grid image
        Graphics2D g2d = croppedImage.createGraphics();
        g2d.translate(-cornerX, -cornerY);
        g2d.drawImage(img.getWrapped(), 0, 0, null);
        slide.drawElements(new ProcessorPen(g2d), drawOptions);
        g2d.dispose();

        return croppedImage;
    }

    @Override
    public void drawElements(Drawable stilus, DrawOptions drawOptions) {
        slide.drawElements(stilus, drawOptions);
    }

    @Override
    public void repositionSlide() {
        slide.repositionSlide();
    }

    @Override
    public void update() {
        view.ifPresent(v -> v.notifiySlideChanged());
    }

    /**
     * Closes this application.
     */
    @Override
    public void exit() {
        log.info(String.format("Shutting down MARTin in %s mode", runType));
        System.exit(0);
    }

    /**
     * Sets wether adaptive filtering should currently be enabled.
     *
     * @param enabled if true, filtering is shown in the canvas
     */
    public void setFilterEnabled(boolean enabled) {
        Image img = ensureImageOpen().duplicate();
        if (enabled) {
            log.config("Adaptive filter was enabled.");
            img.normalizeLocalContrast(5, 5, 1);
        } else {
            log.config("Adaptive filter was disabled.");
        }
        view.ifPresent(v -> v.setDisplayImage(img, false));
        update();
    }

    @Override
    public void toggleFilter() {
        isFilterEnabled = !isFilterEnabled;
        setFilterEnabled(isFilterEnabled);
    }

    @Override
    public void openImage() {
        // Reset the filter to not enabled
        this.isFilterEnabled = false;

        // Ask the user for a file to open
        log.config("Opened select file dialog.");

        Optional<Path> path = view.get().chooseFile(
                FileDialog.LOAD, Const.bundle.getString("control.openImage.text"), "");

        // Exit if cancel was pressed
        if (path.isEmpty()) {
            log.info("User canceled new image selection");
            return;
        }

        log.info(String.format("Loading image file: %s", path.get()));
        Image img;
        try {
            img = ImageImpl.read(path.get());
        } catch (IllegalArgumentException e) {
            String msg = Const.bundle.getString("control.errorCompositeStackImage.text");
            log.warning(msg);
            view.ifPresent(v -> v.showErrorDialog(msg, Const.bundle.getString("control.errorOpeningFile.title")));
            return;
        } catch (IOException e) {
            // Display error message and exit if the file could not be loaded
            String msg = Const.bundle.getString("control.errorOpeningFile.text");
            log.severe(msg);
            // TODO: Notify GUI status text somehow that the loading failed instead
            view.ifPresent(v -> v.showErrorDialog(msg, Const.bundle.getString("control.errorOpeningFile.title")));
            return;
        }

        // Replacing the current image with the new one
        view.ifPresent(v -> v.setDisplayImage(img.duplicate(), true));
        this.image = Optional.of(img);
        this.slide = new SlideMask(settings);

        update();
        log.config("Successfully opened new image.");

        // Positions out mainGui in front of the opened image
        view.ifPresent(v -> v.toFront());
    }

    @Override
    public void imageClosed() {
        log.info("Image window has been closed");
        this.image = Optional.empty();
    }

    @Override
    public boolean isImageLoaded() {
        return this.image.isPresent();
    }

    @Override
    public boolean isFilterEnabled() {
        return this.isFilterEnabled;
    }

    /**
     * This restores all functions of the main gui, which were restricted while the
     * optionGui was open.
     */
    @Override
    public void optionsGuiClosed() {
        /*
         * We could add a log info here,
         * but I think it would be better to add additional logging to the GUIs
         * themselves.
         */
        view.get().setOtherGuiOpened(false);
        view.get().toggleAllButtons(true);
    }

    /**
     * This restricts or releases some functionalities of the main gui in relation
     * to the exportGui.
     */
    public void exportGuiOpen(boolean exportGuiOpened) {
        if (exportGuiOpened) {
            view.get().toggleAllButtons(false);
            view.get().setOtherGuiOpened(true);
            view.get().setCanvasInteractible(false);
        } else {
            view.get().toggleAllButtons(true);
            view.get().setOtherGuiOpened(false);
            view.get().setCanvasInteractible(true);
        }
    }

    @Override
    public void setActiveMaskSettings(MaskExt activeMaskSettings) {
        slide.setMaskParameters(activeMaskSettings);
    }

    /**
     * Debug method to import and set an mask during testing.
     *
     * @param maskJsonPath the path to the mask JSON file
     * @throws IOException        if IO stuff happens
     * @throws JsonParseException if the JSON isn't valid
     */
    public void setActiveMask(Path maskJsonPath) throws IOException, JsonParseException {
        MaskExt mask = importMask(maskJsonPath);
        slide.setMaskParameters(mask);
    }
}

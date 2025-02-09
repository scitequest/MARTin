package com.scitequest.martin.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.scitequest.martin.Const;
import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.DataStatistics;
import com.scitequest.martin.export.Datapoint;
import com.scitequest.martin.export.DatapointStatistics;
import com.scitequest.martin.export.Image;
import com.scitequest.martin.export.Incubation;
import com.scitequest.martin.export.Measurepoint;
import com.scitequest.martin.export.Metadata;
import com.scitequest.martin.export.Parameters;
import com.scitequest.martin.export.Patient;
import com.scitequest.martin.export.Project;
import com.scitequest.martin.export.Quantity;
import com.scitequest.martin.export.Quantity.Unit;
import com.scitequest.martin.settings.ProjectExt;
import com.scitequest.martin.settings.ProjectSettings;
import com.scitequest.martin.settings.Settings;

public final class ExportGui extends JDialog {

    /** The logger for this class. */
    private static final Logger log = Logger.getLogger("com.scitequest.martin.view.ExportGui");

    private final Controlable control;
    private final Settings settings;

    private final ZonedDateTime datetime;
    private final Parameters parameters;
    private final Data data;
    private final DataStatistics dataStatistics;

    private final JComboBox<String> projectDropdown;

    private final JTextField sampleId = new JTextField();
    private final JTextField name = new JTextField();
    private final TimestampInput timestampInput;
    private final TagSelection sampleTags = TagSelection.empty();
    private final IncubationInput incubationInput = new IncubationInput();
    private final JTextField imager = new JTextField();
    private final SpinnerNumberModel exposureTime;
    private final SpinnerNumberModel pixelBinning;

    public ExportGui(Frame owner, Controlable control, Settings settings,
            ZonedDateTime datetime, Optional<LocalDateTime> assayDatetime,
            Parameters parameters,
            Data data, DataStatistics dataStatistics) {
        super(owner, "Enter Export Metadata", false);

        this.control = control;
        this.settings = settings;
        this.datetime = datetime;
        this.parameters = parameters;
        this.data = data;
        this.dataStatistics = dataStatistics;

        setLayout(new BorderLayout());

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeExportGui();
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });

        // ======== projectPanel ========

        // ---- projectDropdown ----
        projectDropdown = new JComboBox<>();
        updateProjectDropdownList();
        projectDropdown.setSelectedIndex(settings.getProjectSettings().getLastUsedProjectIndex() + 1);
        projectDropdown.addActionListener(e -> loadProject(projectDropdown.getSelectedIndex()));

        // ---- projectManagerButton ----
        JButton projectManagerButton = new JButton();
        projectManagerButton.setText(
                Const.bundle.getString("exportGui.projectManagerButton.text"));
        projectManagerButton.setBackground(UIManager.getColor("Actions.Blue"));
        projectManagerButton.setForeground(SystemColor.text);
        projectManagerButton.addActionListener(e -> openProjectManager());

        JPanel projectPanel = new JPanel();
        projectPanel.setBorder(new EmptyBorder(5, 200, 5, 200));
        projectPanel.setBackground(UIManager.getColor("MenuBar.background"));
        projectPanel.setLayout(new GridLayout(1, 0, 5, 0));
        projectPanel.add(projectDropdown);
        projectPanel.add(projectManagerButton);

        add(projectPanel, BorderLayout.NORTH);

        // ======== mainMetadata ========
        JPanel mainMetadata = new JPanel();
        mainMetadata.setBorder(new EmptyBorder(5, 5, 5, 5));
        mainMetadata.setLayout(new GridBagLayout());
        ((GridBagLayout) mainMetadata.getLayout()).columnWidths = new int[] {
                205, 200, 0 };
        ((GridBagLayout) mainMetadata.getLayout()).rowHeights = new int[] {
                0, 0, 0, 0, 0, 0, 0, 0, 95, 0, 0, 0, 0, 0, 0, 0, 0 };
        ((GridBagLayout) mainMetadata.getLayout()).columnWeights = new double[] {
                1.0, 1.0, 1.0E-4 };
        ((GridBagLayout) mainMetadata.getLayout()).rowWeights = new double[] {
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4 };

        // ---- sampleHeader ----
        JLabel sampleHeader = new JLabel();
        sampleHeader.setText(Const.bundle.getString("exportGui.sampleHeader.text"));
        sampleHeader.putClientProperty("FlatLaf.styleClass", "h2");
        mainMetadata.add(sampleHeader, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

        // ---- sampleIdLabel ----
        JLabel sampleIdLabel = new JLabel();
        sampleIdLabel.setText(Const.bundle.getString("exportGui.sampleIdLabel.text"));
        sampleIdLabel.setLabelFor(sampleId);
        sampleIdLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(sampleIdLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- sampleId ----
        sampleId.putClientProperty("JTextField.showClearButton", true);
        sampleId.putClientProperty("JTextField.placeholderText", "CS001");
        mainMetadata.add(sampleId, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- sampleIdDescription ----
        JTextArea sampleIdDescription = new JTextArea(2, 1);
        sampleIdDescription.setText(Const.bundle.getString("exportGui.sampleIdDescription.text"));
        sampleIdDescription.setLineWrap(true);
        sampleIdDescription.setWrapStyleWord(true);
        sampleIdDescription.setEditable(false);
        sampleIdDescription.setEnabled(false);
        sampleIdDescription.setOpaque(false);
        mainMetadata.add(sampleIdDescription, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- nameLabel ----
        JLabel nameLabel = new JLabel();
        nameLabel.setText(Const.bundle.getString("exportGui.nameLabel.text"));
        nameLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(nameLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- name ----
        name.putClientProperty("JTextField.placeholderText", "control sample 1");
        name.putClientProperty("JTextField.showClearButton", true);
        mainMetadata.add(name, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- nameDescription ----
        JTextArea nameDescription = new JTextArea(2, 1);
        nameDescription.setText(Const.bundle.getString("exportGui.nameDescription.text"));
        nameDescription.setLineWrap(true);
        nameDescription.setWrapStyleWord(true);
        nameDescription.setEditable(false);
        nameDescription.setEnabled(false);
        nameDescription.setOpaque(false);
        mainMetadata.add(nameDescription, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- assayDateLabel ----
        JLabel assayDateTimeLabel = new JLabel();
        assayDateTimeLabel.setText(Const.bundle.getString("exportGui.assayDateTimeLabel.text"));
        assayDateTimeLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(assayDateTimeLabel, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- assayTimestampInput
        timestampInput = TimestampInput.of(assayDatetime);
        mainMetadata.add(timestampInput, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- assayDateTimeDescription ----
        JTextArea assayDateDescription = new JTextArea();
        assayDateDescription.setText(
                Const.bundle.getString("exportGui.assayDateDescription.text"));
        assayDateDescription.setLineWrap(true);
        assayDateDescription.setWrapStyleWord(true);
        assayDateDescription.setEditable(false);
        assayDateDescription.setEnabled(false);
        assayDateDescription.setOpaque(false);
        mainMetadata.add(assayDateDescription, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- tagsLabel ----
        JLabel tagsLabel = new JLabel();
        tagsLabel.setText(Const.bundle.getString("exportGui.tagsLabel.text"));
        tagsLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(tagsLabel, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- tag selection ----
        mainMetadata.add(sampleTags, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- imageHeader ----
        JLabel imageHeader = new JLabel();
        imageHeader.setText(Const.bundle.getString("exportGui.imageHeader.text"));
        imageHeader.putClientProperty("FlatLaf.styleClass", "h2");
        mainMetadata.add(imageHeader, new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 0, 10, 0), 0, 0));

        // ---- imagerLabel ----
        JLabel imagerLabel = new JLabel();
        imagerLabel.setText(Const.bundle.getString("exportGui.imagerLabel.text"));
        imagerLabel.setLabelFor(imager);
        imagerLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(imagerLabel, new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- imager ----
        imager.putClientProperty("JTextField.placeholderText", "Imager");
        imager.putClientProperty("JTextField.showClearButton", true);
        mainMetadata.add(imager, new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- imagerDescription ----
        JTextArea imagerDescription = new JTextArea();
        imagerDescription.setText(Const.bundle.getString("exportGui.imagerDescription.text"));
        imagerDescription.setLineWrap(true);
        imagerDescription.setWrapStyleWord(true);
        imagerDescription.setEditable(false);
        imagerDescription.setEnabled(false);
        imagerDescription.setOpaque(false);
        mainMetadata.add(imagerDescription, new GridBagConstraints(0, 12, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- exposureTimeLabel ----
        JLabel exposureTimeLabel = new JLabel();
        exposureTimeLabel.setText(Const.bundle.getString("exportGui.exposureTimeLabel.text"));
        exposureTimeLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(exposureTimeLabel, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- exposureTime ---
        exposureTime = new SpinnerNumberModel(15, 0, Integer.MAX_VALUE, 10);
        JSpinner exposureTimeSpinner = new JSpinner(exposureTime);
        ((JSpinner.DefaultEditor) exposureTimeSpinner.getEditor())
                .getTextField()
                .setColumns(2);
        mainMetadata.add(exposureTimeSpinner, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- exposureTimeDescription ----
        JTextArea exposureTimeDescription = new JTextArea(3, 1);
        exposureTimeDescription.setText(
                Const.bundle.getString("exportGui.exposureTimeDescription.text"));
        exposureTimeDescription.setLineWrap(true);
        exposureTimeDescription.setWrapStyleWord(true);
        exposureTimeDescription.setEditable(false);
        exposureTimeDescription.setEnabled(false);
        exposureTimeDescription.setOpaque(false);
        mainMetadata.add(exposureTimeDescription, new GridBagConstraints(0, 15, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- pixelBinningLabel ----
        JLabel pixelBinningLabel = new JLabel();
        pixelBinningLabel.setText(Const.bundle.getString("exportGui.pixelBinningLabel.text"));
        pixelBinningLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(pixelBinningLabel, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- pixelBinning
        pixelBinning = new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1);
        JSpinner pixelBinningSpinner = new JSpinner(pixelBinning);
        ((JSpinner.DefaultEditor) pixelBinningSpinner.getEditor())
                .getTextField()
                .setColumns(2);
        mainMetadata.add(pixelBinningSpinner, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- pixelBinningDescription ----
        JTextArea pixelBinningDescription = new JTextArea(3, 1);
        pixelBinningDescription.setText(
                Const.bundle.getString("exportGui.pixelBinningDescription.text"));
        pixelBinningDescription.setLineWrap(true);
        pixelBinningDescription.setWrapStyleWord(true);
        pixelBinningDescription.setEditable(false);
        pixelBinningDescription.setEnabled(false);
        pixelBinningDescription.setOpaque(false);
        mainMetadata.add(pixelBinningDescription, new GridBagConstraints(1, 15, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 0, 0), 0, 0));
        add(mainMetadata, BorderLayout.CENTER);

        // ======== incubations ========
        JPanel incubationPanel = new JPanel();
        incubationPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        incubationPanel.setLayout(new GridBagLayout());
        ((GridBagLayout) incubationPanel.getLayout()).columnWidths = new int[] { 0, 0 };
        ((GridBagLayout) incubationPanel.getLayout()).rowHeights = new int[] { 0, 0, 0 };
        ((GridBagLayout) incubationPanel.getLayout()).columnWeights = new double[] { 1.0, 1.0E-4 };
        ((GridBagLayout) incubationPanel.getLayout()).rowWeights = new double[] {
                0.0, 1.0, 1.0E-4 };

        // ---- incubationsHeader ----
        JLabel incubationsHeader = new JLabel();
        incubationsHeader.setText(Const.bundle.getString("exportGui.incubationsHeader.text"));
        incubationsHeader.putClientProperty("FlatLaf.styleClass", "h2");
        incubationPanel.add(incubationsHeader, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

        // ---- incubationInput
        incubationPanel.add(incubationInput, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        add(incubationPanel, BorderLayout.EAST);

        // ======== actionPanel ========
        JPanel actionPanel = new JPanel();
        actionPanel.setBackground(UIManager.getColor("Menu.background"));
        actionPanel.setLayout(new GridBagLayout());
        ((GridBagLayout) actionPanel.getLayout()).columnWidths = new int[] { 0, 0, 0, 0, 0, 0, 0 };
        ((GridBagLayout) actionPanel.getLayout()).rowHeights = new int[] { 0, 0 };
        ((GridBagLayout) actionPanel.getLayout()).columnWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                1.0E-4 };
        ((GridBagLayout) actionPanel.getLayout()).rowWeights = new double[] { 0.0, 1.0E-4 };

        // ---- showResultTableButton ----
        JButton showResultTableButton = new JButton();
        showResultTableButton.setText(
                Const.bundle.getString("exportGui.showResultTableButton.text"));
        showResultTableButton.addActionListener(e -> handleShowResultTable());
        actionPanel.add(showResultTableButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 0), 0, 0));

        // ---- showAnalysisTableButton ----
        JButton showAnalysisTableButton = new JButton();
        showAnalysisTableButton.setText(
                Const.bundle.getString("exportGui.showAnalysisTableButton.text"));
        showAnalysisTableButton.addActionListener(e -> handleShowAnalysisTable());
        actionPanel.add(showAnalysisTableButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 0), 0, 0));

        // ---- showGriddedImageButton ----
        JButton showGriddedImageButton = new JButton();
        showGriddedImageButton.setText(
                Const.bundle.getString("exportGui.showGriddedImageButton.text"));
        showGriddedImageButton.addActionListener(e -> handleShowGriddedImage());
        actionPanel.add(showGriddedImageButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 5), 0, 0));

        // ---- exportButton ----
        JButton exportButton = new JButton();
        exportButton.setText(
                Const.bundle.getString("exportGui.exportButton.text"));
        exportButton.setBackground(UIManager.getColor("Actions.Green"));
        exportButton.setForeground(SystemColor.text);
        exportButton.addActionListener(e -> handleExportButtonPressed());
        actionPanel.add(exportButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(5, 5, 5, 0), 0, 0));

        // ---- cancelButton ----
        JButton cancelButton = new JButton();
        cancelButton.setText(
                Const.bundle.getString("exportGui.cancelButton.text"));
        cancelButton.setBackground(UIManager.getColor("Actions.Red"));
        cancelButton.setForeground(SystemColor.text);
        cancelButton.addActionListener(e -> closeExportGui());
        actionPanel.add(cancelButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                new Insets(5, 5, 5, 5), 0, 0));
        add(actionPanel, BorderLayout.SOUTH);

        // Load a dummy incubation for the layouting.
        Quantity dummyQty = Quantity.of(100., Unit.PERCENT);
        incubationInput.setIncubations(List.of(
                Incubation.of("solution", dummyQty, dummyQty, Duration.ZERO)));

        pack();

        // Set the preferred size of the incubation input based on the size of the input
        // with one incubation so that the width does not change.
        incubationInput.setPreferredSize(incubationInput.getSize());

        // Load all projects and initialize the "None" project
        loadProject(projectDropdown.getSelectedIndex());

        control.exportGuiOpen(true);
        setResizable(false);
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private void handleShowGriddedImage() {
        BufferedImage img = control.generateGridImage();
        ImageViewer viewer = new ImageViewer();
        viewer.setImage(img, false);
    }

    /**
     * Opens projectGui and handles the changes made by loading
     * the standard project.
     */
    private void openProjectManager() {
        ProjectGui.open(this, control, settings);
        updateProjectDropdownList();
        projectDropdown.setSelectedIndex(settings.getProjectSettings().getLastUsedProjectIndex() + 1);
        // Maybe unneccessary if actionListener behaves like changeListener
        loadProject(projectDropdown.getSelectedIndex());
    }

    private void handleExportButtonPressed() {
        Metadata exportMetadata;
        try {
            // Get the project metadata
            Optional<Project> project = Optional.empty();
            int projIdx = projectDropdown.getSelectedIndex();
            if (projIdx > 0) {
                ProjectExt projectSettings = settings.getProjectSettings()
                        .getProject(projIdx - 1);
                project = Optional.of(Project.fromProjectSettings(projectSettings));
            }
            // Get the patient metadata
            Patient patient = Patient.of(this.sampleId.getText(), this.name.getText(),
                    this.sampleTags.getSelectedTags());
            // Get the image metadata
            Duration exposureTime = Duration.ofSeconds(this.exposureTime.getNumber().intValue());
            int pixBinning = this.pixelBinning.getNumber().intValue();
            ZonedDateTime created = timestampInput.getDateTime();
            Image image = Image.of(created, this.imager.getText(), pixBinning, exposureTime);
            // Get the incubations
            List<Incubation> incubations = this.incubationInput.getMetadata();
            // Store the entered metadata
            exportMetadata = Metadata.of(datetime, project,
                    patient, image, incubations);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    Const.bundle.getString("messageDialog.invalidInput.text") + " "
                            + e.getMessage(),
                    Const.bundle.getString("messageDialog.invalidInput.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        control.export(exportMetadata, parameters, data, dataStatistics);
        closeExportGui();
    }

    private void handleShowResultTable() {
        GenericTable table = new GenericTable();

        // Create columns
        IntColumn spotfieldColumn = new IntColumn("Spotfield");
        IntColumn rowColumn = new IntColumn("Row");
        IntColumn columnColumn = new IntColumn("Column");
        DoubleColumn minColumn = new DoubleColumn("Min");
        DoubleColumn maxColumn = new DoubleColumn("Max");
        DoubleColumn meanColumn = new DoubleColumn("Mean");
        DoubleColumn stdDevColumn = new DoubleColumn("Standard Deviation");
        DoubleColumn meanMinusMinColumn = new DoubleColumn("Mean minus min");
        DoubleColumn iNormColumn = new DoubleColumn("Internally normalized");

        for (Datapoint datapoint : data.getValues()) {
            Measurepoint measurePoint = datapoint.getMeasurePoint();
            spotfieldColumn.add(measurePoint.getSpot());
            rowColumn.add(measurePoint.getRow());
            columnColumn.add(measurePoint.getCol());
            minColumn.add(measurePoint.getMin());
            maxColumn.add(measurePoint.getMax());
            meanColumn.add(measurePoint.getMean());
            stdDevColumn.add(measurePoint.getStdDev());
            meanMinusMinColumn.add(datapoint.getMeanMinusMin());
            iNormColumn.add(datapoint.getNormalizedMean());
        }

        // Add the created columns to the table
        table.add(spotfieldColumn);
        table.add(rowColumn);
        table.add(columnColumn);
        table.add(minColumn);
        table.add(maxColumn);
        table.add(meanColumn);
        table.add(stdDevColumn);
        table.add(meanMinusMinColumn);
        table.add(iNormColumn);

        // Show the table
        table.setVisible(true);
    }

    public void loadProject(int projectIndex) {
        ProjectSettings projectSettings = settings.getProjectSettings();
        if (projectIndex == 0) {
            sampleTags.clearTags();
            incubationInput.emptyIncubationInput();
            imager.setText(null);
            pixelBinning.setValue(1);
            exposureTime.setValue(10);
            projectSettings.setLastUsedProjectIndex(-1);
        } else {
            int settingsProjIdx = projectIndex - 1;
            ProjectExt project = projectSettings.getProject(settingsProjIdx);
            sampleTags.setTags(project.getTags());
            incubationInput.setIncubations(project.getIncubations());
            imager.setText(project.getImageDefaults().getImager());
            pixelBinning.setValue(project.getImageDefaults().getPixelBinning());
            // Duration can't be used as value for the spinner model, but long seems to work
            exposureTime.setValue(project.getImageDefaults().getExposureTime().getSeconds());
            projectSettings.setLastUsedProjectIndex(settingsProjIdx);
        }

        settings.store();
        try {
            settings.save();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not save settings", e);
            String msg = String.format("We could not save the settings you made: %s", e);
            GuiUtils.showErrorDialog(this, msg, "Unable to save settings");
        }
    }

    private void handleShowAnalysisTable() {
        GenericTable table = new GenericTable();

        IntColumn rowColumn = new IntColumn("Row");
        IntColumn columnColumn = new IntColumn("Column");

        DoubleColumn rawAvg = new DoubleColumn("Average raw signal");
        DoubleColumn stdDevRawAvg = new DoubleColumn("Raw standard deviation");
        DoubleColumn relStdDevRawAvg = new DoubleColumn("Raw standard deviation in [%]");

        DoubleColumn avgNormalized = new DoubleColumn("Normalized");
        DoubleColumn stdDevNormalized = new DoubleColumn("Norm standard deviation");
        DoubleColumn relStdDevNormalized = new DoubleColumn("Norm standard deviation in [%]");

        for (DatapointStatistics spot : dataStatistics.getSpotStatistics()) {
            rowColumn.add(spot.getRow());
            columnColumn.add(spot.getColumn());
            rawAvg.add(spot.getRawAvg());
            stdDevRawAvg.add(spot.getStdDevRawAvg());
            relStdDevRawAvg.add(spot.getRelStdDevRawAvg());

            avgNormalized.add(spot.getRawAvgNormalized());
            stdDevNormalized.add(spot.getStdDevNormAvg());
            relStdDevNormalized.add(spot.getRelStdDevNormAvg());
        }

        table.add(rowColumn);
        table.add(columnColumn);

        table.add(rawAvg);
        table.add(stdDevRawAvg);
        table.add(relStdDevRawAvg);

        table.add(avgNormalized);
        table.add(stdDevNormalized);
        table.add(relStdDevNormalized);

        table.setVisible(true);
    }

    /**
     * Updates the project-dropdown menu.
     * Used in initialization and after using the project manager.
     */
    private void updateProjectDropdownList() {
        List<String> nameList = settings.getProjectSettings().getNameList();
        nameList.add(0, "None");
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addAll(nameList);
        projectDropdown.setModel(model);
    }

    private void closeExportGui() {
        control.exportGuiOpen(false);
        dispose();
    }
}

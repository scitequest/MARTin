package com.scitequest.martin.view;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.scitequest.martin.Const;
import com.scitequest.martin.export.Incubation;
import com.scitequest.martin.export.JsonParseException;
import com.scitequest.martin.export.Project;
import com.scitequest.martin.export.Quantity;
import com.scitequest.martin.export.Quantity.Unit;
import com.scitequest.martin.settings.ImageDefaults;
import com.scitequest.martin.settings.ProjectExt;
import com.scitequest.martin.settings.ProjectSettings;
import com.scitequest.martin.settings.Settings;
import com.scitequest.martin.settings.exception.DuplicateElementException;

/**
 * This class generates a GUI that is able to generate, alter and delete
 * mProjects.
 */
public final class ProjectGui extends JDialog implements ListDialog {

    /** The logger for this class. */
    private static final Logger log = Logger.getLogger("com.scitequest.martin.view.OptionsGui");

    /** Access to the control instance to issue commands. */
    private final Controlable control;
    /** Access to dialog owner to make it visible if ProjectGuiCloses */
    private final Dialog owner;
    /**
     * Link between ProjectGui and exportGui.
     * Stores all Projects and handles load and save.
     */
    private final Settings settings;
    /** Shorthand for {@code settings.getProjectSettings()}. */
    private final ProjectSettings projectSettings;

    /**
     * InteractiveList element that is used to load
     * and save projects.
     * In effect representing an instance of ProjectSettings.
     */
    private final InteractiveList interactiveList;
    private final JTextField projectName = new JTextField();
    /**
     * TextArea for entry of project description.
     */
    private final JTextArea projectDescription = new JTextArea();
    /**
     * Inpiut field for Project tags.
     */
    private final TagInput tags = TagInput.empty();
    /**
     * Dynamically interactable list of incubations.
     */
    private final IncubationInput incubationInput = new IncubationInput();
    /**
     * Input field for the name of the used imager.
     */
    private final JTextField imager = new JTextField();
    /**
     * Spinner to set the exposure time the image was taken.
     */
    private final JSpinner exposureTime;
    /**
     * Spinner to set the pixel binning the image was taken.
     */
    private final JSpinner pixelBinning;
    /**
     * This button applies changes that were made in this GUI.
     * This includes the generation of new projects.
     */
    private final JButton applyButton = new JButton();
    /**
     * This button closes the window.
     */
    private final JButton closeButton = new JButton();

    /**
     * Set to true while a project is validating.
     *
     * <p>
     * This is used to <i>not</i> replace the selected projects name with the text
     * field input. This mitigates the case if there are three projects. The name
     * text
     * field is set to the name of project B and then switched to project C. Without
     * this
     * flag we would briefly replace the name of project C with the text field input
     * from project A during the time the warning dialog is displayed.
     * </p>
     *
     * It is not recommended to use this flag for other cases.
     */
    private boolean validating = false;

    /**
     * Constructor of ProjectGUI.
     */
    private ProjectGui(Dialog owner, Controlable control, Settings settings) {
        super(owner, "Project Manager", true);

        this.owner = owner;
        this.control = control;
        this.settings = settings;
        this.projectSettings = settings.getProjectSettings();

        setLayout(new BorderLayout());

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCloseButtonPressed();
            }
        });

        // ======== projectList ========
        interactiveList = new InteractiveList(this);
        add(interactiveList, BorderLayout.WEST);

        // ======== mainMetadata ========
        JPanel mainMetadata = new JPanel();
        mainMetadata.setBorder(new EmptyBorder(5, 5, 5, 5));
        mainMetadata.setLayout(new GridBagLayout());
        ((GridBagLayout) mainMetadata.getLayout()).columnWidths = new int[] {
                205, 200, 0 };
        ((GridBagLayout) mainMetadata.getLayout()).rowHeights = new int[] {
                0, 0, 0, 0, 85, 0, 0, 125, 0, 0, 0,
                0, 0, 0, 0, };
        ((GridBagLayout) mainMetadata.getLayout()).columnWeights = new double[] {
                1.0, 1.0, 1.0E-4 };
        ((GridBagLayout) mainMetadata.getLayout()).rowWeights = new double[] {
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4 };

        // ---- projectDescriptionHeader ----
        JLabel projectDescriptionHeader = new JLabel();
        projectDescriptionHeader.setText(
                Const.bundle.getString("projectManager.projectDescriptionHeader.text"));
        projectDescriptionHeader.putClientProperty("FlatLaf.styleClass", "h2");
        mainMetadata.add(projectDescriptionHeader, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 10, 0), 0, 0));

        // ---- projectNameLabel ----
        JLabel projectNameLabel = new JLabel();
        projectNameLabel.setText(Const.bundle.getString("projectManager.projectNameLabel.text"));
        projectNameLabel.putClientProperty("FlatLaf.styleClass", "h4");
        projectNameLabel.setForeground(UIManager.getColor("Actions.Blue"));
        mainMetadata.add(projectNameLabel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        String specialCharsProjectName = "()._";
        projectName.setDocument(new InputLimitations(30,
                true, true, true, specialCharsProjectName));
        projectName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                interactiveList.update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                interactiveList.update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                interactiveList.update();
            }
        });
        mainMetadata.add(projectName, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- projectDescriptionLabel ----
        JLabel projectDescriptionLabel = new JLabel();
        projectDescriptionLabel.setText(
                Const.bundle.getString("projectManager.projectDescriptionLabel.text"));
        projectDescriptionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        projectDescriptionLabel.setForeground(UIManager.getColor("Actions.Blue"));
        mainMetadata.add(projectDescriptionLabel, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- projectDescription ----
        projectDescription.setLineWrap(true);
        String specialCharsDescription = ",._+-?!";
        projectDescription.setDocument(new InputLimitations(500,
                true, true, true, specialCharsDescription));

        // ======== projectDescriptionScrollPane ========
        JScrollPane projectDescriptionScrollPane = new JScrollPane();
        projectDescriptionScrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        projectDescriptionScrollPane.setViewportView(projectDescription);
        mainMetadata.add(projectDescriptionScrollPane, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- defaultMetadataHeader ----
        JLabel defaultMetadataHeader = new JLabel();
        defaultMetadataHeader.setText(
                Const.bundle.getString("projectManager.defaultMetadataHeader.text"));
        defaultMetadataHeader.putClientProperty("FlatLaf.styleClass", "h2");
        mainMetadata.add(defaultMetadataHeader, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 0, 10, 0), 0, 0));

        // ---- tagsLabel ----
        JLabel tagsLabel = new JLabel();
        tagsLabel.setText(Const.bundle.getString("projectManager.tagsLabel.text"));
        tagsLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(tagsLabel, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- tags ----
        mainMetadata.add(tags, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- imagerLabel ----
        JLabel imagerLabel = new JLabel();
        imagerLabel.setText(Const.bundle.getString("projectManager.imagerLabel.text"));
        imagerLabel.setLabelFor(imager);
        imagerLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(imagerLabel, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- imager ----
        imager.putClientProperty("JTextField.placeholderText", "Imager");
        imager.putClientProperty("JTextField.showClearButton", true);
        mainMetadata.add(imager, new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- imagerDescription ----
        JTextArea imagerDescription = new JTextArea(2, 1);
        imagerDescription.setOpaque(false);
        imagerDescription.setText(
                Const.bundle.getString("projectManager.imagerDescription.text"));
        imagerDescription.setLineWrap(true);
        imagerDescription.setEditable(false);
        imagerDescription.setEnabled(false);
        imagerDescription.setWrapStyleWord(true);
        mainMetadata.add(imagerDescription, new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- exposureTimeLabel ----
        JLabel exposureTimeLabel = new JLabel();
        exposureTimeLabel.setText(
                Const.bundle.getString("projectManager.exposureTimeLabel.text"));
        exposureTimeLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(exposureTimeLabel, new GridBagConstraints(0, 11, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        exposureTime = new JSpinner(new SpinnerNumberModel(15, 0, Integer.MAX_VALUE, 5));
        ((JSpinner.DefaultEditor) exposureTime.getEditor())
                .getTextField()
                .setColumns(2);
        mainMetadata.add(exposureTime, new GridBagConstraints(0, 12, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- exposureTimeDescription ----
        JTextArea exposureTimeDescription = new JTextArea(3, 1);
        exposureTimeDescription.setOpaque(false);
        exposureTimeDescription.setText(
                Const.bundle.getString("projectManager.exposureTimeDescription.text"));
        exposureTimeDescription.setLineWrap(true);
        exposureTimeDescription.setEditable(false);
        exposureTimeDescription.setEnabled(false);
        exposureTimeDescription.setWrapStyleWord(true);
        mainMetadata.add(exposureTimeDescription, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, -5, 5, 0), 0, 0));

        // ---- pixelBinningLabel ----
        JLabel pixelBinningLabel = new JLabel();
        pixelBinningLabel.setText(
                Const.bundle.getString("projectManager.pixelBinningLabel.text"));
        pixelBinningLabel.putClientProperty("FlatLaf.styleClass", "h4");
        mainMetadata.add(pixelBinningLabel, new GridBagConstraints(1, 11, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));
        pixelBinning = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) pixelBinning.getEditor())
                .getTextField()
                .setColumns(2);
        mainMetadata.add(pixelBinning, new GridBagConstraints(1, 12, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- pixelBinningDescription ----
        JTextArea pixelBinningDescription = new JTextArea(3, 1);
        pixelBinningDescription.setOpaque(false);
        pixelBinningDescription.setText(
                Const.bundle.getString("projectManager.pixelBinningDescription.text"));
        pixelBinningDescription.setLineWrap(true);
        pixelBinningDescription.setEditable(false);
        pixelBinningDescription.setEnabled(false);
        pixelBinningDescription.setWrapStyleWord(true);
        mainMetadata.add(pixelBinningDescription, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0,
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
        incubationsHeader.setText(Const.bundle.getString("projectManager.incubationsHeader.text"));
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
        actionPanel.setBackground(UIManager.getColor("MenuBar.background"));
        actionPanel.setLayout(new FlowLayout());

        // ---- applyButton ----
        applyButton.setText(
                Const.bundle.getString("projectManager.applyButton.text"));
        applyButton.setBackground(UIManager.getColor("Actions.Green"));
        applyButton.setForeground(SystemColor.text);
        applyButton.addActionListener(e -> handleSaveButtonPressed());
        actionPanel.add(applyButton);

        // ---- closeButton ----
        closeButton.setText(
                Const.bundle.getString("projectManager.closeButton.text"));
        closeButton.setBackground(UIManager.getColor("Actions.Red"));
        closeButton.setForeground(SystemColor.text);
        closeButton.addActionListener(e -> handleCloseButtonPressed());
        actionPanel.add(closeButton);
        add(actionPanel, BorderLayout.SOUTH);

        // Load a dummy incubation for the layouting.
        Quantity dummyQty = Quantity.of(100., Unit.PERCENT);
        incubationInput.setIncubations(List.of(
                Incubation.of("solution", dummyQty, dummyQty, Duration.ZERO)));

        pack();

        // Set the preferred size of the incubation input based on the size of the input
        // with one incubation so that the width does not change.
        incubationInput.setPreferredSize(incubationInput.getSize());

        // This prevents no selection on Startup of ProjectGui
        int lastUsedIndex = projectSettings.getLastUsedProjectIndex();
        if (lastUsedIndex > 0) {
            interactiveList.select(lastUsedIndex);
        } else {
            interactiveList.select(0);
        }

        // Focus the project name text field at the start
        projectName.requestFocusInWindow();

        setResizable(false);
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    /**
     * Opens a new project GUI dialog relative to the specified owner dialog.
     *
     * @param owner    the owner dialog
     * @param control  the reference to control
     * @param settings the reference to the current project settings
     * @return the new project GUI dialog object
     */
    public static ProjectGui open(Dialog owner, Controlable control, Settings settings) {
        return new ProjectGui(owner, control, settings);
    }

    private void handleCloseButtonPressed() {
        /*
         * This if clause prevents automatically discarding if the current input is
         * invalid. As such the user does not accidentally lose any of his progress.
         */
        if (validateAndStoreInputsInSettings(interactiveList.getSelectedIndex())) {
            if (!settings.isDirty()) {
                owner.setVisible(true);
                dispose();
                return;
            }
        }
        int option = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to discard all changes made?",
                "Discard changes?",
                JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            settings.abort();
            owner.setVisible(true);
            dispose();
        }
    }

    private void handleSaveButtonPressed() {
        // Ask the user if he really wants to save
        int option = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to save all changes?",
                "",
                JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            // Store the current edits beforehand
            if (!validateAndStoreInputsInSettings(interactiveList.getSelectedIndex())) {
                // Inputs were invalid, dialogs are created by the validation so we exit
                return;
            }
            // Save all changes
            settings.store();
            try {
                settings.save();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Could not save settings", e);
                String msg = String.format("We could not save the settings you made: %s", e);
                GuiUtils.showErrorDialog(this, msg, "Unable to save settings");
                return;
            }
        }
    }

    /**
     * Validates the current inputs and returns a project, if all entries are valid.
     *
     * This shows an error message, if inputs are not valid informing the user, what
     * is wrong.
     *
     * @return the validated project or empty if invalid
     */
    private Optional<ProjectExt> validateInputs() {
        ProjectExt proj;
        try {
            proj = getProjectFromInputsUnchecked();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    Const.bundle.getString("messageDialog.invalidInput.text") + " "
                            + e.getMessage(),
                    Const.bundle.getString("messageDialog.invalidInput.title"),
                    JOptionPane.ERROR_MESSAGE);
            return Optional.empty();
        }
        return Optional.of(proj);
    }

    /**
     * Returns a project from the currently entered values.
     *
     * This method is unchecked and will throw an {@code IllegalArgumentException}
     * if the input is invalid.
     *
     * @return a project based on the input
     * @throws IllegalArgumentException if a project could not be created
     */
    private ProjectExt getProjectFromInputsUnchecked() throws IllegalArgumentException {
        String name = projectName.getText().trim();
        String description = projectDescription.getText().trim();
        // Only used to verify if the project itself is valid
        Project.of(name, description);
        Set<String> projectTags = tags.getTags();
        List<Incubation> incubationMetadata = incubationInput.getMetadata();
        ImageDefaults imageMetadata = ImageDefaults.of(
                imager.getText().trim(),
                (int) pixelBinning.getValue(),
                Duration.ofSeconds((int) exposureTime.getValue()));

        ProjectExt proj = ProjectExt.of(name, description, projectTags,
                incubationMetadata, imageMetadata);
        return proj;
    }

    /**
     * Validates the current inputs and returns an instance of slideMaskParameters,
     * if all entries are valid.
     *
     * This shows an error message, if inputs are not valid informing the user, what
     * is wrong.
     *
     * @return the validated slideMaskParameters or empty if invalid
     */
    private Optional<ProjectExt> validateInputsInternal() {
        ProjectExt pParams;
        try {
            pParams = getProjectFromInputsUnchecked();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this,
                    Const.bundle.getString("messageDialog.invalidInput.text") + " "
                            + e.getMessage(),
                    Const.bundle.getString("messageDialog.invalidInput.title"),
                    JOptionPane.ERROR_MESSAGE);
            return Optional.empty();
        }
        return Optional.of(pParams);
    }

    /*
     * Here as well.
     * Is this a code duplication?
     */
    private boolean validateAndStoreInputsInSettings(int index) {
        Optional<ProjectExt> maybeProject = validateInputsInternal();
        if (maybeProject.isEmpty()) {
            return false;
        }
        ProjectExt project = maybeProject.get();
        try {
            projectSettings.setProject(index, project);
        } catch (DuplicateElementException e) {
            JOptionPane.showMessageDialog(this,
                    Const.bundle.getString("messageDialog.copyDuplicatesNotAllowed.text"),
                    Const.bundle.getString("messageDialog.copyDuplicatesNotAllowed.title"),
                    JOptionPane.WARNING_MESSAGE);
            // This allows us to store information even when theres been a duplicate name
            ProjectExt helperProject = project.withName(projectSettings.getProject(index).getName());
            projectSettings.setProject(index, helperProject);
            return false;
        }
        return true;
    }

    @Override
    public void populateInputs(int index) {
        log.finer(String.format("Populating mask inputs with data from index %d", index));
        ProjectExt project = projectSettings.getProject(index);

        projectName.setText(project.getName());
        projectDescription.setText(project.getDescription());
        tags.setTags(project.getTags());
        incubationInput.setIncubations(project.getIncubations());

        ImageDefaults imgDefaults = project.getImageDefaults();
        imager.setText(imgDefaults.getImager());
        exposureTime.setValue((int) imgDefaults.getExposureTime().getSeconds());
        pixelBinning.setValue(imgDefaults.getPixelBinning());
    }

    @Override
    public boolean validateInputs(int index) {
        log.finer(String.format("Validating project with index %d", index));
        validating = true;
        boolean validationResult = validateAndStoreInputsInSettings(index);
        validating = false;
        return validationResult;
    }

    @Override
    public void handleExport(int index, Path path) {
        log.finer(String.format("Exporting project with index %d as path %s", index, path));
        Optional<ProjectExt> maybeProject = validateInputsInternal();

        if (!maybeProject.isEmpty()) {
            control.exportProject(maybeProject.get(), path);
        }
    }

    @Override
    public void handleImport(Path path) {
        log.finer(String.format("Importing a mask from path", path));

        try {
            control.importProject(path);
        } catch (IOException | JsonParseException e) {
            String msg = "Could not fully load project file";
            log.log(Level.SEVERE, msg, e);
            GuiUtils.showWarningDialog(this, String.format(
                    "%s: %s%nThe project could not be imported", msg, e.getMessage()), null);
            return;
        }

        log.info(String.format("Project file '%s' has been imported", path));
    }

    @Override
    public void deleteIndex(int index) {
        log.finer(String.format("Deleting project with index %d", index));
        projectSettings.removeProject(index);
    }

    @Override
    public void addPlaceholder() {
        log.finer("Adding placeholder project");
        projectSettings.addProject(ProjectExt.placeholder());
    }

    @Override
    public void addCopyOfElement(int index, String name) {
        log.finer(String.format("Copying project with index %d and new name %s", index, name));
        ProjectExt maskCopy = projectSettings.getProject(index).withName(name);
        projectSettings.addProject(maskCopy);
    }

    @Override
    public List<String> getNameList() {
        var nameList = projectSettings.getNameList();
        int index = interactiveList.getSelectedIndex();
        String projectNameText = projectName.getText().trim();
        if (index >= 0 && index < nameList.size()
                && !projectNameText.isEmpty()
                && !validating) {
            nameList.set(index, projectNameText);
        }
        return nameList;
    }

    @Override
    public String getDefaultName() {
        return ProjectExt.DEFAULT_NAME;
    }
}

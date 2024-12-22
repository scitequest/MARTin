package com.scitequest.martin.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.MouseInputListener;

import com.scitequest.martin.Const;
import com.scitequest.martin.GitInfo;
import com.scitequest.martin.Image;
import com.scitequest.martin.Version;
import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.DataStatistics;
import com.scitequest.martin.export.Parameters;
import com.scitequest.martin.settings.Settings;
import com.scitequest.martin.utils.SystemUtils;
import com.scitequest.martin.view.IntegrityCheckResult.IntegrityCheckContext;
import com.scitequest.martin.view.IntegrityCheckResult.IntegrityCheckError;

/**
 * This GUI is the main control unit for the user.
 *
 * It is able to initiate functions of control as well as open the optionsGUI.
 */
public final class Gui extends JFrame implements View, MouseInputListener {

    /** The logger for this class. */
    private static final Logger log = Logger.getLogger("com.scitequest.martin.view.Gui");

    private final Controlable control;
    private final Settings settings;

    private final JEditorPane helpText = new JEditorPane();
    private final JButton prevTipButton = new JButton();
    private final JButton nextTipButton = new JButton();
    private final JButton settingsButton = new JButton();
    private final JButton openImageButton = new JButton();
    private final JButton toggleFilterButton = new JButton();
    private final JButton autofitButton = new JButton();
    private final JButton measureButton = new JButton();
    private final JButton checkIntegrityButton = new JButton();
    private final JSpinner angleSpinner;
    private final JSpinner xPositionSpinner;
    private final JSpinner yPositionSpinner;

    private Optional<SlideViewer> slideViewer = Optional.empty();
    private boolean otherGuiOpened = false;
    private int noHoverMessageIndex = 0;
    private boolean activeListener = true;

    private Version version;
    private GitInfo gitInfo;

    /**
     * Constructor of this class.
     *
     * @param control   bidirectional association with control
     * @param resources collection of relevant parameters
     * @param version   version-number of current program iteration
     * @param gitInfo   the Git information such as commit ID
     */
    public Gui(Controlable control, Settings resources, Version version, GitInfo gitInfo) {
        this.control = control;
        this.settings = resources;

        // ======== this ========
        setTitle("MARTin");
        setResizable(false);

        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);

        JMenu helpMenu = new JMenu("?");
        menuBar.add(helpMenu);

        JMenuItem aboutButton = new JMenuItem("About");
        aboutButton.addActionListener(e -> openAboutDialog());
        helpMenu.add(aboutButton);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                control.exit();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }
        });

        JPanel content = new JPanel();
        content.setBorder(new EmptyBorder(5, 5, 5, 5));
        content.setLayout(new GridBagLayout());
        ((GridBagLayout) content.getLayout()).columnWidths = new int[] { 0, 0, 0 };
        ((GridBagLayout) content.getLayout()).rowHeights = new int[] {
                0, 0, 155, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, };
        ((GridBagLayout) content.getLayout()).columnWeights = new double[] { 1.0, 1.0, 1.0E-4 };
        ((GridBagLayout) content.getLayout()).rowWeights = new double[] {
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                1.0E-4 };

        // ---- header ----
        JLabel header = new JLabel();
        header.setText(Const.bundle.getString("mainGui.header.text"));
        header.putClientProperty("FlatLaf.styleClass", "h1");
        content.add(header, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ======== infoPanel ========
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(0, 1));

        // ---- versionText ----
        this.version = version;
        JLabel versionText = new JLabel();
        versionText.setText("Version: " + version);
        versionText.setFont(new Font("sansserif", Font.BOLD, 14));
        infoPanel.add(versionText);

        // ---- gitCommitText ----
        this.gitInfo = gitInfo;
        JLabel gitCommitText = new JLabel();
        gitCommitText.setText("Git commit ID: " + gitInfo);
        infoPanel.add(gitCommitText);
        content.add(infoPanel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ======== helpScrollPane ========

        // ---- helpText ----
        helpText.setContentType("text/html");
        helpText.setEditable(false);
        helpText.setEnabled(false);
        helpText.setText(Const.bundle.getString("infoText.startupMessageNoImage.text"));
        JScrollPane helpScrollPane = new JScrollPane();
        helpScrollPane.setViewportView(helpText);
        content.add(helpScrollPane, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ======== helpNavigationPanel ========
        JPanel helpNavigationPanel = new JPanel();
        helpNavigationPanel.setLayout(new GridLayout(1, 0, 5, 0));

        // ---- prevTipButton ----
        prevTipButton.setText(Const.bundle.getString("mainGui.prevTipButton.text"));
        prevTipButton.addActionListener(e -> rotateThroughInfoText(-1));
        helpNavigationPanel.add(prevTipButton);

        // ---- nextTipButton ----
        nextTipButton.setText(Const.bundle.getString("mainGui.nextTipButton.text"));
        nextTipButton.addActionListener(e -> rotateThroughInfoText(1));
        helpNavigationPanel.add(nextTipButton);
        content.add(helpNavigationPanel, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- settingsButton ----
        settingsButton.setText(Const.bundle.getString("mainGui.settingsButton.text"));
        settingsButton.addActionListener(e -> optionsGuiOpened());
        settingsButton.addMouseListener(this);
        content.add(settingsButton, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 5));

        // ---- openImageButton ----
        openImageButton.setText(Const.bundle.getString("mainGui.openImageButton.text"));
        openImageButton.addActionListener(e -> control.openImage());
        openImageButton.addMouseListener(this);
        content.add(openImageButton, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 5));

        // ---- toggleFilterButton ----
        toggleFilterButton.setText(Const.bundle.getString("mainGui.toggleFilterButton.text"));
        toggleFilterButton.addActionListener(e -> control.toggleFilter());
        toggleFilterButton.addMouseListener(this);
        content.add(toggleFilterButton, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 5));

        // ---- autofitButton ----
        autofitButton.setText(Const.bundle.getString("mainGui.autofitButton.text"));
        autofitButton.addActionListener(e -> control.measureFieldFit());
        autofitButton.addMouseListener(this);
        content.add(autofitButton, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 5));

        // ---- measureButton ----
        measureButton.setText(Const.bundle.getString("mainGui.measureButton.text"));
        measureButton.addActionListener(e -> control.measure());
        measureButton.addMouseListener(this);
        content.add(measureButton, new GridBagConstraints(0, 8, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 5));

        // ---- validateButton ----
        checkIntegrityButton.setText(Const.bundle.getString("mainGui.integrityCheckButton.text"));
        checkIntegrityButton.addActionListener(e -> handleCheckIntegrity());
        checkIntegrityButton.addMouseListener(this);
        content.add(checkIntegrityButton, new GridBagConstraints(0, 9, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 5));

        // ---- maskPositionHeader ----
        JLabel maskPositionHeader = new JLabel();
        maskPositionHeader.setText(Const.bundle.getString("mainGui.maskPositionHeader.text"));
        maskPositionHeader.putClientProperty("FlatLaf.styleClass", "h2");
        content.add(maskPositionHeader, new GridBagConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(10, 0, 5, 0), 0, 0));

        // ---- angleLabel ----
        JLabel angleLabel = new JLabel();
        angleLabel.setText(Const.bundle.getString("mainGui.angleLabel.text"));
        angleLabel.putClientProperty("FlatLaf.styleClass", "h4");
        content.add(angleLabel, new GridBagConstraints(0, 11, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- angleSpinner ----
        angleSpinner = new JSpinner(new SpinnerNumberModel(0., -359., 361., 0.1));
        angleSpinner.addChangeListener(l -> handleAngleChanged());
        angleSpinner.addMouseListener(this);
        content.add(angleSpinner, new GridBagConstraints(0, 12, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- xPositionLabel ----
        JLabel xPositionLabel = new JLabel();
        xPositionLabel.setText(Const.bundle.getString("mainGui.xPositionLabel.text"));
        xPositionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        content.add(xPositionLabel, new GridBagConstraints(0, 13, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- yPositionLabel ----
        JLabel yPositionLabel = new JLabel();
        yPositionLabel.setText(Const.bundle.getString("mainGui.yPositionLabel.text"));
        yPositionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        content.add(yPositionLabel, new GridBagConstraints(1, 13, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));

        // ---- xPositionSpinner ----
        xPositionSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        xPositionSpinner.addChangeListener(l -> handlePositionChanged());
        xPositionSpinner.addMouseListener(this);
        ((JSpinner.DefaultEditor) xPositionSpinner.getEditor())
                .getTextField()
                .setColumns(6);
        content.add(xPositionSpinner, new GridBagConstraints(0, 14, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        // ---- xPositionSpinner ----
        yPositionSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        yPositionSpinner.addChangeListener(l -> handlePositionChanged());
        yPositionSpinner.addMouseListener(this);
        ((JSpinner.DefaultEditor) yPositionSpinner.getEditor())
                .getTextField()
                .setColumns(6);
        content.add(yPositionSpinner, new GridBagConstraints(1, 14, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 0), 0, 0));
        add(content);

        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                SwingUtilities.invokeLater(() -> resetComponentProperties());
            }
        });
        resetComponentProperties();

        setVisible(true);
        stateNoImageOpened();
        openImageButton.requestFocusInWindow();

        setCanvasInteractible(true);
    }

    /**
     * Opens the About-Dialog.
     * This Dialog contains information about:
     * - The creators of MARTin
     * - Version number
     * - Licensing
     * - Contact-info for errors and such
     */
    private void openAboutDialog() {
        String aboutDialogTitle = Const.bundle.getString("aboutDialog.title");
        JDialog aboutDialog = new JDialog(this, aboutDialogTitle, true);
        aboutDialog.setLayout(new BorderLayout());
        aboutDialog.setResizable(false);

        String aboutText = String.format(Const.bundle.getString("aboutDialog.aboutTextLabel.text"),
                version, gitInfo);
        JLabel aboutTextLabel = new JLabel(aboutText, SwingConstants.CENTER);
        aboutTextLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        aboutDialog.add(aboutTextLabel, BorderLayout.CENTER);

        String licenseInfoDialogTitle = Const.bundle.getString(
                "aboutDialog.licenseInfoDialog.title");
        JDialog licenseInfoDialog = new JDialog(this, licenseInfoDialogTitle, true);
        JTextPane licenseTextPane = new JTextPane();
        try {
            Path licenseInfoPath = SystemUtils.getAppContentPath()
                    .resolve("html")
                    .resolve("LICENSE_3RD_PARTY.html");
            String licenseTextHtml = Files.readString(licenseInfoPath, StandardCharsets.UTF_8);
            licenseTextPane.setContentType("text/html");
            licenseTextPane.setText(licenseTextHtml);
        } catch (IOException e) {
            log.log(Level.WARNING, "Failed to load license information file", e);
            licenseTextPane.setContentType("text/plain");
            licenseTextPane.setText("Error loading license information: " + e);
        }
        licenseTextPane.setEditable(false);
        // Scroll to top
        licenseTextPane.setCaretPosition(0);
        // Set up the HyperlinkListener to handle hyperlink clicks
        licenseTextPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                openInBrowser(e.getURL());
            }
        });

        JScrollPane licenseScrollPane = new JScrollPane(licenseTextPane);
        licenseScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        licenseScrollPane.setPreferredSize(new Dimension(600, 800));
        licenseScrollPane.setMinimumSize(new Dimension(300, 400));
        licenseScrollPane.setBorder(null);
        licenseInfoDialog.add(licenseScrollPane);
        licenseInfoDialog.pack();

        JButton showLicenseInfoButton = new JButton(
                Const.bundle.getString("aboutDialog.showLicenseInfoButton.text"));
        showLicenseInfoButton.addActionListener(l -> licenseInfoDialog.setVisible(true));
        JPanel showLicenseInfoButtonPanel = new JPanel();
        showLicenseInfoButtonPanel.add(showLicenseInfoButton);
        aboutDialog.add(showLicenseInfoButtonPanel, BorderLayout.SOUTH);

        aboutDialog.pack();
        aboutDialog.setLocationRelativeTo(null); // Centers on screen

        aboutDialog.setVisible(true);
    }

    private static void openInBrowser(URL url) {
        if (Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(url.toURI());
            } catch (IOException | URISyntaxException e) {
                // Simply do nothing
            }
        }
    }

    private void rotateThroughInfoText(int i) {
        if (control.isImageLoaded()) {

            String[] guideLabels = {
                    Const.bundle.getString("infoText.noHoverGeneralPositioning.text"),
                    Const.bundle.getString("infoText.noHoverSingleElements.text"),
                    Const.bundle.getString("infoText.noHoverDelRects.text"),
                    Const.bundle.getString("infoText.noHoverInvertLut.text"),
                    Const.bundle.getString("infoText.noHoverZooming.text"),
                    Const.bundle.getString("infoText.noHoverMoveWholeImage.text"),
                    Const.bundle.getString("infoText.noHoverRGBinput.text"),
                    Const.bundle.getString("infoText.noHover.keyShortcut.reset.text"),
                    Const.bundle.getString("infoText.noHover.keyShortcut.autofit.text"),
                    Const.bundle.getString("infoText.noHover.keyShortcut.filter.text"),
                    Const.bundle.getString("infoText.noHover.keyShortcut.measure.text")
            };
            noHoverMessageIndex = (noHoverMessageIndex + i) % guideLabels.length;
            // Wrap around
            noHoverMessageIndex = noHoverMessageIndex < 0
                    ? (guideLabels.length - 1)
                    : noHoverMessageIndex;
            helpText.setText(guideLabels[noHoverMessageIndex]);
        } else {
            helpText.setText(Const.bundle.getString("infoText.noImage.text"));
        }
    }

    /**
     * Sets the value of the angle spinner.
     *
     * @param angle new value of angle spinner
     */
    public void setAngleSpinner(double angle) {
        angleSpinner.setValue(angle);
    }

    /**
     * Sets the value of the x position spinner.
     *
     * @param x new value of x position spinner
     */
    public void setxPositionSpinner(int x) {
        xPositionSpinner.setValue(x);
    }

    /**
     * Sets the value of the y position spinner.
     *
     * @param y new value of y position spinner
     */
    public void setyPositionSpinner(int y) {
        yPositionSpinner.setValue(y);
    }

    @Override
    public void showErrorDialog(String message, String title) {
        GuiUtils.showErrorDialog(this, message, title);
    }

    @Override
    public void showWarningDialog(String message, String title) {
        GuiUtils.showWarningDialog(this, message, title);
    }

    @Override
    public void showInfoDialog(String message, String title) {
        GuiUtils.showInfoDialog(this, message, title);
    }

    @Override
    public Optional<Path> chooseFile(int mode, String title, String fileprefill) {
        return GuiUtils.chooseFile(this, mode, title, fileprefill);
    }

    @Override
    public Optional<Path> chooseDirectory(String title) {
        return GuiUtils.chooseDirectory(this, title);
    }

    @Override
    public void setDisplayImage(Image img, boolean recreateWindow) {
        if (recreateWindow || slideViewer.isEmpty()) {
            // Close existing window
            slideViewer.ifPresent(viewer -> {
                viewer.dispose();
            });
            // Create new stack window
            createImageCanvas(img);
            // Update GUI
            toggleAllButtons(true);
            helpText.setText(Const.bundle.getString("infoText.fileOpened.text"));
        } else {
            ImageViewer viewer = slideViewer.get();
            viewer.setImage(img.getWrapped(), true);
        }
    }

    /**
     * Creates a new canvas to display an image and sets the instance variables.
     */
    private void createImageCanvas(Image img) {
        // Creates an empty drawable image window including the canvas.
        var viewer = new SlideViewer(control, settings);
        viewer.setImage(img.getWrapped(), false);

        viewer.addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent arg0) {
            }

            @Override
            public void windowClosed(WindowEvent arg0) {
            }

            @Override
            public void windowClosing(WindowEvent arg0) {
                // Destroy the stack window / "us"
                viewer.dispose();
                // We don't have any stack window open
                slideViewer = Optional.empty();
                // Update GUI
                stateNoImageOpened();
                // Notifiy control that the image has been closed.
                control.imageClosed();
            }

            @Override
            public void windowDeactivated(WindowEvent arg0) {
            }

            @Override
            public void windowDeiconified(WindowEvent arg0) {
            }

            @Override
            public void windowIconified(WindowEvent arg0) {
            }

            @Override
            public void windowOpened(WindowEvent arg0) {
            }
        });

        this.slideViewer = Optional.of(viewer);
    }

    @Override
    public void openExportGui(
            ZonedDateTime datetime, Optional<LocalDateTime> assayDatetime,
            Parameters parameters,
            Data data, DataStatistics dataStatistics) {

        new ExportGui(this, control, settings,
                datetime, assayDatetime, parameters, data, dataStatistics);
    }

    private void stateNoImageOpened() {
        toggleAllButtons(false);
        openImageButton.setEnabled(true);
    }

    public void optionsGuiOpened() {
        otherGuiOpened = true;
        helpText.setText(Const.bundle.getString("infoText.optionsGuiOpen.text"));
        openImageButton.setEnabled(false);
        measureButton.setEnabled(false);
        autofitButton.setEnabled(false);
        settingsButton.setEnabled(false);
        toggleFilterButton.setEnabled(false);

        new SettingsGui(this, control, settings);
    }

    public void toggleAllButtons(boolean toggle) {
        openImageButton.setEnabled(toggle);
        measureButton.setEnabled(toggle);
        autofitButton.setEnabled(toggle);
        settingsButton.setEnabled(toggle);
        toggleFilterButton.setEnabled(toggle);
        angleSpinner.setEnabled(toggle);
        xPositionSpinner.setEnabled(toggle);
        yPositionSpinner.setEnabled(toggle);
        mouseExited(null);
    }

    public boolean isOtherGuiOpened() {
        return otherGuiOpened;
    }

    public void setOtherGuiOpened(boolean optionsGuiOpened) {
        this.otherGuiOpened = optionsGuiOpened;
    }

    private void handleAngleChanged() {
        if (activeListener) {
            double angle = (double) angleSpinner.getValue();
            control.rotateSlide(angle);
        }
    }

    private void handlePositionChanged() {
        if (activeListener) {
            int x = (int) xPositionSpinner.getValue();
            int y = (int) yPositionSpinner.getValue();
            control.moveSlide(x, y);
        }
    }

    private void handleCheckIntegrity() {
        GuiUtils.chooseDirectory(this,
                Const.bundle.getString("mainGui.integrityCheck.directoryChooser.title"))
                .ifPresent(path -> outputCheckIntegrityResult(control.checkIntegrity(path)));
    }

    private void outputCheckIntegrityResult(IntegrityCheckResult res) {
        if (res.isSuccess()) {
            MessageFormat formatter = new MessageFormat(
                    Const.bundle.getString("mainGui.integrityCheck.success.text"),
                    SystemUtils.getLocale());
            String title = Const.bundle.getString("mainGui.integrityCheck.success.title");
            String text = formatter.format(new Object[] {
                    res.unwrapSize(),
                    res.unwrapAndFormatAsTable()
            });
            GuiUtils.showInfoDialog(this, text, title);
        } else if (res.isFailure()) {
            MessageFormat formatter = new MessageFormat(
                    Const.bundle.getString("mainGui.integrityCheck.failure.text"),
                    SystemUtils.getLocale());
            String title = Const.bundle.getString("mainGui.integrityCheck.failure.title");
            String text = formatter.format(new Object[] {
                    res.unwrapSize(),
                    res.unwrapAndFormatAsTable()
            });
            GuiUtils.showErrorDialog(this, text, title);
        } else {
            IntegrityCheckError err = res.unwrapErr();
            IntegrityCheckContext ctx = res.getContext();
            String title = Const.bundle.getString("mainGui.integrityCheck.error.title");
            String text = getLocalizedIntegrityCheckErrorText(err, ctx);
            GuiUtils.showWarningDialog(this, text, title);
        }
    }

    private static String getLocalizedIntegrityCheckErrorText(
            IntegrityCheckError err, IntegrityCheckContext ctx) {
        switch (err) {
            case INVALID_MEASUREMENT_DIRECTORY:
                return MessageFormat.format(
                        Const.bundle.getString("mainGui.integrityCheck.error.invalidMeasurementDirectoryText"),
                        ctx.folder.toString());
            case MISSING_IMAGE:
                return MessageFormat.format(
                        Const.bundle.getString("mainGui.integrityCheck.error.missingImageText"),
                        ctx.imagePath);
            case IMAGE_OPEN_FAILED:
                return MessageFormat.format(
                        Const.bundle.getString("mainGui.integrityCheck.error.imageOpenFailedText"),
                        ctx.imagePath);
            case IO_EXCEPTION:
                return Const.bundle.getString("mainGui.integrityCheck.error.ioExceptionText");
            case NO_FILES_CHECKED:
                return Const.bundle.getString("mainGui.integrityCheck.error.noFilesCheckedText");
            case MEASUREMENT_EXCEPTION:
                return Const.bundle.getString("mainGui.integrityCheck.error.measurementException");
            default:
                return "Unknown integrity check error";
        }
    }

    public void setCanvasInteractible(boolean interactible) {
        ImageViewer.toggleCanvas(interactible);
    }

    /**
     * Redraws the GUI drawing canvas.
     *
     * Currently this means only the slide is redrawn.
     */
    @Override
    public void notifiySlideChanged() {
        activeListener = false;
        setAngleSpinner(control.getSlideRotation());
        setxPositionSpinner(control.getSlidexPosition());
        setyPositionSpinner(control.getSlideyPosition());
        activeListener = true;
        slideViewer.ifPresent(viewer -> viewer.repaint());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        if (!otherGuiOpened
                && control.isImageLoaded()
                && settings.getDisplaySettings().isEnableButtonHelp()) {
            Object source = e.getSource();
            if (source.equals(openImageButton)) {
                helpText.setText(Const.bundle.getString("infoText.hoverOpenImage.text"));
                return;
            }
            if (source.equals(settingsButton)) {
                helpText.setText(Const.bundle.getString("infoText.hoverOptionsMenu.text"));
                return;
            }
            if (source.equals(measureButton)) {
                helpText.setText(Const.bundle.getString("infoText.hoverMeasure.text"));
                return;
            }
            if (source.equals(autofitButton)) {
                helpText.setText(Const.bundle.getString("infoText.hoverFitMeasureCircles.text"));
                return;
            }
            if (source.equals(toggleFilterButton)) {
                if (control.isFilterEnabled()) {
                    helpText.setText(Const.bundle.getString("infoText.hoverToggleFilterActivated.text"));
                } else {
                    helpText.setText(Const.bundle.getString("infoText.hoverToggleFilterDeactivated.text"));
                }
                return;
            }
            if (source.equals(checkIntegrityButton)) {
                helpText.setText(Const.bundle.getString("infoText.hoverValidation.text"));
                return;
            }
            Component[] spinnerComponents = angleSpinner.getComponents();
            for (Component comp : spinnerComponents) {
                if (source.equals(comp)) {
                    helpText.setText(Const.bundle.getString("infoText.hoverRotationSpinner.text"));
                    return;
                }
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (control.isImageLoaded() && !otherGuiOpened) {
            rotateThroughInfoText(0);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    /**
     * Resets properties such as colors that are lost on theme change.
     */
    public void resetComponentProperties() {
        openImageButton.setBackground(UIManager.getColor("Actions.Yellow"));
        openImageButton.setForeground(UIManager.getColor("Label.background"));
        toggleFilterButton.setBackground(UIManager.getColor("Objects.Purple"));
        toggleFilterButton.setForeground(UIManager.getColor("Label.background"));
        autofitButton.setBackground(UIManager.getColor("Actions.Blue"));
        autofitButton.setForeground(SystemColor.text);
        measureButton.setBackground(UIManager.getColor("Actions.Green"));
        measureButton.setForeground(SystemColor.text);
        // We have to set the default text before packing or else we don't get the
        // correct size.
        String currentHelpText = helpText.getText();
        helpText.setText(Const.bundle.getString("infoText.startupMessageNoImage.text"));
        pack();
        helpText.setText(currentHelpText);
    }
}

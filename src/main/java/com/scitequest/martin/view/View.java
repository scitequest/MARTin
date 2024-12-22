package com.scitequest.martin.view;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.scitequest.martin.Image;
import com.scitequest.martin.export.Data;
import com.scitequest.martin.export.DataStatistics;
import com.scitequest.martin.export.Parameters;

/**
 * A view defines which operations a GUI/TUI etc. must support.
 *
 * This effectively decouples the actual implementation of a view such as a GUI
 * from the control layer.
 */
public interface View {

    /**
     * Show a error message dialog to the user.
     *
     * Note: The title can be null or empty. In this case the title is set to
     * "Error".
     *
     * @param message The message to display. Recommended usage
     *                {@code String.format("Something happened:%n%s", exeption);}
     * @param title   The title of the dialog
     */
    void showErrorDialog(String message, String title);

    /**
     * Show a warning message dialog to the user.
     *
     * Note: The title can be null or empty. In this case the title is set to
     * "Warning".
     *
     * @param message The message to display. Recommended usage
     *                {@code String.format("Something happened:%n%s", exeption);}
     * @param title   The title of the dialog
     */
    void showWarningDialog(String message, String title);

    /**
     * Show a error message dialog to the user.
     *
     * Note: The title can be null or empty. In this case the title is set to
     * "Information".
     *
     * @param message The message to display. Recommended usage
     *                {@code String.format("Something happened:%n%s", exeption);}
     * @param title   The title of the dialog
     */
    void showInfoDialog(String message, String title);

    /**
     * Wrapper for choosing a file.
     *
     * @param mode        {@code FileDialog.LOAD} or {@code FileDialog.SAVE}
     * @param title       the title of the dialog
     * @param fileprefill the text that should be prefilled in the filename.
     * @return empty, if the dialog was canceled, otherwise the selected file
     */
    Optional<Path> chooseFile(int mode, String title, String fileprefill);

    /**
     * Wrapper for choosing a directory.
     *
     * @param title the title to use, can be left null
     * @return empty, if the dialog was canceled, otherwise the selected file
     */
    Optional<Path> chooseDirectory(String title);

    /**
     * Notifies this view, that the slide has changed.
     *
     * Basically this should be used to redraw the slide and image to reflect any
     * changes which may have been made.
     */
    void notifiySlideChanged();

    /**
     * Set the image which is displayed in the canvas.
     *
     * This has no effect on measuring.
     *
     * @param img            the new image to be displayed
     * @param recreateWindow if true, the old window is closed and a fresh window
     *                       will be created to show the image
     */
    void setDisplayImage(Image img, boolean recreateWindow);

    /**
     * Either activates or deactivates all buttons of the gui.
     *
     * @param toggled if true all buttons of gui are activate, the opposite happens
     *                if false
     */
    void toggleAllButtons(boolean toggled);

    void setOtherGuiOpened(boolean otherGuiOpened);

    void setCanvasInteractible(boolean interactible);

    void toFront();

    /**
     * Get metadata of a measurement by asking the user to provide the necessary
     * information.
     *
     * The datetime is passed as this is the time when the measurement took place
     * and is embedded in the {@code MeasurementMetadata} structure.
     *
     * @param datetime       the datetime when the measurement took place
     * @param assayDate      if present, the date to prefill as the assay datetime
     * @param parameters     the measurement parameters used
     * @param data           the measurement values to show the result table if
     *                       requested
     * @param dataStatistics the statistics calculated from the measurement data
     */
    void openExportGui(ZonedDateTime datetime, Optional<LocalDateTime> assayDate,
            Parameters parameters,
            Data data, DataStatistics dataStatistics);
}

package com.scitequest.martin.view;

import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import com.scitequest.martin.Const;
import com.scitequest.martin.utils.SystemUtils;

/**
 * Users can input time and date with instances of this interface.
 * Dates use a JDatepicker, time uses a JSpinner.
 */
public class TimestampInput extends JPanel {
    /**
     * Instance of JDatePicker to select the assay date.
     * Format used is yyyy-mm-dd.
     */
    private final JDatePickerImpl datePicker;
    /**
     * JSpinner to select assay time.
     * Format used is HH:mm.
     */
    private final JSpinner timeSpinner;
    /**
     * Number of columns of the TimestampInput JPanel.
     */
    private final static int ELEMENT_COLS = 2;
    /**
     * Number of rows of the TimestampInput JPanel.
     */
    private final static int ELEMENT_ROWS = 1;

    private Properties convertToProperties(ResourceBundle resource) {
        Properties properties = new Properties();
        Enumeration<String> keys = resource.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            properties.put(key, resource.getString(key));
        }
        return properties;
    }

    /**
     * Constructor of TimestampInput.
     * All elements are directly generated and preloaded
     * either with a timestamp of the assay or the moment of measuring.
     *
     * @param assayDatetime date and time of assay. Can be empty, if so current date
     *                      will be used.
     */
    private TimestampInput(Optional<LocalDateTime> assayDatetime) {
        this.setLayout(new GridLayout(ELEMENT_ROWS, ELEMENT_COLS));
        // Set the start and end date for the spinner
        Calendar calendar = Calendar.getInstance();
        Date dateTime = calendar.getTime();

        UtilDateModel model = new UtilDateModel();
        model.setSelected(true);

        ResourceBundle resourceBundle = ResourceBundle.getBundle(
                "org.jdatepicker.i18n.Text", SystemUtils.getLocale());
        datePicker = new JDatePickerImpl(
                new JDatePanelImpl(model, convertToProperties(resourceBundle)),
                new DateLabelFormatter());
        add(datePicker);

        if (assayDatetime.isPresent()) {
            dateTime = Date.from(assayDatetime.get().atZone(ZoneId.systemDefault()).toInstant());

            datePicker.getModel().setDate(
                    assayDatetime.get().getYear(),
                    assayDatetime.get().getMonthValue(),
                    assayDatetime.get().getDayOfMonth());
        } else {
            datePicker.getModel().setDate(
                    Calendar.getInstance().get(Calendar.YEAR),
                    Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        }

        /*
         * Sets up the timeSpinner with the HH:mm format.
         * Caret has to be positioned at the end of the element to allow for proper
         * functioning of the jSpinner. This is also the reason for setting editable and
         * focusable to false.
         */
        timeSpinner = new JSpinner(new SpinnerDateModel(dateTime, null, null, Calendar.MINUTE));
        JSpinner.DateEditor timeEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm");
        timeEditor.getTextField().setCaretPosition(timeEditor.getTextField().getText().length());
        timeEditor.getTextField().setEditable(false);
        timeEditor.getTextField().setFocusable(false);
        timeSpinner.setEditor(timeEditor);
        add(timeSpinner);
    }

    /**
     * Calls for the constructor of TimestampInput and returns an instance of
     * TimeStampInput.
     *
     * @param assayDatetime timestamp of the assay. Can be empty.
     * @return instance of TimestampInput
     */
    public static TimestampInput of(Optional<LocalDateTime> assayDatetime) {
        return new TimestampInput(assayDatetime);
    }

    /**
     * Returns the selected time and date.
     *
     * @return Selected time and date as a ZonedDateTime instance.
     */
    public ZonedDateTime getDateTime() {
        // Returns current time and date if no date was selected
        if (datePicker.getModel().getValue() == null) {
            throw new IllegalArgumentException(Const.bundle.getString("exportGui.noDateSelected.errorMessage"));
        }

        // Extract time from time JSpinner
        Date selectedTime = (Date) timeSpinner.getValue();
        LocalDateTime time = LocalDateTime.ofInstant(selectedTime.toInstant(), ZoneId.systemDefault());

        // Extract date from datePicker
        Date date = (Date) datePicker.getModel().getValue();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        // Combine date with time
        localDateTime = localDateTime.withHour(time.getHour()).withMinute(time.getMinute());

        // Return as ZonedDateTime
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    }
}

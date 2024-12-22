package com.scitequest.martin;

import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/** Class containing the entrypoint for the standalone application. */
public final class MartinStandalone {

    private static final Logger log = Logger.getLogger("com.scitequest.martin");

    /** Prevent instantiation. */
    private MartinStandalone() {
    }

    /**
     * Entrypoint for the standalone application.
     *
     * @param args they are unused
     */
    public static void main(String[] args) {
        // Initialize an handler that is called if an unexpected exception happens
        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler.withLogger(log));

        // Launch the application
        SwingUtilities.invokeLater(() -> {
            Control.standalone();
        });
    }
}

package de.hirola.kintojava.util;

import javax.swing.text.DateFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static Logger instance;
    private LoggerConfiguration configuration;
    private boolean localLoggingEnabled;
    private boolean remoteLoggingEnabled;

    private Logger(LoggerConfiguration configuration) {

        this.configuration = configuration;

    }

    private String buildLogEntryString(LogEntry entry) {

        Date logDate = new Date(entry.getTimeStamp());
        SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        String entryString = simpleDateFormatter.format(logDate);
        switch (entry.getSeverity()) {

            case LogEntry.Severity.INFO:
                entryString = entryString + " - " + "Info: ";
                break;

            case LogEntry.Severity.WARNING:
                entryString = entryString + " - " + "Warning: ";
                break;

            case LogEntry.Severity.ERROR:
                entryString = entryString + " - " + "Error: ";
                break;

            case LogEntry.Severity.DEBUG:
                entryString = entryString + " - " + "Debug: ";
                break;

            default:
                entryString = entryString + " - " + "Unknown: ";
        }
        return entryString + entry.getMessage();

    }

    //  simple print to console
    private void out(LogEntry entry) {

        System.out.println(this.buildLogEntryString(entry));

    }

    public static Logger getInstance(LoggerConfiguration configuration) {
        if (instance == null) {
            instance = new Logger(configuration);
        }
        return instance;
    }

    /**
     * Logging message to different destinations.
     *
     * @param severity: severity of the log message
     * @param  message: message to log
     * @see LogEntry
     */
    public void log(int severity, String message) {

        LogEntry entry = new LogEntry(severity, message);
        // determine the log destination
        switch (configuration.getLogggingDestination()) {

            case LoggerConfiguration.LOGGING_DESTINATION.CONSOLE:
                this.out(entry);
            // first all out to console ...
            default:
                this.out(entry);
        }
    }

    /**
     * Sends a feedback for the app.
     *
     * @param type: tye of feedback
     * @param feedback: content of the feedback
     * @see FeedbackEntry
     */
    public void feedback(int type, String feedback) {

       FeedbackEntry entry = new FeedbackEntry(type, feedback);

    }
}

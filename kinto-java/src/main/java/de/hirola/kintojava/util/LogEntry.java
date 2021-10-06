package de.hirola.kintojava.util;

import java.util.Date;

/**
 * An object of the class presents an entry in the log datastore.
 */
public class LogEntry {

    /*
        The collection ("tables") for logs (remote logging).
     */
    public static final String collection = "logs";
    /**
        The "struct" of  possible values for log severity.
     */
    public static class Severity {
        /**
         *  A info log entry.
         */
        public static final int INFO = 0;
        /**
         * A warning log entry.
         */
        public static final int WARNING = 1;
        /**
         A error log entry.
         */
        public static final int ERROR = 2;
        /**
         A debug log entry.
         */
        public static final int DEBUG = 3;
    }

    private long timeStamp;
    private int severity;
    private String message;

    /**
     * Create a log entry with the actual timestamp.
     *
     * @since 0.1.0
     * @param severity The severity of log entry.
     * @param message The content of log entry.
     */
    public LogEntry(int severity, String message) {

        this.timeStamp = System.currentTimeMillis();
        this.severity = this.validate(severity);
        this.message = message;

    }

    //  validate the severity parameter
    //  returns 0 if the parameter not valid
    private int validate(int severity) {
        return switch (severity) {
            case 0, 1, 2, 3 -> severity;
            default -> 0;
        };
    }
}
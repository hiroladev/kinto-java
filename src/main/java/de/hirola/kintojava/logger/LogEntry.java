package de.hirola.kintojava.logger;

import java.time.Instant;

/**
 * An object of the class presents an entry in the log datastore.
 */
public class LogEntry {

    private long timeStamp;
    private int severity;
    private String message;

    /*
        The collection ("table") for logs (remote logging).
     */
    public static final String COLLECTION = "logs";

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

    //  validate the severity parameter
    //  returns 0 if the parameter not valid
    private int validate(int severity) {
        switch (severity) {
            case 0:
            case 1:
            case 2:
                return severity;
            default:
                return 0;
        }
    }

    /**
     * Create a log entry with the actual timestamp.
     *
     * @since 0.1.0
     * @param severity The severity of log entry.
     * @param message The content of log entry.
     */
    public LogEntry(int severity, String message) {

        this.timeStamp = Instant.now().toEpochMilli();
        this.severity = this.validate(severity);
        this.message = message;

    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public int getSeverity() {
        return this.severity;
    }

    public String getMessage() {
        return this.message;
    }
}
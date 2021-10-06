package de.hirola.kintojava.util;

public class Logger {

    private static Logger instance;
    private LoggerConfiguration configuration;
    private boolean localLoggingEnabled;
    private boolean remoteLoggingEnabled;

    private Logger(LoggerConfiguration configuration) {
        this.configuration = configuration;
    }


    public static Logger getInstance(LoggerConfiguration configuration) {
        if (instance == null) {
            instance = new Logger(configuration);
        }
        return instance;
    }

    /**
     * Logging a normal message.
     *
     * @param  message: message to log
     * @see LogEntry
     */
    public void log(String message) {

        LogEntry entry = new LogEntry(LogEntry.Severity.INFO, message);
        //self.addLogEntry(entry)
    }

    /**
     * Logging a warning message.
     *
     * @param message: message to log
     * @see LogEntry
     */
    public void warning(String message) {

        LogEntry entry = new LogEntry(LogEntry.Severity.WARNING, message);

    }

    /**
     * Logging an error message.
     *
     * @param message: message to log
     * @see LogEntry
     */
    public void error(String message) {

        LogEntry entry = new LogEntry(LogEntry.Severity.ERROR, message);

    }

    /**
     * Logging a debug message.
     *
     * @param message: message to log
     * @see LogEntry
     */
    public void debug(String message) {

        LogEntry entry = new LogEntry(LogEntry.Severity.DEBUG, message);

    }

    /**
     * Sends a feedback for the app.
     *
     * @param type: typ of feedback
     * @param feedback: content of the feedback
     * @see FeedbackEntry
     */
    public void feedback(int type, String feedback) {

       FeedbackEntry entry = new FeedbackEntry(type, feedback);

    }
}

package de.hirola.kintojava.util;

/**
 * An object of the class presents an entry in the log datastore.
 */
public class FeedbackEntry {

    /*
        The collection ("tables") for feedbacks (remote logging).
     */
    public static final String collection = "feedbacks";
    /**
        The "struct" of  possible values for log severity.
     */
    public static class Severity {
        /**
         *  A normal feedback.
         */
        public static final int INFO = 0;
        /**
         * A Feedback for a app issue.
         */
        public static final int ISSUE = 1;
        /**
         A Feedback for a new feature.
         */
        public static final int FEATURE_REQUEST = 2;
    }

    private long timeStamp;
    private int severity;
    private String feedback;

    /**
     * Create a log entry with the actual timestamp.
     *
     * @since 0.1.0
     * @param severity The severity of log entry.
     * @param feedback The content of log entry.
     */
    public FeedbackEntry(int severity, String feedback) {

        this.timeStamp = System.currentTimeMillis();
        this.severity = this.validate(severity);
        this.feedback = feedback;

    }

    //  validate the severity parameter
    //  returns 0 if the parameter not valid
    private int validate(int severity) {
        return switch (severity) {
            case 0, 1, 2 -> severity;
            default -> 0;
        };
    }
}
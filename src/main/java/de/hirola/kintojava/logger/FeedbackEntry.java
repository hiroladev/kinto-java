package de.hirola.kintojava.logger;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * An object of the class presents an entry in the log datastore.
 */
public class FeedbackEntry {

    private long timeStamp;
    private int severity;
    private String feedback;

    /*
        The collection ("table") for feedbacks (remote logging).
     */
    public static final String COLLECTION = "feedbacks";
    /**
        The "struct" of  possible values for log severity.
     */
    public static class Severity {
        /**
         *  A normal feedback.
         */
        public static final int INFO = 0;
        /**
         * A Feedback for an app issue.
         */
        public static final int ISSUE = 1;
        /**
         A Feedback for a new feature.
         */
        public static final int FEATURE_REQUEST = 2;
    }

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
        switch (severity) {
            case 0:
            case 1:
            case 2:
                return severity;
            default:
                return 0;
        }
    }
}
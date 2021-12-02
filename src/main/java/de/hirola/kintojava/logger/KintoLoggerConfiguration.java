package de.hirola.kintojava.logger;

import java.io.File;

public class KintoLoggerConfiguration {

    private String bucket;
    private String localLogPath;
    private int logggingDestination;

    /**
     The "struct" of  possible destinations for logging.
     You can combine for multiple targets, e.g. 3 means
     log to console and file.
     For remote logging must be specified a valid log server.
     */
    public static class LOGGING_DESTINATION {
        /**
         *  A normal feedback.
         */
        public static final int CONSOLE = 1;
        /**
         * A Feedback for an app issue.
         */
        public static final int FILE = 3;
        /**
         A Feedback for a new feature.
         */
        public static final int REMOTE = 5;
    }

    public KintoLoggerConfiguration(Builder builder) {
        this.bucket = builder.bucket;
        this.localLogPath = builder.localLogPath;
        this.logggingDestination = builder.logggingDestination;
    }

    /**
     *
     * @return the name of bucket ("database") for api logs
     */
    public String getBucket() {

        return this.bucket;

    }

    /**
     *
     * @return the path to local log files
     */
    public String getLocalLogPath() {

        return this.localLogPath;

    }

    /**
     *
     * @return an int value to determine the destination of log
     */
    public int getLogggingDestination() {

        return this.logggingDestination;
    }

    /**
     * Building dynamic logger configurations.
     */
    public static class Builder {

        private String bucket;
        private String localLogPath;
        private int logggingDestination;

        public Builder(String bucket) {
            this.bucket = bucket;
            //  default path for local database
            String userHomeDir = System.getProperty("user.home");
            this.localLogPath = userHomeDir + File.separator + ".kinto-java" + File.separator + "logs";
            // default log to console
            this.logggingDestination = LOGGING_DESTINATION.CONSOLE;
        }

        public Builder localLogPath(String path) {
            this.localLogPath = path;
            return this;
        }

        public Builder logggingDestination(int logggingDestination) {
            this.logggingDestination = logggingDestination;
            return this;
        }

        public KintoLoggerConfiguration build() {
            return new KintoLoggerConfiguration(this);
        }

    }
}
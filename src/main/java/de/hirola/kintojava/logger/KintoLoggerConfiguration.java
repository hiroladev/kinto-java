package de.hirola.kintojava.logger;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
 public class KintoLoggerConfiguration {

    private final String appPackageName;
    private final int logggingDestination;

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
        this.appPackageName = builder.appPackageName;
        this.logggingDestination = builder.logggingDestination;
    }

    public String getAppPackageName() {
        return appPackageName;
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

        private String appPackageName;
        private int logggingDestination;

        public Builder(String appPackageName) {
            // get the bucket name from package name, e.g. com.myfirm.AppName
            this.appPackageName = appPackageName;
            this.logggingDestination = LOGGING_DESTINATION.CONSOLE;
        }

        public Builder logggingDestination(int logggingDestination) {
            this.logggingDestination = logggingDestination;
            return this;
        }

        public Builder appPackageName(String appPackageName) {
            this.appPackageName = appPackageName;
            return this;
        }

        public KintoLoggerConfiguration build() {
            return new KintoLoggerConfiguration(this);
        }

    }
}

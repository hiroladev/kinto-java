package de.hirola.kintojava.util;

import java.io.File;

public class LoggerConfiguration {

    private String bucket;
    private String kintoLogServer;
    private int kintoLogPort;
    private String localLogPath;

    public LoggerConfiguration(Builder builder) {
        this.bucket = builder.bucket;
        this.kintoLogServer = builder.kintoLogServer;
        this.kintoLogPort = builder.kintoLogPort;
        this.localLogPath = builder.localLogPath;
    }

    public static class Builder {

        private String bucket;
        private String kintoLogServer;
        private int kintoLogPort;
        private String localLogPath;

        public Builder(String bucket) {
            this.bucket = bucket;
            // default port
            this.kintoLogPort = 443;
            //  default path for local database
            String userHomeDir = System.getProperty("user.home");
            this.localLogPath = userHomeDir + File.separator + ".kintojava" + File.separator + "logs";
        }

        public Builder kintoLogServer(String url) {
            this.kintoLogServer = url;
            return this;
        }

        public Builder kintoLogPort(int port) {
            this.kintoLogPort = port;
            return this;
        }

        public Builder localLogPath(String path) {
            this.localLogPath = path;
            return this;
        }

        public LoggerConfiguration build() {
            return new LoggerConfiguration(this);
        }

    }
}

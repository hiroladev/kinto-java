package de.hirola.kintojava;

import java.io.File;

/**
 * Configuration
 * The default dir for local databases ist $HOME/.kintojava.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public class KintoConfiguration {

    private String bucket;
    private String kintoServer;
    private int kintoPort;
    private String localdbPath;

    /**
     *
     * @param builder Configuration builder (pattern)
     */
    public KintoConfiguration(Builder builder) {
        this.bucket = builder.bucket;
        this.kintoServer = builder.kintoServer;
        this.kintoPort = builder.kintoPort;
        this.localdbPath = builder.localdbPath;
    }

    public String getLocaldbPath() {
        return this.localdbPath;
    }

    /**
     *
     * @return the base url for the kinto service
     */
    public String getKintoURL() {
        return "https://" + this.kintoServer + ":" + this.kintoPort + "/v1/";
    }

    /**
     *
     * @return the name of bucket ("database")
     */
    public String getBucket() {
        return this.bucket;
    }

    public static class Builder {

        private String bucket;
        private String kintoServer;
        private int kintoPort;
        private String localdbPath;

        public Builder(String bucket) {
            this.bucket = bucket;
            // default port
            this.kintoPort = 443;
            //  default path for local database
            String userHomeDir = System.getProperty("user.home");
            this.localdbPath = userHomeDir + File.separator + ".kintojava" + File.separator + bucket + ".sqlite";
        }

        public Builder kintoServer(String url) {
            this.kintoServer = url;
            return this;
        }

        public Builder kintoPort(int port) {
            this.kintoPort = port;
            return this;
        }

        public Builder localdbPath(String path) {
            this.localdbPath = path;
            return this;
        }

        public KintoConfiguration build() {
            return new KintoConfiguration(this);
        }

    }

}

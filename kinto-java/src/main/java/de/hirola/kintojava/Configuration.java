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
public class Configuration {

    private String bucket;
    private String kintoRemoteURL;
    private int kintoRemotePort;
    private String localdbPath;

    /**
     *
     * @param builder Configuration builder (pattern)
     */
    public Configuration(ConfigurationBuilder builder) {
        this.bucket = builder.bucket;
        this.kintoRemoteURL = builder.kintoRemoteURL;
        this.kintoRemotePort = builder.kintoRemotePort;
        this.localdbPath = builder.localdbPath;
    }

    public String getLocaldbPath() {
        return this.localdbPath;
    }

    public static class ConfigurationBuilder {

        private String bucket;
        private String kintoRemoteURL;
        private int kintoRemotePort;
        private String localdbPath;

        public ConfigurationBuilder(String bucket) {
            this.bucket = bucket;
            //  default path for local datbase
            String userHomeDir = System.getProperty("user.home");
            this.localdbPath = userHomeDir + File.separator + ".kintojava" + File.separator + bucket + ".sqlite";
        }

        public ConfigurationBuilder kintoRemoteURL(String url) {
            this.kintoRemoteURL = url;
            return this;
        }

        public ConfigurationBuilder kintoRemotePort(int port) {
            this.kintoRemotePort = port;
            return this;
        }

        public ConfigurationBuilder localdbPath(String path) {
            this.localdbPath = path;
            return this;
        }

        //Return the finally consrcuted User object
        public Configuration build() {
            return new Configuration(this);
        }

    }

}

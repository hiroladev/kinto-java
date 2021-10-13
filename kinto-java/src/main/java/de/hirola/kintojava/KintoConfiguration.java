package de.hirola.kintojava;

import java.io.File;
import java.util.ArrayList;

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
    private ArrayList<Class<? extends KintoObject>> objectTypes;
    private String kintoServer;
    private int kintoPort;
    private String localdbPath;

    /**
     *
     * @param builder Configuration builder (pattern)
     */
    public KintoConfiguration(Builder builder) {
        this.bucket = builder.bucket;
        this.objectTypes = builder.objectTypes;
        this.kintoServer = builder.kintoServer;
        this.kintoPort = builder.kintoPort;
        this.localdbPath = builder.localdbPath;
    }

    /**
     *
     * @return the name of bucket ("database")
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * The collections with the types managed by Kinto.
     *
     * @return The collections managed by Kinto.
     */
    public ArrayList<Class<? extends KintoObject>> getObjectTypes() {
        return objectTypes;
    }

    /**
     *
     * @return The base url for the kinto service.
     */
    public String getKintoURL() {
        return "https://" + this.kintoServer + ":" + this.kintoPort + "/v1/";
    }

    /**
     *
     * @return the path to local database
     */
    public String getLocaldbPath() {
        return this.localdbPath;
    }

    /**
     * Building dynamic kinto configurations.
     */
    public static class Builder {

        private String bucket;
        private ArrayList<Class<? extends KintoObject>> objectTypes;
        private String kintoServer;
        private int kintoPort;
        private String localdbPath;

        public Builder(String bucket) {
            this.bucket = bucket;
            objectTypes = new ArrayList<Class<? extends KintoObject>>();
            // default server
            kintoServer = "localhost";
            // default port
            kintoPort = 443;
            //  default path for local database
            String userHomeDir = System.getProperty("user.home");
            localdbPath = userHomeDir + File.separator + ".kinto-java" + File.separator + bucket + ".sqlite";
        }

        public Builder objectTypes(ArrayList<Class<? extends KintoObject>> types) {
            this.objectTypes = types;
            return this;
        }

        public Builder kintoServer(String url) {
            kintoServer = url;
            return this;
        }

        public Builder kintoPort(int port) {
            kintoPort = port;
            return this;
        }

        public Builder localdbPath(String path) {
            localdbPath = path;
            return this;
        }

        public KintoConfiguration build() {
            return new KintoConfiguration(this);
        }

    }

}

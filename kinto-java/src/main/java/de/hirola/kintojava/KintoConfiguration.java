package de.hirola.kintojava;

import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

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
    public KintoConfiguration(Builder builder) throws KintoException {
        this.bucket = builder.bucket;
        this.objectTypes = builder.objectTypes;
        validateObjectList();
        this.kintoServer = builder.kintoServer;
        this.kintoPort = builder.kintoPort;
        this.localdbPath = builder.localdbPath;
    }

    // check if all attributes types in object list
    // use reflection to check attributes
    private void validateObjectList() throws KintoException {
        try {
            Iterator<Class <? extends KintoObject>> typeIterator = objectTypes.stream().iterator();
            while (typeIterator.hasNext()) {
                Class<? extends KintoObject> type = typeIterator.next();
                Field[] declaredFields = type.getDeclaredFields();
                Iterator<Field> attributeIterator = Arrays.stream(declaredFields).iterator();
                while (attributeIterator.hasNext()) {
                    Field attribute = attributeIterator.next();
                    if (attribute.isAnnotationPresent(Persisted.class)) {
                        //  "embedded" kinto objects
                        Class<?> attributeSuperClass = attribute.getType().getSuperclass();
                        if (attributeSuperClass != null) {
                            if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                                String attributeClassName = attribute.getType().getSimpleName();
                                Iterator<Class <? extends KintoObject>> iterator = objectTypes.stream().iterator();
                                boolean typeIsInList = false;
                                while (iterator.hasNext()) {
                                    // is type in object list?
                                    Class<? extends KintoObject> typeInList = iterator.next();
                                    if (typeInList.getSimpleName().equalsIgnoreCase(attributeClassName)) {
                                        typeIsInList = true;
                                    }
                                }
                                if (!typeIsInList) {
                                    String errorMessage = "The kinto object type of attribute \""
                                            .concat(attribute.getName())
                                            .concat("\" is not in the object types configuration.");

                                    throw new KintoException(errorMessage);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(exception.getMessage());
        }
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

        public KintoConfiguration build() throws KintoException {
            return new KintoConfiguration(this);
        }

    }

}

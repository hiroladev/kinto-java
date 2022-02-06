package de.hirola.kintojava;

import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Configuration
 * The default directory for local databases is $HOME/.kintojava.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public class KintoConfiguration {

    private final String appPackageName;
    private final ArrayList<Class<? extends KintoObject>> objectTypes;
    private final String kintoServer;
    private final int kintoPort;

    /**
     * Create a new kinto configuration with given builder.
     *
     * @param builder Configuration builder (pattern)
     */
    public KintoConfiguration(Builder builder) throws KintoException {
        this.appPackageName = builder.appPackageName;
        this.objectTypes = builder.objectTypes;
        validateObjectList();
        this.kintoServer = builder.kintoServer;
        this.kintoPort = builder.kintoPort;
    }

    /**
     * Get the name of bucket of remote kinto.
     *
     * @return The name of bucket ("database").
     */
    public String getAppPackageName() {
        return appPackageName;
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
     * Get the url for the remote kinto.
     *
     * @return The base url for the kinto service.
     */
    public String getKintoURL() {
        return "https://" + this.kintoServer + ":" + this.kintoPort + "/v1/";
    }

    /**
     * Building dynamic kinto configurations.
     */
    public static class Builder {

        private String appPackageName;
        private ArrayList<Class<? extends KintoObject>> objectTypes;
        private String kintoServer;
        private int kintoPort;

        public Builder(String packageName) {
            this.appPackageName = packageName;  // get the bucket name from package name, e.g. com.myfirm.AppName
            objectTypes = new ArrayList<>(); // all types managed by kinto
            kintoServer = "localhost"; // default server
            kintoPort = 443; // default port
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

        public Builder appPackageName(String appPackageName) {
            this.appPackageName = appPackageName;
            return this;
        }

        public Builder kintoLogger() {
            return this;
        }

        public KintoConfiguration build() throws KintoException {
            return new KintoConfiguration(this);
        }
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
            throw new KintoException(exception.getMessage());
        }
    }
}

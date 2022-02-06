package de.hirola.kintojava;

import de.hirola.kintojava.logger.KintoLogger;
import de.hirola.kintojava.logger.KintoLoggerConfiguration;
import de.hirola.kintojava.model.DataSet;
import de.hirola.kintojava.model.KintoObject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.*;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * The singleton object for data management with sqlite and kinto.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
public final class Kinto {

    private static final String TAG = Kinto.class.getSimpleName();

    private static Kinto instance;
    private final String bucket;
    private final KintoLogger kintoLogger;
    private final String appPackageName;
    private final ArrayList<KintoCollection> collections;
    private KintoDatabaseAdapter dataBase;
    private final boolean syncEnabled;

    /**
     * Create a singleton instance for local data management and sync.
     *
     * @param kintoConfiguration The configuration for local and remote kinto datastore.
     * @return The singleton object for data management.
     */
    public static Kinto getInstance(KintoConfiguration kintoConfiguration) throws KintoException {
        if (instance == null) {
            instance = new Kinto(kintoConfiguration);
        }
        return instance;
    }

    /**
     * Get the instance of kinto logger object for local and remote logging.
     *
     * @return The active instance of KintoLogger.
     */
    public KintoLogger getKintoLogger() {
        return kintoLogger;
    }

    /**
     * Get the local datastore connection.
     *
     * @return An opened connection to the local datastore.
     * @throws KintoException if the connection to local datastore couldn't' created
     */
    public KintoDatabaseAdapter getLocalDatastoreConnection() throws KintoException {
        if (dataBase == null) {
            dataBase = KintoDatabaseAdapter.getInstance(this, appPackageName);
        }
        return dataBase;
    }

    /**
     * Try to log in to the remote kinto service.
     *
     * @param credentials for the login to remote kinto sync data store
     * @throws KintoException if the login failed
     */
    public void login(@NotNull Credentials credentials) throws KintoException {
        System.out.println("Not implemented yet " + credentials.getBasicAuthString());
    }

    /**
     * Add a new KintoObject to the local datastore.
     * If the kintoObject exists, it is updated with the new properties.
     *
     *
     * @param kintoObject Object to be added to the local datastore.
     * @throws KintoException if the object null or an error occurred while adding to datastore
     */
    public void add(KintoObject kintoObject) throws KintoException {
        if (kintoObject == null) {
            throw new KintoException("Can't add a null object.");
        }
        if (isOpen()) {
            Iterator<KintoCollection> iterator = collections.stream().iterator();
            while (iterator.hasNext()) {
                KintoCollection collection = iterator.next();
                if (collection.getType().equals(kintoObject.getClass())) {
                    collection.addRecord(kintoObject);
                }
            }
        } else {
            throw new KintoException("The local datastore is not available.");
        }
    }

    /**
     * Update an (existing )object in local datastore.
     *
     * @param kintoObject object to update in local datastore
     * @throws KintoException if the object not existing in local datastore or
     *                        an error occurred while updating in datastore
     */
    public void update(@NotNull KintoObject kintoObject) throws KintoException {
        if (!kintoObject.isPersistent()) {
            throw new KintoException("Can't update a non existing object.");
        }
        if (isOpen()) {
            Iterator<KintoCollection> iterator = collections.stream().iterator();
            while (iterator.hasNext()) {
                KintoCollection collection = iterator.next();
                if (collection.getType().equals(kintoObject.getClass())) {
                    collection.updateRecord(kintoObject);
                }
            }
        } else {
            throw new KintoException("The local datastore is not available.");
        }
    }

    /**
     * Remove an (existing) object from local datastore. If the object exists in remote kinto,
     * it will not be deleted there without sync.
     *
     * @param kintoObject Object to remove
     * @throws KintoException if the object not existing in local datastore
     *                        or an error occurred while removing from datastore
     */
    public void remove(@NotNull KintoObject kintoObject) throws KintoException{
        if (!kintoObject.isPersistent()) {
            throw new KintoException("Can't remove a non existing object.");
        }
        if (isOpen()) {
            if (kintoObject.isUseInRelation()) {
                throw new KintoException("Can't remove the object. It's used in an other object. Please update the other object before.");
            }
            Iterator<KintoCollection> iterator = collections.stream().iterator();
            while (iterator.hasNext()) {
                KintoCollection collection = iterator.next();
                if (collection.getType().equals(kintoObject.getClass())) {
                    collection.removeRecord(kintoObject);
                }
            }
        } else {
            throw new KintoException("The local datastore is not available.");
        }
    }

    /**
     * Get an object from datastore with given type and uuid. The object can be null, if the object could not found.
     *
     * @param type the type og object
     * @param uuid  the uuid of the object
     * @return An object from datastore wih given uuid or null, if the object was not found in local datastore.
     * @throws KintoException if the length of uuid is 0 or an exception occurred while getting object from datastore
     */
    public KintoObject findByUUID(@NotNull Class<? extends KintoObject> type, @NotNull String uuid) throws KintoException {
        if (uuid.length() == 0) {
            throw new KintoException("The uuid must be not null and greater than 0.");
        }
        if (isOpen()) {
            // the kinto object for the given uuid
            KintoObject kintoObject = null;
            // create an object list with native attributes
            boolean collectionFound = false;
            Iterator<KintoCollection> iterator = collections.stream().iterator();
            while (iterator.hasNext()) {
                KintoCollection collection = iterator.next();
                if (collection.getType().equals(type)) {
                    collectionFound = true;
                    try {
                        // returns a kinto object
                        // contains embedded objects with empty values
                        kintoObject = collection.findByUUID(uuid);
                        Field[] attributes = kintoObject.getClass().getDeclaredFields();
                        Iterator<Field> fieldIterator = Arrays.stream(attributes).iterator();
                        while (fieldIterator.hasNext()) {
                            // 1:1 embedded attribute
                            Field attribute = fieldIterator.next();
                            Class<?> attributeType = attribute.getType();
                            if (DataSet.haveAttributeKintoObjectAsSuperClass(attributeType)) {
                                // get the uuid from embedded object
                                attribute.setAccessible(true);
                                KintoObject embeddedObject = (KintoObject) attribute.get(kintoObject);
                                // check if object has values from datastore - have no null values
                                if (embeddedObject != null) {
                                    String embeddedObjectUUID = embeddedObject.getUUID();
                                    // call this func recursive
                                    // fill the "empty" object with values
                                    //TODO: NullPointerException
                                    embeddedObject = findByUUID((Class<? extends KintoObject>) attributeType, embeddedObjectUUID);
                                    if (embeddedObject == null) {
                                        throw new KintoException("An embedded object was not found in datastore.");
                                    }
                                }
                                // update the enclosed object
                                attribute.set(kintoObject, embeddedObject);
                            }
                            // 1:m embedded attributes
                            if (attributeType.isAssignableFrom(List.class)) {
                                attribute.setAccessible(true);
                                // the list for the objects with all attributes
                                List<KintoObject> embeddedObjects = new ArrayList<>();
                                ArrayList<?> arrayListObjects = (ArrayList<?>) attribute.get(kintoObject);
                                for (Object arrayListObject : arrayListObjects) {
                                    if (KintoObject.class.isAssignableFrom(arrayListObject.getClass())) {
                                        // get the uuid from embedded object
                                        attributeType = arrayListObject.getClass();
                                        KintoObject embeddedObject = (KintoObject) arrayListObject;
                                        if (!embeddedObject.isPersistent()) {
                                            String embeddedObjectUUID = embeddedObject.getUUID();
                                            // call this func recursive
                                            // fill the "empty" object with values
                                            embeddedObject = findByUUID((Class<? extends KintoObject>) attributeType, embeddedObjectUUID);
                                            if (embeddedObject == null) {
                                                throw new KintoException("An embedded object was not found in datastore.");
                                            }
                                        }
                                        // add the object to the embedded list
                                        embeddedObjects.add(embeddedObject);
                                    }
                                }
                                // update the enclosed object
                                attribute.set(kintoObject, embeddedObjects);
                            }
                        }
                    } catch (IllegalAccessException | IllegalArgumentException exception) {
                        // TODO: Error Message
                        throw new KintoException(exception);
                    }
                }
            }
            if (!collectionFound) {
                String errorMessage = "Cant' find the collection for the object type "
                        + type
                        + ".";
                throw new KintoException(errorMessage);
            }
            return kintoObject;
        } else {
            throw new KintoException("The local datastore is not available.");
        }
    }

    public List<? extends KintoObject> findAll(@NotNull Class<? extends KintoObject> type) throws KintoException {
        if (isOpen()) {
            List<KintoObject> objects = new ArrayList<>();
            // the collection for the object class
            KintoCollection kintoObjectClassCollection = null;
            // create an object list with native attributes
            boolean collectionFound = false;
            Iterator<KintoCollection> iterator = collections.stream().iterator();
            while (iterator.hasNext()) {
                KintoCollection collection = iterator.next();
                if (collection.getType().equals(type)) {
                    collectionFound = true;
                    try {
                        objects = collection.findAll();
                        // save the actual collection
                        kintoObjectClassCollection = collection;
                    } catch (KintoException exception) {
                        exception.printStackTrace();
                    }
                }
            }
            if (!collectionFound) {
                String errorMessage = "Cant' find the collection for the object type "
                        + type
                        + ".";
                throw new KintoException(errorMessage);
            }
            // load 1:1 and 1:m embedded objects
            if (kintoObjectClassCollection != null) {
                HashMap<String, DataSet> storableAttributes = kintoObjectClassCollection.getStorableAttributes();
                for (KintoObject kintoObject : objects) {
                    for (String attributeName : storableAttributes.keySet()) {
                        try {
                            DataSet dataSet = storableAttributes.get(attributeName);
                            Field attribute = dataSet.getAttribute();
                            // 1:1
                            if (dataSet.isKintoObject()) {
                                // 1:1 embedded object
                                // set the values
                                // get the uuid from the "empty" embedded object
                                Class<?> attributeType = attribute.getType();
                                if (!DataSet.haveAttributeKintoObjectAsSuperClass(attributeType)) {
                                    String errorMessage = "The object must extends KintoObject. This object extends "
                                            + attributeType.getName();
                                    throw new KintoException(errorMessage);
                                }
                                @SuppressWarnings("unchecked")
                                Class<? extends KintoObject> embeddedObjectClazz = (Class<? extends KintoObject>) attributeType;
                                Field embeddedObjectAttribute = type.getDeclaredField(attributeName);
                                embeddedObjectAttribute.setAccessible(true);
                                KintoObject embeddedObject = (KintoObject) embeddedObjectAttribute.get(kintoObject);
                                // embedded object can be null
                                if (embeddedObject != null) {
                                    // get the uuid from embedded object
                                    String embeddedObjectUUID = embeddedObject.getUUID();
                                    embeddedObject = findByUUID(embeddedObjectClazz, embeddedObjectUUID);
                                    if (embeddedObject == null) {
                                        String errorMessage = "Cant' find the the embedded object with the UUID '"
                                                + type
                                                + "'.";
                                        throw new KintoException(errorMessage);
                                    }
                                    // the embedded object can have another embedded objects
                                    // save the embedded object in kinto object
                                    embeddedObjectAttribute.set(kintoObject, embeddedObject);
                                }
                            }
                            if (dataSet.isList()) {
                                // 1:m embedded objects
                                // set the values
                                // get the uuid from the "empty" embedded objects
                                Class<?> embeddedObjectClazz = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                                Field embeddedObjectAttribute = type.getDeclaredField(attributeName);
                                embeddedObjectAttribute.setAccessible(true);
                                // get the list of the objects
                                List<?> arrayListObjects = (ArrayList<?>) embeddedObjectAttribute.get(kintoObject);
                                // the list for the objects with all attributes
                                List<KintoObject> embeddedObjects = new ArrayList<>();
                                // get the uuid from embedded objects
                                for (Object arrayListObject : arrayListObjects) {
                                    if (!KintoObject.class.isAssignableFrom(arrayListObject.getClass())) {
                                        String errorMessage = "The object must extends KintoObject. This object extends "
                                                + arrayListObject.getClass().getName();
                                        throw new KintoException(errorMessage);
                                    }
                                    KintoObject embeddedObject = (KintoObject) arrayListObject;
                                    // get the uuid from embedded object
                                    String embeddedObjectUUID = embeddedObject.getUUID();
                                    embeddedObject = findByUUID((Class<? extends KintoObject>) embeddedObjectClazz, embeddedObjectUUID);
                                    if (embeddedObject == null) {
                                        String errorMessage = "Cant' find the the embedded object with the UUID '"
                                                + type
                                                + "'.";
                                        throw new KintoException(errorMessage);
                                    }
                                    // add the object to the embedded list
                                    embeddedObjects.add(embeddedObject);
                                }
                                // the embedded object can have another embedded objects
                                // save the embedded object in kinto object
                                embeddedObjectAttribute.set(kintoObject, embeddedObjects);
                            }
                        } catch (NoSuchFieldException exception) {
                            String errorMessage = "Can't get the attribute "
                                    + attributeName
                                    + " using reflection: "
                                    + exception.getMessage();
                            kintoLogger.log(KintoLogger.ERROR, TAG, errorMessage, exception);
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            throw new KintoException(errorMessage);
                        } catch (IllegalAccessException exception) {
                            String errorMessage = "Getting value for attribute "
                                    + attributeName
                                    + " using reflection failed: "
                                    + exception.getMessage();
                            kintoLogger.log(KintoLogger.ERROR, TAG, errorMessage, exception);
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            throw new KintoException(errorMessage);
                        }
                    }

                }
            }
            return objects;
        } else {
            throw new KintoException("The local datastore is not available.");
        }
    }

    /**
     * Publish all local data to the server, import remote changes.
     * Not implemented yet.
     */
    public void sync() throws KintoException {
        System.out.println("Sync to remote kinto with bucket " + bucket +" Not implemented yet.");
    }

    /**
     * A flag to determine if the sync enabled.
     *
     * @return The flag, if remote kinto enabled or not.
     */
    public boolean syncEnabled() {
        return syncEnabled;
    }

    /**
     * A flag to determine if the local datastore opened.
     *
     * @return The flag, if remote kinto enabled or not.
     */
    public boolean isOpen() {
        return dataBase.isOpen();
    }

    /**
     * Closed the local datastore.
     */
    public void close() {
        if (isOpen()) {
            try {
                dataBase.close();
            } catch (SQLException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private Kinto(@NotNull KintoConfiguration kintoConfiguration) throws KintoException {
        appPackageName = kintoConfiguration.getAppPackageName();
        if (appPackageName.contains(".")) {
            bucket = appPackageName.substring(appPackageName.lastIndexOf(".") + 1);
        } else {
            bucket = appPackageName;
        }
        // check managed objects
        int size = kintoConfiguration.getObjectTypes().size();
        if (size == 0) {
            throw new KintoException("There are no managed object types in configuration.");
        }
        // activate logging
        // create the library logger
        KintoLoggerConfiguration loggerConfiguration = new KintoLoggerConfiguration.Builder(appPackageName)
                .logggingDestination(KintoLoggerConfiguration.LOGGING_DESTINATION.CONSOLE
                        + KintoLoggerConfiguration.LOGGING_DESTINATION.FILE)
                .build();
        kintoLogger = KintoLogger.getInstance(loggerConfiguration);
        collections = new ArrayList<>(size);
        syncEnabled = false;
        // initialize the local datastore for the collection
        dataBase = KintoDatabaseAdapter.getInstance(this, appPackageName);
        // create or check collections (schema)
        for (Class<? extends KintoObject> aClass : kintoConfiguration.getObjectTypes()) {
            initializeCollection(aClass);
        }
    }

    // fill list of collections
    private void initializeCollection(Class<? extends KintoObject> type) throws KintoException {
        KintoCollection kintoCollection = new KintoCollection(type, this);
        collections.add(kintoCollection);
    }

}

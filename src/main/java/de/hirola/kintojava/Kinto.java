package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;
import de.hirola.kintojava.model.DataSet;
import de.hirola.kintojava.model.KintoObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * The singleton object for data management with sqlite and kinto.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
public final class Kinto {

    private static Kinto instance;
    private final KintoConfiguration kintoConfiguration;
    private Logger logger;
    private String bucket;
    private final ArrayList<KintoCollection> collections;
    private Connection localdbConnection;
    private boolean loggerIsAvailable;
    private boolean isLocalDBConnected;
    private boolean syncEnabled;

    /**
     * Create a singleton instance for local data management and sync.
     *
     * @param kintoConfiguration The configuration for local and remote kinto datastore.
     * @return singleton object for data management
     */
    public static Kinto getInstance(KintoConfiguration kintoConfiguration) throws KintoException {
        if (instance == null) {
            instance = new Kinto(kintoConfiguration);
        }
        return instance;
    }

    /**
     *
     * @return a (open) connection to the local datastore
     * @throws KintoException if the connection to local datastore couldn't' created
     */
    public Connection getLocalDBConnection() throws KintoException {
        if (localdbConnection == null) {
            initLocalDB();
            isLocalDBConnected = true;
        }
        return localdbConnection;
    }

    public void login(Credentials credentials) {

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
        if (isLocalDBConnected) {
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
     *
     * @param kintoObject Object to update in local datastore
     * @throws KintoException if the object null or an error occurred while updating in datastore
     */
    public void update(KintoObject kintoObject) throws KintoException {
        if (kintoObject == null) {
            throw new KintoException("Can't update a null object.");
        }
        if (isLocalDBConnected) {
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
     *
     * @param kintoObject Object to remove
     * @throws KintoException if the object null or an error occurred while removing from datastore
     */
    public void remove(KintoObject kintoObject) throws KintoException{
        if (kintoObject == null) {
            throw new KintoException("Can't remove a null object.");
        }
        if (isLocalDBConnected) {
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
     *
     * @param type searching for all kinto objects of this type (class)
     * @return a (empty) list of kinto objects from type
     * @throws KintoException if an error occurred while searching in local datastore
     */
    public ArrayList<? extends KintoObject> findAll(Class<? extends KintoObject> type) throws KintoException {
        if (isLocalDBConnected) {
            ArrayList<KintoObject> objects = new ArrayList<>();
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
                                if (!attributeType.getSuperclass().equals(KintoObject.class)) {
                                    String errorMessage = "The object must extends KintoObject. This object extends "
                                            + attributeType.getName();
                                    throw new KintoException(errorMessage);
                                }
                                @SuppressWarnings("unchecked")
                                Class<? extends KintoObject> embeddedObjectClazz = (Class<? extends KintoObject>) attributeType;
                                Field embeddedObjectAttribute = type.getDeclaredField(attributeName);
                                embeddedObjectAttribute.setAccessible(true);
                                KintoObject embeddedObject = (KintoObject) embeddedObjectAttribute.get(kintoObject);
                                // get the uuid from embedded object
                                String embeddedObjectUUID = embeddedObject.getUUID();
                                // get the collection for the embedded object
                                collectionFound = false;
                                iterator = collections.stream().iterator();
                                while (iterator.hasNext()) {
                                    KintoCollection collection = iterator.next();
                                    if (collection.getType().equals(embeddedObjectClazz)) {
                                        collectionFound = true;
                                        try {
                                            embeddedObject = collection.findByUUID(embeddedObjectUUID);
                                            if (embeddedObject == null) {
                                                String errorMessage = "Cant' find the the embedded object with the UUID '"
                                                        + type
                                                        + "'.";
                                                throw new KintoException(errorMessage);
                                            }
                                            // save the embedded object in kinto object
                                            embeddedObjectAttribute.set(kintoObject, embeddedObject);
                                        } catch (KintoException exception) {
                                            exception.printStackTrace();
                                        }
                                    }
                                }
                                if (!collectionFound) {
                                    String errorMessage = "Cant' find the collection for the embedded object type "
                                            + type
                                            + ".";
                                    throw new KintoException(errorMessage);
                                }
                            }
                            if (dataSet.isArray()) {
                                // 1:m embedded objects
                                // set the values
                                // get the uuid from the "empty" embedded objects
                                Class<?> embeddedObjectClazz = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                                Field embeddedObjectAttribute = type.getDeclaredField(attributeName);
                                embeddedObjectAttribute.setAccessible(true);
                                // get the list of the objects
                                ArrayList<?> arrayListObjects = (ArrayList<?>) embeddedObjectAttribute.get(kintoObject);
                                // the list for the objects with all attributes
                                ArrayList<KintoObject> embeddedObjects = new ArrayList<>();
                                // get the uuid from embedded objects
                                for (Object arrayListObject : arrayListObjects) {
                                    if (!KintoObject.class.isAssignableFrom(arrayListObject.getClass())) {
                                        String errorMessage = "The object must extends KintoObject. This object extends "
                                                + arrayListObject.getClass().getName();
                                        throw new KintoException(errorMessage);
                                    }
                                    KintoObject embeddedObject = (KintoObject) arrayListObject;
                                    String embeddedObjectUUID = embeddedObject.getUUID();
                                    // get the collection for the embedded object
                                    collectionFound = false;
                                    iterator = collections.stream().iterator();
                                    while (iterator.hasNext()) {
                                        KintoCollection collection = iterator.next();
                                        if (collection.getType().equals(embeddedObjectClazz)) {
                                            collectionFound = true;
                                            try {
                                                embeddedObject = collection.findByUUID(embeddedObjectUUID);
                                                if (embeddedObject == null) {
                                                    String errorMessage = "Cant' find the the embedded object with the UUID '"
                                                            + type
                                                            + "'.";
                                                    throw new KintoException(errorMessage);
                                                }
                                                // add to the list of objects with all attributes
                                                embeddedObjects.add(embeddedObject);
                                            } catch (KintoException exception) {
                                                exception.printStackTrace();
                                            }
                                        }
                                    }
                                    // save the embedded object in kinto object
                                    embeddedObjectAttribute.set(kintoObject, embeddedObjects);
                                    if (!collectionFound) {
                                        String errorMessage = "Cant' find the collection for the embedded object type "
                                                + type
                                                + ".";
                                        throw new KintoException(errorMessage);
                                    }
                                }
                            }
                        } catch (NoSuchFieldException exception) {
                            String errorMessage = "Can't get the attribute "
                                    + attributeName
                                    + " using reflection: "
                                    + exception.getMessage();
                            if (loggerIsAvailable) {
                                logger.log(LogEntry.Severity.ERROR, errorMessage);
                            }
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            throw new KintoException(errorMessage);
                        } catch (IllegalAccessException exception) {
                            String errorMessage = "Getting value for attribute "
                                    + attributeName
                                    + " using reflection failed: "
                                    + exception.getMessage();
                            if (loggerIsAvailable) {
                                logger.log(LogEntry.Severity.ERROR, errorMessage);
                            }
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

    public void clearLocalDataStore() throws KintoException {
        if (isLocalDBConnected) {
            try {
                // use transaction
                localdbConnection.setAutoCommit(false);
                Statement statement = localdbConnection.createStatement();
                String sql = new String("PRAGMA writable_schema = 1;");
                statement.execute(sql);
                sql = new String("DELETE FROM sqlite_master "
                        + "WHERE type in ('view', 'table', 'index', 'trigger');");
                statement.execute(sql);
                sql = new String("PRAGMA writable_schema = 0;");
                statement.execute(sql);
                // commit all commands
                localdbConnection.commit();
                localdbConnection.setAutoCommit(true);
                // free the space
                // sql = new String("VACUUM;");
                // statement.execute(sql);
            } catch (SQLException exception) {
                try {
                    // rollback all transactions
                    localdbConnection.rollback();
                } catch (SQLException sqlException) {
                    if (Global.DEBUG) {
                        sqlException.printStackTrace();
                    }
                }
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
                String errorMessage = "Error occurred while remove all collections from local datastore: "
                        + exception;
                throw new KintoException(errorMessage);
            } finally {
                close();
                isLocalDBConnected = false;
                System.out.println("Local datastore cleared. Connection to database closed.");
            }
        }
    }

    // Publish all local data to the server, import remote changes
    public void sync() {

    }

    public boolean syncEnabled() {
        return syncEnabled;
    }

    public boolean isOpen() {
        return isLocalDBConnected;
    }

    public void close() {
        if (isLocalDBConnected) {
            try {
                localdbConnection.close();
                localdbConnection = null;
            } catch (SQLException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
            }
        }
    }

    private Kinto(KintoConfiguration kintoConfiguration) throws KintoException {
        this.kintoConfiguration = kintoConfiguration;
        // check managed objects
        int size = kintoConfiguration.getObjectTypes().size();
        if (size == 0) {
            throw new KintoException("There are no managed object types in configuration.");
        }

        // activate logging
        try {
            this.logger = Logger.getInstance();
            loggerIsAvailable = true;
        } catch (InstantiationException exception) {
            loggerIsAvailable = false;
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
        collections = new ArrayList<>(size);
        isLocalDBConnected = false;
        syncEnabled = false;

        // initialize the local datastore for the collection
        initLocalDB();
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

    // create / open local (sql) datastore
    private void initLocalDB() throws KintoException {
        // create or open local sqlite db
        // connect to an SQLite database (bucket) that does not exist, it automatically creates a new database
        try {
            // use sqlite jdbc driver
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + kintoConfiguration.getLocaldbPath();
            this.localdbConnection = DriverManager.getConnection(url);
            this.isLocalDBConnected = true;
            if (Global.DEBUG) {
                this.logger.log(LogEntry.Severity.INFO, "Connection to SQLite has been established.");
            }
        } catch (ClassNotFoundException exception) {
            // sqlite driver not found
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "The JDBC driver for SQLite wasn't found: "
                    + exception;

            throw new KintoException(errorMessage);
        } catch (SQLException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "Can't access the local datastore: "
                    + exception;
            throw new KintoException(errorMessage);
        }
    }
}

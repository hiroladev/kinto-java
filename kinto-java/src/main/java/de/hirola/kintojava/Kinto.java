package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;
import de.hirola.kintojava.model.KintoObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * The singleton object for data management with sqlite and kinto.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public final class Kinto {

    private static Kinto instance;
    private KintoConfiguration kintoConfiguration;
    private Logger logger;
    private String bucket;
    private ArrayList<KintoCollection> collections;
    private Connection localdbConnection;
    private boolean loggerIsAvailable;
    private boolean localdbConnected;
    private boolean syncEnabled;

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
        localdbConnected = false;
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
            Class.forName("org.sqlite.JDBC");

            String url = "jdbc:sqlite:" + kintoConfiguration.getLocaldbPath();
            this.localdbConnection = DriverManager.getConnection(url);
            this.localdbConnected = true;
            if (Global.DEBUG) {
                this.logger.log(LogEntry.Severity.INFO, "Connection to SQLite has been established.");
            }
        } catch (Exception exception) {
            throw new KintoException(exception);
        }
    }

    // create / check relations (tables)
    private void manageRelations() {

    }
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

    public Connection getlocaldbConnection() {
        return this.localdbConnection;
    }

    public void login(Credentials credentials) {

    }

    /**
     * Add a new object to the local datastore.
     * If the object exists, it is updated with the new properties.
     *
     *
     * @param object Object to be added to the local datastore.
     * @return The unique id for the object.
     */
    public void add(KintoObject object) throws KintoException {
        Iterator<KintoCollection> iterator = collections.stream().iterator();
        while (iterator.hasNext()) {
            KintoCollection collection = iterator.next();
            if (collection.getType().equals(object.getClass())) {
                collection.addRecord(object);
            }
        }
    }

    public void update(KintoObject object) {

    }

    public void remove(KintoObject object) {

    }

    public List<KintoObject> findAll(Class<? extends KintoObject> type) {
        return null;
    }

    // Publish all local data to the server, import remote changes
    public void sync() {

    }

    public void close() {
        if (localdbConnected) {
            try {
                this.localdbConnection.close();
            } catch (SQLException exception) {
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public boolean syncEnabled() {
        return syncEnabled;
    }
}

package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;
import org.apache.commons.math3.analysis.function.Log;

import javax.naming.directory.InvalidAttributesException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * The singleton object for data management with sqllite and kinto.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public final class Kinto {

    private static Kinto instance;
    private KintoConfiguration configuration;
    private Logger logger;
    private String bucket;
    // key=class, value=collection object
    private HashMap<Class<? extends KintoObject>,Collection> collections;
    private Connection localdbConnection;
    private boolean loggerIsAvailable;
    private boolean localdbConnected;
    private boolean syncEnabled;

    private void initLocalDB() throws SQLException {
        // create or open local sqlite db
        // connect to an SQLite database (bucket) that does not exist, it automatically creates a new database
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            new SQLException(exception.getMessage());
        }
        String url = "jdbc:sqlite:" + this.configuration.getLocaldbPath();
        this.localdbConnection = DriverManager.getConnection(url);
        this.localdbConnected = true;
        if (Global.DEBUG) {
            this.logger.log(LogEntry.Severity.INFO, "Connection to SQLite has been established.");
        }
    }

    private Kinto(KintoConfiguration configuration) throws InstantiationException {
        this.configuration = configuration;
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
        this.localdbConnected = false;
        this.syncEnabled = false;

        try {
            // create or open local db
            initLocalDB();
        } catch (SQLException exception) {
            String errorMessage = "Error occurred while creating or accessing local database.";
            if (loggerIsAvailable) {
                this.logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new InstantiationException(errorMessage);
        }
    }

    private void createBucket() {}

    /*
        create a table for a class of objects, if not exits
     */
    private void createCollection() {}

    /**
     * Create a singleton instance for local data management and sync.
     *
     * @param configuration configuration for local storage and sync
     * @return singleton object for data management
     */
    public static Kinto getInstance(KintoConfiguration configuration) throws InstantiationException {
        if (instance == null) {
            instance = new Kinto(configuration);
        }
        return instance;
    }

    public Connection getlocaldbConnection() {
        return this.localdbConnection;
    }

    public void login(Credentials credentials) {

    }

    public String add(KintoObject object) {
        // reflection 1:1 and 1:m
        return "ID";
    }

    public void update(KintoObject object) {

    }

    public void remove(KintoObject object) {

    }

    public List<KintoObject> findAll(Class<? extends KintoObject> type) {
        try {
            if (this.collections == null) {
                // no collections in cache
                if (Global.DEBUG) {
                    String message = "No collections in cache.";
                    logger.log(LogEntry.Severity.DEBUG, message);
                }
                Collection collection = new Collection(type, this);
                collections = new HashMap<>();
                collections.put(type, collection);
            } else {
                if (!collections.containsKey(type)) {
                    if (Global.DEBUG) {
                        String message = "Collection of " + type.toString() + " is not in cache.";
                        logger.log(LogEntry.Severity.DEBUG, message);
                    }
                    Collection collection = new Collection(type, this);
                    collections.put(type, collection);
                }
            }
            return collections.get(type).findAll();
        } catch (InvalidAttributesException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            return null;
        }
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

package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * A collection contains a list of objects from same type.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public class Collection {

    private Logger logger;
    private Connection localdbConnection;
    private Class<KintoObject> type;
    private boolean loggerIsAvailable;
    private boolean tableCreated;
    private boolean isSynced;

    private boolean collectionExists() {
        // local
        String sql = "SELECT \"name\" FROM pragma_table_info(\"" + type.getName() + "\") LIMIT 1;";
        System.out.println(sql);
        try {
            Statement statement = localdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            // table exist
            if (resultSet.first()) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Collection of " + type.toString() + " exists in local datastore.";
                    logger.log(LogEntry.Severity.DEBUG,message);
                }
                return true;
            }
            // create table for collection
            sql = "";
        } catch (SQLException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
        if (Global.DEBUG && loggerIsAvailable) {
            String message = "Collection of " + type.toString() + " exists not in local datastore.";
            logger.log(LogEntry.Severity.DEBUG,message);
        }

        // kinto remote

        return false;
    }

    /**
     * Create a collection for objects of class type.
     *
     * @param type Type of objects
     */
    public Collection(Class<KintoObject> type, Kinto kinto) {
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
        this.localdbConnection = kinto.getlocaldbConnection();
        this.type = type;
        // check if table for collection exists
        // local and remote
       tableCreated = collectionExists();
    }

    /**
     * Returns the name of collection.
     *
     * @return name of collection
     */
    public String getName() {
        return this.type.getName();
    }

    /**
     *
     * @return <code>true</code> if the collection exits in remote kinto
     */
    public boolean isSynced() {
        return this.isSynced;
    }

    public List<KintoObject> findAll() {
        return null;
    }

    public List<KintoObject> findByQuery(KintoQuery query) {
        return null;
    }
}

package de.hirola.kintojava;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.*;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * The transparent access to the database on Android and JVM.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public class KintoDatabaseAdapter {

    private final String TAG = KintoDatabaseAdapter.class.getSimpleName();

    // driver for jdbc connection
    private final static String JDBC_DRIVER = "org.sqlite.JDBC";
    // url for jdbc connection
    private final static String JDB_URL_PREFIX = "jdbc:sqlite:" ;

    private final Connection database; // we use the H2 as embedded database
    private final KintoLogger logger = KintoLogger.getInstance("debug-sql"); // log sql for debug

    /**
     * Create an adapter to access to the local database on Android and JVM.
     * The name of the app is used for the database name.
     *
     * @param appPackageName name of app
     * @throws KintoException if error occurred while creating / accessing the local database.
     */
    public KintoDatabaseAdapter(@NotNull String appPackageName) throws KintoException {
        String databasePath;
        String databaseName;
        if (appPackageName.contains(".")) {
            databaseName = appPackageName.substring(appPackageName.lastIndexOf(".") + 1);
        } else {
            databaseName = appPackageName;
        }
        // build the path, determine if android or jvm
        // see https://developer.android.com/reference/java/lang/System#getProperties()
        try {
            String vendor = System.getProperty("java.vm.vendor"); // can be null
            if (vendor != null) {
                if (vendor.equals("The Android Project")) {
                    // path for local database on Android
                    databasePath = "/data/data/" + appPackageName + "/" + databaseName + ".db";
                } else {
                    //  path for local database on JVM
                    String userHomeDir = System.getProperty("user.home");
                    databasePath = userHomeDir + File.separator + ".kinto-java" + File.separator + databaseName + ".db";
                }
            } else {
                throw new KintoException("Could not determine the runtime environment.");
            }
        } catch (SecurityException exception){
            throw new KintoException("Could not determine the runtime environment.");
        }
        // create or open local sqlite db
        // connect to an SQLite database (bucket) that does not exist, it automatically creates a new database
        try {
            // we use a jdbc compliant database on Android and JVM
            // register the driver
            Class.forName (JDBC_DRIVER);
            String url = JDB_URL_PREFIX + databasePath;
            database = DriverManager.getConnection(url);
       } catch (ClassNotFoundException exception) {
            throw new KintoException("The JDBC driver was not found: " + exception.getMessage());
        } catch (SQLException exception) {
            throw new KintoException("Can't access the local datastore: " + exception.getMessage());
        }
    }

    /**
     * Execute a sql statement.
     *
     * @param sql statement to be execute
     * @throws SQLException if an error occurred while accessing database
     */
    public void executeSQL(String sql) throws SQLException {
        //TODO: check for sql inject?
        if (Global.DEBUG_SQL) {
            logger.log(KintoLogger.DEBUG,TAG, sql, null);
        }
        database.createStatement().execute(sql);
    }

    /**
     * A layer to execute a given query und returns the result of the query.
     *
     * @param sql query to be execute
     * @return The result of the given query
     * @throws SQLException if an error occurred while accessing database
     * @see KintoQueryResultSet
     */
    public KintoQueryResultSet executeQuery(String sql) throws SQLException {
        //TODO check for inject sql?
        if (Global.DEBUG_SQL) {
            logger.log(KintoLogger.DEBUG,TAG, sql, null);
        }
        Statement statement = database.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        return new KintoQueryResultSet(resultSet);
    }

    /**
     * A layer to begin a transaction.
     *
     * @throws SQLException if an error occurred while accessing database
     */
    public void beginTransaction() throws SQLException {
        database.setAutoCommit(false);
    }

    /**
     * A layer to end a transaction.
     *
     * @throws SQLException if an error occurred while accessing database
     */
    public void commit() throws SQLException {
        boolean errorOccurred = false;
        Exception commitException = null;
        try {
            database.commit();
        } catch (SQLException exception) {
            commitException = exception;
            errorOccurred = true;
        } finally {
            // default: transactions not used
            database.setAutoCommit(true);
        }
        if (errorOccurred) {
            throw new SQLException(commitException);
        }
    }

    /**
     * A layer to roll back a transaction.
     *
     * @throws SQLException if an error occurred while accessing database
     */
    public void rollback() throws SQLException {
        database.rollback();
    }

    /**
     * Get the access state of the local database.
     *
     * @return A flag to determine if the local database is open.
     */
    public boolean isOpen() {
        try {
            return !database.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    /**
     * Close the database.
     *
     * @throws SQLException if an error occurred while closing database
     */
    public void close() throws SQLException {
        database.close();
    }

}

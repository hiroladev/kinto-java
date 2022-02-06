package de.hirola.kintojava;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.Cursor;

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

    private final boolean isRunningOnAndroid; // flag for runtime
    private Connection jvmDatabase; // JVM database
    private SQLiteDatabase androidDatabase; // Android database

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
                    // Android
                    isRunningOnAndroid = true;
                    // path for local database on Android
                    databasePath = "/data/data/" + appPackageName + "/" + databaseName + ".sqlite";
                } else {
                    // JVM
                    isRunningOnAndroid = false;
                    //  path for local database on JVM
                    String userHomeDir = System.getProperty("user.home");
                    databasePath = userHomeDir + File.separator + ".kinto-java" + File.separator + databaseName + ".sqlite";
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
            // on Android use the built-in SQLite database implementation
            if (isRunningOnAndroid) {
                androidDatabase = SQLiteDatabase.openOrCreateDatabase(
                        databasePath, null, null);
            } else {
                // on JVM use sqlite jdbc driver
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:" + databasePath;
                jvmDatabase = DriverManager.getConnection(url);
            }
        } catch (SQLiteException exception) {
            // database couldn't open
            throw new KintoException("The database can't be opened: " + exception.getMessage());
        } catch (ClassNotFoundException exception) {
            // sqlite driver not found or database couldn't open
            throw new KintoException("The JDBC driver for SQLite wasn't found: " + exception.getMessage());
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
            System.out.println(sql);
        }
        if (isRunningOnAndroid) {
            // Android
            androidDatabase.execSQL(sql);
        } else {
            // JVM
            jvmDatabase.createStatement().execute(sql);
        }
    }

    /**
     * A layer to execute a given query und returns the result of the query.
     *
     * @param sql query to be execute
     * @return The result of the given query.
     * @throws SQLException if an error occurred while accessing database
     * @see KintoQueryResultSet
     */
    public KintoQueryResultSet executeQuery(String sql) throws SQLException {
        //TODO check for inject sql?
        if (Global.DEBUG_SQL) {
            System.out.println(sql);
        }
        if (isRunningOnAndroid) {
            // Android
            //  SQL string must not be ; terminated
            if (sql.endsWith(";")) {
                sql = sql.substring(0,sql.lastIndexOf(";"));
            }
            Cursor cursor = androidDatabase.rawQuery(sql, null);
            return new KintoQueryResultSet(cursor);
        } else {
            // JVM
            Statement statement = jvmDatabase.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            return new KintoQueryResultSet(resultSet);
        }
    }

    /**
     * A layer to begin a transaction.
     *
     * @throws SQLException if an error occurred while accessing database
     */
    public void beginTransaction() throws SQLException {
        if (isRunningOnAndroid) {
            // Android
            androidDatabase.beginTransaction();
        } else {
            // JVM
            jvmDatabase.setAutoCommit(false);
        }
    }

    /**
     * A layer to end a transaction.
     *
     * @throws SQLException if an error occurred while accessing database
     */
    public void commit() throws SQLException {
        boolean errorOccurred = false;
        Exception commitException = null;
        if (isRunningOnAndroid) {
            // Android
            try {
                androidDatabase.setTransactionSuccessful();
            } catch (IllegalStateException exception) {
                // not in transaction or transaction already succeeded
                commitException = exception;
                errorOccurred = true;
             } finally {
                androidDatabase.endTransaction();
            }
        } else {
            // JVM
            try {
                jvmDatabase.commit();
            } catch (SQLException exception) {
                commitException = exception;
                errorOccurred = true;
            } finally {
                // default: transactions not used
                jvmDatabase.setAutoCommit(true);
            }
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
        if (isRunningOnAndroid) {
            // Android
            if (androidDatabase.inTransaction()) {
                androidDatabase.endTransaction();
            }
        } else {
            // JVM
            jvmDatabase.rollback();
        }
    }

    /**
     * Get the access state of the local database.
     *
     * @return A flag to determine if the local database is open.
     */
    public boolean isOpen() {
        if (isRunningOnAndroid) {
            // Android
            return androidDatabase.isOpen();
        } else {
            // JVM
            try {
                return !jvmDatabase.isClosed();
            } catch (SQLException exception) {
                return false;
            }
        }
    }

    /**
     * Close the database.
     *
     * @throws SQLException if an error occurred while closing database
     */
    public void close() throws SQLException {
        if (isRunningOnAndroid) {
            // Android
            androidDatabase.close();
        } else {
            // JVM
            jvmDatabase.close();
        }
    }

}

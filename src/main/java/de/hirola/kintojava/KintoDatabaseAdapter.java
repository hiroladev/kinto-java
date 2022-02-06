package de.hirola.kintojava;

import de.hirola.kintojava.logger.KintoLogger;

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
 * The transparent access to the database.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public class KintoDatabaseAdapter {

    private static final String TAG = KintoDatabaseAdapter.class.getSimpleName();

    private static KintoDatabaseAdapter instance;
    private boolean isRunningOnAndroid;
    private Connection jvmDatabase; // JVM database
    private SQLiteDatabase androidDatabase; // Android database

    public static KintoDatabaseAdapter getInstance(Kinto kinto, String appPackageName) throws KintoException {
        if (instance == null) {
            instance = new KintoDatabaseAdapter(kinto, appPackageName);
        }
        return instance;
    }

    public void executeSQL(String sql) throws SQLException {
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

    public KintoQueryResultSet executeQuery(String sql) throws SQLException {
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

    public void beginTransaction() throws SQLException {
        if (isRunningOnAndroid) {
            // Android
            androidDatabase.beginTransaction();
        } else {
            // JVM
            jvmDatabase.setAutoCommit(false);
        }
    }

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

    public void close() throws SQLException {
        if (isRunningOnAndroid) {
            // Android
            androidDatabase.close();
        } else {
            // JVM
            jvmDatabase.close();
        }
    }

    private KintoDatabaseAdapter(@NotNull Kinto kinto, @NotNull String appPackageName) throws KintoException {
        KintoLogger kintoLogger = kinto.getKintoLogger();
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
            if (System.getProperty("java.vm.vendor").equals("The Android Project")) {
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
        } catch (SecurityException exception){
            isRunningOnAndroid = false;
            //  path for local database on JVM
            String userHomeDir = System.getProperty("user.home");
            databasePath = userHomeDir + File.separator + ".kinto-java" + File.separator + databaseName + ".sqlite";
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
            if (Global.DEBUG) {
                kintoLogger.log(KintoLogger.DEBUG, TAG, "Connection to SQLite has been established.", null);
            }
        } catch (SQLiteException exception) {
            // database couldn't open
            if (Global.DEBUG) {
                kintoLogger.log(KintoLogger.DEBUG, TAG, "Local database could not opened.", exception);
            }
            String errorMessage = "The database can't be opened: "
                    + exception;
            throw new KintoException(errorMessage);
        } catch (ClassNotFoundException exception) {
            // sqlite driver not found or database couldn't open
            if (Global.DEBUG) {
                kintoLogger.log(KintoLogger.DEBUG, TAG, "sqlite driver not found or database could not open.", exception);
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

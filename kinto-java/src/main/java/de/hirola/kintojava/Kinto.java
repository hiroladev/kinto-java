package de.hirola.kintojava;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
    private String bucket;
    private Connection localdbConnection;
    private boolean localdbConnected;
    private boolean syncEnabled;

    private Kinto(KintoConfiguration configuration) {
        this.configuration = configuration;
        this.localdbConnected = false;
        this.syncEnabled = false;
        // 1. create or open local db
        this.initLocalDB();

    }

    /**
     * Create a singleton instance for local data management and sync.
     *
     * @param configuration configuration for local storage and sync
     * @return singleton object for data management
     */
    public static Kinto getInstance(KintoConfiguration configuration) {
        if (instance == null) {
            instance = new Kinto(configuration);
        }
        return instance;
    }

    public void login(Credentials credentials) {

    }

    private void initLocalDB() {
        //  create or open local sqlite db
        // connect to an SQLite database that does not exist, it automatically creates a new database
        String url = "jdbc:sqlite:" + this.configuration.getLocaldbPath();
        try {
            // create a connection to the database
            this.localdbConnection = DriverManager.getConnection(url);
            this.localdbConnected = true;
            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                if (this.localdbConnection != null) {
                    this.localdbConnection.close();
                }
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}

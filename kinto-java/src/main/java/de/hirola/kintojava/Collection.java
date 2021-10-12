package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;

import javax.naming.directory.InvalidAttributesException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * A collection contains a list of objects from same type.
 * If a collections exists, you must not change the fields of the objects (schema)!
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public class Collection {

    private Logger logger;
    private Connection localdbConnection;
    private Class type;
    private boolean loggerIsAvailable;
    private boolean tableCreated;
    private boolean isSynced;

    private boolean collectionExistsLocal() throws InvalidAttributesException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + type.getSimpleName() + "';";
        try {
            Statement statement = localdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            // table exists?
            // A TYPE_FORWARD_ONLY ResultSet only supports next() for navigation,
            // and not methods like first(), last(), absolute(int), relative(int).
            // The JDBC specification explicitly defines those to throw a SQLException if called on a TYPE_FORWARD_ONLY.
            // TABLE EXISTS LOCAL
            if (resultSet.next()) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Collection of " + type.toString() + " exists in local datastore.";
                    logger.log(LogEntry.Severity.DEBUG,message);
                }
                // check schema
                // The sqlite_schema table contains one row for each table, index, view,
                // and trigger (collectively "objects") in the schema,
                // except there is no entry for the sqlite_schema table itself.
                // The text in the sqlite_schema.sql column is a copy of the original CREATE statement text
                // that created the object,
                // except normalized as described above and as modified by subsequent ALTER TABLE statements.
                sql = "SELECT sql FROM sqlite_master WHERE type='table' AND name='" + type.getSimpleName() + "';";
                resultSet = statement.executeQuery(sql);
                return true;
            }
            // create table for collection
            if (Global.DEBUG && loggerIsAvailable) {
                String message = "Collection of " + type.getSimpleName() + " does not exists in local datastore.";
                logger.log(LogEntry.Severity.DEBUG,message);
            }
            // SQLite is "typeless". This means that you can store any kind of data you want in any column of any table,
            // regardless of the declared datatype of that column.
            try {
                // build sql statement for creating table
                // use reflection to map attributes to columns
                Field declaredFields[] = type.getDeclaredFields();
                Iterator<Field> iterator = Arrays.stream(declaredFields).iterator();
                ArrayList<String> columns = new ArrayList((int) Arrays.stream(declaredFields).count());
                while (iterator.hasNext()) {
                    Field attribute = iterator.next();
                    String columnName = attribute.getName();
                    //  1. "embedded" kinto objects
                    Class attributeSuperClass = attribute.getType().getSuperclass();
                    if (attributeSuperClass != null) {
                        if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                            // "foreign key" -> rename column to lowerclassname+id
                            String attributeClassName = attribute.getType().getSimpleName().toLowerCase(Locale.ROOT);
                            columnName = attributeClassName + "id";
                        }
                    }
                    columns.add(columnName);
                }
                // build the sql statement
                sql = "CREATE TABLE " + type.getSimpleName() +"(id PRIMARY KEY";
                for (int i = 0; i < columns.size(); i++) {
                    String columnName = columns.get(i);
                    sql += "," + columnName;
                }
                sql += ");";
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Create Collection " + type.getSimpleName() + " with sql command: " + sql;
                    logger.log(LogEntry.Severity.INFO,message);
                }
                // create the table in local datastore
                statement.execute(sql);
            }
            catch (Throwable exception) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Reflection of " + type.getName() + " failed, can't create table for collection.";
                    logger.log(LogEntry.Severity.ERROR,message);
                }
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
                return false;
            }
        } catch (SQLException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            return false;
        }
        // default return value
        return false;
    }

    /**
     * Create a collection for objects of class type.
     *
     * @param type Type of objects in collections
     * @throws InvalidAttributesException
     */
    public Collection(Class<? extends KintoObject> type, Kinto kinto) throws InvalidAttributesException {
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
        this.isSynced = kinto.syncEnabled();
        // check if table for collection exists
        // local and remote
       tableCreated = collectionExistsLocal();
    }

    /**
     * Returns the name of collection.
     *
     * @return name of collection
     */
    public String getName() {
        return type.getSimpleName();
    }

    /**
     *
     * @return <code>true</code> if the collection exits in remote kinto
     */
    public boolean isSynced() {
        return isSynced;
    }

    public List<KintoObject> findAll() {
        return null;
    }

    public List<KintoObject> findByQuery(KintoQuery query) {
        return null;
    }
}

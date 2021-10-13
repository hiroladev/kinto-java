package de.hirola.kintojava;

import de.hirola.kintojava.logger.KintoException;
import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
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
public class KintoCollection {

    private Logger logger;
    private final Connection localdbConnection;
    private final Class<? extends KintoObject> type;
    private boolean loggerIsAvailable;
    private boolean isSynced;

    private void createCollectionLocal() throws KintoException {
        StringBuilder sql = new StringBuilder("SELECT name FROM sqlite_master WHERE type='table' AND name='");
        sql.append(type.getSimpleName());
        sql.append("';");
        try {
            Statement statement = localdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql.toString());
            // table exists?
            // A TYPE_FORWARD_ONLY ResultSet only supports next() for navigation,
            // and not methods like first(), last(), absolute(int), relative(int).
            // The JDBC specification explicitly defines those to throw a SQLException if called on a TYPE_FORWARD_ONLY.
            // TABLE EXISTS LOCAL
            if (resultSet.next()) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "KintoCollection of " + type + " exists in local datastore.";
                    logger.log(LogEntry.Severity.DEBUG,message);
                }
                // check schema
                // The sqlite_schema table contains one row for each table, index, view,
                // and trigger (collectively "objects") in the schema,
                // except there is no entry for the sqlite_schema table itself.
                // The text in the sqlite_schema.sql column is a copy of the original CREATE statement text
                // that created the object,
                // except normalized as described above and as modified by subsequent ALTER TABLE statements.
                sql = new StringBuilder("SELECT sql FROM sqlite_master WHERE type='table' AND name='");
                sql.append(type.getSimpleName());
                sql.append("';");
                resultSet = statement.executeQuery(sql.toString());
            }
            // create table for collection
            if (Global.DEBUG && loggerIsAvailable) {
                String message = "KintoCollection of " + type.getSimpleName() + " does not exists in local datastore.";
                logger.log(LogEntry.Severity.DEBUG,message);
            }
            // SQLite is "typeless". This means that you can store any kind of data you want in any column of any table,
            // regardless of the declared datatype of that column.
            try {
                // building the sql statement for creating table
                // use reflection to map attributes to columns
                Field[] declaredFields = type.getDeclaredFields();
                Iterator<Field> iterator = Arrays.stream(declaredFields).iterator();
                ArrayList<String> columns = new ArrayList<>((int) Arrays.stream(declaredFields).count());
                while (iterator.hasNext()) {
                    Field attribute = iterator.next();
                    String columnName = attribute.getName();
                    //  1. "embedded" kinto objects (1:1 relations)
                    Class<?> attributeSuperClass = attribute.getType().getSuperclass();
                    if (attributeSuperClass != null) {
                        if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                            // "foreign key" -> rename column to lower classname+id
                            String attributeClassName = attribute.getType().getSimpleName().toLowerCase(Locale.ROOT);
                            columnName = attributeClassName + "id";
                        }
                    }
                    // 2. "embedded" List of kinto objects (1:m relations)
                    if (attribute.getType().getSimpleName().equalsIgnoreCase("ArrayList")) {
                        // build the sql statement for the collection relation table
                        // <name of type>+idTO<name of type>+id
                        String attributeDeclaringClassName = attribute.getDeclaringClass().getSimpleName();
                        // get the class name of type in list (https://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list)
                        String attributeClassName = ((Class<?>)((ParameterizedType)attribute.getGenericType()).getActualTypeArguments()[0]).getSimpleName();
                        sql = new StringBuilder("CREATE TABLE ");
                        sql.append(attributeDeclaringClassName);
                        sql.append("TO");
                        sql.append(attributeClassName);
                        sql.append(" (");
                        sql.append(attributeDeclaringClassName.toLowerCase(Locale.ROOT));
                        sql.append("id, ");
                        sql.append(attributeClassName.toLowerCase(Locale.ROOT));
                        sql.append("id);");
                        if (Global.DEBUG && loggerIsAvailable) {
                            String message = "Create one-to-many relation table for "
                                    + attributeDeclaringClassName + " and "+ attributeClassName
                                    +" with sql command: " + sql + ".";
                            logger.log(LogEntry.Severity.INFO,message);
                        }
                        // create the table in local datastore
                        statement.execute(sql.toString());
                    }
                    columns.add(columnName);
                }
                // build the sql statement for the collection table
                // id from sqlite, kintoid from kinto, usn = update sequence number
                sql = new StringBuilder("CREATE TABLE ");
                sql.append(type.getSimpleName());
                sql.append("(id PRIMARY KEY, kintoid, usn ");
                for (String columnName : columns) {
                    sql.append(",");
                    sql.append(columnName);
                }
                sql.append(");");
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Create KintoCollection "
                            + type.getSimpleName()
                            + " with sql command: " + sql + ".";
                    logger.log(LogEntry.Severity.INFO,message);
                }
                // create the table in local datastore
                statement.execute(sql.toString());
            }
            catch (Throwable exception) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Reflection of " + type.getName() + " failed, can't create table for collection.";
                    logger.log(LogEntry.Severity.ERROR,message);
                }
                if (Global.DEBUG) {
                    exception.printStackTrace();
                }
                throw new KintoException(exception.getMessage());
            }
        } catch (SQLException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(exception.getMessage());
        }
    }

    /**
     * Create a collection for objects of class type.
     *
     * @param type Type of objects in collections.
     * @param kinto The kinto object for datastore operations.
     * @throws KintoException if collection couldn't initialize.
     */
    public KintoCollection(Class<? extends KintoObject> type, Kinto kinto) throws KintoException {
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
        createCollectionLocal();
    }

    /**
     * .
     * @return Type of objects in the collection
     */
    private Class<? extends KintoObject> getType() {
        return type;
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

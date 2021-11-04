package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;
import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    // storable attributes
    // attribute name, dataset
    HashMap<String,DataSet> storableAttributes;
    // 1:m relations for embedded KintoObject (Class) in table (String)
    private final HashMap<Class<? extends KintoObject>, String> relationTables;
    private boolean loggerIsAvailable;
    private boolean isSynced;

    /**
     * Create a collection for objects of class type.
     *
     * @param type Type of objects in collections.
     * @param kinto The kinto object for datastore operations.
     * @throws KintoException if collection couldn't initialize.
     */
    public KintoCollection(Class<? extends KintoObject> type, Kinto kinto) throws KintoException {
        // build the list of persistent attributes
        storableAttributes = buildAttributesMap(type);
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
        relationTables = new HashMap<>();
        // TODO: Synchronisation
        this.isSynced = false;
        // check if table for collection exists
        // local and remote
        createCollectionLocal();
    }
    /**
     * .
     * @return Type of objects in the collection
     */
    public Class<? extends KintoObject> getType() {
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

    public void addRecord(KintoObject kintoObject) throws KintoException {
        // object from collection type?
        if (isValidObjectType(kintoObject)) {
            // all embedded kinto objects in local datastore?
            // check, if has the object an objectUID
            for (String attributeName : storableAttributes.keySet()) {
                DataSet dataSet = storableAttributes.get(attributeName);
                // 1. 1:1 relations
                if (dataSet.isKintoObject()) {
                    // getAttribute()
                    // capitalize the first letter of a string
                    String methodName = "get" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
                    try {
                        //  obj.getClass().newInstance()
                        Method getEmbeddedObjectMethod = kintoObject.getClass().getMethod(methodName);
                        KintoObject embeddedObject = (KintoObject) getEmbeddedObjectMethod.invoke(kintoObject);
                        if (isNewRecord(embeddedObject)) {
                            String errorMessage = "The embedded object from type "
                                    + embeddedObject.getClass().getSimpleName()
                                    + " must exist in datastore before saving this object.";
                            throw new KintoException(errorMessage);
                        }
                    } catch (NoSuchMethodException exception) {
                        String errorMessage = "The setter method \"" + methodName + "\" for the embedded object was not found.";
                        errorMessage = errorMessage + " - " + exception.getMessage();
                        if (loggerIsAvailable) {
                            logger.log(LogEntry.Severity.ERROR, errorMessage);
                        }
                        if (Global.DEBUG) {
                            exception.printStackTrace();
                        }
                        throw new KintoException(errorMessage);
                    } catch (InvocationTargetException exception) {
                        if (loggerIsAvailable) {
                            logger.log(LogEntry.Severity.ERROR, exception.getMessage());
                        }
                        if (Global.DEBUG) {
                            exception.printStackTrace();
                        }
                    } catch (IllegalAccessException exception) {
                        if (loggerIsAvailable) {
                            logger.log(LogEntry.Severity.ERROR, exception.getMessage());
                        }
                        if (Global.DEBUG) {
                            exception.printStackTrace();
                        }
                    }
                }
                // TODO 2. 1:m relations
            }
            // insert or update?
            if (isNewRecord(kintoObject)) {
                // building sql insert command
                // INSERT INTO table (column1,column2 ,..) VALUES( value1,	value2 ,...);
                StringBuilder sql = new StringBuilder("INSERT INTO ");
                // the name of the collection (table)
                sql.append(getName());
                // attributes = columns
                // build a map with attribute and value
                sql.append(" (uuid, kintoid, usn, ");
                // all attributes -> columns
                int loops = 1;
                int size = storableAttributes.size();
                // attributes and values in same order!
                StringBuilder valuesString = new StringBuilder(") VALUES(");
                //  primary key from uuid
                valuesString.append("'");
                valuesString.append(kintoObject.getUUID());
                // kinto record id later from sync
                // usn = 0 on insert
                valuesString.append("','', 0, ");
                for (String attributeName : storableAttributes.keySet()) {
                    sql.append(attributeName);
                    valuesString.append("'");
                    valuesString.append(getValueForAttributeAsString(kintoObject, attributeName));
                    valuesString.append("'");
                    if (loops < size) {
                        sql.append(" ,");
                        valuesString.append(" ,");
                    }
                    loops++;
                }
                sql.append(valuesString.toString());
                sql.append(");");
                try {
                    Statement statement = localdbConnection.createStatement();
                    statement.execute(sql.toString());
                } catch (SQLException exception) {
                    String errorMessage = "Error adding object to local datastore: " + exception.getMessage();
                    throw new KintoException(errorMessage);
                }
            } else {
                // update
                updateRecord(kintoObject);
            }
        }
    }

    public void updateRecord(KintoObject kintoObject) throws KintoException {
        if (isNewRecord(kintoObject)) {
           throw new KintoException("Can't update a non existent object.");
        }
        // update in local datastore
        // increment the usn
    }

    public List<KintoObject> findAll() {
        return null;
    }

    public List<KintoObject> findByQuery(KintoQuery query) {
        return null;
    }

    // build a map with attribute and value for the object
    // HashMap<attribute name, data set>
    private HashMap<String,DataSet> buildAttributesMap(Class<? extends KintoObject> type) throws KintoException {
        HashMap<String, DataSet> attributes = new HashMap<>();
        try {
            // use reflection to get (storable) attributes of the objects
            // fields with annotation @Persisted
            Field[] declaredFields = type.getDeclaredFields();
            Iterator<Field> iterator = Arrays.stream(declaredFields).iterator();
            ArrayList<String> columns = new ArrayList<>();
            while (iterator.hasNext()) {
                Field attribute = iterator.next();
                // attribute must not be an array
                if (attribute.isAnnotationPresent(Persisted.class) && !attribute.getType().getSimpleName().equalsIgnoreCase("ArrayList")) {
                    DataSet dataSet = new DataSet(attribute);
                    attributes.put(attribute.getName(), dataSet);
                }
            }
            if (attributes.isEmpty()) {
                String message = "Object has no attributes with annotations. Can't create table for collection.";
                if (loggerIsAvailable) {
                    logger.log(LogEntry.Severity.ERROR, message);
                }
                if (Global.DEBUG) {
                    System.out.println(message);
                }
                throw new KintoException(message);
            }
        } catch (Throwable exception) {
            if (loggerIsAvailable) {
                String message = "Reflection of " + this.type.getName() + " failed, can't create table for collection.";
                logger.log(LogEntry.Severity.ERROR, message);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(exception.getMessage());
        }
        return attributes;
    }

    private void createCollectionLocal() throws KintoException {
        try {
            StringBuilder sql = new StringBuilder("SELECT name FROM sqlite_master WHERE type='table' AND name='");
            sql.append(type.getSimpleName());
            sql.append("';");
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
                    logger.log(LogEntry.Severity.DEBUG, message);
                }
                // TODO Schema-Check

            } else {
                // create table for collection
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "KintoCollection of " + type.getSimpleName() + " does not exists in local datastore.";
                    logger.log(LogEntry.Severity.DEBUG, message);
                }
                // SQLite store any kind of data you want in any column of any table
                try {
                    // building the sql statement for creating table
                    // use reflection to map attributes to columns
                    Field[] declaredFields = type.getDeclaredFields();
                    Iterator<Field> iterator = Arrays.stream(declaredFields).iterator();
                    ArrayList<String> columns = new ArrayList<>();
                    while (iterator.hasNext()) {
                        Field attribute = iterator.next();
                        if (attribute.isAnnotationPresent(Persisted.class)) {
                            columns.add(attribute.getName());
                            // create table for "embedded" List of kinto objects (1:m relations)
                            if (attribute.getType().getSimpleName().equalsIgnoreCase("ArrayList")) {
                                // build the sql statement for the collection relation table
                                // <name of type>+idTO<name of type>+id
                                // get the class name of type in list (https://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list)
                                Class<?> arrayListObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                                Class<?> attributeSuperClass  = arrayListObjectClass.getSuperclass();
                                if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                                    String attributeDeclaringClassName = attribute.getDeclaringClass().getSimpleName();
                                    String attributeClassName = arrayListObjectClass.getSimpleName();
                                    // check if table exists
                                    sql = new StringBuilder("SELECT name FROM sqlite_master WHERE type='table' AND name='");
                                    sql.append(attributeDeclaringClassName);
                                    sql.append("TO");
                                    sql.append(attributeClassName);
                                    sql.append("';");
                                    resultSet = statement.executeQuery(sql.toString());
                                    // table exists?
                                    // A TYPE_FORWARD_ONLY ResultSet only supports next() for navigation,
                                    // and not methods like first(), last(), absolute(int), relative(int).
                                    // The JDBC specification explicitly defines those to throw a SQLException if called on a TYPE_FORWARD_ONLY.
                                    // TABLE EXISTS LOCAL
                                    if (!resultSet.next()) {
                                        if (Global.DEBUG && loggerIsAvailable) {
                                            String message = "KintoCollection of " + type + " exists in local datastore.";
                                            logger.log(LogEntry.Severity.DEBUG, message);
                                        }
                                        // create table
                                        sql = new StringBuilder("CREATE TABLE ");
                                        String relationTableName = attributeDeclaringClassName + "TO" + attributeClassName;
                                        sql.append(relationTableName);
                                        sql.append(" (");
                                        sql.append(attributeDeclaringClassName.toLowerCase(Locale.ROOT));
                                        sql.append("uuid TEXT, ");
                                        sql.append(attributeClassName.toLowerCase(Locale.ROOT));
                                        sql.append("uuid TEXT);");
                                        if (Global.DEBUG && loggerIsAvailable) {
                                            String message = "Create one-to-many relation table for "
                                                    + attributeDeclaringClassName + " and " + attributeClassName
                                                    + " with sql command: " + sql + ".";
                                            logger.log(LogEntry.Severity.DEBUG, message);
                                        }
                                        // create the table in local datastore
                                        statement.execute(sql.toString());
                                        // add relation information to Map
                                        relationTables.put((Class<? extends KintoObject>) attribute.getDeclaringClass(), relationTableName);
                                    } else {
                                        //  relation table exists
                                        if (Global.DEBUG && loggerIsAvailable) {
                                            String message = "Relation table for " + attributeDeclaringClassName
                                                    + " and " + attributeClassName + " exists in local datastore.";
                                            logger.log(LogEntry.Severity.DEBUG, message);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // build the sql statement for the collection table
                    // id from sqlite, kintoid from kinto, usn = update sequence number
                    // now add only persistent attributes (@Persisted)
                    sql = new StringBuilder("CREATE TABLE ");
                    sql.append(type.getSimpleName());
                    //  "meta" data
                    sql.append("(uuid TEXT PRIMARY KEY,kintoid TEXT,usn INT");
                    // object attributes
                    for (String columnName : columns) {
                        sql.append(",");
                        sql.append(columnName);
                        //  column data type
                        if (storableAttributes.containsKey(columnName)) {
                            sql.append(" ");
                            sql.append(storableAttributes.get(columnName).getSqlDataTypeString());
                        }
                    }
                    sql.append(");");
                    if (Global.DEBUG && loggerIsAvailable) {
                        String message = "Create KintoCollection "
                                + type.getSimpleName()
                                + " with sql command: " + sql + ".";
                        logger.log(LogEntry.Severity.DEBUG, message);
                    }
                    // create the table in local datastore
                    statement.execute(sql.toString());
                } catch (Throwable exception) {
                    if (loggerIsAvailable) {
                        String message = "Reflection of " + type.getName() + " failed, can't create table for collection.";
                        logger.log(LogEntry.Severity.ERROR, message);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                    throw new KintoException(exception.getMessage());
                }
            }
        } catch(SQLException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(exception);
        }
    }

    // TODO implement func validate schema
    private boolean isSchemeValidForType(Class<? extends KintoObject> type) {
        // check schema
        // The sqlite_schema table contains one row for each table, index, view,
        // and trigger (collectively "objects") in the schema,
        // except there is no entry for the sqlite_schema table itself.
        // The text in the sqlite_schema.sql column is a copy of the original CREATE statement text
        // that created the object,
        // except normalized as described above and as modified by subsequent ALTER TABLE statements.
        /*sql = new StringBuilder("SELECT sql FROM sqlite_master WHERE type='table' AND name='");
        sql.append(type.getSimpleName());
        sql.append("';");
        resultSet = statement.executeQuery(sql.toString());*/
        return false;
    }

    private boolean isValidObjectType(KintoObject kintoObject) throws KintoException {
        // object from collection type?
        if (!kintoObject.getClass().equals(type)) {
            String errorMessage = "The object is not from type " + type.getSimpleName() + " .";
            throw new KintoException(errorMessage);
        }
        return true;
    }

    // check if the object exist in local datastore
    private boolean isNewRecord(KintoObject kintoObject) throws KintoException {
        if (kintoObject == null) {
            throw new KintoException("Can't add an nullable object.");
        }
        boolean isEmbeddedObject = false;
        String objectTypeSimpleName = kintoObject.getClass().getSimpleName();
        if (!objectTypeSimpleName.equals(getName())) {
            isEmbeddedObject = true;
        }
        // search for object with id in local datastore
        // building sql select command
        try {
            StringBuilder sql = new StringBuilder("SELECT uuid FROM ");
            if (isEmbeddedObject) {
                sql.append(objectTypeSimpleName);
            } else {
                sql.append(getName());
            }
            sql.append(" WHERE uuid='");
            sql.append(kintoObject.getUUID());
            sql.append("';");
            Statement statement = localdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql.toString());
            if (resultSet.next()) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Kinto object with id " + kintoObject.getUUID() + " exists in local datastore.";
                    logger.log(LogEntry.Severity.DEBUG, message);
                }
                return false;
            }
        } catch (SQLException exception) {
            throw new KintoException(exception);
        }
        return true;
    }

    private String getValueForAttributeAsString(KintoObject kintoObject, String attributeName) throws KintoException {
        String valueForAttribute = "";
        String methodName = "get" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
        try {
            DataSet dataSet = storableAttributes.get(attributeName);
            Method getAttributeMethod = kintoObject.getClass().getMethod(methodName);
            if (dataSet.isKintoObject()) {
                // return the id of the object
                KintoObject embeddedObject = (KintoObject) getAttributeMethod.invoke(kintoObject);
                valueForAttribute = embeddedObject.getUUID();
            } else {
                // return value for simple data type
                valueForAttribute = String.valueOf(getAttributeMethod.invoke(kintoObject));
            }
        } catch (NoSuchMethodException exception) {
            String errorMessage = "The getter method \""
                    + methodName + "\" for the attribute \""
                    + attributeName
                    + " was not found.";
            errorMessage = errorMessage + " - " + exception.getMessage();
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        } catch (InvocationTargetException exception) {
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, exception.getMessage());
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        } catch (IllegalAccessException exception) {
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, exception.getMessage());
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
        return valueForAttribute;
    }
}

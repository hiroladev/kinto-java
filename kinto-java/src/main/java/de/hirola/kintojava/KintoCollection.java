package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.Logger;
import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.lang.reflect.*;
import java.sql.*;
import java.time.Instant;
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
    private final HashMap<String,DataSet> storableAttributes;
    // 1:m relations for embedded KintoObject in relation table
    private final HashMap<Field, String> relationTables;
    private boolean loggerIsAvailable;
    private final boolean isSynced;

    /**
     * Create a collection for objects of class type.
     *
     * @param type Type of objects in collections.
     * @param kinto The kinto object for datastore operations.
     * @throws KintoException if collection couldn't initialize.
     */
    public KintoCollection(Class<? extends KintoObject> type, Kinto kinto) throws KintoException {
        this.localdbConnection = kinto.getlocaldbConnection();
        this.type = type;
        // TODO: Synchronisation
        this.isSynced = false;
        // build the list of persistent attributes
        relationTables = new HashMap<>();
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
        // check if table for collection exists
        // local and remote
        createLocalDataStoreForCollection();
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

    public HashMap<String, DataSet> getStorableAttributes() {
        return storableAttributes;
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
            // insert or update?
            if (isNewRecord(kintoObject)) {
                // building sql insert command for the kinto object of this collection
                // INSERT INTO table (column1,column2 ,..) VALUES( value1,	value2 ,...);
                StringBuilder createRecordSQL = new StringBuilder("INSERT INTO ");
                // the name of the collection (table)
                createRecordSQL.append(getName());
                // attributes = columns
                // build a map with attribute and value
                createRecordSQL.append(" (uuid, kintoid, usn, ");
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
                    DataSet dataSet = storableAttributes.get(attributeName);
                    if (dataSet == null) {
                        String errorMessage = "Empty dataset for attribute "
                                + attributeName
                                + ".";
                        throw new KintoException(errorMessage);
                    }
                    String sqlDataTypeString = dataSet.getSqlDataTypeString();
                    // 1:m relations in extra tables
                    if (!sqlDataTypeString.equalsIgnoreCase(DataSet.RELATION_DATA_MAPPING_STRING)) {
                        createRecordSQL.append(attributeName);
                        valuesString.append("'");
                        valuesString.append(getValueForAttributeAsString(kintoObject, attributeName));
                        valuesString.append("'");
                        if (loops < size) {
                            createRecordSQL.append(" ,");
                            valuesString.append(" ,");
                        }
                    }
                    loops++;
                }
                createRecordSQL.append(valuesString);
                createRecordSQL.append(");");
                // building sql insert command for the embedded kinto object of this collection (1:m relations)
                // INSERT INTO table (column1,column2 ,..) VALUES( value1,	value2 ,...);
                ArrayList<String> createRelationRecordSQLCommands = new ArrayList<>();
                String attributeName = "";
                try {
                    for (String key : storableAttributes.keySet()) {
                        attributeName = key;
                        DataSet dataSet = storableAttributes.get(attributeName);
                        // get attributes using reflection
                        Class<? extends KintoObject> clazz = kintoObject.getClass();
                        // 1. 1:1 relations
                        if (dataSet.isKintoObject()) {
                            Field embeddedObjectAttribute = clazz.getDeclaredField(attributeName);
                            embeddedObjectAttribute.setAccessible(true);
                            KintoObject embeddedObject = (KintoObject) embeddedObjectAttribute.get(kintoObject);
                            // all objects in local datastore?
                            if (isNewRecord(embeddedObject)) {
                                String errorMessage = "The embedded object from type "
                                        + embeddedObject.getClass().getSimpleName()
                                        + " must exist in datastore before saving this object.";
                                throw new KintoException(errorMessage);
                            }
                        }
                        // 1: m relations
                        if (dataSet.isArray()) {
                            // check if relation table exist
                            Class<? extends KintoObject> arrayObjectType = dataSet.getArrayType();
                            if (type != null){
                                // add for all embedded kinto objects an entry in relation table
                                String relationTable = relationTables.get(dataSet.getAttribute());
                                if (relationTable == null) {
                                    String errorMessage = "The relation table of "
                                            + arrayObjectType.getSimpleName()
                                            + " was not found in configuration.";
                                    throw  new KintoException(errorMessage);
                                }
                                Field arrayAttribute = clazz.getDeclaredField(attributeName);
                                arrayAttribute.setAccessible(true);
                                Object arrayAttributeObject = arrayAttribute.get(kintoObject);
                                if (arrayAttributeObject instanceof ArrayList) {
                                    ArrayList<? extends KintoObject> kintoObjects = (ArrayList<? extends KintoObject>) arrayAttributeObject;
                                    for (KintoObject arrayObject : kintoObjects) {
                                        // all objects in local datastore?
                                        if (isNewRecord(arrayObject)) {
                                            String errorMessage = "The embedded object from type "
                                                    + arrayObject.getClass().getSimpleName()
                                                    + " must exist in datastore before saving this object.";
                                            throw new KintoException(errorMessage);
                                        }
                                        // build sql insert command(s)
                                        StringBuilder sql = new StringBuilder("INSERT INTO ");
                                        // the name of the relation table
                                        sql.append(relationTable);
                                        sql.append(" (");
                                        // first the object type uuid
                                        sql.append(getName().toLowerCase(Locale.ROOT));
                                        sql.append("uuid, ");
                                        // then the attribute type name
                                        sql.append(arrayObjectType.getSimpleName().toLowerCase(Locale.ROOT));
                                        sql.append("uuid) VALUES('");
                                        sql.append(kintoObject.getUUID());
                                        sql.append("', '");
                                        sql.append(arrayObject.getUUID());
                                        sql.append("');");
                                        // add to the sql command list
                                        createRelationRecordSQLCommands.add(sql.toString());
                                    }
                                }
                            }
                        }
                    }
                } catch (NoSuchFieldException exception) {
                    String errorMessage = "Can't get the attribute "
                            + attributeName
                            + " using reflection: "
                            + exception.getMessage();
                    if (loggerIsAvailable) {
                        logger.log(LogEntry.Severity.ERROR, errorMessage);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                    throw new KintoException(errorMessage);
                } catch (IllegalAccessException exception) {
                    String errorMessage = "Getting value for attribute "
                            + attributeName
                            + " using reflection failed: "
                            + exception.getMessage();
                    if (loggerIsAvailable) {
                        logger.log(LogEntry.Severity.ERROR, errorMessage);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                    throw new KintoException(errorMessage);
                }
                try {
                    // execute the sql commands
                    // use transaction for all statements
                    localdbConnection.setAutoCommit(false);
                    Statement statement = localdbConnection.createStatement();
                    // create entry in collection table
                    statement.execute(createRecordSQL.toString());
                    // create relation table entries
                    if (createRelationRecordSQLCommands.size() > 0) {
                        for (String createRelationRecordSQLCommand : createRelationRecordSQLCommands) {
                            statement.execute(createRelationRecordSQLCommand);
                        }
                    }
                    localdbConnection.commit();
                } catch (SQLException exception) {
                    try {
                        // rollback all statements
                        localdbConnection.rollback();
                    } catch (SQLException e) {
                        if (loggerIsAvailable) {
                            String errorMessage = "Save and rollback failed, inconsistent data are possible: "
                                    + exception.getMessage();
                            logger.log(LogEntry.Severity.ERROR, errorMessage);
                        }
                        if (Global.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                    String errorMessage = "Saving the object failed: "
                            + exception.getMessage();
                    if (loggerIsAvailable) {
                        logger.log(LogEntry.Severity.ERROR, errorMessage);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                    throw new KintoException(errorMessage);
                } finally {
                    try {
                        // using no transactions again
                        localdbConnection.setAutoCommit(true);
                    } catch (SQLException exception) {
                        if (Global.DEBUG) {
                            exception.printStackTrace();
                        }
                    }
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

    public void removeRecord(KintoObject kintoObject) throws KintoException {
        String currentAttributeName = "";
        try {
            if (!isNewRecord(kintoObject)) {
                if (isValidObjectType(kintoObject)) {
                    // get all objects (uuid) in relation table
                    for (String attributeName : storableAttributes.keySet()) {
                        currentAttributeName = attributeName;
                        DataSet dataSet = storableAttributes.get(attributeName);
                        if (dataSet == null) {
                            String errorMessage = "Empty dataset for attribute "
                                    + attributeName
                                    + ".";
                            throw new KintoException(errorMessage);
                        }
                        // get attributes using reflection
                        Class<? extends KintoObject> clazz = kintoObject.getClass();
                        // 1: m relations
                        if (dataSet.isArray()) {
                            // check if relation table exist
                            Class<? extends KintoObject> arrayObjectType = dataSet.getArrayType();
                            if (type != null){
                                // add for all embedded kinto objects an entry in relation table
                                String relationTable = relationTables.get(dataSet.getAttribute());
                                if (relationTable == null) {
                                    String errorMessage = "The relation table of "
                                            + arrayObjectType.getSimpleName()
                                            + " was not found in configuration.";
                                    throw  new KintoException(errorMessage);
                                }
                                Field arrayAttribute = clazz.getDeclaredField(attributeName);
                                arrayAttribute.setAccessible(true);
                                Object arrayAttributeObject = arrayAttribute.get(kintoObject);
                                ArrayList<String> uuids = new ArrayList<>();
                                if (arrayAttributeObject instanceof ArrayList) {
                                    ArrayList<? extends KintoObject> kintoObjects = (ArrayList<? extends KintoObject>) arrayAttributeObject;
                                    for (KintoObject arrayObject : kintoObjects) {
                                        // remove in relation tables
                                        // use transactions
                                        // build sql select command for relation table
                                        StringBuilder sql = new StringBuilder("SELECT count(*) as rowcount, ");
                                        // all uuid from objects in list
                                        sql.append(arrayObjectType.getSimpleName().toLowerCase(Locale.ROOT));
                                        sql.append("uuid FROM ");
                                        // the name of the relation table
                                        sql.append(relationTable);
                                        sql.append(" WHERE ");
                                        // the object type uuid
                                        sql.append(getName().toLowerCase(Locale.ROOT));
                                        sql.append("uuid='");
                                        sql.append(kintoObject.getUUID());
                                        sql.append("';");
                                        try {
                                            Statement statement = localdbConnection.createStatement();
                                            ResultSet resultSet = statement.executeQuery(sql.toString());
                                            // get the count of rows
                                            int countOfResults = resultSet.getInt("rowcount");
                                            // uuid from list in local datastore not found -> error and rollback
                                            if (countOfResults == 0) {
                                                String errorMessage = "Objects from the list attribute "
                                                        + attributeName
                                                        + " wasn't found in local datastore. "
                                                        + "Check the datastore.";
                                                throw new KintoException("");
                                            }
                                            // remove all entries for kinto object in relation table
                                            sql = new StringBuilder("DELETE FROM ");
                                            sql.append(relationTable);
                                            sql.append(" WHERE ");
                                            // the object type uuid
                                            sql.append(getName().toLowerCase(Locale.ROOT));
                                            sql.append("uuid='");
                                            sql.append(kintoObject.getUUID());
                                            sql.append("';");
                                            // use transaction
                                            localdbConnection.setAutoCommit(false);
                                            statement.execute(sql.toString());
                                            // remove the kinto object
                                            sql = new StringBuilder("DELETE FROM ");
                                            sql.append(getName());
                                            sql.append(" WHERE ");
                                            sql.append("uuid='");
                                            sql.append(kintoObject.getUUID());
                                            sql.append("';");
                                            statement.execute(sql.toString());
                                            localdbConnection.commit();
                                        } catch (SQLException exception) {
                                            String errorMessage = "Error occured while removing from local datastore: "
                                                    + exception.getMessage();
                                            throw  new KintoException(errorMessage);
                                        } finally {
                                            try {
                                                // using no transactions again
                                                localdbConnection.setAutoCommit(true);
                                            } catch (SQLException exception) {
                                                if (Global.DEBUG) {
                                                    exception.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // error remove an object before saving
                throw new KintoException("Can't remove an unsaved object.");
            }
        } catch (NoSuchFieldException exception) {
            String errorMessage = "Can't get the attribute "
                    + currentAttributeName
                    + " using reflection: "
                    + exception.getMessage();
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        } catch (IllegalAccessException exception) {
            String errorMessage = "Getting value for attribute "
                    + currentAttributeName
                    + " using reflection failed: "
                    + exception.getMessage();
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        }
    }

    public List<KintoObject> findAll() throws KintoException {
        ArrayList<KintoObject> objects = new ArrayList<>();
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(getName());
            sql.append(";");
            Statement statement = localdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql.toString());
            while (resultSet.next()) {
                objects.add(createObjectFromResultSet(resultSet));
            }
        } catch (SQLException exception) {
            if (loggerIsAvailable) {
                String errorMessage = "Error while searching for objects in local datastore: "
                        + exception.getMessage();
                logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
        return objects;
    }

    /**
     *
     * @param uuid The UUID of the wanted object.
     * @return A kinto object with the given UUID or <b>null</b>, if no object with the UUID found.
     * @throws KintoException if the UUID null, if exists more than one object or a sql error occurred
     */
    public KintoObject findByUUID(String uuid) throws KintoException {
        if (uuid == null) {
            throw new KintoException("Can't search for object with empty uuid.");
        }
        try {
            StringBuilder sql = new StringBuilder("SELECT count(*) as rowcount, * FROM ");
            sql.append(getName());
            sql.append(" WHERE uuid='");
            sql.append(uuid);
            sql.append("';");
            Statement statement = localdbConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql.toString());
            // get the count of rows
            int countOfResults = resultSet.getInt("rowcount");
            if (countOfResults == 0) {
                return null;
            }
            if (countOfResults > 1) {
                String errorMessage = "There are more as one objects with UUID "
                        + uuid
                        + "in local datastore. Please check the datastore.";
                if (loggerIsAvailable) {
                    logger.log(LogEntry.Severity.ERROR, errorMessage);
                }
                if (Global.DEBUG) {
                    System.out.println(errorMessage);
                }
                throw new KintoException(errorMessage);
            }
            while (resultSet.next()) {
                // create object from this collection
                return createObjectFromResultSet(resultSet);
            }
        } catch (SQLException exception) {
            if (loggerIsAvailable) {
                String errorMessage = "Error while searching for objects in local datastore: "
                        + exception.getMessage();
                logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
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
            while (iterator.hasNext()) {
                Field attribute = iterator.next();
                if (attribute.isAnnotationPresent(Persisted.class)) {
                    DataSet dataSet = new DataSet(attribute);
                    attributes.put(attribute.getName(), dataSet);
                    // create table for "embedded" List of kinto objects (1:m relations)
                    if (attribute.getType().getSimpleName().equalsIgnoreCase("ArrayList")) {
                        // relation table = <name of type>TO<name of type>
                        // get the class name of type in list (https://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list)
                        Class<?> arrayListObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                        Class<?> attributeSuperClass = arrayListObjectClass.getSuperclass();
                        if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                            String attributeDeclaringClassName = attribute.getDeclaringClass().getSimpleName();
                            String attributeClassName = arrayListObjectClass.getSimpleName();
                            String relationTableName = attributeDeclaringClassName + "TO" + attributeClassName;
                            // add relation information to Map
                            relationTables.put(attribute, relationTableName);
                        }
                    }
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

    private void createLocalDataStoreForCollection() throws KintoException {
        // check if collection table exists
        try {
            StringBuilder sql = new StringBuilder("SELECT name FROM sqlite_master WHERE type='table' AND name='");
            sql.append(getName());
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
                    String message = "KintoCollection of " + getName() + " does not exists in local datastore.";
                    logger.log(LogEntry.Severity.DEBUG, message);
                }
                // SQLite store any kind of data you want in any column of any table
                // build the sql statement for the collection table
                // id from sqlite, kintoid from kinto, usn = update sequence number
                // now add only persistent attributes (@Persisted)
                sql = new StringBuilder("CREATE TABLE ");
                sql.append(getName());
                //  "meta" data
                sql.append("(uuid TEXT PRIMARY KEY, kintoid TEXT, usn INT");
                // object attributes
                int loops = 1;
                int size = storableAttributes.size();
                if (size > 0) {
                    sql.append(", ");
                    for (String attributeName : storableAttributes.keySet()) {
                        DataSet dataSet = storableAttributes.get(attributeName);
                        if (dataSet == null) {
                            String errorMessage = "Empty dataset for attribute "
                                    + attributeName
                                    + ".";
                            throw new KintoException(errorMessage);
                        }
                        String sqlDataTypeString = dataSet.getSqlDataTypeString();
                        // 1:m relations in extra tables
                        if (!sqlDataTypeString.equalsIgnoreCase(DataSet.RELATION_DATA_MAPPING_STRING)) {
                            sql.append(attributeName);
                            sql.append(" ");
                            sql.append(sqlDataTypeString);
                            if (loops < size) {
                                sql.append(", ");

                            }
                        }
                        loops++;
                    }
                }
                sql.append(");");
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Create KintoCollection "
                            + getName()
                            + " with sql command: " + sql + ".";
                    logger.log(LogEntry.Severity.DEBUG, message);
                }
                // create the table in local datastore
                statement.execute(sql.toString());
            }
        } catch (SQLException exception) {
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "Creation of table for the collection "
                    + getName()
                    + " has failed. "
                    + exception.getMessage();
            throw new KintoException(errorMessage);
        }
        // check if relation tables exists (if needed)
        try {
            if (relationTables.size() > 0) {
                for (Field attribute : relationTables.keySet()) {
                    Class<?> arrayListObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                    String attributeClassName = arrayListObjectClass.getSimpleName();
                    String relationTableName = relationTables.get(attribute);
                    // check if table exists
                    StringBuilder sql = new StringBuilder("SELECT name FROM sqlite_master WHERE type='table' AND name='");
                    sql.append(relationTableName);
                    sql.append("';");
                    Statement statement = localdbConnection.createStatement();
                    ResultSet resultSet = statement.executeQuery(sql.toString());
                    // table exists?
                    // A TYPE_FORWARD_ONLY ResultSet only supports next() for navigation,
                    // and not methods like first(), last(), absolute(int), relative(int).
                    // The JDBC specification explicitly defines those to throw a SQLException if called on a TYPE_FORWARD_ONLY.
                    // TABLE EXISTS LOCAL
                    if (resultSet.next()) {
                        //  relation table exists
                        if (Global.DEBUG && loggerIsAvailable) {
                            String message = "Relation table for "
                                    + getName()
                                    + " and " + attributeClassName
                                    + " exists in local datastore.";
                            logger.log(LogEntry.Severity.DEBUG, message);
                        }
                    } else {
                        // create table
                        sql = new StringBuilder("CREATE TABLE ");
                        sql.append(relationTableName);
                        sql.append(" (");
                        // collection type name
                        sql.append(getName().toLowerCase(Locale.ROOT));
                        sql.append("uuid TEXT, ");
                        // attribute type name
                        sql.append(attributeClassName.toLowerCase(Locale.ROOT));
                        sql.append("uuid TEXT);");
                        if (Global.DEBUG && loggerIsAvailable) {
                            String message = "Create one-to-many relation table for "
                                    + getName()
                                    + " and "
                                    + attributeClassName
                                    + " with sql command: " + sql + ".";
                            logger.log(LogEntry.Severity.DEBUG, message);
                        }
                        // create the table in local datastore
                        statement.execute(sql.toString());
                    }
                }
            }
        } catch (SQLException exception) {

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
            throw new KintoException("Can't manage a nullable object.");
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
        String valueForAttribute;
        try {
            Class<? extends KintoObject> clazz = kintoObject.getClass();
            DataSet dataSet = storableAttributes.get(attributeName);
            if (dataSet.isKintoObject()) {
                // return the id of the object
                Field embeddedObjectAttribute = clazz.getDeclaredField(attributeName);
                embeddedObjectAttribute.setAccessible(true);
                KintoObject embeddedObject = (KintoObject) embeddedObjectAttribute.get(kintoObject);
                valueForAttribute = embeddedObject.getUUID();
            } else {
                // return value for simple data type
                Field attributeField = clazz.getDeclaredField(attributeName);
                attributeField.setAccessible(true);
                valueForAttribute = String.valueOf(attributeField.get(kintoObject));
            }
        } catch (NoSuchFieldException exception) {
            String errorMessage = " The attribute "
                    + attributeName
                    + " does not exist or couldn't determine with reflection: "
                    + exception.getMessage();
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, exception.getMessage());
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        } catch (IllegalAccessException exception) {
            String errorMessage = "Error while getting value from attribute "
                    + attributeName
                    + " :"
                    + exception.getMessage();
            if (loggerIsAvailable) {
                logger.log(LogEntry.Severity.ERROR, exception.getMessage());
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        }
        return valueForAttribute;
    }

    private KintoObject createObjectFromResultSet(ResultSet resultSet) throws KintoException {
        try {
            // create object from local datastore using reflection
            Constructor<? extends KintoObject> constructor = type.getConstructor();
            KintoObject kintoObject = constructor.newInstance();
            // fields from KintoObject
            Class<?> clazz = kintoObject.getClass().getSuperclass();
            if (clazz != KintoObject.class) {
                throw new KintoException("The superclass of the object is not KintoObject.");
            }
            // set the uuid
            Field uuid = clazz.getDeclaredField("uuid");
            uuid.setAccessible(true);
            uuid.set(kintoObject, resultSet.getString("uuid"));
            // set the kinto id
            Field kintoid = clazz.getDeclaredField("kintoID");
            kintoid.setAccessible(true);
            kintoid.set(kintoObject, resultSet.getString("kintoID"));
            // set the other attributes
            for (String attributeName : storableAttributes.keySet()) {
                DataSet dataSet = storableAttributes.get(attributeName);
                Field attribute = dataSet.getAttribute();
                Object value = null;
                if (dataSet.isKintoObject()) {
                    // 1:1 embedded object
                    // create an "empty" object with uuid
                    if (!KintoObject.class.isAssignableFrom(attribute.getType())) {
                        throw new KintoException("The superclass of the embedded object is not KintoObject.");
                    }
                    String embeddedKintoObjectUUID = resultSet.getString(attributeName);
                    // create embedded object using reflection
                    //noinspection unchecked
                    constructor = (Constructor<? extends KintoObject>) attribute.getType().getConstructor();
                    KintoObject embeddedKintoObject = constructor.newInstance();
                    // fields from KintoObject
                    uuid.set(embeddedKintoObject, embeddedKintoObjectUUID);
                    // add the embedded object
                    value = embeddedKintoObject;
                } else if (dataSet.isArray()) {
                    // 1:m embedded object(s)
                    // create "empty" object(s) with uuid
                    ArrayList<KintoObject> embeddedObjectList = new ArrayList<>();
                    Class<?> arrayListObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                    if (!KintoObject.class.isAssignableFrom(arrayListObjectClass)) {
                        throw new KintoException("The superclass of the embedded object is not KintoObject.");
                    }
                    //noinspection unchecked
                    constructor = (Constructor<? extends KintoObject>) arrayListObjectClass.getConstructor();
                    // get the uuid from relation table
                    String relationTableName = relationTables.get(attribute);
                    // the name of the embedded object column uuid
                    String uuidColumnName = arrayListObjectClass.getSimpleName().toLowerCase(Locale.ROOT) + "uuid";
                    if (relationTableName == null) {
                        String errorMessage = "Can't find the relation table name of type '"
                                + attributeName
                                +"'.";
                        throw new KintoException(errorMessage);
                    }
                    StringBuilder sql = new StringBuilder("SELECT ");
                    sql.append(uuidColumnName);
                    sql.append(" FROM ");
                    sql.append(relationTableName);
                    sql.append(" WHERE ");
                    sql.append(getName().toLowerCase(Locale.ROOT));
                    sql.append("uuid='");
                    sql.append(kintoObject.getUUID());
                    sql.append("';");
                    Statement statement = localdbConnection.createStatement();
                    ResultSet uuidResultSet = statement.executeQuery(sql.toString());
                    if (uuidResultSet.next()) {
                        // create an object with uuid
                        KintoObject arrayListKintoObject = constructor.newInstance();
                        uuid.set(arrayListKintoObject, uuidResultSet.getString(uuidColumnName));
                        // add to the list
                        embeddedObjectList.add(arrayListKintoObject);
                    }
                    // add the list of embedded objects to the kinto object
                    Field arrayListField = kintoObject.getClass().getDeclaredField(attributeName);
                    arrayListField.setAccessible(true);
                    arrayListField.set(kintoObject, embeddedObjectList);
                } else {
                    // attributes
                    String attributeJavaTypeString = dataSet.getJavaDataTypeString();
                    value = switch (attributeJavaTypeString) {
                        case "boolean" -> resultSet.getBoolean(attributeName);
                        case "int" -> resultSet.getInt(attributeName);
                        case "float" -> resultSet.getFloat(attributeName);
                        case "double" -> resultSet.getDouble(attributeName);
                        case "java.time.Instant" -> Instant.ofEpochMilli(resultSet.getDate(attributeName).getTime());
                        case "java.lang.String" -> resultSet.getString(attributeName);
                        default -> null;
                    };
                }
                // set value to attribute
                if (value != null) {
                    attribute.setAccessible(true);
                    attribute.set(kintoObject, value);
                }
            }
            return kintoObject;
        } catch (NoSuchMethodException exception) {
            // constructor not found
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        } catch (NoSuchFieldException exception) {
            // field not found
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        } catch (InvocationTargetException e) {
            // object creation
            e.printStackTrace();
        } catch (InstantiationException exception) {
            // object creation
            exception.printStackTrace();
        } catch (IllegalAccessException e) {
            // object creation
            e.printStackTrace();
        } catch (SQLException exception) {
            if (loggerIsAvailable) {
                String errorMessage = "Error while searching for objects in local datastore: "
                        + exception.getMessage();
                logger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
        return null;
    }
}

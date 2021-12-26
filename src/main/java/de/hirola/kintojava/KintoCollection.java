package de.hirola.kintojava;

import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.logger.KintoLogger;
import de.hirola.kintojava.model.DataSet;
import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A collection contains a list of objects from same type.
 * If a collections exists, you must not change the fields of the objects (schema)!
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
public class KintoCollection {

    private final KintoLogger kintoLogger;
    private final KintoDatabaseAdapter dataBase;
    private final Class<? extends KintoObject> type;
    // storable attributes
    // attribute name, dataset
    private final HashMap<String, DataSet> storableAttributes;
    // 1:m relations for embedded KintoObject in relation table
    private final HashMap<Field, String> relationTables;
    private boolean loggerIsAvailable;
    private final boolean isSynced;

    /**
     * Create a collection for objects of class type.
     *
     * @param type type of objects in collections.
     * @param kinto the kinto object for datastore operations
     * @throws KintoException if collection couldn't initialize.
     */
    public KintoCollection(Class<? extends KintoObject> type, Kinto kinto) throws KintoException {
        dataBase = kinto.getLocalDBConnection();
        this.type = type;
        // TODO: Synchronisation
        isSynced = false;
        // build the list of persistent attributes
        relationTables = new HashMap<>();
        storableAttributes = buildAttributesMap(type);
        // get logging
        kintoLogger = kinto.getKintoLogger();
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
                createRecordSQL.append(" (uuid, kintoid, usn");
                // all attributes -> columns
                StringBuilder valuesString = new StringBuilder(") VALUES(");
                //  primary key from uuid
                valuesString.append("'");
                valuesString.append(kintoObject.getUUID());
                // kinto record id later from sync
                // usn = 0 on insert
                valuesString.append("','', 0");
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
                        createRecordSQL.append(", ");
                        valuesString.append(", ");
                        createRecordSQL.append(attributeName);
                        valuesString.append("'");
                        valuesString.append(dataSet.getValueAsString(kintoObject));
                        valuesString.append("'");
                    }
                }
                createRecordSQL.append(valuesString);
                createRecordSQL.append(");");
                // all embedded kinto objects (all list attributes) for the kinto object
                ArrayList<KintoObject> useInRelationObjects = new ArrayList<>();
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
                            // embedded object can be null
                            if (embeddedObject != null) {
                                // all objects in local datastore?
                                if (isNewRecord(embeddedObject)) {
                                    String errorMessage = "The embedded object from type "
                                            + embeddedObject.getClass().getSimpleName()
                                            + " must exist in datastore before saving this object.";
                                    throw new KintoException(errorMessage);
                                }
                            }
                        }
                        // 1: m relations
                        if (dataSet.isList()) {
                            // check if relation table exist
                            Class<? extends KintoObject> listObjectType = dataSet.getListType();
                            if (type != null){
                                // add for all embedded kinto objects an entry in relation table
                                String relationTable = relationTables.get(dataSet.getAttribute());
                                if (relationTable == null) {
                                    String errorMessage = "The relation table of "
                                            + listObjectType.getSimpleName()
                                            + " was not found in configuration.";
                                    throw  new KintoException(errorMessage);
                                }
                                Field listAttribute = clazz.getDeclaredField(attributeName);
                                listAttribute.setAccessible(true);
                                Object listAttributeObject = listAttribute.get(kintoObject);
                                if (listAttributeObject instanceof List) {
                                    List<?> listObjects = (List<?>) listAttributeObject;
                                    for (Object listObject : listObjects) {
                                        if (!KintoObject.class.isAssignableFrom(listObject.getClass())) {
                                            String errorMessage = "The object must extends KintoObject. This object extends "
                                                    + listObject.getClass().getName();
                                            throw new KintoException(errorMessage);
                                        }
                                        KintoObject listKintoObject = (KintoObject) listObject;
                                        // all objects in local datastore?
                                        if (isNewRecord(listKintoObject)) {
                                            String errorMessage = "The embedded object from type "
                                                    + listKintoObject.getClass().getSimpleName()
                                                    + " must exist in datastore before saving this object.";
                                            throw new KintoException(errorMessage);
                                        }
                                        // build sql insert command(s)
                                        // the name of the relation table
                                        String sql = "INSERT INTO " + relationTable +
                                                " (" +
                                                // first the object type uuid
                                                getName().toLowerCase(Locale.ROOT) +
                                                "uuid, " +
                                                // then the attribute type name
                                                listObjectType.getSimpleName().toLowerCase(Locale.ROOT) +
                                                "uuid) VALUES('" +
                                                kintoObject.getUUID() +
                                                "', '" +
                                                listKintoObject.getUUID() +
                                                "');";
                                        // add to the sql command list
                                        createRelationRecordSQLCommands.add(sql);
                                        // add the object to the "global" list
                                        useInRelationObjects.add(listKintoObject);
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
                        kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
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
                        kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                    throw new KintoException(errorMessage);
                }
                try {
                    // execute the sql commands
                    // use transaction for all statements
                    dataBase.beginTransaction();
                    // create entry in collection table
                    dataBase.executeSQL(createRecordSQL.toString());
                    // create relation table entries
                    if (createRelationRecordSQLCommands.size() > 0) {
                        for (String createRelationRecordSQLCommand : createRelationRecordSQLCommands) {
                            dataBase.executeSQL(createRelationRecordSQLCommand);
                        }
                    }
                    // set the flag for used in relation
                    for(KintoObject useInRelationObject : useInRelationObjects) {
                        try {
                            Field attributeField = KintoObject.class.getDeclaredField("isUseInRelation");
                            attributeField.setAccessible(true);
                            attributeField.set(useInRelationObject, true);
                        } catch (NoSuchFieldException exception) {
                            // rollback all statements
                            dataBase.rollback();
                            String errorMessage = "Can't determine the attribute 'isUseInRelation' for object "
                                    + useInRelationObject.toString()
                                    + " : "
                                    + exception.getMessage();
                            if (loggerIsAvailable) {
                                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                            }
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            throw new KintoException(errorMessage);
                        } catch (IllegalAccessException exception) {
                            // rollback all statements
                            dataBase.rollback();
                            String errorMessage = "Can't set the attribute 'isUseInRelation' for object "
                                    + useInRelationObject.toString()
                                    + " using reflection: "
                                    + exception.getMessage();
                            if (loggerIsAvailable) {
                                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                            }
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            throw new KintoException(errorMessage);
                        }
                    }
                    // commit all statements
                    dataBase.commit();
                    // set the flag for local persistence
                    Field isPersistentAttribute = KintoObject.class.getDeclaredField("isPersistent");
                    isPersistentAttribute.setAccessible(true);
                    isPersistentAttribute.set(kintoObject, true);
                } catch (SQLException exception) {
                    try {
                        // rollback all statements
                        dataBase.rollback();
                    } catch (SQLException e) {
                        if (loggerIsAvailable) {
                            String errorMessage = "Save and rollback failed, inconsistent data are possible: "
                                    + exception.getMessage();
                            kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                        }
                        if (Global.DEBUG) {
                            e.printStackTrace();
                        }
                    }
                    String errorMessage = "Saving the object failed: "
                            + exception.getMessage();
                    if (loggerIsAvailable) {
                        kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                    throw new KintoException(errorMessage);
                } catch (NoSuchFieldException | IllegalAccessException exception) {
                    String errorMessage = "Error occurred while set the flag 'isPersistent'"
                            + exception.getMessage();
                    if (loggerIsAvailable) {
                        kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                    }
                    if (Global.DEBUG) {
                        exception.printStackTrace();
                    }
                } finally {
                    createRelationRecordSQLCommands.clear();
                }
            } else {
                // update
                updateRecord(kintoObject);
            }
        }
    }

    public void updateRecord(KintoObject kintoObject) throws KintoException {
        String currentAttributeName = "";
        ArrayList<String> replaceRelationRecordSQLCommands = new ArrayList<>();
        try {
            if (!isNewRecord(kintoObject)) {
                if (isValidObjectType(kintoObject)) {
                    // get all objects (uuid) in relation table
                    // use transactions
                    dataBase.beginTransaction();
                    // get attributes using reflection
                    Class<? extends KintoObject> clazz = kintoObject.getClass();
                    // build sql command for update the kinto object in local datastore
                    // UPDATE table_name SET column1 = value1, column2 = value2...., columnN = valueN
                    // WHERE [condition];
                    int attributeCount = storableAttributes.size();
                    StringBuilder updateSQL = new StringBuilder("UPDATE ");
                    updateSQL.append(getName());
                    updateSQL.append(" SET ");
                    for (String attributeName : storableAttributes.keySet()) {
                        attributeCount--;
                        currentAttributeName = attributeName;
                        DataSet dataSet = storableAttributes.get(attributeName);
                        if (dataSet == null) {
                            String errorMessage = "Empty dataset for attribute "
                                    + attributeName
                                    + ".";
                            throw new KintoException(errorMessage);
                        }
                        if (dataSet.isList()) {
                            // remove 1:m relations, the objects are still exists in collections
                            // check if relation table exist
                            Class<? extends KintoObject> listObjectType = dataSet.getListType();
                            if (type != null) {
                                // add for all embedded kinto objects an entry in relation table
                                String relationTable = relationTables.get(dataSet.getAttribute());
                                if (relationTable == null) {
                                    String errorMessage = "The relation table of "
                                            + listObjectType.getSimpleName()
                                            + " was not found in configuration.";
                                    throw new KintoException(errorMessage);
                                }
                                Field listAttribute = clazz.getDeclaredField(attributeName);
                                listAttribute.setAccessible(true);
                                Object listAttributeObject = listAttribute.get(kintoObject);
                                // build sql delete command for relation table
                                StringBuilder deleteSQL = new StringBuilder("DELETE FROM ");
                                // the name of the relation table
                                deleteSQL.append(relationTable);
                                deleteSQL.append(" WHERE ");
                                // all uuid from objects in list
                                deleteSQL.append(listObjectType.getSimpleName().toLowerCase(Locale.ROOT));
                                deleteSQL.append("uuid NOT IN (");
                                // add the attributes (columns) values to sql statements
                                if (listAttributeObject instanceof List) {
                                    List<?> listObjects = (List<?>) listAttributeObject;
                                    int objectListSize = listObjects.size();
                                    if (objectListSize > 0) {
                                        for (Object listObject : listObjects) {
                                            if (!KintoObject.class.isAssignableFrom(listObject.getClass())) {
                                                String errorMessage = "The object must extends KintoObject. This object extends "
                                                        + listObject.getClass().getName();
                                                throw new KintoException(errorMessage);
                                            }
                                            KintoObject listKintoObject = (KintoObject) listObject;
                                            // build sql insert command for relation table - using
                                            // REPLACE INTO table(column_list) VALUES(value_list);
                                            // the name of the relation table
                                            String replaceRelationSQL = "REPLACE INTO " + relationTable +
                                                    // columns
                                                    " (" +
                                                    getName().toLowerCase(Locale.ROOT) +
                                                    "uuid, " +
                                                   listObjectType.getSimpleName().toLowerCase(Locale.ROOT) +
                                                    // values
                                                    "uuid) VALUES(" +
                                                    // kinto object uuid
                                                    "'" +
                                                    kintoObject.getUUID() +
                                                    "','" +
                                                    listKintoObject.getUUID() +
                                                    "');";
                                            // add to the sql command batch
                                            replaceRelationRecordSQLCommands.add(replaceRelationSQL);
                                            // build the sql delete statement
                                            // remove all uuid (objects) from relation table, there not in list
                                            deleteSQL.append("'");
                                            deleteSQL.append(kintoObject.getUUID());
                                            deleteSQL.append("'");
                                            if (objectListSize > 1) {
                                                deleteSQL.append(",");
                                            }
                                            objectListSize--;
                                        }
                                        deleteSQL.append(");");
                                    } else {
                                        // empty list
                                        deleteSQL.append("'');");
                                    }
                                    // if the last attribute a list the remove the last comma
                                    if(attributeCount == 0) {
                                        updateSQL.deleteCharAt(updateSQL.lastIndexOf(","));
                                    }
                                    try {
                                        // first delete
                                        dataBase.executeSQL(deleteSQL.toString());
                                        // then replace (insert), if exists
                                        if (replaceRelationRecordSQLCommands.size() > 0) {
                                            for (String replaceRelationRecordSQLCommand : replaceRelationRecordSQLCommands) {
                                                dataBase.executeSQL(replaceRelationRecordSQLCommand);
                                            }
                                        }
                                    } catch (SQLException exception) {
                                        String errorMessage = "Error occurred while removing relation entries from local datastore: "
                                                + exception.getMessage();
                                        throw new KintoException(errorMessage);
                                    } finally {
                                        // clear all sql commands
                                        replaceRelationRecordSQLCommands.clear();
                                    }
                                }
                            }
                        }
                        else {
                            // simple attribute and 1:1 relation
                            // column (attribute) name
                            updateSQL.append(attributeName);
                            // SQLite store any kind of data you want in any column of any table
                            updateSQL.append("='");
                            // column (attribute) value
                            updateSQL.append(dataSet.getValueAsString(kintoObject));
                            updateSQL.append("'");
                            if(attributeCount > 0) {
                                updateSQL.append(",");
                            }
                        }
                    }
                    updateSQL.append(" WHERE uuid='");
                    updateSQL.append(kintoObject.getUUID());
                    updateSQL.append("';");
                    try {
                        // remove the object from local datastore
                        dataBase.executeSQL(updateSQL.toString());
                        // commit all updates to local datastore
                        // inclusive all delete statements from list attributes
                        dataBase.commit();
                    } catch (SQLException exception) {
                        // rollback all changes
                        dataBase.rollback();
                        String errorMessage = "Error occurred while updating the local datastore: "
                                + exception.getMessage();
                        throw new KintoException(errorMessage);
                    }
                }
            } else {
                // error update an object before saving
                throw new KintoException("Can't update an unsaved object.");
            }
        } catch (SQLException exception) {
            // auto commit error
        } catch (NoSuchFieldException exception) {
            String errorMessage = "Can't get the attribute "
                    + currentAttributeName
                    + " using reflection: "
                    + exception.getMessage();
            if (loggerIsAvailable) {
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
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
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        }
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
                        if (dataSet.isList()) {
                            // check if relation table exist
                            Class<? extends KintoObject> listObjectType = dataSet.getListType();
                            if (type != null){
                                // add for all embedded kinto objects an entry in relation table
                                String relationTable = relationTables.get(dataSet.getAttribute());
                                if (relationTable == null) {
                                    String errorMessage = "The relation table of "
                                            + listObjectType.getSimpleName()
                                            + " was not found in configuration.";
                                    throw  new KintoException(errorMessage);
                                }
                                Field listAttribute = clazz.getDeclaredField(attributeName);
                                listAttribute.setAccessible(true);
                                Object listAttributeObject = listAttribute.get(kintoObject);
                                if (listAttributeObject instanceof List) {
                                    // remove in relation tables
                                    // use transactions
                                    // build sql select command for relation table
                                    StringBuilder sql = new StringBuilder("SELECT count(*) as "
                                            + Global.rowcountColumnName + ", ");
                                    // all uuid from objects in list
                                    sql.append(listObjectType.getSimpleName().toLowerCase(Locale.ROOT));
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
                                        KintoQueryResultSet resultSet = dataBase.executeQuery(sql.toString());
                                        // get the count of rows
                                        int countOfResults = resultSet.getInt(Global.rowcountColumnName);
                                        // uuid from list in local datastore not found -> error and rollback
                                        if (countOfResults == 0) {
                                            String errorMessage = "Objects from the list attribute "
                                                    + attributeName
                                                    + " wasn't found in local datastore. "
                                                    + "Check the datastore.";
                                            throw new KintoException(errorMessage);
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
                                        dataBase.beginTransaction();
                                        dataBase.executeSQL(sql.toString());
                                        // remove the kinto object
                                        sql = new StringBuilder("DELETE FROM ");
                                        sql.append(getName());
                                        sql.append(" WHERE ");
                                        sql.append("uuid='");
                                        sql.append(kintoObject.getUUID());
                                        sql.append("';");
                                        dataBase.executeSQL(sql.toString());
                                        dataBase.commit();
                                    } catch (SQLException exception) {
                                        String errorMessage = "Error occurred while removing from local datastore: "
                                                + exception.getMessage();
                                        throw  new KintoException(errorMessage);
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
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
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
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        }
    }

    public List<KintoObject> findAll() throws KintoException {
        List<KintoObject> objects = new ArrayList<>();
        try {
            String sql = "SELECT * FROM " + getName() + ";";
            KintoQueryResultSet resultSet = dataBase.executeQuery(sql);
            while (resultSet.next()) {
                objects.add(createObjectFromResultSet(resultSet));
            }
        } catch (SQLException exception) {
            if (loggerIsAvailable) {
                String errorMessage = "Error while searching for objects in local datastore: "
                        + exception.getMessage();
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(exception);
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
            String sql = "SELECT count(*) as "+ Global.rowcountColumnName + ", * FROM " + getName()
                    + " WHERE uuid='" + uuid + "';";
            KintoQueryResultSet resultSet = dataBase.executeQuery(sql);
            // get the count of rows
            int countOfResults = resultSet.getInt(Global.rowcountColumnName);
            if (countOfResults == 0) {
                return null;
            }
            if (countOfResults > 1) {
                String errorMessage = "There are more as one objects with UUID "
                        + uuid
                        + "in local datastore. Please check the datastore.";
                if (loggerIsAvailable) {
                    kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
                }
                if (Global.DEBUG) {
                    System.out.println(errorMessage);
                }
                throw new KintoException(errorMessage);
            }
            if (resultSet.next()) {
                // create object from this collection
                KintoObject kintoObject = createObjectFromResultSet(resultSet);
                return createObjectFromResultSet(resultSet);
            }
        } catch (SQLException exception) {
            if (loggerIsAvailable) {
                String errorMessage = "Error while searching for objects in local datastore: "
                        + exception.getMessage();
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    public List<KintoObject> findByQuery(KintoQuery query) throws KintoException {
        throw new KintoException("Sorry. Not implemented yet.");
    }

    // build a map with attribute and value for the object
    // HashMap<attribute name, data set>
    private @NotNull HashMap<String,DataSet> buildAttributesMap(Class<? extends KintoObject> type) throws KintoException {
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
                    if (attribute.getType().getSimpleName().equalsIgnoreCase("List")) {
                        // relation table = <name of type>To<name of type>
                        // get the class name of type in list (https://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list)
                        Class<?> listObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                        Class<?> attributeSuperClass = listObjectClass.getSuperclass();
                        if (DataSet.haveAttributeKintoObjectAsSuperClass(attributeSuperClass)) {
                            String attributeDeclaringClassName = attribute.getDeclaringClass().getSimpleName();
                            String attributeClassName = listObjectClass.getSimpleName();
                            String relationTableName = attributeDeclaringClassName + "To" + attributeClassName;
                            // add relation information to Map
                            relationTables.put(attribute, relationTableName);
                        }
                    }
                }
            }
            if (attributes.isEmpty()) {
                String message = "Object has no attributes with annotations. Can't create table for collection.";
                if (loggerIsAvailable) {
                    kintoLogger.log(LogEntry.Severity.ERROR, message);
                }
                if (Global.DEBUG) {
                    System.out.println(message);
                }
                throw new KintoException(message);
            }
        } catch (Throwable exception) {
            if (loggerIsAvailable) {
                String message = "Reflection of " + this.type.getName() + " failed, can't create table for collection.";
                kintoLogger.log(LogEntry.Severity.ERROR, message);
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
            KintoQueryResultSet resultSet = dataBase.executeQuery(sql.toString());
            // table exists?
            // A TYPE_FORWARD_ONLY ResultSet only supports next() for navigation,
            // and not methods like first(), last(), absolute(int), relative(int).
            // The JDBC specification explicitly defines those to throw a SQLException if called on a TYPE_FORWARD_ONLY.
            // TABLE EXISTS LOCAL
            if (resultSet.next()) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "KintoCollection of " + type + " exists in local datastore.";
                    kintoLogger.log(LogEntry.Severity.DEBUG, message);
                }
                // TODO Schema-Check
            } else {
                // create table for collection
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "KintoCollection of " + getName() + " does not exists in local datastore.";
                    kintoLogger.log(LogEntry.Severity.DEBUG, message);
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
                int size = storableAttributes.size();
                if (size > 0) {
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
                            sql.append(", ");
                            sql.append(attributeName);
                            sql.append(" ");
                            sql.append(sqlDataTypeString);
                        }
                    }
                }
                sql.append(");");
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Create KintoCollection "
                            + getName()
                            + " with sql command: " + sql + ".";
                    kintoLogger.log(LogEntry.Severity.DEBUG, message);
                }
                // create the table in local datastore
                dataBase.executeSQL(sql.toString());
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
                    Class<?> listObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                    String attributeClassName = listObjectClass.getSimpleName();
                    String relationTableName = relationTables.get(attribute);
                    // check if table exists
                    StringBuilder sql = new StringBuilder("SELECT name FROM sqlite_master WHERE type='table' AND name='");
                    sql.append(relationTableName);
                    sql.append("';");
                    KintoQueryResultSet resultSet = dataBase.executeQuery(sql.toString());
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
                            kintoLogger.log(LogEntry.Severity.DEBUG, message);
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
                            kintoLogger.log(LogEntry.Severity.DEBUG, message);
                        }
                        // create the table in local datastore
                        dataBase.executeSQL(sql.toString());
                    }
                }
            }
        } catch (SQLException exception) {
            String errorMessage = "Error occurred while checking the relation table: "
                    + exception.getMessage();
            if (loggerIsAvailable) {
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
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
        return false;
    }

    private boolean isValidObjectType(@NotNull KintoObject kintoObject) throws KintoException {
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
            KintoQueryResultSet resultSet = dataBase.executeQuery(sql.toString());
            if (resultSet.next()) {
                if (Global.DEBUG && loggerIsAvailable) {
                    String message = "Kinto object with id " + kintoObject.getUUID() + " exists in local datastore.";
                    kintoLogger.log(LogEntry.Severity.DEBUG, message);
                }
                return false;
            }
        } catch (SQLException exception) {
            throw new KintoException(exception);
        }
        return true;
    }

    private @NotNull KintoObject createObjectFromResultSet(KintoQueryResultSet resultSet) throws KintoException {
        try {
            // create object from local datastore using reflection
            Constructor<? extends KintoObject> constructor = type.getConstructor();
            KintoObject kintoObject = constructor.newInstance();
            // fields from KintoObject
            Class<?> clazz = kintoObject.getClass().getSuperclass();
            if (!DataSet.haveAttributeKintoObjectAsSuperClass(clazz)) {
                throw new KintoException("The superclass of the object is not KintoObject.");
            }
            // set the uuid
            Field uuid = KintoObject.class.getDeclaredField("uuid");
            uuid.setAccessible(true);
            uuid.set(kintoObject, resultSet.getString("uuid"));
            // set the kinto id
            Field kintoid = KintoObject.class.getDeclaredField("kintoID");
            kintoid.setAccessible(true);
            kintoid.set(kintoObject, resultSet.getString("kintoid"));
            // set the other attributes
            for (String attributeName : storableAttributes.keySet()) {
                DataSet dataSet = storableAttributes.get(attributeName);
                Field attribute = dataSet.getAttribute();
                Object value = null;
                if (dataSet.isKintoObject()) {
                    // 1:1 embedded object
                    // create an "empty" object with uuid
                    if (!DataSet.haveAttributeKintoObjectAsSuperClass(attribute.getType())) {
                        throw new KintoException("The superclass of the embedded object is not KintoObject.");
                    }
                    String embeddedKintoObjectUUID = resultSet.getString(attributeName);
                    if (embeddedKintoObjectUUID.length() > 0) {
                        // embedded object can be null
                        // create embedded object using reflection
                        //noinspection unchecked
                        constructor = (Constructor<? extends KintoObject>) attribute.getType().getConstructor();
                        KintoObject embeddedKintoObject = constructor.newInstance();
                        // fields from KintoObject
                        uuid.set(embeddedKintoObject, embeddedKintoObjectUUID);
                        // set the use in relation flag
                        try {
                            Field attributeField = KintoObject.class.getDeclaredField("isUseInRelation");
                            attributeField.setAccessible(true);
                            attributeField.set(embeddedKintoObject, true);
                        } catch (NoSuchFieldException exception) {
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            String errorMessage = "The attribute field 'isUseInRelation' wasn't found: "
                                    + exception;
                            throw new KintoException(errorMessage);
                        } catch (IllegalAccessException exception) {
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            String errorMessage = "The value of attribute field 'isUseInRelation' couldn't set: "
                                    + exception;
                            throw new KintoException(errorMessage);
                        }
                        // add the embedded object
                        value = embeddedKintoObject;
                    }
                } else if (dataSet.isList()) {
                    // 1:m embedded object(s)
                    // create "empty" object(s) with uuid
                    ArrayList<KintoObject> embeddedObjectList = new ArrayList<>();
                    Class<?> listObjectClass = ((Class<?>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
                    if (!KintoObject.class.isAssignableFrom(listObjectClass)) {
                        throw new KintoException("The superclass of the embedded object is not KintoObject.");
                    }
                    //noinspection unchecked
                    constructor = (Constructor<? extends KintoObject>) listObjectClass.getConstructor();
                    // get the uuid from relation table
                    String relationTableName = relationTables.get(attribute);
                    // the name of the embedded object column uuid
                    String uuidColumnName = listObjectClass.getSimpleName().toLowerCase(Locale.ROOT) + "uuid";
                    if (relationTableName == null) {
                        String errorMessage = "Can't find the relation table name of type '"
                                + attributeName
                                +"'.";
                        throw new KintoException(errorMessage);
                    }
                    String sql = "SELECT " + uuidColumnName
                            + " FROM " + relationTableName
                            + " WHERE " + getName().toLowerCase(Locale.ROOT) + "uuid='"
                            + kintoObject.getUUID() + "';";
                    KintoQueryResultSet uuidResultSet = dataBase.executeQuery(sql);
                    while (uuidResultSet.next()) {
                        // create an object with uuid
                        KintoObject listKintoObject = constructor.newInstance();
                        uuid.set(listKintoObject, uuidResultSet.getString(uuidColumnName));
                        // set the use in relation flag
                        try {
                            Field attributeField = KintoObject.class.getDeclaredField("isUseInRelation");
                            attributeField.setAccessible(true);
                            attributeField.set(listKintoObject, true);
                        } catch (NoSuchFieldException exception) {
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            String errorMessage = "The attribute field 'isUseInRelation' wasn't found: "
                                    + exception;
                            throw new KintoException(errorMessage);
                        } catch (IllegalAccessException exception) {
                            if (Global.DEBUG) {
                                exception.printStackTrace();
                            }
                            String errorMessage = "The value of attribute field 'isUseInRelation' couldn't set: "
                                    + exception;
                            throw new KintoException(errorMessage);
                        }
                        // add to the list
                        embeddedObjectList.add(listKintoObject);
                    }
                    // add the list of embedded objects to the kinto object
                    Field listField = kintoObject.getClass().getDeclaredField(attributeName);
                    listField.setAccessible(true);
                    listField.set(kintoObject, embeddedObjectList);
                } else {
                    // attributes
                    String attributeJavaTypeString = dataSet.getJavaDataTypeString();
                    switch (attributeJavaTypeString) {
                        case "boolean":
                            value = resultSet.getBoolean(attributeName);
                            break;
                        case "int":
                            value = resultSet.getInt(attributeName);
                            break;
                        case "float":
                            value = resultSet.getFloat(attributeName);
                            break;
                        case "double":
                            value = resultSet.getDouble(attributeName);
                            break;
                        case "java.time.LocalDate":
                            value = resultSet.getDate(attributeName);
                            break;
                        case "java.lang.String":
                            value = resultSet.getString(attributeName);
                            break;
                    }
                }
                // set value to attribute
                if (value != null) {
                    attribute.setAccessible(true);
                    attribute.set(kintoObject, value);
                }
            }
            // set the flag for local persistence
            Field isPersistentAttribute = KintoObject.class.getDeclaredField("isPersistent");
            isPersistentAttribute.setAccessible(true);
            isPersistentAttribute.set(kintoObject, true);
            return kintoObject;
        } catch (NoSuchMethodException exception) {
            // constructor not found
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "The constructor wasn't found: "
                    + exception;
            throw new KintoException(errorMessage);
        } catch (NoSuchFieldException exception) {
            // field not found error
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "An attribute field wasn't found: "
                    + exception;
            throw new KintoException(errorMessage);
        } catch (InvocationTargetException exception) {
            // object creation
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "Can't invoke: "
                    + exception;
            throw new KintoException(errorMessage);
        } catch (InstantiationException exception) {
            // object creation error
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "An object couldn't create: "
                    + exception;
            throw new KintoException(errorMessage);
        } catch (IllegalAccessException exception) {
            // field access error
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            String errorMessage = "An attribute couldn't set or get: "
                    + exception;
            throw new KintoException(errorMessage);
        } catch (SQLException exception) {
            String errorMessage = "Error while searching for objects in local datastore: "
                    + exception.getMessage();
            if (loggerIsAvailable) {
                kintoLogger.log(LogEntry.Severity.ERROR, errorMessage);
            }
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        }
    }
}

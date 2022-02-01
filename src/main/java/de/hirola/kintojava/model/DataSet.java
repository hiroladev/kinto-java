package de.hirola.kintojava.model;

import de.hirola.kintojava.Global;
import de.hirola.kintojava.KintoException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Attribute data set
 *  *
 *  * <table>
 *  *     <th>SQL data type</th>
 *  *     <th>Java data type</th>
 *  *     <tr><td>TEXT</td><td>String</td>
 *  *     <tr><td>NUMERIC</td><td>boolean</td>
 *  *     <tr><td>REAL</td><td>float, double</td></tr>
 *  *     <tr><td>TEXT</td><td>LocalDate as text in iso format</td>
 *  * </table>
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public final class DataSet {

    public static final String RELATION_DATA_MAPPING_STRING = "RELATION";

    private final Field attribute;
    private String sqlDataTypeString;
    private Class<? extends KintoObject> listType;
    private boolean isKintoObject;
    private boolean isList;

    // SQLite uses a more general dynamic type system
    private final Map<String,String> DATA_MAPPINGS;
    {
        DATA_MAPPINGS = new HashMap<>();
        DATA_MAPPINGS.put("java.lang.String", "TEXT");
        DATA_MAPPINGS.put("boolean", "NUMERIC");
        DATA_MAPPINGS.put("int", "INTEGER");
        DATA_MAPPINGS.put("long", "INTEGER");
        DATA_MAPPINGS.put("float", "REAL");
        DATA_MAPPINGS.put("double", "REAL");
        DATA_MAPPINGS.put("java.time.LocalDate", "TEXT");
        DATA_MAPPINGS.put("java.time.LocalDateTime", "NUMERIC");
        DATA_MAPPINGS.put("java.util.List", "RELATION");
    }

    public DataSet(Field attribute) throws KintoException {
        if (attribute == null) {
            throw new KintoException("Attribute must not be null.");
        }
        // filter attribute names, such sql commands and in kinto used keywords
        if (attributeHasInvalidName(attribute)) {
            throw new KintoException("Attribute "
                    + attribute.getName()
                    + " is not a valid name.");
        }
        this.attribute = attribute;
        isKintoObject = false;
        isList = false;
        initAttributes();
    }

    private void initAttributes() throws KintoException {
        Class<?> attributeType = attribute.getType();
        // list of kinto objects
        if (attributeType.getSimpleName().equalsIgnoreCase("List")) {
            // TODO: Cast
            listType = ((Class<? extends KintoObject>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
            isList = true;
        }
        // kinto object -> foreign key
        if (haveAttributeKintoObjectAsSuperClass(attributeType)) {
            // "foreign key" classname+id
            sqlDataTypeString = "TEXT";
            isKintoObject = true;
        }
        if (sqlDataTypeString == null) {
            // primitive or unsupported data types
            sqlDataTypeString = DATA_MAPPINGS.get(attributeType.getName());
            if (sqlDataTypeString == null) {
                String errorMessage = "Unsupported data type: "
                        + attributeType.getName()
                        + " in "
                        + attribute.getDeclaringClass();
                throw new KintoException(errorMessage);
            }
        }
    }

    public Field getAttribute() {
        return attribute;
    }

    public String getValueAsString(KintoObject forKintoObject) throws KintoException {
        if (forKintoObject == null) {
            throw new KintoException("Object must not null.");
        }
        String valueForAttribute;
        String attributeName = attribute.getName();
        try {
            Class<? extends KintoObject> clazz = forKintoObject.getClass();
            Field attributeField = clazz.getDeclaredField(attributeName);
            attributeField.setAccessible(true);
            if (isKintoObject) {
                // return the id of the object
                KintoObject embeddedObject = (KintoObject) attributeField.get(forKintoObject);
                // embedded object can be null
                if (embeddedObject == null) {
                    valueForAttribute = "";
                } else {
                    valueForAttribute = embeddedObject.getUUID();
                }
            } else if (attributeField.getType().getName().equalsIgnoreCase("java.time.LocalDate")) {
                // return values as text (date in iso format)
                LocalDate date = (LocalDate) attributeField.get(forKintoObject);
                try {
                    valueForAttribute = date.format(DateTimeFormatter.ISO_DATE);
                } catch (DateTimeException exception) {
                    // set a default value
                    valueForAttribute = "1971-11-07";
                }
            } else if (attributeField.getType().getName().equalsIgnoreCase("java.time.LocalDateTime")) {
                // return values as text (time in milli)
                LocalDateTime time = (LocalDateTime) attributeField.get(forKintoObject);
                try {
                    valueForAttribute = String.valueOf(time
                                                        .atZone(ZoneId.systemDefault())
                                                        .toInstant()
                                                        .toEpochMilli());
                } catch (DateTimeException exception) {
                    // set a default value
                    valueForAttribute = "0";
                }
            } else if (attributeField.getType().getSimpleName().equalsIgnoreCase("boolean")){
                // return value for boolean, 0 = false / 1 = true
                String value = String.valueOf(attributeField.get(forKintoObject));
                if (value.equalsIgnoreCase("true")) {
                    valueForAttribute = String.valueOf(1);
                } else {
                    valueForAttribute = String.valueOf(0);
                }
            } else {
                // return value for simple data type
                valueForAttribute = String.valueOf(attributeField.get(forKintoObject));
            }
        } catch (NoSuchFieldException exception) {
            String errorMessage = " The attribute "
                    + attributeName
                    + " does not exist or couldn't determine with reflection: "
                    + exception.getMessage();
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        } catch (IllegalAccessException exception) {
            String errorMessage = "Error while getting value from attribute "
                    + attributeName
                    + " :"
                    + exception.getMessage();
            if (Global.DEBUG) {
                exception.printStackTrace();
            }
            throw new KintoException(errorMessage);
        }
        return valueForAttribute;
    }

    public String getSqlDataTypeString() {
        return sqlDataTypeString;
    }

    public String getJavaDataTypeString() {
        return attribute.getType().getName();
    }

    public boolean isKintoObject() {
        return isKintoObject;
    }

    public boolean isList() {
        return isList;
    }

    public Class<? extends KintoObject> getListType() {
        return listType;
    }

    public static boolean haveAttributeKintoObjectAsSuperClass(Class<?> type) {
        Class<?> superClass = type.getSuperclass();
        while (superClass != null) {
            type = superClass;
            // attribute extends KintoObject
            if (type.getName().equals("de.hirola.kintojava.model.KintoObject")) {
                return true;
            }
            superClass = type.getSuperclass();
        }
        // attribute not extends KintoObject
        return false;
    }

    public static boolean attributeHasInvalidName(Field attribute) {
        String attributeName = attribute.getName();
        return Global.illegalAttributeNames.contains(attributeName.toUpperCase());
    }
}

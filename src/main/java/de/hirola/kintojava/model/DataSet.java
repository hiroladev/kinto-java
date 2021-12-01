package de.hirola.kintojava.model;

import de.hirola.kintojava.Global;
import de.hirola.kintojava.KintoException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Attribute data set
 *
 * <table>
 *     <th>SQL data type</th>
 *     <th>Java data type</th>
 *     <tr><td>TEXT</td><td>String</td>
 *     <tr><td>NUMERIC</td><td>boolean</td>
 *     <tr><td>REAL</td><td>float, double</td></tr>
 *     <tr><td>TEXT</td><td>LocalDate as text in iso format</td>
 * </table>
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
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
        DATA_MAPPINGS.put("float", "REAL");
        DATA_MAPPINGS.put("double", "REAL");
        DATA_MAPPINGS.put("java.time.LocalDate", "TEXT");
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
        if (hasKintoObjectAsSuperClass(attributeType)) {
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
            if (isKintoObject) {
                // return the id of the object
                attributeField = clazz.getDeclaredField(attributeName);
                attributeField.setAccessible(true);
                KintoObject embeddedObject = (KintoObject) attributeField.get(forKintoObject);
                valueForAttribute = embeddedObject.getUUID();
            } else if (attributeField.getType().getName().equalsIgnoreCase("java.time.LocalDate")) {
                // return values as text (date in iso format
                LocalDate date = (LocalDate) attributeField.get(forKintoObject);
                try {
                    valueForAttribute = date.format(DateTimeFormatter.ISO_DATE);
                } catch (DateTimeException exception) {
                    // set a default value
                    valueForAttribute = "1971-11-07";
                }
            } else {
                // return value for simple data type
                attributeField = clazz.getDeclaredField(attributeName);
                attributeField.setAccessible(true);
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

    public static boolean hasKintoObjectAsSuperClass(Class<?> type) {
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

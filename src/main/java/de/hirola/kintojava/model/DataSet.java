package de.hirola.kintojava.model;

import de.hirola.kintojava.Global;
import de.hirola.kintojava.KintoException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;

/**
 * Attribute data set
 *
 * <table>
 *     <th>SQL data type</th>
 *     <th>Java data type</th>
 *     <tr><td>TEXT</td><td>String</td>
 *     <tr><td>NUMERIC</td><td>boolean</td>
 *     <tr><td>INTEGER</td><td>integer, date (Instant) as Unix Time, the number of seconds since 1970-01-01 00:00:00 UTC</td>
 *     <tr><td>REAL</td><td>float, double</td></tr>
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
    private Class<? extends KintoObject> arrayType;
    private boolean isKintoObject;
    private boolean isArray;

    // SQLite uses a more general dynamic type system
    private final Map<String,String> DATA_MAPPINGS = Map.of(
            "java.lang.String","TEXT",
            "boolean","NUMERIC",
            "int","INTEGER",
            "float","REAL",
            "double","REAL",
            "java.time.Instant","INTEGER",
            "java.util.ArrayList","RELATION");

    public DataSet(Field attribute) throws KintoException {
        if (attribute == null) {
            throw new KintoException("Attribute must not be null.");
        }
        this.attribute = attribute;
        isKintoObject = false;
        isArray = false;
        initAttributes();
    }

    private void initAttributes() throws KintoException {
        // array of kinto objects
        if (attribute.getType().getSimpleName().equalsIgnoreCase("ArrayList")) {
            arrayType = ((Class<? extends KintoObject>) ((ParameterizedType) attribute.getGenericType()).getActualTypeArguments()[0]);
            isArray = true;
        }
        // kinto object -> foreign key
        Class<?> attributeSuperClass = attribute.getType().getSuperclass();
        if (attributeSuperClass != null) {
            if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                // "foreign key" classname+id
                sqlDataTypeString = "TEXT";
                isKintoObject = true;
            }
        }
        if (sqlDataTypeString == null) {
            // primitive data types
            sqlDataTypeString = DATA_MAPPINGS.get(attribute.getType().getName());
            if (sqlDataTypeString == null) {
                throw new KintoException("Unsupported data type.");
            }
        }
    }

    public Field getAttribute() {
        return attribute;
    }

    public String getValueAsString(KintoObject forKintoObject) throws KintoException {
        String valueForAttribute;
        String attributeName = attribute.getName();
        try {
            Class<? extends KintoObject> clazz = forKintoObject.getClass();
            if (isKintoObject) {
                // return the id of the object
                Field embeddedObjectAttribute = clazz.getDeclaredField(attributeName);
                embeddedObjectAttribute.setAccessible(true);
                KintoObject embeddedObject = (KintoObject) embeddedObjectAttribute.get(forKintoObject);
                valueForAttribute = embeddedObject.getUUID();
            } else {
                // return value for simple data type
                Field attributeField = clazz.getDeclaredField(attributeName);
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

    public boolean isArray() {
        return isArray;
    }

    public Class<? extends KintoObject> getArrayType() {
        return arrayType;
    }
}

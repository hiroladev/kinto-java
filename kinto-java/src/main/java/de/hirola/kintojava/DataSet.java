package de.hirola.kintojava;

import de.hirola.kintojava.model.KintoObject;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
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

    private String sqlDataTypeString;
    private Class<? extends KintoObject> arrayType;
    private boolean isKintoObject;
    private boolean isArray;

    // SQLite uses a more general dynamic type system
    private Map<String,String> dataMappings = Map.of(
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
        isKintoObject = false;
        isArray = false;
        initAttributes(attribute);
    }

    private void initAttributes(Field attribute) throws KintoException {
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
        } if (sqlDataTypeString == null) {
            // primitive data types
            sqlDataTypeString = dataMappings.get(attribute.getType().getName());
            if (sqlDataTypeString == null) {
                throw new KintoException("Unsupported data type.");
            }
        }
    }

    public String getSqlDataTypeString() {
        return sqlDataTypeString;
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

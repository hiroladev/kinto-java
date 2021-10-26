package de.hirola.kintojava;

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

    private String sqlDataTypeString;
    private String value;

    // SQLite uses a more general dynamic type system
    private Map<Class<?>,String> dataMappings = Map.of(
            String.class,"TEXT",
            Boolean.class,"NUMERIC",
            Integer.class,"INTEGER",
            Float.class,"REAL",
            Double.class,"REAL",
            Instant.class,"INTEGER");

    public DataSet(Field attribute) throws KintoException {
        if (attribute == null) {
            throw new KintoException("Attribute must not null.");
        }
        initAttributes(attribute);
    }

    private void initAttributes(Field attribute) throws KintoException {
        // array of kinto objects
        if (attribute.getType().getSimpleName().equalsIgnoreCase("ArrayList")) {
            // ignore
            sqlDataTypeString = null;
            value =null;
            return;
        }
        // kinto object
        Class<?> attributeSuperClass = attribute.getType().getSuperclass();
        if (attributeSuperClass != null) {
            if (attributeSuperClass.getSimpleName().equalsIgnoreCase("KintoObject")) {
                // "foreign key" classname+id
                sqlDataTypeString = "TEXT";
            }
        } if (sqlDataTypeString == null) {
            // primitive data types
            sqlDataTypeString = dataMappings.get(attribute.getType());
            if (sqlDataTypeString == null) {
                throw new KintoException("Unsupported data type.");
            }
        }
        try {
            // Values stored as TEXT, INTEGER, INTEGER, REAL, TEXT.
            // INSERT INTO t1 VALUES('500.0', '500.0', '500.0', '500.0', '500.0');
            value = attribute.toString();
        } catch (Exception exception) {
            throw new KintoException(exception);
        }
    }

    public String getSqlDataTypeString() {
        return sqlDataTypeString;
    }

    public String getValue() {
        return value;
    }

}

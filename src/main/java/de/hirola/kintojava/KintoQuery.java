package de.hirola.kintojava;

import de.hirola.kintojava.model.KintoObject;

import java.sql.Connection;
import java.util.List;

/**
 * A query object can be used to get objects by a filter.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public final class KintoQuery {

    private Connection localdbConnection;

    public KintoQuery(Connection localdbConnection) {
        this.localdbConnection = localdbConnection;
    }

    /**
     *
     * @return list of all objects in the kintoCollection or null if an error occurred
     */
    public static List<KintoObject> findAll(KintoCollection kintoCollection) {
        String sql = "SELECT * from " + kintoCollection.getName();
        return null;
    }

    public static List<KintoObject> between(){return null;}
    public static List<KintoObject> equalTo(){return null;}
    public static List<KintoObject> greaterThan(){return null;}
    public static List<KintoObject> in(){return null;}
    public static List<KintoObject> lessThan(){return null;}
    public static List<KintoObject> lessThanOrEqualTo(){return null;}
    public static List<KintoObject> notEqualTo(){return null;}
}

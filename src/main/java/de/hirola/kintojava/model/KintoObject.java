package de.hirola.kintojava.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A kind of KintoObject can manipulate by Kinto.
 * Objects can create, update and delete.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public abstract class KintoObject implements KintoModel {

    // the kinto record object id
    private String kintoID;
    // the local (sqlite) id
    private String uuid;
    // to prevent inconsistent datastore
    private boolean isUseInRelation;
    private long lastModified;

    protected KintoObject() {
        kintoID = null;
        uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        isUseInRelation = false;
        lastModified = 0;
    }

    /**
     *
     * @return the unique uid for the object
     */
    public String getUUID() {
        return uuid;
    }

    /**
     *
     * @return  the kinto record object id
     */
    public String getKintoID() {
        return this.kintoID;
    }

    /**
     *
     * @return <b>true</b>, if the object used in other kinto objects
     */
    public boolean isUseInRelation() {
        return isUseInRelation;
    }

    @Override
    public String toString() {
        return uuid + "@" + getClass().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KintoObject that = (KintoObject) o;
        return lastModified == that.lastModified && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, lastModified);
    }

}

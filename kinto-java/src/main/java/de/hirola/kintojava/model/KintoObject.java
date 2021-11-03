package de.hirola.kintojava.model;

import java.util.Objects;
import java.util.UUID;

/**
 * A kind of KintoObject can manipulated by Kinto.
 * Objects can created, updated and deleted.
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
    private long lastModified;

    protected KintoObject() {
        kintoID = null;
        uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        lastModified = 0;
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
     * @param kintoID - the kinto record object id
     * @throws UnsupportedOperationException - has the object already an id
     */
    public void setKintoId(String kintoID) throws UnsupportedOperationException {
        if (this.kintoID == null) {
            this.kintoID = kintoID;
        } else {
            throw new UnsupportedOperationException("Cant set the kinto id. This object has already an kinto id.");
        }
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        if (this.uuid == null) {
            this.uuid = uuid;
        } else {
            throw new UnsupportedOperationException("Cant set objectUID. This object has already an objectUID.");
        }
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

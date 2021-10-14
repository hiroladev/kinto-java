package de.hirola.kintojava;

import java.util.Objects;

/**
 * A kind of KintoObject can manipulated by Kinto.
 * Objects can created, updated and deleted.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public abstract class KintoObject implements KintoModel {

    private String id = null;
    private long lastModified;

    protected KintoObject() {
        this.lastModified = 0;
    }

    /**
     *
     * @return  the unique id of the object
     */
    public String getId() {
        return this.id;
    }

    /**
     *
     * @param id - the unique id of the object
     * @throws UnsupportedOperationException - has the object already an id
     */
    public void setId(String id) throws UnsupportedOperationException {
        if (this.id == null) {
            this.id = id;
        } else {
            throw new UnsupportedOperationException("Cant set id. This object has already an id.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KintoObject that = (KintoObject) o;
        return lastModified == that.lastModified && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lastModified);
    }
}

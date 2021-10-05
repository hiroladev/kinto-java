package de.hirola.kintojava;

/**
 * A kind of KintoObject can manipulated by Kinto.
 * Objects can created, updated and deleted.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public abstract class KintoObject implements KintoModel{

    private String id = null;
    private int lastModified;

    protected KintoObject() {
        this.lastModified = 0;
    }

    protected KintoObject(String id, int lastModified) {
        this.id = id;
        this.lastModified = lastModified;
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
}

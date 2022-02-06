package de.hirola.kintojava.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A kind of KintoObject can manipulate by Kinto.
 * Objects can create, update and delete.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
public abstract class KintoObject implements KintoModel {

    // the kinto record object id
    private final String kintoID;
    // the local (sqlite) id
    private final String uuid;
    // to prevent inconsistent datastore
    private final boolean isUseInRelation;
    // saved to local datastore
    private final boolean isPersistent;
    // synced to remote kinto?
    private final boolean isSynced;
    private final long lastModified;

    /**
     * All objects must derive from this class to handle with local and remote datastore.
     */
    protected KintoObject() {
        kintoID = null;
        uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        isUseInRelation = false;
        isPersistent = false;
        isSynced = false;
        lastModified = 0;
    }

    /**
     * Get the UUID for the object.
     *
     * @return The unique UUID for the object.
     */
    public @NotNull String getUUID() {
        return uuid;
    }

    /**
     * Get the kinto id for the object. The id is used to sync objects.
     *
     * @return  The kinto record object id.
     */
    public String getKintoID() {
        return this.kintoID;
    }

    /**
     * Get the flag, if an object used in relation to other objects.
     *
     * @return A flag to determine if the object used in other kinto objects.
     */
    public boolean isUseInRelation() {
        return isUseInRelation;
    }

    /**
     * Get the flag, if an object saved in local datastore.
     *
     * @return A flag to determine, if the object successfully saved in local datastore.
     */
    public boolean isPersistent() {
        return isPersistent;
    }

    /**
     * Get the flag, if an object synced to a remote kinto.
     *
     * @return A flag to determine, if the object synced to a remote kinto.
     */
    public boolean isSynced() {
        return isSynced;
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

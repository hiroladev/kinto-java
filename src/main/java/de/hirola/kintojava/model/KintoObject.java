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

    protected KintoObject() {
        kintoID = null;
        uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        isUseInRelation = false;
        isPersistent = false;
        isSynced = false;
        lastModified = 0;
    }

    /**
     *
     * @return the unique uid for the object
     */
    public @NotNull String getUUID() {
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

    /**
     *
     * @return <b>true</b>, if the object successfully saved in local datastore
     */
    public boolean isPersistent() {
        return isPersistent;
    }

    /**
     *
     * @return <b>true</b>, if the object synced in a remote kinto
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

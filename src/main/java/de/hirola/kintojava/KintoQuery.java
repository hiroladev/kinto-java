package de.hirola.kintojava;

import de.hirola.kintojava.model.KintoObject;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.List;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * A query object can be used to get objects by a filter.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public final class KintoQuery {

    private final Connection localdbConnection;

    public KintoQuery(@NotNull Connection localdbConnection) {
        this.localdbConnection = localdbConnection;
    }

    public static List<KintoObject> between(){return null;}
    public static List<KintoObject> equalTo(){return null;}
    public static List<KintoObject> greaterThan(){return null;}
    public static List<KintoObject> in(){return null;}
    public static List<KintoObject> lessThan(){return null;}
    public static List<KintoObject> lessThanOrEqualTo(){return null;}
    public static List<KintoObject> notEqualTo(){return null;}

    @Override
    public String toString() {
        return "KintoQuery{" +
                "localdbConnection=" + localdbConnection +
                '}';
    }
}

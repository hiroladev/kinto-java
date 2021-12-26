package de.hirola.kintojava;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 */
public final class Global {

    // enable / disable debug mode
    public static final boolean DEBUG = true;
    // enable / disable debug mode for sql
    public static final boolean DEBUG_SQL = true;
    // workaround for "rowcount"
    // with sqlite jdbc the cursor can only forward
    public static final String rowcountColumnName = "rowcount";
    public static final List<String> illegalAttributeNames;

    static {
        illegalAttributeNames = new ArrayList<>();
        illegalAttributeNames.add("UUID");
        illegalAttributeNames.add("ALTER");
        illegalAttributeNames.add("UUID");
    }
}

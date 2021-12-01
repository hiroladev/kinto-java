package de.hirola.kintojava;

import java.util.ArrayList;
import java.util.List;

public final class Global {

    // enable / disable debug mode
    public static final boolean DEBUG = true;

    public static final List<String> illegalAttributeNames;

    static {
        illegalAttributeNames = new ArrayList<>();
        illegalAttributeNames.add("UUID");
        illegalAttributeNames.add("ALTER");
        illegalAttributeNames.add("UUID");
    }
}

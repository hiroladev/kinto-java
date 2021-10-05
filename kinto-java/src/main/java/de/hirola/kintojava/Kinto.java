package de.hirola.kintojava;

/**
 * The singleton object for data management with sqllite and kinto.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public final class Kinto {

    private static Kinto instance;
    private String bucket;

    private Kinto() {}

    /**
     *
     * @param configuration configuration for local storage and sync
     * @return singleton object for data management
     */
    public static Kinto getInstance(Configuration configuration) {

        if (instance == null) {
            instance = new Kinto();
        }

        return instance;
    }

    public void login() {



    }

}

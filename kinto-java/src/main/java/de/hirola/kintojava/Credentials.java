package de.hirola.kintojava;

/**
 * Credentials represent a login with different methods.
 * At the moment are only simple auth with user and password available.
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
 *
 */
public final class Credentials {

    public enum Method { SIMPLE }
    private static Credentials instance;
    private String user;
    private String password;
    private Method authenticationMode;

    private Credentials(String user, String password) {
        this.user = user;
        this.password = password;
        authenticationMode = Method.SIMPLE;
    }

    public static Credentials simpleAuth(String user, String password) {
        if (instance == null) {
            instance = new Credentials(user, password);
        }
        return instance;
    }

    public String getSimpleAuthString() {
        return this.user.concat(":").concat(this.password);
    }

}

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

    public enum Method {BASIC}
    private static Credentials instance;
    private String user;
    private String password;
    private Method authenticationMode;

    private Credentials(String user, String password) {
        this.user = user;
        this.password = password;
        authenticationMode = Method.BASIC;
    }

    /**
     * Kinto Accounts expects a valid username and password as Basic Auth.
     *
     * @param user username for data access
     * @param password password for data access
     * @return Credentials to login on kinto remote
     */
    public static Credentials basicAuth(String user, String password) {
        if (instance == null) {
            instance = new Credentials(user, password);
        }
        return instance;
    }

    public String getBasicAuthString() {
        return this.user + (":") + this.password;
    }

}

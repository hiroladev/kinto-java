package de.hirola.kintojava;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
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
    private final String user;
    private final String password;
    private final Method authenticationMethod;

    /**
     * Create an object to authenticate to remote kinto.
     *
     * @param user which one to log to remote kinto
     * @param password of the user
     */
    private Credentials(String user, String password) {
        this.user = user;
        this.password = password;
        authenticationMethod = Method.BASIC;
    }

    /**
     * Kinto Accounts expects a valid username and password as Basic Auth.
     *
     * @param user username for data access
     * @param password password for data access
     * @return Credentials to login to kinto remote.
     */
    public static Credentials basicAuth(String user, String password) {
        if (instance == null) {
            instance = new Credentials(user, password);
        }
        return instance;
    }

    /**
     * Get the string to authenticate.
     *
     * @return The authentication string to use while login.
     */
    public String getBasicAuthString() {
        return this.user + (":") + this.password;
    }

    /**
     * Get the authentication method. Actual only the basic methode is available.
     *
     * @return The method that is used to log in.
     */
    public Method getAuthenticationMethode() {
        return authenticationMethod;
    }
}

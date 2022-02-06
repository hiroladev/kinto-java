package de.hirola.kintojava;

/**
 * Copyright 2021 by Michael Schmidt, Hirola Consulting
 * This software us licensed under the AGPL-3.0 or later.
 *
 * Exception ...
 *
 * @author Michael Schmidt (Hirola)
 * @since 1.1.1
 *
 */
public class KintoException extends Exception {

    /**
     * Create an empty exception object.
     *
     */
    public KintoException() {
        super();
    }

    /**
     * Create an exception object with a given exception object.
     *
     * @param exception to instantiate the kinto exception object
     */
    public KintoException(Exception exception) {
        super(exception);
    }


    /**
     * Create an exception object with a given error message.
     *
     * @param message of the exception
     */
    public KintoException(String message) {
        super(message);
    }

}

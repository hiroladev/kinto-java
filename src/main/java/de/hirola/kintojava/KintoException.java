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

    public KintoException() {
        super();
    }

    public KintoException(Exception exception) {
        super(exception);
    }


    public KintoException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}

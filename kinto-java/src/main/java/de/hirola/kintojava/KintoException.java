package de.hirola.kintojava;

/**
 * Exception ...
 *
 * @author Michael Schmidt (Hirola)
 * @since 0.1.0
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

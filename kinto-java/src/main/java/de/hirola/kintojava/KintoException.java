package de.hirola.kintojava;

public class KintoException extends Exception {

    public KintoException() {
        super();
    }

    public KintoException(Exception exception) {
        super(exception);
    }

    public KintoException(String errorMessage) {
        super(errorMessage);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}

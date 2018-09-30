package bpiwowar.ml;

public class IntegrityException extends RuntimeException {

    public IntegrityException() {
    }

    public IntegrityException(String format, Object... p) {
        super(String.format(format, p));
    }

    public IntegrityException(String message) {
        super(message);
    }

    public IntegrityException(Throwable cause) {
        super(cause);
    }

    public IntegrityException(String message, Throwable cause) {
        super(message, cause);
    }

}

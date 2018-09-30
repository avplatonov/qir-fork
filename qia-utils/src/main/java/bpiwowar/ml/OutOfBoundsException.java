package bpiwowar.ml;

public class OutOfBoundsException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public OutOfBoundsException() {
    }

    public OutOfBoundsException(String message) {
        super(message);
    }

    public OutOfBoundsException(Throwable cause) {
        super(cause);
    }

    public OutOfBoundsException(String message, Throwable cause) {
        super(message, cause);
    }

    public OutOfBoundsException(String format, Object... args) {
        super(String.format(format, args));
    }

}

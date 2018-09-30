package bpiwowar;

/**
 * Exception thrown when the called method is not yet implemented
 *
 * @author bpiwowar
 */
public class NotImplementedException extends RuntimeException {

    private static final long serialVersionUID = -5584016947174440286L;

    public NotImplementedException() {
    }

    public NotImplementedException(String message) {
        super(message);
    }

    public NotImplementedException(String format, Object... values) {
        super(String.format(format, values));
    }

    public NotImplementedException(Throwable cause) {
        super(cause);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

}

package bpiwowar.lang;

public class EndOfStreamException extends java.lang.RuntimeException {
    private static final long serialVersionUID = 1L;

    public EndOfStreamException() {
    }

    public EndOfStreamException(String message) {
        super(message);
    }

    public EndOfStreamException(Throwable cause) {
        super(cause);
    }

    public EndOfStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public EndOfStreamException(Throwable cause, String format, Object... args) {
        super(String.format(format, args), cause);
    }

    public EndOfStreamException(String format, Object... args) {
        super(String.format(format, args));
    }

}

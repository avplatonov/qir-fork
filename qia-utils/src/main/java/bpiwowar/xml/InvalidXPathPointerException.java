package bpiwowar.xml;

public class InvalidXPathPointerException extends Exception {
    public InvalidXPathPointerException() {
        super();
    }

    public InvalidXPathPointerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidXPathPointerException(String message) {
        super(message);
    }

    public InvalidXPathPointerException(Throwable cause) {
        super(cause);
    }

    public InvalidXPathPointerException(String format, Object... args) {
        super(String.format(format, args));
    }

    private static final long serialVersionUID = 8803868857492124352L;

}

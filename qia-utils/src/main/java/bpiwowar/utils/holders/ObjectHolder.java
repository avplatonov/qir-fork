package bpiwowar.utils.holders;

/**
 * Wrapper class which ``holds'' an Object reference, enabling methods to return Object references through arguments.
 */
public class ObjectHolder<T> implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Value of the Object reference, set and examined by the application as needed.
     */
    public T value;

    /**
     * Constructs a new <code>ObjectHolder</code> with an initial value of
     * <code>null</code>.
     */
    public ObjectHolder() {
        value = null;
    }

    /**
     * Constructs a new <code>ObjectHolder</code> with a specific initial value.
     *
     * @param o Initial Object reference.
     */
    public ObjectHolder(T o) {
        value = o;
    }

    public static <T> ObjectHolder<T> create(T o) {
        return new ObjectHolder<T>(o);
    }

}

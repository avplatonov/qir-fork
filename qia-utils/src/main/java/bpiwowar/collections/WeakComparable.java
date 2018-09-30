package bpiwowar.collections;

public interface WeakComparable<T> {
    public static enum Status {
        GREATER, EQUAL, LESS, NOT_COMPARABLE
    }

    /**
     * Compare this with other
     *
     * @param other The other value to compare with
     * @return GREATER if a is greater than b, etc., and NOT_COMPARABLE if a and b cannot be compared
     */
    abstract public Status compare(T other);
}

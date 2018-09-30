package bpiwowar.utils;

/**
 * @param <T>
 * @param <U>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 *
 * A comparable pair
 */
public class ComparablePair<T extends Comparable<T>, U extends Comparable<U>>
    extends Pair<T, U> implements Comparable<ComparablePair<T, U>> {
    private static final long serialVersionUID = 871055007809649549L;

    public static <T extends Comparable<T>, U extends Comparable<U>> ComparablePair<T, U> create(T t, U u) {
        return new ComparablePair<T, U>(t, u);
    }

    public ComparablePair() {
    }

    public ComparablePair(final T x, final U y) {
        super(x, y);
    }

    public int compareTo(ComparablePair<T, U> o) {
        // Compares with the first
        if (first == null) {
            if (o.first != null)
                return -1;
            // otherwise both are null and we have to compare the second part of the value
        }
        else if (o.first == null)
            return 1;
        else {
            // both are not null, try to compare them
            int z = first.compareTo(o.first);
            if (z != 0)
                return z;
        }

        // Compares with the second
        if (second == null)
            return o.second == null ? 0 : -1;

        if (o.second == null)
            return 1;

        return second.compareTo(o.second);
    }

}

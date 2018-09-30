package bpiwowar.utils;

import java.io.Serializable;
import java.lang.reflect.Constructor;

final public class IntegerPair implements Comparable<IntegerPair>, Serializable {
    private static final long serialVersionUID = -8525897274045616011L;

    public int first;

    public int second;

    public IntegerPair() {
        first = 0;
        second = 0;
    }

    public IntegerPair(final int x, final int y) {
        first = x;
        second = y;
    }

    public final int getFirst() {
        return first;
    }

    public final void setFirst(final int x) {
        first = x;
    }

    public final int getSecond() {
        return second;
    }

    public final void setSecond(final int y) {
        second = y;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final IntegerPair o) {
        final int z = first - o.first;
        if (z != 0)
            return z;
        return second - o.second;
    }

    @Override
    public int hashCode() {
        return first + second;
    }

    @Override
    public String toString() {
        return String.format("<%d,%d>", first, second);
    }

    public static Constructor<IntegerPair> getDefaultConstructor() {
        try {
            return IntegerPair.class.getConstructor();
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

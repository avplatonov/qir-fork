package bpiwowar.utils;

import java.lang.reflect.Constructor;

public final class MutableInteger implements Comparable<MutableInteger> {
    public int value;

    public MutableInteger() {
        value = 0;
    }

    public MutableInteger(final int x) {
        value = x;
    }

    public int compareTo(final MutableInteger o) {
        return value - o.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    public static Constructor<MutableInteger> getDefaultConstructor() {
        try {
            return MutableInteger.class.getConstructor();
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

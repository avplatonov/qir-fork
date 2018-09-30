package bpiwowar.utils.holders;

import java.lang.reflect.Constructor;

public final class IntHolder implements Comparable<IntHolder> {
    public int value;

    public IntHolder() {
        value = 0;
    }

    public IntHolder(final int x) {
        value = x;
    }

    public int compareTo(final IntHolder o) {
        return value - o.value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    public static Constructor<IntHolder> getDefaultConstructor() {
        try {
            return IntHolder.class.getConstructor();
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

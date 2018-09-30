package bpiwowar.utils;

import java.lang.reflect.Constructor;

public final class MutableIntegerPair {
    public int first;
    public int second;

    public MutableIntegerPair() {
        first = 0;
        second = 0;
    }

    public MutableIntegerPair(final int x, final int y) {
        first = x;
        second = y;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)", first, second);
    }

    public static Constructor<MutableIntegerPair> getDefaultConstructor() {
        try {
            return MutableIntegerPair.class.getConstructor();
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

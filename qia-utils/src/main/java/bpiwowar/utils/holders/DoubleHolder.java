package bpiwowar.utils.holders;

import java.lang.reflect.Constructor;

public final class DoubleHolder implements Comparable<DoubleHolder> {
    public double value;

    public DoubleHolder() {
        value = 0;
    }

    public DoubleHolder(final double x) {
        value = x;
    }

    public int compareTo(final DoubleHolder o) {
        return Double.compare(value, o.value);
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    public static Constructor<DoubleHolder> getDefaultConstructor() {
        try {
            return DoubleHolder.class.getConstructor();
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package bpiwowar.utils.iterators;

import java.lang.reflect.Constructor;

public class SelfAggregable<K, T extends Updatable<T>> implements Aggregator<K, T, T> {

    private final Constructor<? extends T> constructor;
    private T instance;

    public SelfAggregable(Constructor<? extends T> constructor) {
        this.constructor = constructor;
    }

    public void set(int index, K k, T v) {
        instance.add(v);
    }

    public T aggregate() {
        return instance;
    }

    public void reset() {
        try {
            instance = constructor.newInstance();
        }
        catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}

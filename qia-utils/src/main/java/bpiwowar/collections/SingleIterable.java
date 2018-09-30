package bpiwowar.collections;

import java.util.Iterator;

public class SingleIterable<T> implements Iterable<T> {
    private Iterator<T> iterator;

    public SingleIterable(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    public static <T> SingleIterable<T> create(Iterator<T> iterator) {
        return new SingleIterable<T>(iterator);
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> it = iterator;
        iterator = null;
        return it;
    }

}

package bpiwowar.collections;

import bpiwowar.utils.Pair;
import java.util.Iterator;
import java.util.Map;

public class IntersectionIterable<Key extends Comparable<Key>, Value>
    implements Iterable<Pair<Key, Value[]>> {

    Iterable<? extends Map.Entry<Key, Value>>[] iterables;
    private Class<Value> vClass;

    public <T extends Iterable<? extends Map.Entry<Key, Value>>> IntersectionIterable(
        final Class<Value> vClass, T... iterables) {
        this.vClass = vClass;
        this.iterables = iterables;
    }

    public static final <U extends Comparable<U>, V, T extends Iterable<? extends Map.Entry<U, V>>> IntersectionIterable<U, V> create(
        Class<V> vClass, T... iterables) {
        return new IntersectionIterable<U, V>(vClass, iterables);
    }

    public Iterator<Pair<Key, Value[]>> iterator() {
        final Aggregator.MapValueArray<Key, Value> aggregator = new Aggregator.MapValueArray<Key, Value>(
            vClass, iterables.length);
        return new MapIntersectionIterator<Key, Value, Value[]>(aggregator, iterables);
    }

}

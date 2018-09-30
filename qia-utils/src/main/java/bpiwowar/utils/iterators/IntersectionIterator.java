/**
 *
 */
package bpiwowar.utils.iterators;

import bpiwowar.utils.Heap;
import bpiwowar.utils.HeapElement;
import bpiwowar.utils.Pair;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Intersection iterator between two sorted collections
 *
 * @param <Key>
 * @param <Value>
 * @author bpiwowar
 * @deprecated use {@linkplain bpiwowar.collections.MapIntersectionIterator}
 */
@Deprecated
public class IntersectionIterator<Key extends Comparable<Key>, Value>
    implements Iterator<Pair<Key, Value[]>> {

    /**
     * An entry that implements the heap code
     *
     * @param <U>
     * @param <V>
     * @author bpiwowar
     */
    static class Entry<U extends Comparable<U>, V> implements
        HeapElement<IntersectionIterator.Entry<U, V>>,
        Comparable<IntersectionIterator.Entry<U, V>> {
        final Iterator<Map.Entry<U, V>> iterator;

        Map.Entry<U, V> current;

        final int entryIndex;

        int index = -1;

        /**
         * @param i
         * @param map
         */
        public Entry(int i, Map<U, V> map) {
            iterator = map.entrySet().iterator();
            this.entryIndex = i;
            next();
        }

        protected void next() {
            if (iterator.hasNext())
                current = iterator.next();
            else
                current = null;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(IntersectionIterator.Entry<U, V> o) {
            return current.getKey().compareTo(o.current.getKey());
        }

        /*
         * (non-Javadoc)
         *
         * @see bpiwowar.logtools.sessionize.HeapElement#getIndex()
         */
        public int getIndex() {
            return index;
        }

        /*
         * (non-Javadoc)
         *
         * @see bpiwowar.logtools.sessionize.HeapElement#setIndex(int)
         */
        public void setIndex(int index) {
            this.index = index;
        }

    }

    Heap<IntersectionIterator.Entry<Key, Value>> heap = new Heap<IntersectionIterator.Entry<Key, Value>>();

    final ArrayList<Value> list;

    int size = 0;

    private Class<Value> vClass;

    private Pair<Key, Value[]> current;

    public <T extends Map<Key, Value>> IntersectionIterator(
        final Class<Value> vClass, T... maps) {
        this.vClass = vClass;
        list = new ArrayList<Value>(maps.length);
        for (Map<Key, Value> map : maps) {
            final IntersectionIterator.Entry<Key, Value> entry = new IntersectionIterator.Entry<Key, Value>(
                size++, map);
            if (entry.current != null)
                heap.add(entry);
            list.add(null);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        checkNext();
        return current != null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    public Pair<Key, Value[]> next() {
        checkNext();
        Pair<Key, Value[]> x = current;
        current = null;
        return x;
    }

    void checkNext() {
        if (!heap.isEmpty() && current == null)
            current = doNext();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    public Pair<Key, Value[]> doNext() {
        if (heap.isEmpty())
            throw new NoSuchElementException();
        IntersectionIterator.Entry<Key, Value> entry = heap.peek();

        // Loop until we have found a key for which we have all the values
        int N;
        Key u;
        do {
            // Get all the values with the same key
            N = 0;
            u = entry.current.getKey();
            for (int i = 0; i < size; i++)
                list.set(i, null);
            do {
                // Set the value
                list.set(entry.entryIndex, entry.current.getValue());
                // System.err.format("(%d) %s: %s => %s%n", entry.entryIndex, u,
                // entry.current.getKey(), entry.current.getValue());

                // get the next value from this stream and update
                entry.next();
                if (entry.current != null)
                    heap.update(entry);
                else
                    heap.pop();

                entry = heap.isEmpty() ? null : heap.peek();
                N++;
            }
            while (entry != null && entry.current.getKey().compareTo(u) == 0);
            // System.err.format("End of loop N=%d/%d%n", N, size);
        }
        while (entry != null && N != size);

        if (N != size)
            return null;

        @SuppressWarnings("unchecked")
        Value[] table = (Value[])Array.newInstance(vClass, size);

        // System.err.format("Returns a new pair for %s, heap is empty = %b%n",
        // u, heap.isEmpty());

        return new Pair<Key, Value[]>(u, list.toArray(table));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new NoSuchMethodError(
            "remove is not implemented in UnionIterator");
    }
}

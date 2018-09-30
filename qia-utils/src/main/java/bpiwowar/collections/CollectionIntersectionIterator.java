/**
 *
 */
package bpiwowar.collections;

import bpiwowar.log.Logger;
import bpiwowar.utils.Heap;
import bpiwowar.utils.HeapElement;
import bpiwowar.utils.Pair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Intersection iterator between two sorted collections
 *
 * @param <Key> The key
 * @param <Value> The value
 * @author bpiwowar
 */
public class CollectionIntersectionIterator<Key extends Comparable<Key>, Value>
    implements Iterator<Pair<Key, Value>> {
    final static private Logger logger = Logger.getLogger();

    /**
     * An entry that implements the heap code
     *
     * @param <U>
     * @param <V>
     * @author bpiwowar
     */
    static class Entry<U extends Comparable<U>> implements
        HeapElement<Entry<U>>, Comparable<Entry<U>> {
        final Iterator<? extends U> iterator;

        U current;

        final int entryIndex;

        int index = -1;

        /**
         * @param i
         * @param map
         */
        public Entry(int i, Iterator<? extends U> iterator) {
            this.iterator = iterator;
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
        public int compareTo(Entry<U> o) {
            return current.compareTo(o.current);
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

    Heap<Entry<Key>> heap = new Heap<Entry<Key>>();

    final ArrayList<Key> list;

    int size = 0;

    private Pair<Key, Value> current;

    private final Aggregator<Key, Value> aggregator;

    /**
     * Creates a new intersection iterator
     *
     * @param <T>
     * @param vClass
     * @param iterables
     */
    public <T extends Iterable<? extends Key>> CollectionIntersectionIterator(
        Aggregator<Key, Value> aggregator, T... iterables) {
        this.aggregator = aggregator;
        list = new ArrayList<Key>(iterables.length);
        for (T iterable : iterables) {
            final Entry<Key> entry = new Entry<Key>(size++, iterable.iterator());
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
    public Pair<Key, Value> next() {
        checkNext();
        Pair<Key, Value> x = current;
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
    public Pair<Key, Value> doNext() {
        if (heap.isEmpty())
            throw new NoSuchElementException();
        Entry<Key> entry = heap.peek();

        // Loop until we have found a key for which we have all the values
        int N;
        Key u;
        do {
            // Get all the values with the same key
            N = 0;
            u = entry.current;
            for (int i = 0; i < size; i++)
                list.set(i, null);

            do {
                // Set the value
                logger.debug("(%d) %s: %s", entry.entryIndex, u, entry.current);

                // get the next value from this stream and update
                entry.next();
                if (entry.current != null)
                    heap.update(entry);
                else
                    heap.pop();

                entry = heap.isEmpty() ? null : heap.peek();
                N++;
            }
            while (entry != null && entry.current.compareTo(u) == 0);

            logger.debug("End of loop N=%d/%d", N, size);
        }
        while (entry != null && N != size);

        if (N != size)
            return null;

        logger.debug("Returns a new pair for %s, heap is empty = %b", u, heap
            .isEmpty());

        aggregator.reset();
        for (int i = 0; i < N; i++)
            aggregator.set(i, list.get(i));
        return Pair.create(u, aggregator.aggregate());
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

    public static <Key extends Comparable<Key>, Value> CollectionIntersectionIterator<Key, Value> newInstance(
        Aggregator<Key, Value> aggregator, Iterable<Key>... iterables) {
        return new CollectionIntersectionIterator<Key, Value>(aggregator,
            iterables);
    }

    public static <Key extends Comparable<Key>> CollectionIntersectionIterator<Key, Integer> newInstance(
        Iterable<Key>... iterables) {
        return new CollectionIntersectionIterator<Key, Integer>(
            new CountAgregator<Key>(), iterables);
    }

}

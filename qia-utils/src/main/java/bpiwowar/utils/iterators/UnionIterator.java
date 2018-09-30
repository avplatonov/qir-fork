/**
 *
 */
package bpiwowar.utils.iterators;

import bpiwowar.utils.Heap;
import bpiwowar.utils.HeapElement;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * Union operator between two sorted collections
 *
 * @param <U> The key (must be comparable)
 * @param <V> The value
 * @param <W> The aggregated value
 * @author bpiwowar
 */
public class UnionIterator<U extends Comparable<U>, V, W> implements
    Iterator<Map.Entry<U, W>> {

    /**
     * The entry for the heap
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static class Entry<U extends Comparable<U>, V> implements
        HeapElement<UnionIterator.Entry<U, V>>,
        Comparable<UnionIterator.Entry<U, V>> {
        final Iterator<? extends Map.Entry<U, V>> iterator;

        Map.Entry<U, V> current;

        final int iteratorIndex;

        int index = -1;

        /**
         * @param iteratorIndex
         * @param map
         */
        public Entry(int iteratorIndex, Map<U, V> map) {
            iterator = map.entrySet().iterator();
            this.iteratorIndex = iteratorIndex;
            next();
        }

        public Entry(int entryIndex, Iterator<? extends Map.Entry<U, V>> iterator) {
            this.iterator = iterator;
            this.iteratorIndex = entryIndex;
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
        public int compareTo(UnionIterator.Entry<U, V> o) {
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

    private static final Logger logger = Logger.getLogger(UnionIterator.class);

    Heap<UnionIterator.Entry<U, V>> heap = new Heap<UnionIterator.Entry<U, V>>();

    int size = 0;

    final Aggregator<U, V, W> aggregator;

    /**
     * Intialiase with a set of iterators
     *
     * @param <T>
     * @param vClass
     * @param compressed If the array should only contain non-null iterators
     * @param iterators
     */
    public <T extends Iterator<? extends Map.Entry<U, V>>> UnionIterator(
        Aggregator<U, V, W> aggregator, T... iterators) {
        this.aggregator = aggregator;
        for (Iterator<? extends Map.Entry<U, V>> map : iterators) {
            final UnionIterator.Entry<U, V> entry = new UnionIterator.Entry<U, V>(
                size++, map);
            if (entry.current != null)
                heap.add(entry);
        }
    }

    public <T extends Map<U, V>> UnionIterator(Aggregator<U, V, W> aggregator,
        boolean compressed, T... maps) {
        this.aggregator = aggregator;
        for (Map<U, V> map : maps) {
            final UnionIterator.Entry<U, V> entry = new UnionIterator.Entry<U, V>(
                size++, map);
            if (entry.current != null)
                heap.add(entry);
        }
    }

    public static final <U extends Comparable<U>, V extends Updatable<V>,
        Iterators extends Iterator<? extends Map.Entry<U, V>>> UnionIterator<U, V, V> create(
        Class<V> selfUpdatable, Iterators... iterators) {
        SelfAggregable<U, V> selfAgregator;
        try {
            selfAgregator = new SelfAggregable<U, V>(selfUpdatable
                .getConstructor());
        }
        catch (SecurityException e) {
            throw new RuntimeException(e);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return new UnionIterator<U, V, V>(selfAgregator, iterators);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return !heap.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    public Map.Entry<U, W> next() {
        if (heap.isEmpty())
            throw new NoSuchElementException();
        UnionIterator.Entry<U, V> entry = heap.peek();
        U u = entry.current.getKey();
        aggregator.reset();

        do {
            // Set the value
            aggregator.set(entry.iteratorIndex, u, entry.current.getValue());

            if (logger.isDebugEnabled())
                logger.debug(String.format(
                    "Adding for key (%d) %s: %s => %s%n", entry.iteratorIndex,
                    u, entry.current.getKey(), entry.current.getValue()));

            // get the next value from this stream and update so that it gets
            // sorted properly
            final U oldKey = entry.current.getKey();
            entry.next();
            if (entry.current != null) {
                // Just check the order
                assert oldKey.compareTo(entry.current.getKey()) < 0;
                heap.update(entry);
            }
            else
                heap.pop();

            entry = heap.isEmpty() ? null : heap.peek();
        }
        while (entry != null && entry.current.getKey().compareTo(u) == 0);

        return new SimpleEntry<U, W>(u, aggregator.aggregate());
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

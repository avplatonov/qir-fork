package bpiwowar.utils;

import java.util.AbstractList;

/**
 * Utility class to get a (non mutable) sub list from an array
 *
 * @param <E>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
final public class PartialArrayList<E> extends AbstractList<E> {
    E[] list;
    private int start;
    private int end;

    /**
     * end must be superior to start
     *
     * @param list
     * @param start inclusive
     * @param end exclusive
     */
    public PartialArrayList(E[] list, int start, int end) {
        this.start = start;
        this.end = end;
        this.list = list;
    }

    /* (non-Javadoc)
     * @see java.util.AbstractList#get(int)
     */
    @Override
    public E get(int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException(index + " inferior to " + 0);
        if (index >= end - start)
            throw new IndexOutOfBoundsException(index + " superior or equal to " + (end - start));
        return list[index + start];
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
        return end - start;
    }

}

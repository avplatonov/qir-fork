package bpiwowar.collections;

import bpiwowar.utils.iterators.AbstractIterator;
import java.util.Iterator;

public class Range implements Iterable<Integer> {

    private final int start;
    private final int end;

    /**
     * Construct an iterator on the range [start, end)
     *
     * @param start The first integer (inclusive)
     * @param end The last integer (exclusive)
     */
    public Range(int start, int end) {
        this.start = start;
        this.end = end;

    }

    @Override
    public Iterator<Integer> iterator() {
        return new AbstractIterator<Integer>() {
            int i = start;

            @Override
            protected boolean storeNext() {
                if (i >= end)
                    return false;
                value = i;
                i++;
                return true;
            }
        };
    }

}

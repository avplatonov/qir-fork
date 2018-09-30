package bpiwowar.collections;

import bpiwowar.utils.iterators.AbstractIterator;
import java.util.Iterator;

/**
 * Glue a series of iterators together to form an iterator over the whole sequence
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class IteratorSequence<T> extends AbstractIterator<T> {
    private final Iterator<T>[] iterators;

    public IteratorSequence(Iterator<T>... iterators) {
        this.iterators = iterators;

    }

    int i = 0;

    public static <T> IteratorSequence<T> create(Iterator<T>... iterators) {
        return new IteratorSequence<T>(iterators);
    }

    @Override
    protected boolean storeNext() {
        while (i < iterators.length) {
            if (iterators[i].hasNext()) {
                value = iterators[i].next();
                return true;
            }
            i++;
        }
        return false;
    }

}

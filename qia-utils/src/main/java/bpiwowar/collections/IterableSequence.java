package bpiwowar.collections;

import java.util.Iterator;

/**
 * Glue a series of iterables together to form an iterable over the whole sequence
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class IterableSequence<T> implements Iterable<T> {
    private final Iterable<T>[] iterables;

    public IterableSequence(Iterable<T>... iterables) {
        this.iterables = iterables;

    }

    public static <T> IterableSequence<T> create(Iterable<T>... iterables) {
        return new IterableSequence<T>(iterables);
    }

    @Override
    public Iterator<T> iterator() {
        @SuppressWarnings("unchecked")
        Iterator<T>[] iterators = new Iterator[iterables.length];
        for (int i = iterables.length; --i >= 0; )
            iterators[i] = iterables[i].iterator();
        return IteratorSequence.create(iterators);
    }

}

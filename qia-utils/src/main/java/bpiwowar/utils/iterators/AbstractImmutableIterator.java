package bpiwowar.utils.iterators;

import bpiwowar.pipe.CloseableIterator;

/**
 * Deprecated: use {@see AbstractIterator}
 *
 * @param <E>
 * @author bpiwowar
 */
@Deprecated
abstract public class AbstractImmutableIterator<E> implements CloseableIterator<E> {
    @SuppressWarnings("serial")
    static class EndOfStream extends Throwable {
    }

    protected E value;
    byte status = -1;

    /**
     * Stores a new element in value
     *
     * @return true if there was a new element, false otherwise
     */
    protected abstract boolean storeNext();

    final public boolean hasNext() {
        if (status == -1)
            status = (byte)(storeNext() ? 1 : 0);
        return status == 1;
    }

    final public E next() {
        E next = value;
        status = (byte)(storeNext() ? 1 : 0);
        return next;
    }

    final public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }

}

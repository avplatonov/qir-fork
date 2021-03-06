package bpiwowar.utils.iterators;

import bpiwowar.pipe.CloseableIterator;
import java.util.NoSuchElementException;

abstract public class AbstractIterator<E> implements CloseableIterator<E> {
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

    final protected void store(E e) {
        this.value = e;
    }

    final public boolean hasNext() {
        if (status == -1)
            status = (byte)(storeNext() ? 1 : 0);
        return status == 1;
    }

    @Override final public E next() {
        if (!hasNext())
            throw new NoSuchElementException();
        E next = value;
        status = -1;
        return next;
    }

    final public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }

}

/**
 *
 */
package bpiwowar.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class EmptyIterator<T> implements
    Iterator<T> {
    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public T next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new NoSuchElementException();
    }

    final static public <T> EmptyIterator<T> create() {
        return new EmptyIterator<T>();
    }
}

package bpiwowar.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * An empty set
 *
 * @param <T>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 * @deprecated Moved to bpiwowar.collections
 */
@Deprecated
public class EmptySet<T> implements Set<String> {
    @Override
    public boolean add(String o) {
        throw new RuntimeException("Cannot add objects in an immutable empty set");
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        throw new RuntimeException("Cannot add objects in an immutable empty set");
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Iterator<String> iterator() {
        return null;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Object[] toArray() {
        return null;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return null;
    }

}

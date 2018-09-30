package bpiwowar.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A (read-only) set which is the union of other sets
 *
 * @param <T>
 * @author bpiwowar
 */
public class UnionSet<T> implements Set<T> {
    ArrayList<Set<T>> sets = new ArrayList<Set<T>>();

    public UnionSet(Set<T>... sets) {
        for (Set<T> set : sets)
            this.sets.add(set);
    }

    public boolean add(Object e) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(Object o) {
        for (Set<T> set : sets)
            if (set.contains(o))
                return true;
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        for (Object o : c)
            if (!contains(o))
                return false;
        return true;
    }

    public boolean isEmpty() {
        for (Set<T> set : sets)
            if (!set.isEmpty())
                return false;
        return true;
    }

    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the sum of the sizes of the contained sets This number can be different from the actual real size of the
     * union of the sets.
     */
    public int size() {
        int size = 0;
        for (Set<T> set : sets)
            size += set.size();
        return size;
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public Object[] toArray(Object[] a) {
        throw new UnsupportedOperationException();
    }

}

package bpiwowar.utils;

/**
 * @author bpiwowar
 */
public interface HeapElement<E> extends Comparable<E> {
    int getIndex();

    void setIndex(int index);
}

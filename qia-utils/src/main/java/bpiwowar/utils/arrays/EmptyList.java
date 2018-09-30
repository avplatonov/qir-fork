package bpiwowar.utils.arrays;

import java.util.AbstractList;

public class EmptyList<T> extends AbstractList<T> {

    /**
     * Construct a list adaptator from an array
     *
     * @param array
     */
    public EmptyList() {
    }

    @Override
    public T get(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public int size() {
        return 0;
    }

    public T set(int index, T value) {
        throw new IndexOutOfBoundsException();
    }

    ;

}

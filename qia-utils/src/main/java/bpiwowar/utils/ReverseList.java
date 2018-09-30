package bpiwowar.utils;

import java.util.AbstractList;
import java.util.List;

final public class ReverseList<E> extends AbstractList<E> {
    List<E> list;

    public ReverseList(List<E> list) {
        this.list = list;
    }

    /* (non-Javadoc)
     * @see java.util.AbstractList#get(int)
     */
    @Override
    public E get(int index) {
        return list.get(size() - index - 1);
    }

    /* (non-Javadoc)
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
        return list.size();
    }

}

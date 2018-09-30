package bpiwowar.collections;

import java.util.AbstractList;
import java.util.List;

/**
 * Adaptor for a list so that one cannot modify it
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class ImmutableListAdaptor<E> extends AbstractList<E> {
    private final List<E> list;

    public ImmutableListAdaptor(List<E> list) {
        this.list = list;
    }

    @Override
    public E get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

}

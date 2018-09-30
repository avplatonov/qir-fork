package bpiwowar.collections;

import bpiwowar.utils.Holder;
import java.util.Iterator;

public class SynchronisedIterator<T> {
    Iterator<T> iterator;

    public SynchronisedIterator(Iterator<T> iterator) {
        super();
        this.iterator = iterator;
    }

    public boolean next(Holder<T> holder) {
        synchronized (iterator) {
            if (iterator.hasNext()) {
                holder.set(iterator.next());
                return true;
            }
        }
        return false;
    }

}

package bpiwowar.utils;

/**
 * A heap with a fixed maximum size: smallest elements are discarded
 *
 * @param <E>
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class BoundedHeap<E extends HeapElement<E>> extends Heap<E> {
    private int maxSize;

    public BoundedHeap(int size) {
        this.maxSize = size;
    }

    @Override
    public void add(E item) {
        enqueue(item);
    }

    public boolean enqueue(E item) {
        // Do we still have space ?
        if (size() < maxSize)
            super.add(item);
        else {
            E peek = peek();
            // If the item is smaller than the smallest, quit
            if (item.compareTo(peek) <= 0)
                return false;

            // replace the smallest
            swap(0, item);
        }

        return true;

    }

    ;

}

package bpiwowar.collections;

import bpiwowar.utils.Heap;
import bpiwowar.utils.HeapElement;

/**
 * A heap that keeps a maximum number of elements (only the highest elements)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class BoundedSizeHeap<E extends HeapElement<E>> extends Heap<E> {
    private final int maximumSize;

    public BoundedSizeHeap(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * Add a new item; if the heap is full, then add the element only if it is greater than the minimum - otherwise,
     * discard it
     *
     * @see bpiwowar.utils.Heap#add(bpiwowar.utils.HeapElement)
     */
    public void add(E item) {
        // If we are at the limit
        if (size() < maximumSize) {
            super.add(item);
        }
        else {
            if (peek().compareTo(item) < 0) {
                super.pop();
                super.add(item);
            }
        }
    }

    public static <E extends HeapElement<E>> BoundedSizeHeap<E> create(
        int maximumSize) {
        return new BoundedSizeHeap<E>(maximumSize);
    }

    ;
}

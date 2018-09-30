package bpiwowar.utils;

/**
 * A sliding window, which keeps track of the most recent items added
 */
public class SlidingWindow<T> {
    /** Number of objects added so far */
    int size = 0;

    /** Current index in the storage array */
    int index = -1;

    /** The window */
    final T[] window;

    public SlidingWindow(T[] array) {
        this.window = array;
    }

    /**
     * Add a new object to the sliding window
     *
     * @param t The object to add
     */
    public void add(T t) {
        index = (index + 1) % window.length;
        size = size + 1;
        window[index] = t;
    }

    public int getSize() {
        return size;
    }

    public int getCapacity() {
        return window.length;
    }

    /**
     * Get the i<sup>th</sup> of the sequence
     *
     * @param position
     * @return
     * @throws ArrayIndexOutOfBoundsException if the current position is not within the window
     */
    public T get(int position) {
        if (position >= size || position < (size - window.length))
            throw new ArrayIndexOutOfBoundsException(
                String
                    .format(
                        "Position %d of an array of size %d with a window of size %d",
                        position, size, window.length));

        int i = (window.length - (size - 1 - position) + index) % window.length;

        return window[i];
    }

    /**
     * Returns true if the window is full
     *
     * @return
     */
    public boolean isFull() {
        return size >= window.length;
    }

    public void add(T... list) {
        for (T t : list)
            add(t);
    }

    /**
     * Get the first item of the current window
     *
     * @param relativePosition the relative position
     * @return The corresponding object
     */
    public T getRelative(int relativePosition) {
        // index is the next element
        if (relativePosition < 0 || relativePosition >= window.length
            || relativePosition >= size)
            throw new ArrayIndexOutOfBoundsException();

        // If the window is full, the beginning of the window is index + 1
        if (isFull())
            return window[(index + 1 + relativePosition) % window.length];
        else
            return window[relativePosition];
    }

}

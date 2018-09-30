package bpiwowar.utils.iterators;

public interface Aggregator<K, V, W> {
    /**
     * Called before a new key appears
     */
    public void reset();

    /**
     * Called to add a new value
     *
     * @param index The iterator index
     * @param k The corresponding key
     * @param v The value to add
     */
    public void set(int index, K k, V v);

    /**
     * Called to retrieve the value
     */
    public W aggregate();
}

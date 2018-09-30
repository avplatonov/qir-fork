package bpiwowar.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A (read-only) map which is the union of other sets
 *
 * @author bpiwowar
 */
public class UnionMap<K, V> implements Map<K, V> {
    ArrayList<Map<K, V>> maps = new ArrayList<Map<K, V>>();

    public UnionMap(Map<K, V>... maps) {
        for (Map<K, V> map : maps)
            this.maps.add(map);
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        for (Map<K, V> map : maps)
            if (map.containsKey(key))
                return true;
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object value) {
        for (Map<K, V> map : maps)
            if (map.containsValue(value))
                return true;
        return false;
    }

    /* (non-Javadoc)
     * @see java.util.Map#entrySet()
     */
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the sum of the sizes of the contained sets This number can be different from the actual real size of the
     * union of the sets.
     */
    public int size() {
        int size = 0;
        for (Map<K, V> map : maps)
            size += map.size();
        return size;
    }

    public V remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#get(java.lang.Object)
     */
    public V get(Object key) {
        V v = null;
        for (Map<K, V> map : maps)
            if ((v = map.get(key)) != null)
                return v;
        return null;
    }

    /* (non-Javadoc)
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        for (Map<K, V> map : maps)
            if (!map.isEmpty())
                return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.util.Map#keySet()
     */
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see java.util.Map#values()
     */
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

}

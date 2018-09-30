package bpiwowar.utils.iterators;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Taken from java.util.AbstractMap
 */
public class SimpleEntry<K, V> implements Entry<K, V> {
    K key;
    V value;

    public SimpleEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public SimpleEntry(Entry<K, V> e) {
        this.key = e.getKey();
        this.value = e.getValue();
    }

    public SimpleEntry() {
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        @SuppressWarnings("unchecked")
        Map.Entry<K, V> e = (Map.Entry<K, V>)o;
        return eq(key, e.getKey()) && eq(value, e.getValue());
    }

    public int hashCode() {
        return ((key == null) ? 0 : key.hashCode())
            ^ ((value == null) ? 0 : value.hashCode());
    }

    public String toString() {
        return key + "=" + value;
    }

    private static boolean eq(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    /**
     * Helper method to create a new entry
     *
     * @param <K>
     * @param <V>
     * @param key
     * @param value
     * @return
     */
    public static <K, V> Entry<K, V> create(K key,
        V value) {
        return new SimpleEntry<K, V>(key, value);
    }
}

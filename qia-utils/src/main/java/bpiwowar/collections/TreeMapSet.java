package bpiwowar.collections;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A TreeMap where a value a set
 *
 * @param <K>
 * @param <V>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class TreeMapSet<K, V, W extends Set<V>> extends TreeMap<K, W> {
    private static final long serialVersionUID = 5935599516863242454L;
    private final Class<? extends W> setClass;

    public TreeMapSet(Class<? extends W> setClass) {
        super();
        this.setClass = setClass;
    }

    public TreeMapSet(Class<? extends W> setClass, Comparator<? super K> c) {
        super(c);
        this.setClass = setClass;
    }

    public TreeMapSet(Class<? extends W> setClass, Map<? extends K, W> m) {
        super(m);
        this.setClass = setClass;
    }

    public TreeMapSet(Class<? extends W> setClass, SortedMap<K, W> m) {
        super(m);
        this.setClass = setClass;
    }

    /**
     * Add a new value to the array list for a given key
     *
     * @param key
     * @param value
     */
    public void add(K key, V value) {
        W list = get(key);
        if (list == null) {
            try {
                list = setClass.newInstance();
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }
            put(key, list);
        }
        list.add(value);
    }

    public static <K, V, W extends Set<V>> TreeMapSet<K, V, W> newInstance(
        Class<? extends W> setClass) {
        return new TreeMapSet<K, V, W>(setClass);
    }

}

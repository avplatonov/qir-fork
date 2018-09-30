package bpiwowar.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A TreeMap where a value is an ArrayList of a given type
 *
 * @param <K>
 * @param <V>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class TreeMapArray<K, V> extends TreeMap<K, ArrayList<V>> {
    private static final long serialVersionUID = 5935599516863242454L;

    public TreeMapArray() {
        super();
    }

    public TreeMapArray(Comparator<? super K> c) {
        super(c);
    }

    public TreeMapArray(Map<? extends K, ? extends ArrayList<V>> m) {
        super(m);
    }

    public TreeMapArray(SortedMap<K, ? extends ArrayList<V>> m) {
        super(m);
    }

    /**
     * Add a new value to the array list for a given key
     *
     * @param key
     * @param value
     */
    public void add(K key, V value) {
        ArrayList<V> list = getList(key);
        list.add(value);
    }

    /**
     * @param key
     * @return
     */
    private ArrayList<V> getList(K key) {
        ArrayList<V> list = get(key);
        if (list == null)
            put(key, list = new ArrayList<V>());
        return list;
    }

    public static <K, V> TreeMapArray<K, V> newInstance() {
        return new TreeMapArray<K, V>();
    }

    public static <K, V> TreeMapArray<K, V> newInstance(
        Comparator<? super K> comparator) {
        return new TreeMapArray<K, V>(comparator);
    }

    public void addAll(K key, Collection<? extends V> values) {
        ArrayList<V> list = getList(key);
        list.addAll(values);
    }

}

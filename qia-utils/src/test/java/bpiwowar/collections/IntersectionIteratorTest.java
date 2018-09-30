package bpiwowar.collections;

import bpiwowar.collections.Aggregator.MapValueArray;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Pair;
import java.util.TreeMap;
import org.junit.Assert;
import org.junit.Test;

public class IntersectionIteratorTest {
    @Test
    public void simpleCheck() {
        TreeMap<Integer, Integer> map1 = GenericHelper.newTreeMap();
        fill(map1, 1, 2, 3, 4, 5, 6);
        TreeMap<Integer, Integer> map2 = GenericHelper.newTreeMap();
        fill(map2, 1, 0, 4, 5, 5, 8);

        MapValueArray<Integer, Integer> aggregator = new Aggregator.MapValueArray<Integer, Integer>(
            Integer.class, 2);

        MapIntersectionIterator<Integer, Integer, Integer[]> it = new MapIntersectionIterator<Integer, Integer, Integer[]>(
            aggregator, map1, map2);

        Pair<Integer, Integer[]> v = it.next();
        assert v.getFirst() == 1;
        Assert.assertArrayEquals(v.getSecond(), new Integer[] {2, 0});

        v = it.next();
        assert v.getFirst() == 5;
        Assert.assertArrayEquals(v.getSecond(), new Integer[] {6, 8});

    }

    private void fill(TreeMap<Integer, Integer> map, int... values) {
        assert values.length % 2 == 0;
        for (int i = 0; i < values.length; i += 2)
            map.put(values[i], values[i + 1]);
    }
}

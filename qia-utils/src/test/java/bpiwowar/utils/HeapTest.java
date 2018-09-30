/**
 *
 */
package bpiwowar.utils;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author bpiwowar
 */
public class HeapTest {

    static public class Element extends Heap.DefaultElement<Element> {
        int count;

        public Element(int i) {
            count = i;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Element o) {
            return count - o.count;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return Integer.toString(count);
        }
    }

    private ArrayList<Element> toElementArray(int[] a) {
        ArrayList<Element> list = new ArrayList<Element>(a.length);
        for (int x : a)
            list.add(new Element(x));
        return list;
    }

    private Heap<Element> construct(Iterable<Element> a) {
        Heap<Element> heap = new Heap<Element>();
        for (Element x : a)
            heap.add(x);
        return heap;
    }

    private void compare(Heap<Element> heap, ArrayList<Element> a) {
        int z[] = new int[a.size()];
        int i = 0;
        while (!heap.isEmpty()) {
            z[i] = heap.pop().count;
            i++;
        }

        Element[] x = new Element[a.size()];
        a.toArray(x);

        Arrays.sort(x);
//		System.err.format("Comparing: %s and %s%n", Arrays.toString(z), Arrays.toString(x));
        for (i = 0; i < x.length; i++)
            Assert.assertEquals(z[i], x[i].count);
    }

    private void simpleTest(int... a) {
        final ArrayList<Element> list = toElementArray(a);
        Heap<Element> heap = construct(list);
        compare(heap, list);
    }

    @Test
    public void test1() {
        simpleTest(4, 3, 2, 1);
    }

    @Test
    public void test2() {
        simpleTest(1, 2, 3, 4);
    }

    @Test
    public void test3() {
        simpleTest(1, 3, -1, 2);
    }

    @Test
    public void testUpdate1() {
        ArrayList<Element> a = toElementArray(new int[] {1, 5, 7, 9});
        Heap<Element> heap = construct(a);
        a.get(0).count = 10;
        heap.update(a.get(0));
        compare(heap, a);
    }

    @Test
    public void testUpdate2() {
        ArrayList<Element> a = toElementArray(new int[] {1, 5, 7, 9});
        Heap<Element> heap = construct(a);

        a.get(2).count = 0;
        heap.update(a.get(2));
        compare(heap, a);
    }

    @Test
    public void testRandom1() {
        final int MAX_SIZE = 500;
        final int NUM_UPDATES = 100;
        final int NUM_TRIALS = 1000;

        for (int i = 0; i < NUM_TRIALS; i++) {
            ArrayList<Element> elements = new ArrayList<Element>();
            int N = (int)(Math.random() * MAX_SIZE);
            for (int k = 0; k < N; k++)
                elements.add(new Element((int)(Math.random() * MAX_SIZE * 2)));

            Heap<Element> heap = construct(elements);
            for (int k = 0; k < (int)(Math.random() * NUM_UPDATES); k++) {
                if (!heap.isEmpty() && Math.random() > 0.5) {
                    Element e = heap.list.get((int)(Math.random() * heap.list.size()));
                    e.count = (int)(Math.random() * MAX_SIZE * 2);
                    heap.update(e);
                }
                else {
                    Element e = new Element((int)(Math.random() * MAX_SIZE * 2));
                    elements.add(e);
                    heap.add(e);
                }
            }

            compare(heap, elements);
        }
    }

}

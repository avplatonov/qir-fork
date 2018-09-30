/**
 *
 */
package bpiwowar.testng;

import bpiwowar.utils.iterators.AbstractIterator;
import java.util.Iterator;

public class CartesianProduct implements Iterable<Object[]> {
    private final Iterable<? extends Object[]>[] iterables;

    public CartesianProduct(Iterable<? extends Object[]>... iterables) {
        this.iterables = iterables;
    }

    @Override
    public Iterator<Object[]> iterator() {
        @SuppressWarnings("unchecked") final Iterator<? extends Object[]>[] iterators = new Iterator[iterables.length];

        return new AbstractIterator<Object[]>() {
            boolean eof = false;
            Object[][] current;

            @Override
            protected boolean storeNext() {
                if (eof)
                    return false;

                if (current == null) {
                    // Initialisation
                    current = new Object[iterables.length][];
                    for (int i = 0; i < iterables.length; i++) {
                        iterators[i] = iterables[i].iterator();
                        if (!iterators[i].hasNext()) {
                            eof = true;
                            return false;
                        }
                        current[i] = iterators[i].next();
                    }
                }
                else {
                    // Next
                    for (int i = 0; i < iterables.length; i++) {
                        if (!iterators[i].hasNext()) {
                            if (iterables.length - 1 == i) {
                                eof = true;
                                return false;
                            }
                            iterators[i] = iterables[i].iterator();
                            current[i] = iterators[i].next();
                        }
                        else {
                            current[i] = iterators[i].next();
                            break;
                        }
                    }
                }

                // Everything is OK, copy
                int size = 0;
                for (int i = 0; i < iterables.length; i++)
                    size += current[i].length;

                value = new Object[size];

                int index = 0;
                for (int i = 0; i < iterables.length; i++) {
                    final int L = current[i].length;
                    System.arraycopy(current[i], 0, value, index, L);
                    index += L;
                }

                return true;
            }
        };
    }

}

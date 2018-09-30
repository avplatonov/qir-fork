package bpiwowar.pipe;

import bpiwowar.utils.Pair;
import java.util.Iterator;

/**
 * An iterator on iterators of outer.
 *
 * @param <Outer> The type of the objects for the outer loop
 * @param <Inner> The type of the objects for the inner loop
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public interface NestedIterator<Outer, Inner> extends CloseableIterator<Pair<Outer, Iterator<Inner>>> {

}

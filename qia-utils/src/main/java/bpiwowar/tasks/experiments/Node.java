package bpiwowar.tasks.experiments;

import bpiwowar.collections.EmptyIterator;
import java.util.Iterator;
import java.util.Map;

abstract public class Node implements Iterable<Map<String, String>> {
    static final public Node EMPTY = new Node() {
        @Override
        public Iterator<Map<String, String>> iterator() {
            return EmptyIterator.create();
        }
    };
}

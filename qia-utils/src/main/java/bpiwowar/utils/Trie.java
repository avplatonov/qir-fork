/**
 *
 */
package bpiwowar.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

@Deprecated
public class Trie<T, U> implements Iterable<Trie<T, U>> {
    TreeMap<T, Trie<T, U>> children;
    U data;

    public void reset() {
        children = new TreeMap<T, Trie<T, U>>();
        data = null;
    }

    {
        reset();
    }

    public Trie<T, U> add(Iterator<T> iterator) {
        if (iterator.hasNext()) {
            T id = iterator.next();
            Trie<T, U> child = children.get(id);
            if (child == null) {
                child = new Trie<T, U>();
                children.put(id, child);
            }
            return child.add(iterator);
        }
        return this;
    }

    public Map<T, Trie<T, U>> children() {
        return children;
    }

    public Iterator<Trie<T, U>> iterator() {
        return new Iterator<Trie<T, U>>() {
            Iterator<Trie<T, U>> iterator = children.values().iterator();

            Iterator<Trie<T, U>> childIterator = null;

            private boolean selfDone = false;

            public void remove() {
                throw new RuntimeException("Cannot modify");
            }

            public Trie<T, U> next() {
                // Ourselves first
                if (!selfDone) {
                    selfDone = true;
                    return Trie.this;
                }
                // Otherwise, use one of our children iterators
                if (childIterator != null && childIterator.hasNext()) {
                    return childIterator.next();
                }

                // Otherwise, init an iterator for our current child
                if (!iterator.hasNext())
                    throw new NoSuchElementException();

                childIterator = iterator.next().iterator();
                return childIterator.next();
            }

            public boolean hasNext() {
                return !selfDone || iterator.hasNext()
                    || (childIterator != null && childIterator.hasNext());
            }

        };
    }

    public U getData() {
        return data;
    }

    public void setData(U data) {
        this.data = data;
    }

}

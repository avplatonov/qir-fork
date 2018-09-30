package bpiwowar.pipe;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

public class Flattener<T> implements CloseableIterator<T> {
    final static private Logger logger = Logger.getLogger(Flattener.class);
    Iterator<Iterator<T>> iterator;
    Iterator<T> current = null;

    public Flattener(Iterator<Iterator<T>> iterator) {
        this.iterator = iterator;
        searchNext();
    }

    @Override
    public String toString() {
        return "Flattener(" + iterator + ")";
    }

    private void searchNext() {
        // Search the next non empty iterator
        while (iterator.hasNext()) {
            current = iterator.next();
            if (current.hasNext())
                return;
        }

        // we did not find anything
        current = null;
        close();

    }

    public void close() {
        logger.debug("Closing iterator");
        if (iterator instanceof CloseableIterator)
            ((CloseableIterator<Iterator<T>>)iterator).close();
    }

    public boolean hasNext() {
        return current != null && current.hasNext();
    }

    public T next() {
        if (current == null)
            throw new NoSuchElementException();
        T t = current.next();
        if (!current.hasNext())
            searchNext();
        return t;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

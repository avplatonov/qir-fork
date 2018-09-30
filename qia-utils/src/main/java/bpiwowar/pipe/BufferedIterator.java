package bpiwowar.pipe;

import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Creates a buffered iterator from an iterator
 *
 * @param <T>
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class BufferedIterator<T> extends BufferedProducer<T> {
    private static final Logger logger = Logger.getLogger(BufferedIterator.class);

    private Iterator<T> iterator;

    public BufferedIterator(Iterator<T> iterator, int bufferSize) {
        super(bufferSize, true);
        this.iterator = iterator;
        readyToProduce();
    }

    @Override
    public String toString() {
        return "BufferedIterator(" + iterator + ")";
    }

    @Override
    public void close() {
        super.close();
        if (iterator instanceof CloseableIterator<?>) {
            ((CloseableIterator<T>)iterator).close();
        }
    }

    @Override
    protected void produce() throws InterruptedException {
        try {
            while (iterator.hasNext()) {
                T next = iterator.next();
                produce(next);
            }
        }
        catch (InterruptedException e) {
            // Production was interrupted
            logger.debug("Production of " + this + " was interrupted");
            throw e;
        }

    }
}

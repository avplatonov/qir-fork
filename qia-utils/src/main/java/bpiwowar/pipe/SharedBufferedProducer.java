package bpiwowar.pipe;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An abstract buffered producer, for parallelising. This producer output can be shared between different outputs
 * instead of being duplicated (useful when the production process is heavy)
 *
 * @param <T> The type of the produced objects
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class SharedBufferedProducer<T> implements Runnable,
    Iterator<T>, Cloneable {

    /**
     * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
     */
    @SuppressWarnings("serial")
    static public class NoSuchElementException extends RuntimeException {
    }

    /** Holds the last objects */
    private ArrayList<T> stack = new ArrayList<T>();

    /** Maximum buffer size */
    private int bufferSize = 10;

    /** Size of the list (only volatile object) */
    volatile private int size = 0;

    /** EOS end of stream */
    private boolean endOfStream = false;

    /** List of cloned iterators along with their position in the stack */
    private ArrayList<Long> list = new ArrayList<Long>();

    public SharedBufferedProducer() {
        this(10);
    }

    public SharedBufferedProducer(int bufferSize) {
        this.bufferSize = bufferSize;
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Should register another listener with current position
        throw new CloneNotSupportedException();
    }

    /**
     * Add a new object to a shared buffer or wait if no room is available.
     *
     * @param object The object to be added to the buffer.
     */
    protected synchronized void produce(T o) {
        while (size > bufferSize) {
            try {
                wait();
            }
            catch (InterruptedException e) {
            }
        }
        stack.add(o);
        size++;

        notifyAll();
    }

    /**
     * Remove the first object waiting in the buffer or wait until there is one.
     *
     * @return The first object in the buffer.
     */
    final private synchronized T consume() {
        while (size == 0 && !endOfStream) {
            try {
                wait();
            }
            catch (InterruptedException e) {
            }
        }
        if (size == 0 && endOfStream)
            throw new NoSuchElementException();
        size--;
        notifyAll();
        return stack.remove(0);
    }

    final private synchronized void check() {
        while (size == 0 && !endOfStream) {
            try {
                wait();
            }
            catch (InterruptedException e) {
            }
        }
        notifyAll();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    final public boolean hasNext() {
        check();
        if (size == 0 && endOfStream)
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#next()
     */
    public T next() {
        return consume();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new RuntimeException("Non mutable iterator");
    }

    final synchronized private void markEndOfStream() {
        endOfStream = true;
        notifyAll();
    }

    final public void run() {
        produce();
        markEndOfStream();
    }

    /**
     * The produce method is the only one that <b>must</b> be overwritten by the producer
     */
    protected abstract void produce();

    /**
     * Test
     *
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        SharedBufferedProducer<Integer> producer = new SharedBufferedProducer<Integer>() {
            public void produce() {
                for (int i = 0; i < 1000; i++) {
                    try {
                        if (i % 25 == 0)
                            Thread.sleep(20);
                    }
                    catch (InterruptedException e) {
                    }
                    produce(i);
                    System.out.format("Produced %d%n", i);
                }
            }
        };

        System.out.println("Here");
        Thread.sleep(100);
        int i = 0;
        while (producer.hasNext()) {
            if (i++ % 25 == 0)
                Thread.sleep(20);
            System.out.format("Read %d%n", producer.next());
        }
    }

}

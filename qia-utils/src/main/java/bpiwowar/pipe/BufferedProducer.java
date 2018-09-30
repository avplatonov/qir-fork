package bpiwowar.pipe;

import bpiwowar.io.LoggerPrintStream;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An abstract buffered producer, for parallelising. A real producer needs to subclass this class.
 *
 * @param <T> The type of the produced objects
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class BufferedProducer<T> implements Runnable,
    CloseableIterator<T> {
    final static private Logger logger = Logger
        .getLogger(BufferedProducer.class);

    /** Holds the last file */
    private ArrayList<T> stack = new ArrayList<T>();

    /** Maximum buffer size */
    private int bufferSize = 10;

    /** Size of the list (only volatile object) */
    volatile private int size = 0;

    /**
     * Should we wait for the object to be initialized?
     */
    protected volatile boolean wait;

    /** EOS end of stream */
    private boolean endOfStream = false;

    private Thread thread;

    public BufferedProducer() {
        this(10, false);
    }

    public BufferedProducer(int bufferSize, boolean wait) {
        this.bufferSize = bufferSize;
        thread = new Thread(this);
        this.wait = wait;
        thread.start();
    }

    public BufferedProducer(boolean wait) {
        this(10, wait);
    }

    @Override
    protected void finalize() throws Throwable {
        // Interrupt the thread if the iterator was not used until the end
        close();
    }

    /**
     * Add a new object to a shared buffer or wait if no room is available.
     *
     * @param object The object to be added to the buffer.
     * @throws InterruptedException
     */
    protected synchronized void produce(T o) throws InterruptedException {
        while (size > bufferSize) {
            wait();
        }
        stack.add(o);
        size++;

        notifyAll();
    }

    /**
     * Remove the first object waiting in the buffer or wait until there is one.
     *
     * @return The first object in the buffer.
     * @throws InterruptedException
     */
    final private synchronized T consume() throws InterruptedException {
        while (size == 0 && !endOfStream)
            wait();
        if (size == 0 && endOfStream)
            throw new NoSuchElementException();
        size--;
        notifyAll();
        if (logger.isDebugEnabled())
            logger.debug(String.format("Consuming an item (stack size is %d)",
                size));
        return stack.remove(0);
    }

    final private synchronized void check() throws InterruptedException {
        while (size == 0 && !endOfStream) {
            wait();
        }
        notifyAll();
    }

    public void close() {
        logger.debug("Closing buffered producer");
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#hasNext()
     */
    final public boolean hasNext() {
        try {
            check();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(
                "Thread was interrupted while next() was called", e);
        }
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
        try {
            return consume();
        }
        catch (InterruptedException e) {
            if (logger.isDebugEnabled()) {
                LoggerPrintStream out = new LoggerPrintStream(logger,
                    Level.DEBUG);
                out
                    .print("Sending a Interrupted exception to the next() caller");
                e.printStackTrace(out);
            }
            throw new RuntimeException(
                "Thread was interrupted while next() was called", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new RuntimeException("Non mutable iterator");
    }

    /**
     * Notify the end of the stream
     */
    final synchronized private void markEndOfStream() {
        endOfStream = true;
        notifyAll();
    }

    /**
     * The object is ready to produce
     */
    final synchronized protected void readyToProduce() {
        wait = false;
        notifyAll();
    }

    final public void run() {
        // Wait
        try {
            synchronized (this) {
                if (wait)
                    logger.debug("Waiting that the object be initialized");
                while (wait)
                    wait();
            }

            // Produce
            logger.debug("Starting production of " + this);
            produce();
            logger.debug("End of production of " + this);
        }
        catch (InterruptedException e) {
            logger.debug("Interruption: end of production");
            // No need to interrupt ourselves since we are finishing now
            thread = null;
        }

        // Mark the end of the stream
        markEndOfStream();
    }

    /**
     * The produce method is the only one that <b>must</b> be overwritten by the producer
     *
     * @throws InterruptedException If the thread is interrupted
     */
    protected abstract void produce() throws InterruptedException;

    /**
     * Test
     *
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        BufferedProducer<Integer> producer = new BufferedProducer<Integer>() {
            public void produce() throws InterruptedException {
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

package bpiwowar.pipe;

import bpiwowar.io.LoggerPrintStream;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An abstract buffered consumer, for parallelising. The object "produce" method can be called by an external thread
 *
 * @param <T> The type of the produced objects
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
abstract public class BufferedConsumer<T> implements Runnable {
    final static private Logger logger = Logger
        .getLogger(BufferedConsumer.class);

    /**
     * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
     */
    @SuppressWarnings("serial")
    static public class NoSuchElementException extends RuntimeException {
    }

    /** Holds the last file */
    private List<T> stack = new ArrayList<T>();

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

    public BufferedConsumer() {
        this(10, false);
    }

    public BufferedConsumer(int bufferSize, boolean wait) {
        this.bufferSize = bufferSize;
        thread = new Thread(this);
        this.wait = wait;
        thread.start();
    }

    public BufferedConsumer(boolean wait) {
        this(10, wait);
    }

    public int getBufferSize() {
        return bufferSize;
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
     * @throws EOFException
     */
//	public synchronized void put(Pair<Integer, List<DoubleMatrix1D>> pair) throws InterruptedException, EOFException {
    public synchronized void put(T pair)
        throws InterruptedException, EOFException {
        if (endOfStream)
            throw new EOFException("Stream was closed");

        while (size > bufferSize) {
            wait();
        }
        stack.add(pair);
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

    final protected boolean hasNext() {
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

    protected T next() {
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
     * Notify the end of the stream. Note that it waits until the buffer is empty before stopping.
     */
    final synchronized public void close() {
        endOfStream = true;
        notifyAll();
    }

    /**
     * The object is ready to produce
     */
    final synchronized protected void readyToConsume() {
        wait = false;
        notifyAll();
    }

    public void run() {
        // Wait
        try {
            synchronized (this) {
                if (wait)
                    logger.debug("Waiting that the object be initialized");
                while (wait)
                    wait();
            }
        }
        catch (InterruptedException e) {
            logger.debug("Interruption: end of production");
            // No need to interrupt ourselves since we are finishing now
            thread = null;
        }

    }

    /**
     * Test
     *
     * @throws InterruptedException
     * @throws EOFException
     */
    public static void main(String[] args) throws InterruptedException,
        EOFException {
        BufferedConsumer<Integer> producer = new BufferedConsumer<Integer>() {
            @Override
            public void run() {
                super.run();
                while (hasNext()) {
                    Integer value = next();
                    System.out.format("Read %d%n", value);
                }
                System.out.println("Finished");
            }
        };

        for (int i = 0; i < 1000; i++) {
            try {
                if (i % 25 == 0)
                    Thread.sleep(20);
            }
            catch (InterruptedException e) {
            }
            producer.put(i);
            System.out.format("Produced %d%n", i);
        }
        producer.close();
        producer.put(1001);

    }

}

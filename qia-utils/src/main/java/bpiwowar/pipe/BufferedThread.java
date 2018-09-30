package bpiwowar.pipe;

import bpiwowar.io.LoggerPrintStream;
import bpiwowar.utils.SlidingWindow;
import java.io.EOFException;
import java.util.NoSuchElementException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * An abstract buffered consumer or producer
 *
 * @param <T> The type of the produced objects
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
abstract class BufferedThread<T> implements Runnable, CloseableIterator<T> {
    final static private Logger logger = Logger
        .getLogger(BufferedThread.class);

    /** Holds the last file */
    private SlidingWindow<T> window;

    /** Position within the sliding window */
    volatile private int position = 0;

    /**
     * Should we wait for the object to be initialized?
     */
    protected volatile boolean wait;

    /** EOS end of stream */
    private boolean endOfStream = false;

    private Thread thread;

    public BufferedThread() {
        this(10, false);
    }

    @SuppressWarnings("unchecked")
    public BufferedThread(int bufferSize, boolean wait) {
        this.window = new SlidingWindow<T>((T[])new Object[bufferSize]);
        thread = new Thread(this);
        this.wait = wait;
        thread.start();
    }

    public BufferedThread(boolean wait) {
        this(10, wait);
    }

    public int getBufferSize() {
        return window.getCapacity();
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
    public synchronized void put(T o) throws InterruptedException, EOFException {
        if (endOfStream)
            throw new EOFException("Stream was closed");

        // Wait until we have room
        while (window.getSize() - position >= window.getCapacity()) {
            wait();
        }

        window.add(o);
        notifyAll();
    }

    /**
     * Remove the first object waiting in the buffer or wait until there is one.
     *
     * @return The first object in the buffer.
     * @throws InterruptedException
     * @throws EOFException If the stream was EOF before
     */
    final private synchronized T consume() throws InterruptedException {
        while (window.getSize() == position && !endOfStream)
            wait();
        if (window.getSize() == position && endOfStream)
            throw new NoSuchElementException();

        // Get the element and notify so that producer can fill up the window
        // if they were locked
        T o = window.get(position);
        position++;
        notifyAll();

        if (logger.isDebugEnabled())
            logger.debug(String.format("Consuming an item (stack size is %d)",
                window.getSize() - position));
        return o;
    }

    final private synchronized void check() throws InterruptedException {
        while (window.getSize() == position && !endOfStream) {
            wait();
        }
        notifyAll();
    }

    public final boolean hasNext() {
        try {
            check();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(
                "Thread was interrupted while next() was called", e);
        }
        if (window.getSize() == position && endOfStream)
            return false;
        return true;
    }

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
    final synchronized public void close() {
        endOfStream = true;
        notifyAll();
    }

    /**
     * The object is ready to produce or consume
     */
    final synchronized protected void ready() {
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

}

package bpiwowar.mt;

import bpiwowar.log.Logger;
import org.apache.log4j.Level;

/**
 * Useful to wait until some threads have finished
 *
 * @author bpiwowar
 */
public class ThreadCount {
    final static Logger logger = Logger.getLogger();
    volatile int counter;

    public synchronized void add() {
        counter++;
    }

    public synchronized void del() {
        counter--;
        notify();
    }

    public synchronized int getCount() {
        return counter;
    }

    /**
     * Wait until the count is zero
     */
    public void resume() {
        resume(0);
    }

    /**
     * Wait until the count is less than a given value
     */
    public void resume(int n) {
        while (getCount() > n)
            try {
                synchronized (this) {
                    wait();
                }
            }
            catch (IllegalMonitorStateException e) {
                logger.warn("Illegal monitor exception while sleeping (SHOULD NOT HAPPEN)", e);
            }
            catch (Exception e) {
                logger.debug("Interrupted while sleeping: %s", e.toString());
                if (logger.isDebugEnabled()) {
                    logger.printException(Level.DEBUG, e);
                }
            }
    }
}

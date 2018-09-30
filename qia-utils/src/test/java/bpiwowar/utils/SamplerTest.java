package bpiwowar.utils;

import bpiwowar.lang.EndOfStreamException;
import bpiwowar.log.Logger;
import cern.jet.random.engine.MersenneTwister;
import org.junit.Test;

public class SamplerTest {
    final static private Logger logger = Logger.getLogger();

    @Test
    public void simpleTests() {
        check(5, 50);
        check(49, 50);
        check(1000, 1000);
        check(1000, 1001);
    }

    /**
     * @param n
     * @param size
     */
    private void check(int n, int size) {
        Sampler sampler = new Sampler(n, size, new MersenneTwister());
        long j = -1;
        for (int i = 0; i < n; i++) {
            final long next = sampler.next();
            logger.debug("Next sample: %d (%d over %d)", next, n, size);
            assert next > j : String.format("Expected %d to be > to %d", next, j);
            assert next >= 0 && next < size;
            j = next;
            logger.debug("Sampled %d (sample no %d / %d - %d)%n", next, i, n, size);
        }

        try {
            long b = sampler.next();
            assert false : String.format("Expected end of stream but got a new integer %d (%d/%d)", b, n, size);
        }
        catch (EndOfStreamException e) {
        }
    }
}

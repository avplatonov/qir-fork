package bpiwowar.utils;

import bpiwowar.lang.EndOfStreamException;
import bpiwowar.random.RandomSampler;
import cern.jet.random.engine.RandomEngine;

/**
 * Sample
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Sampler {
    final long[] buffer = new long[100];
    final private RandomSampler sampler;

    int i = buffer.length;
    long toSample;

    /**
     * Creates a new sampler
     *
     * @param n The number of samples
     * @param size The (excluded) maximum interval boundary
     * @param randomGenerator The random generator
     */
    public Sampler(long n, long size, RandomEngine randomGenerator) {
        this.toSample = n;

        sampler = new RandomSampler(n, size, 0, randomGenerator);
    }

    public boolean hasNext() {
        return toSample > 0;
    }

    /**
     * Returns the next integer
     *
     * @return The next integer
     */
    public long next() {
        if (toSample == 0)
            throw new EndOfStreamException("EOS exception: cannot sample more");

        // Read more if needed
        if (i == buffer.length) {
            sampler.nextBlock((int)Math.min(buffer.length, toSample), buffer,
                0);
            i = 0;
        }

        toSample--;
        return buffer[i++];
    }

}

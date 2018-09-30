package bpiwowar.maths;

import static java.lang.Math.sqrt;

/**
 * Incremental version of computing mean and standard deviation
 *
 * Follows Knuth for numerically stable iterative computation of variance and mean
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class IncrementalMeanAndVariance {
    int n = 0;
    double mean = 0;
    double nvar = 0;

    public final void addSample(final double x) {
        n++;

        // Update mean
        final double oldmean = mean;
        mean += 1. / (double)n * (x - mean);
        nvar += (x - mean) * (x - oldmean);
    }

    /**
     * Add a number of samples with the value 0
     *
     * @param nbZeros
     */
    public final void addZeros(final int nbZeros) {
        if (nbZeros == 0)
            return;
        final double ratio = n / (double)(n + nbZeros);
        nvar += mean * mean * ((double)nbZeros * ratio);
        mean *= ratio;
        n += nbZeros;
    }

    public final int getCount() {
        return n;
    }

    public final double getMean() {
        return mean;
    }

    public final double getStandardDeviation() {
        return sqrt(nvar / (double)n);
    }
}

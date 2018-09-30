package bpiwowar.utils;

/**
 * Useful to take track of the number of infinites!
 *
 * @author bpiwowar
 */
public class InfiniteSum {
    double sum = 0;
    int infiniteCount = 0;

    public void add(final double x) {
        if (x == Double.POSITIVE_INFINITY)
            infiniteCount++;
    }
}

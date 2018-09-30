package bpiwowar.utils.maths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;

/**
 * Utility class to compute log SUM exp(log(x))
 *
 * usually this correspond to P(x_1) + ... + P(x_n) computed with logs.
 *
 * <p>
 * Takes care of approximation
 * </p>
 *
 * <p>
 * Idea: sum log(exp(sum(x_i))) = m + log(sum e(log(x)-m)) where m evolves dynamically with x
 * </p>
 *
 * Does not work with a very long sum of small values preceded by a big one, e.g. 1 + sum(i=1 to 1e500) 1e-500 (should
 * give 2, but will give 1 as a result)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class LogSumExpLog {
    final static double ERROR_THRESHOLD = 1.1;

    final static double LOG_ERROR_THRESHOLD = log(ERROR_THRESHOLD);

    double current;

    double min, max;

    public LogSumExpLog() {
        reset();
    }

    /**
     * Add a log value to the sum
     *
     * @param x The log value to add
     */
    public void addLogValue(final double x) {
        if (x != Double.NEGATIVE_INFINITY) {

            // First time
            if (min == Double.NEGATIVE_INFINITY) {
                current = 1.;
                min = x;
                return;
            }

            final double m = max(x, min + log(current)) - (Double.SIZE - 1);
            current = current * exp(min - m) + exp(x - m);
            min = m;
        }
    }

    public double getLogSum() {
        return min + log(current);
    }

    public double getNormalizedLogSum() {
        return Math.normaliseLog(min + log(current));
    }

    public void reset() {
        current = 1;
        min = Double.NEGATIVE_INFINITY;
    }

    static public void main(final String[] args) throws NumberFormatException,
        IOException {
        final LogSumExpLog s = new LogSumExpLog();
        String line;
        System.err.println(s.getLogSum());
        final BufferedReader in = new BufferedReader(new InputStreamReader(
            System.in));
        while ((line = in.readLine()) != null) {
            s.addLogValue(Double.parseDouble(line));
            System.err.println(s.getLogSum() + " -> " + exp(s.getLogSum())
                + " / " + s.min + " and " + s.current);
        }
    }

    /**
     * @param logValues values to add
     */
    public static double add(double... logValues) {
        final LogSumExpLog s = new LogSumExpLog();
        for (double x : logValues)
            s.addLogValue(x);
        return s.getLogSum();
    }
}

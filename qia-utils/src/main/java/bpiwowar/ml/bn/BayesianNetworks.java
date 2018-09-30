package bpiwowar.ml.bn;

import bpiwowar.ml.OutOfBoundsException;
import java.util.List;

public class BayesianNetworks {
    /** Minimum probability */
    static final double MINPROBA = 1e-30;
    static final double MAXPROBA = 1. - MINPROBA;
    static final double ERROR_THRESHOLD = 1.1;

    public static final double MINLOGPROBA = Math.log(MINPROBA);
    static final double MAXLOGPROBA = Math.log(MAXPROBA);
    static final double LOG_ERROR_THRESHOLD = Math.log(ERROR_THRESHOLD);

    /** A function that gives the partial for a variable */
    abstract static class ErrorFunction {
        /** Do we need parent value in order to compute the error? */
        abstract boolean needs_parents();

        protected static double sqr(double x) {
            return x * x;
        }

        abstract double get_partial(int e, int[] e_p, final AbstractVariable v, final List<Double> log_output,
            final double[] wanted_output);

        abstract double get_value(int e, final int[] e_p, final AbstractVariable v, final List<Double> log_output,
            final double[] wanted_output);
    }

    public static final int noEvidence = -1;
    static final int stochasticEvidence = -2;

    /**
     * Returns the log of a normalized probability, that is a value which is between log(MINLOGPROBA) and
     * log(MAXLOGPROBA)
     *
     * @throws OutOfBoundsException if the log probability is too much out of bound
     */
    public static double normaliseProbability(double p) {
        if (!(p < ERROR_THRESHOLD))
            throw new OutOfBoundsException(p + " is too high for a probability, even with errors");
        if (p < MINPROBA)
            return MINPROBA;
        else if (p > MAXPROBA)
            return MAXPROBA;
        return p;
    }

    /**
     * Returns the log of a normalized probability, that is a value which is between log(MINLOGPROBA) and
     * log(MAXLOGPROBA)
     *
     * @throws OutOfBoundsException if the log probability is too much out of bound
     */
    public static double logNormalize(double p) {
        if (!(p < ERROR_THRESHOLD))
            throw new OutOfBoundsException(p + " is too high for a probability, even with errors");
        if (p < MINPROBA)
            return MINLOGPROBA;
        else if (p > MAXPROBA)
            return MAXLOGPROBA;
        return Math.log(p);
    }

    /**
     * Returns the log of a normalized probability, that is a value which is between log(MINLOGPROBA) and
     * log(MAXLOGPROBA)
     */
    public static double normalizeLogProbability(double logP) {
        if (!(logP < LOG_ERROR_THRESHOLD))
            throw new OutOfBoundsException(logP + " is too high for a log probability, even with errors");
        if (logP < MINLOGPROBA)
            return MINLOGPROBA;
        if (logP > MAXLOGPROBA)
            return MAXLOGPROBA;
        return logP;
    }

}


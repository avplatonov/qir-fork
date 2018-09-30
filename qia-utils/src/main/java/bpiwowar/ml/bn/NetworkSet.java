package bpiwowar.ml.bn;

import bpiwowar.ml.Likelihoods;
import java.io.PrintStream;

import static java.lang.Math.exp;

/**
 * Represents a set of bayesian networks (used by EM or any other learning algorithm) and the set of functions
 */

public abstract class NetworkSet {
    /** Our bayesian functions */
    Functions functions;

    /** Print likelihood statistics */
    void printLikelihoodStatistics(PrintStream out) {
        double llh = 0;
        int nb = 0;
        double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;

        reset();
        while (hasNext()) {
            Network bn = next();
            double a = bn.getLogLikelihood();
            if (a < min)
                min = a;
            if (a > max)
                max = a;
            nb++;
            llh += a;
        }

        out.print("Vraissemblance = " + exp(llh) + " (" + llh + ")"
            + ", Max = " + exp(max)
            + ", Min = " + exp(min)
            + ", Mean = " + exp(llh / nb) + "\n");
    }

    /** Set the functions */
    void set_functions(final Functions functions) {
        this.functions = functions;
    }

    /** Get the functions */
    final Functions get_functions() {
        return functions;
    }

    /** Reset the iteration on the network set */
    abstract public void reset();

    /** Returns true if we have a next element */
    abstract public boolean hasNext();

    /** Get the next */
    abstract public Network next();

    /** Invalidate all the networks */
    public void invalidate() {
        reset();
        while (hasNext()) {
            next().invalidate();
        }
    }

    /**
     * Computes statistics on the networks
     */
    public Likelihoods getStatistics() {
        Likelihoods s = new Likelihoods();
        reset();
        while (hasNext()) {
            Network network = next();
            s.add(network);
        }

        return s;
    }

}

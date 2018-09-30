package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import org.apache.log4j.Logger;

import static java.lang.Math.log;

/**
 * A set of Poisson distributions (must have a single parent)
 *
 * <b>Warning</b>: can only be used when the associated variable is observed
 *
 * @author bpiwowar
 */
public class CPPoissons extends CPAbstract {
    final static Logger logger = Logger.getLogger(CPPoissons.class);

    int nbParentStates;

    public CPPoissons(String name, int nbParentStates) {
        // there are # parents parameters (one per Poisson)
        super(name, nbParentStates);
        this.nbParentStates = nbParentStates;
        Random random = new Random();
        for (int i = 0; i < nbParentStates; i++) {
            parameters[i] = 0.5 + random.nextFloat() * 10.;
        }
    }

    static final private double logfactorial(int k) {
        // Srinivasa Ramanujan approximation
        // \log n! \approx n\log n - n + \frac {\log(n(1+4n(1+2n)))} {6} + \frac
        // {\log(\pi)} {2}
        if (k == 0)
            return 0;
        if (k <= 2)
            return log(k);
        return k * log(k) - k + log(k * (1. + 4. * k * (1. + 2. * k))) / 6.
            + log(Math.PI) / 2.;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#update_partials(yrla.ml.bn.AbstractVariable,
     *      int, int[], double, double)
     */
    @Override
    protected void update_partials(boolean analytical, AbstractVariable bv,
        int s, int[] pc, double pcond, double weight) {
        assert pc.length == 1;
        assert partials != null;
        if (analytical) {
            if (logger.isDebugEnabled())
                logger
                    .debug(String
                        .format(
                            "Poisson update (%d) for %s: %.3f for %d => %.3f for %.3f%n",
                            s, this, pcond, pc[0],
                            (partials[pc[0]] + weight * pcond * s),
                            partials[pc[0] + nbParentStates]
                                + weight * pcond));
            partials[pc[0]] += weight * pcond * s;
            partials[pc[0] + nbParentStates] += weight * pcond;
        }
        else {
            final double lambda = parameters[pc[0]];
            partials[pc[0]] += weight * pcond * (s / lambda - 1.);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#doAnalyticalResolve(int, double[])
     */
    @Override
    protected void doAnalyticalResolve(int j, double[] theta) {
        for (int i = 0; i < nbParentStates; i++) {
            final double d = partials[i + nbParentStates];
            if (d > 0)
                theta[j + i] = partials[i] / d;
            else
                theta[j + i] = 1;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#log_probability(yrla.ml.bn.AbstractVariable,
     *      int, int[])
     */
    @Override
    public double log_probability(AbstractVariable variable, int state, int[] pc) {
        assert pc.length == 1;
        double lambda = parameters[pc[0]];
        if (lambda <= 1e-30)
            return state == 0 ? BayesianNetworks.MAXLOGPROBA
                : BayesianNetworks.MINLOGPROBA;
        // System.err.format("Pss(%d; l[%d]=%f)= %f%n", state, pc[0], lambda,
        // state * log(lambda) - lambda - logfactorial(state));
        return state * log(lambda) - lambda - logfactorial(state);
    }

    @Override
    public void initPartials(boolean analytical) {
        if (analytical) {
            partials = new double[parameters.length * 2];
        }
        else
            super.initPartials(analytical);
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#print(java.io.PrintStream, double[])
     */
    @Override
    public void print(PrintStream out, double[] theta) {
        out.println(name + ", poisson dists. of parameters "
            + Arrays.toString(theta));
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#verify(yrla.ml.bn.AbstractVariable)
     */
    @Override
    public void verify(AbstractVariable variable) throws IntegrityException {
        if (variable.getNumberOfParents() != 1
            || variable.getParent(0).getNumberOfStates() != parameters.length)
            throw new IntegrityException();
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#write(java.io.ObjectOutputStream)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(nbParentStates);
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#read(java.io.ObjectInputStream)
     */
    @Override
    public void read(DataInput in) throws IOException {
        super.read(in);
        nbParentStates = in.readInt();
    }
}

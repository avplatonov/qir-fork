package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import bpiwowar.utils.maths.LogSumExpLog;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import org.apache.log4j.Logger;

import static bpiwowar.ml.bn.BayesianNetworks.noEvidence;

/**
 * A special CP for a to simulate a more complex network
 * <code>P -> (c1 -> c2 -> c3 -> ...)</code> we use
 * <code>P -> c1 -> c2 -> c3 -> ...</code> where the c variables (from c2 to
 * cn) have #c1 states * #p states
 *
 * This CP is for the variable c1 Let A be the number of parent P states, B be the number of true states of variable c.
 * We have the properties:
 * <li> P(v = k| pa = n) = 0 if k not in [trunc(n/A)*A, (1+trunc(n/A))*A-1].
 * This implies we store A tables of size B^2
 * <li>
 *
 * parameters are indexed by A
 *
 * @author bpiwowar
 */
public class CPFirstVirtualParent extends CPAbstract {
    final static Logger logger = Logger.getLogger(CPFirstVirtualParent.class);

    /**
     * Number of parent states
     */
    private int nbParentStates;

    /**
     * Number of real states (multiplied by number of parent states to get the actual variable number of states)
     */
    private int nbRStates;

    // Default initialisations
    {
        is_mutable = true;
    }

    /**
     * Initialise the function.
     *
     * The number of states of the variable must be the number of shared states of the "true" variable times the number
     * of states of the parent.
     *
     * @param name Name of the variable
     * @param nbRStates Number of (real) states of the variable
     * @param nbParentStates Number of states of the (shared) parent variable
     */
    public CPFirstVirtualParent(final String name, final int nbRStates,
        final int nbParentStates) {
        super(name, nbRStates * nbParentStates);
        this.nbRStates = nbRStates;
        this.nbParentStates = nbParentStates;

        // --- Random initialisation ---
        // Loop over shared parent, and then parent state
        Random r = new Random();
        int k = 0;
        for (int e_p = 0; e_p < nbParentStates; e_p++) {
            double s = 0;
            for (int e = 0; e < nbRStates; e++) {
                final double d = r.nextDouble();
                parameters[k + e] = d;
                s += d;
            }
            for (int e = 0; e < nbRStates; e++)
                parameters[k + e] = BayesianNetworks.logNormalize(parameters[e
                    + k]
                    / s);

            k += nbRStates;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#setParameters(double[])
     */
    @Override
    public void setParameters(double[] ds) {
        super.setParameters(ds);
        int k = 0;
        for (int e_p = 0; e_p < nbParentStates; e_p++) {
            LogSumExpLog s = new LogSumExpLog();
            for (int e = 0; e < nbRStates; e++)
                s.addLogValue(parameters[k + e]);
            final double x = Math.exp(s.getNormalizedLogSum());
            if (Math.abs(1 - x) > 1e-5)
                throw new IllegalArgumentException(
                    "Marginalisation is too different from 1 (" + x + ")");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.Function#log_probability(yrla.ml.bn.Variable, int, int[])
     */
    @Override
    public double log_probability(AbstractVariable variable, int state,
        final int[] pc) {

        // "State" of the shared parent
        int position = getPosition(variable, state, pc);
        if (position < 0)
            return BayesianNetworks.MINLOGPROBA;
        return parameters[position];
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.Function#analytical_resolve(double[])
     */
    @Override
    protected void doAnalyticalResolve(int d, double[] theta) {
        if (partials == null)
            throw new IntegrityException("Partials are null");

        int k = 0;
        for (int e_p = 0; e_p < nbParentStates; e_p++) {
            double s = 0;
            for (int e = 0; e < nbRStates; e++) {
                s += partials[d + k + e];
            }

            if (s > 0)
                for (int e = 0; e < nbRStates; e++)
                    parameters[d + k + e] = BayesianNetworks
                        .logNormalize(partials[d + e + k] / s);
            else {
                final double lp = BayesianNetworks.logNormalize(1. / nbRStates);
                for (int e = 0; e < nbRStates; e++) {
                    parameters[d + k + e] = lp;
                }
            }
            k += nbRStates;
        }

        // We just normalize for the different states of the variable
        partials = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#update_partials(yrla.ml.bn.Variable, int,
     *      int[], double)
     */
    @Override
    protected void update_partials(boolean analytical, AbstractVariable bv, int evidence,
        int[] pc, double pcond, double weight) {
        final int position = getPosition(bv, evidence, pc);
        // Just add the observation
        if (position >= 0) {
            if (logger.isDebugEnabled())
                logger.debug(String.format("[CPFVP] Updating %s (s=%d / p=%s) [%d] with %.4f%n",
                    this, evidence, pc[0], position, pcond));
            partials[position] += weight * pcond;
        }
        else {
            System.err.println("WAARRRRRRRRRRNING: invalid position");
        }
    }

    /**
     * Returns the position of the variable
     *
     * @param variable
     * @param state
     * @param pc
     * @return -1 if it is a grammar forbidden position
     */
    final public int getPosition(final AbstractVariable variable, int state,
        int[] pc) {
        final int s_sp = pc[0];

        // A valid state must be between
        // s_sp * nbStates and (s_sp + 1) * nbStates - 1
        // i.e. state - s_sp * nbStates must be between 0 and nbStates - 1
        final int ts = state - s_sp * nbRStates;
        if (ts < 0 || ts >= nbRStates)
            return -1;

        return state;
    }

    @Override
    public void print(PrintStream out, final double[] theta) {
        out.format("Virtual parent (first) CP (%s) with %d/%d%n", name,
            nbParentStates, nbRStates);
        int k = 0;
        for (int e_p = 0; e_p < nbParentStates; e_p++) {
            out.format("P(v | spa=%d) = {", e_p);
            for (int e = 0; e < nbRStates; e++) {
                if (e > 0)
                    out.print(", ");
                out.print(Math.exp(parameters[k + e]));
            }
            out.println("}");
            k += nbRStates;
        }
    }

    public ParentConfiguration getParentConfigurations(
        final AbstractVariable v, int e, final boolean noParentConfiguration) {
        // We use the standard method if there is no evidence
        if (e == noEvidence)
            return super.getParentConfigurations(v, e, noParentConfiguration);

        // --- We have evidence: use it!

        // State of the virtual parent
        final int s_sp = e / nbRStates;
        final AbstractVariable p = v.getParent(0);

        return new ParentConfiguration() {
            {
                // Initialise
                current = new int[] {s_sp};
                if (p.has_evidence()) {
                    if (p.get_evidence() != current[0])
                        current = null;
                }
            }

            /*
             * (non-Javadoc)
             *
             * @see yrla.ml.bn.ParentConfiguration#next()
             */
            public void next() {
                if (current == null)
                    throw new NoSuchElementException("");
                current = null;
            }

        };
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#verify(yrla.ml.bn.AbstractVariable)
     */
    @Override
    public void verify(AbstractVariable variable) throws IntegrityException {
        if (variable.getNumberOfStates() != nbRStates * nbParentStates)
            throw new IntegrityException("The number of states (%d) is different from the " +
                "expected number of states (%d * %d = %d)", variable.getNumberOfStates(),
                nbRStates, nbParentStates, nbRStates * nbParentStates);
    }

    /**
     * Get the evidences compatible with ones
     *
     * @param pc
     * @return
     */
    @Override
    public Iterable<Integer> getEvidences(final AbstractVariable v,
        final int[] pc) {
        final int s_sp = pc[0];
        if (s_sp == -1)
            return super.getEvidences(v, pc);

        final int max = (1 + s_sp) * nbRStates;

        return new Iterable<Integer>() {
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int evidence;

                    {
                        if (v.has_evidence()) {
                            evidence = v.get_evidence();
                            if (evidence >= max || evidence < (max - nbRStates))
                                evidence = noEvidence;
                        }
                        else
                            evidence = s_sp * nbRStates;
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    public Integer next() {
                        final int e = evidence;
                        evidence = v.getNext(evidence);
                        if (evidence >= max)
                            evidence = noEvidence;
                        return e;
                    }

                    public boolean hasNext() {
                        return evidence != noEvidence;
                    }

                };
            }

        };
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#write(java.io.ObjectOutputStream)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(nbParentStates);
        out.writeInt(nbRStates);
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#read(java.io.ObjectInputStream)
     */
    @Override
    public void read(DataInput in) throws IOException {
        super.read(in);
        nbParentStates = in.readInt();
        nbRStates = in.readInt();
    }

}

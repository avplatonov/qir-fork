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
 * Let A be the number of parent P states, B be the number of true states of variable c We have the properties:
 * <li> (v = k| pa = n) = 0 if k not in [trunc(n/A)*A, (1+trunc(n/A))*A-1]. This
 * implies we store A tables of size B^2
 * <li>
 *
 * parameters are indexed by A (B*C parameters for each distinct value of A)
 *
 * @author bpiwowar
 */
public class CPVirtualParent extends CPAbstract {
    final static Logger logger = Logger.getLogger(CPFirstVirtualParent.class);

    /**
     * Real number of states of our parent
     */
    private int nbRParentStates;

    /**
     * Real number of states of the variable
     */
    private int nbRStates;

    /**
     * Number of states of the shared parent
     */
    private int nbSParentStates;

    // Default initialisations
    {
        is_mutable = true;
    }

    /**
     * Initialise the function.
     *
     * @param name Name of the variable
     * @param nbRStates Number of (true) states of the variable
     * @param nbRParentStates Number of (true) states of the parent variable. The real number of states must be this
     * number multipled by the number of states of the shared parent.
     * @param nbSParentStates Number of states of the shared parent variable
     */
    public CPVirtualParent(final String name, final int nbRStates,
        final int nbRParentStates, final int nbSParentStates) {
        super(name, nbRStates * nbRParentStates * nbSParentStates);
        this.nbRStates = nbRStates;
        this.nbSParentStates = nbSParentStates;
        this.nbRParentStates = nbRParentStates;

        // --- Random initialisation ---
        // Loop over shared parent, and then parent state
        Random r = new Random();
        int k = 0;
        for (int a = 0; a < nbSParentStates; a++) {
            for (int e_p = 0; e_p < nbRParentStates; e_p++) {
                double s = 0;
                for (int e = 0; e < nbRStates; e++) {
                    final double d = r.nextDouble();
                    parameters[k + e] = d;
                    s += d;
                }
                for (int e = 0; e < nbRStates; e++)
                    parameters[k + e] = BayesianNetworks
                        .logNormalize(parameters[e + k] / s);

                k += nbRStates;
            }
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
        for (int a = 0; a < nbSParentStates; a++) {
            for (int e_p = 0; e_p < nbRParentStates; e_p++) {
                LogSumExpLog s = new LogSumExpLog();
                for (int e = 0; e < nbRStates; e++)
                    s.addLogValue(parameters[k + e]);
                final double x = Math.exp(s.getNormalizedLogSum());
                if (Math.abs(1 - x) > 1e-5)
                    throw new IllegalArgumentException(
                        "Marginalisation is too different from 1 (" + x
                            + ")");
            }
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
        for (int a = 0; a < nbSParentStates; a++) {
            for (int e_p = 0; e_p < nbRParentStates; e_p++) {
                double s = 0;

                for (int e = 0; e < nbRStates; e++)
                    s += partials[d + k + e];

                if (s > 0)
                    for (int e = 0; e < nbRStates; e++)
                        theta[d + k + e] = BayesianNetworks
                            .logNormalize(partials[d + e + k] / s);
                else {
                    final double lp = BayesianNetworks
                        .logNormalize(1. / nbRStates);
                    for (int e = 0; e < nbRStates; e++)
                        theta[d + k + e] = lp;
                    k += nbRStates;
                }
            }
        }

        // We just normalize for the different states of the variable
        partials = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#update_partials(boolean, yrla.ml.bn.Variable,
     *      int, int[], double)
     */
    @Override
    protected void update_partials(boolean analytical, AbstractVariable bv,
        int evidence, int[] pc, double pcond, double weight) {
        final int position = getPosition(bv, evidence, pc);
        // Just add the observation
        if (position >= 0) {
            if (logger.isDebugEnabled())
                logger
                    .debug(String
                        .format(
                            "[CPFVP] Updating %s (s=%d / [%d] s=%s, p=%s) [%d] with %.4f%n",
                            this, evidence, pc[0], pc[0]
                                / nbRParentStates, pc[0]
                                % nbRParentStates, position,
                            pcond));
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
    final int getPosition(final AbstractVariable variable, int state, int[] pc) {
        // Shared parent state
        final int s_sp = pc[0] / nbRParentStates;
        // Parent state
        int s_p = pc[0] % nbRParentStates;

        // A valid state must be between
        // s_sp * nbStates and (s_sp + 1) * nbStates - 1
        // i.e. state - s_sp * nbStates must be between 0 and nbStates - 1
        final int ts = state - s_sp * nbRStates;
        if (ts < 0 || ts >= nbRStates)
            return -1;

        return s_sp * (nbRParentStates * nbRStates) + s_p * nbRStates + ts;
    }

    @Override
    public void print(PrintStream out, final double[] theta) {
        out.println("Table CP (" + name + ")");
        int k = 0;
        for (int a = 0; a < nbSParentStates; a++) {
            for (int e_p = 0; e_p < nbRParentStates; e_p++) {
                out.format("P(v | spa=%d, pa=%d) = ", a, e_p);
                for (int e = 0; e < nbRStates; e++) {
                    if (e > 0)
                        out.print(", ");
                    out.print(Math.exp(parameters[k + e]));
                }
                out.println();
                k += nbRStates;
            }
        }
    }

    public ParentConfiguration getParentConfigurations(
        final AbstractVariable v, final int e,
        final boolean noParentConfiguration) {
        // We use the standard method if there is no evidence
        if (e == noEvidence && !v.has_evidence())
            return super.getParentConfigurations(v, e, noParentConfiguration);

        // --- We have evidence: use it!

        // State of the shared parent
        final int s_sp = e / nbRStates;
        // min inclusive
        final int min = s_sp * nbRParentStates;
        // max exclusive

        return new ParentConfiguration() {
            final int max;

            {
                // Initialise
                current = new int[1];
                final AbstractVariable p = v.getParent(0);
                if (p.has_evidence()) {
                    current[0] = max = p.get_evidence();
                    if (current[0] < min || current[0] >= max)
                        current = null;
                }
                else {
                    max = min + nbRParentStates;
                    current[0] = min;
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
                if (++current[0] >= max)
                    current = null;
            }

        };
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
        if (pc[0] == -1)
            return super.getEvidences(v, pc);

        // State of our shared parent
        final int s_sp = pc[0] / nbRParentStates;
        // Maximum for evidence
        final int max = (s_sp + 1) * nbRStates;

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

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#verify(yrla.ml.bn.AbstractVariable)
     */
    @Override
    public void verify(AbstractVariable variable) throws IntegrityException {
        if (variable.getNumberOfStates() != nbRStates * nbSParentStates)
            throw new IntegrityException(
                "The number of states (%d) is different from the "
                    + "expected number of states (%d * %d = %d)",
                variable.getNumberOfStates(), nbRStates, nbSParentStates,
                nbRStates * nbSParentStates);
        if (variable.getNumberOfParents() != 1)
            throw new IntegrityException("One and only one parent expected");
        if (variable.getParent(0).getNumberOfStates() != nbRParentStates
            * nbSParentStates)
            throw new IntegrityException();
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#write(java.io.ObjectOutputStream)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(nbRParentStates);
        out.writeInt(nbRStates);
        out.writeInt(nbSParentStates);
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#read(java.io.ObjectInputStream)
     */
    @Override
    public void read(DataInput in) throws IOException {
        super.read(in);
        nbRParentStates = in.readInt();
        nbRStates = in.readInt();
        nbSParentStates = in.readInt();
    }

}

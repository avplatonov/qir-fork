package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static bpiwowar.ml.bn.BayesianNetworks.noEvidence;

/**
 * This class descrives a bayesian function (P(d|...)).
 */
public abstract class CPAbstract {
    /** Name */
    protected String name = "";

    /**
     * A Parameter array used while computing partials This array can contain the true partials or the sufficient
     * statistics for analytical resolution
     */
    protected double[] partials = null;

    /** The parameters */
    protected double[] parameters;

    /** Do we want to learn function parameters ? */
    protected boolean is_mutable = true;

    public CPAbstract() {
    }

    public CPAbstract(final String name) {
        this.name = name;
    }

    public CPAbstract(int nbParameters) {
        if (nbParameters > 0)
            parameters = new double[nbParameters];
    }

    /**
     * @param parameters Initialization with given parameters
     */
    public CPAbstract(double[] parameters) {
        this.parameters = parameters;
    }

    /**
     * @param name2
     * @param i
     */
    public CPAbstract(String name, int nbParameters) {
        if (nbParameters > 0)
            parameters = new double[nbParameters];
        this.name = name;
    }

    /** The logit function */
    static double logit(double a) {
        return (1.0 / (1.0 + Math.exp(-a)));
    }

    /**
     * Returns constant a priori
     */
    protected double[] get_a_priori() {
        throw new RuntimeException("Not an a priori function (" + this + ")");
    }

    /**
     * This function returns P(value|parentsValue)
     *
     * @param v The variable for which the CP is computed
     * @param s The variable state
     * @param pc the configuration
     */
    abstract public double log_probability(final AbstractVariable v,
        int s, int[] pc);

    /**
     * Inference P(value,ancestor values). Compute this value from the current
     * setting and the probability function P(child|parents) of this node. By
     * default, this method computes the sum for all configurations, but it can
     * be redefined to optimize this computation (case of or, and, etc.
     * functions).
     *
     * @param p
     *            Where to put the results
     */
    // abstract void do_inference(final Variable variable, int []evidences,
    // double []p, double [][] parents);

    /** Print this function definition and values */
    public void print(PrintStream out, final double[] theta) {
        out.println("Unknown bayesian function !");
    }

    final public void print(PrintStream out) {
        print(out, parameters);
    }

    /** Set mutable parameter flag (are the parameters to be learned ?) */
    void setIsMutable(boolean f) {
        is_mutable = f;
    }

    /** Get the mutable parameter flag (can be overloaded to return always false) */
    boolean isMutable() {
        return is_mutable;
    }

    /**
     * Set the number of parameters. This function destroys the old parameters if they do exist
     *
     * @param n Number of parameters
     */
    protected void setNumberOfParameters(int n) {
        if (n == 0) {
            parameters = null;
        }
        else {
            parameters = new double[n];
        }
    }

    /**
     * @param ds
     */
    public void setParameters(final double[] ds) {
        if (ds.length != getNumberOfParameters())
            throw new IllegalArgumentException(
                "The number of function parameters (" + getNumberOfParameters()
                    + ") is not equal to length of the given parameters ("
                    + ds.length + ")");
        for (int i = 0; i < ds.length; i++)
            parameters[i] = ds[i];
    }

    /** Get the first parameter */
    public double[] getParameters() {
        return parameters;
    }

    /** Get the number of parameters */
    protected int getNumberOfParameters() {
        return parameters.length;
    }

    /** Initialisation of the parameters (default values) */
    void init_parameters() {
    }

    /**
     * Copy the parameters from an array
     *
     * @param j First indice to copy in the given array
     */
    void copy_parameters(int j, double[] newParameters) {
        for (int i = 0; i < parameters.length; i++)
            parameters[i] = newParameters[i + j];
    }

    /** ===== Learning methods ===== */

    /**
     * Initialisation of the partial derivatives
     *
     * Default is to create a partials array, one per parameter, and initialize it to zero.
     *
     * @param analytical This toggles the analytical mode
     */
    public void initPartials(boolean analytical) {
        if (partials != null) {
            throw new RuntimeException(
                "Partials are not null in initStochasticPartials()");
        }
        partials = new double[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            partials[i] = 0.;
    }

    /**
     * Update partials with current bayesian settings
     *
     * @param bv the bayesian variable
     * @param theta is the point where the partial is evaluated
     * @param pcond Probability \f$Q(v\wedge v_{pa}|v_E)\f$ with the current setting of \f$v\f$ and \f$v_{pa}\f$
     * @param weight The multiplicative weight for this example
     */
    protected void update_partials(boolean analytical, final AbstractVariable bv, int evidence,
        final int[] parentConfiguration, double pcond, double weight) {
        bf_not_implemented("updatePartials");
    }

    /**
     * Allow some post-processing and eventualy update a previous gradient. This function is called <i>once</i> at the
     * end of the computation of partials
     *
     * @param gradient The gradient vector to update
     * @param j The index to start with
     */
    void end_partials(double[] gradient, int j) {
        assert (partials != null);
        if (gradient != null)
            for (int i = 0; i < parameters.length; i++) {
                gradient[j + i] += partials[i];
            }
        partials = null;
    }

    /**
     * Handle analytical partials computation
     *
     * @param theta store the results in this array
     * @param j array shift for storage
     */
    protected void doAnalyticalResolve(int j, double[] theta) {
        bf_not_implemented("analytical_resolve");
    }

    final public void analyticalResolve(int j, double[] theta) {
        if (theta == null)
            doAnalyticalResolve(0, parameters);
        else
            doAnalyticalResolve(j, theta);
    }

    final private void bf_not_implemented(String s) {
        throw new RuntimeException(s
            + " is not implemented for this function (" + this + ")");
    }

    final public double log_probability(AbstractVariable variable, int e,
        ParentConfiguration pc) {
        return log_probability(variable, e, pc.current);
    }

    /**
     * Loop on variable configurations. Can be overrided to provide faster loops
     *
     * @param evidence
     * @param noParentConfiguration
     * @return
     */
    public ParentConfiguration getParentConfigurations(
        final AbstractVariable v, int e, final boolean noParentConfiguration) {
        return new ParentConfiguration() {
            {
                current = new int[v.getNumberOfParents()];
                if (current != null)
                    for (int i = 0; i < current.length; i++)
                        current[i] = v.getParent(i).initialize();
            }

            /*
             * (non-Javadoc)
             *
             * @see yrla.ml.bn.ParentConfiguration#next()
             */
            public void next() {
                if (current == null)
                    throw new NoSuchElementException("");

                // Increment
                int i = 0;
                for (; i < current.length; i++) {
                    current[i] = v.getParent(i).getNext(current[i]);
                    if (current[i] != noEvidence)
                        break;
                    current[i] = v.getParent(i).initialize();
                }
                if (i == current.length)
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
    public Iterable<Integer> getEvidences(final AbstractVariable v,
        final int[] pc) {
        return new Iterable<Integer>() {
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int evidence = v.initialize();

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    public Integer next() {
                        final int e = evidence;
                        evidence = v.getNext(evidence);
                        return e;
                    }

                    public boolean hasNext() {
                        return evidence != noEvidence;
                    }

                };
            }

        };
    }

    /**
     * @return the name
     */
    public String toString() {
        return name;
    }

    public void verify(AbstractVariable variable) throws IntegrityException {
    }

    /**
     * Update with precomputed values. Default behaviour is to add up.
     *
     * @param analytical True if analytical mode
     * @param offset The offset in the values array
     * @param values
     */
    public void updatePartials(boolean analytical, int offset, double[] values) {
        for (int j = 0; j < partials.length; j++)
            partials[j] += values[offset + j];
    }

    public void read(DataInput in) throws IOException {
        name = in.readUTF();
        final int N = in.readInt();
        parameters = new double[N];
        for (int i = 0; i < N; i++)
            parameters[i] = in.readDouble();
    }

    public void write(DataOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(parameters.length);
        for (double d : parameters)
            out.writeDouble(d);
    }

    final public void analyticalResolve() {
        analyticalResolve(0, null);
    }

}

package bpiwowar.ml.bn;

import bpiwowar.NotImplementedException;
import bpiwowar.ml.Example;
import bpiwowar.ml.IntegrityException;
import bpiwowar.ml.bn.BayesianNetworks.ErrorFunction;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

import static bpiwowar.ml.bn.BayesianNetworks.noEvidence;

/**
 * A generic Bayesian network
 *
 * @author Piwowarski Benjamin
 */

public abstract class Network extends Example {
    final static Logger logger = Logger.getLogger(Network.class);

    private String name;

    /** Our variables */
    protected Set<AbstractVariable> variables = new HashSet<AbstractVariable>();

    /**
     * Create a network with a given name
     *
     * @param name
     */
    public Network(String name) {
        this.name = name;
    }

    public Network() {
    }

    /** Add a variable */
    public void add(AbstractVariable... newVariables) {
        for (AbstractVariable v : newVariables) {
            variables.add(v);
        }
    }

    /** Remove variable and all its descendants */
    void remove(AbstractVariable v) {
        v.destroy();
        variables.remove(v);
    }

    int size() {
        return variables.size();
    }

    /**
     * To save in dot format
     *
     * @param out The output stream
     * @param logMode true if probabilities are given as log(P), false otherwise
     */
    void save_DOT(PrintStream out, boolean logMode) {
        invalidate();
        getLogLikelihood();
        out.print("digraph G {\n");
        for (AbstractVariable bv : variables) {
            double rb3 = Double.NaN;
            try {
                rb3 = bv.getRB3();
            }
            catch (Exception e) {
                out.println("/* For " + bv);
                e.printStackTrace();
                out.println("*/");
            }
            out.print("N" + Integer.toHexString(bv.hashCode()) + " [label=\""
                + bv.getName() + " (" + bv.getNumberOfStates() + "), RB3="
                + rb3);
            if (bv.has_evidence())
                out.print(", e=" + bv.get_evidence());
            out.print("\"");
            if (bv.has_evidence())
                out.print(",peripheries=2");
            out.print("];\n");
        }
        // Print links between RV
        for (AbstractVariable bv : variables) {
            for (AbstractVariable child : bv.get_children())
                out.print("N" + Integer.toHexString(bv.hashCode()) + " -> N"
                    + Integer.toHexString(child.hashCode()) + ";\n");
        }

        out.print("}\n");
    }

    public void save_DOT(PrintStream out) {
        save_DOT(out, false);
    }

    /**
     * Print the bayesian network
     *
     * @param out The output stream
     * @param logMode true if probabilities are given as log(P), false otherwise
     */

    void print(PrintStream out, boolean logMode) {
        // Ugly, but assure we get good values
        invalidate();
        getLogLikelihood();
        out.print("Bayesian network output (" + getName() + ")\n");
        out.print("=======================\n\n");

        for (AbstractVariable bvp : variables) {
            print(out, bvp, 0, logMode);
        }
    }

    protected static final double getValue(boolean logMode, double logValue) {
        return logMode ? logValue : Math.exp(logValue);
    }

    /**
     * Print a variable of the bayesian network
     *
     * @param out The output stream
     * @param tab_width
     * @param logMode true if probabilities are given as log(P), false otherwise
     */
    void print(PrintStream out, final AbstractVariable bv, int tab_width,
        boolean logMode) {

        String space = "";
        for (int i = 0; i < tab_width; i++)
            space += ' ';

        out.print(space + "### Variable " + bv + " ###");
        if (bv.has_evidence())
            out.print(" with e=" + (bv.get_evidence()));
        else
            out.print(" without evidence");

        final double[] output = bv.get_output();
        if (output != null) {
            out.print(" with output=[");
            for (int e = 0; e < bv.getNumberOfStates(); e++)
                out.print((e > 0 ? "," : "") + output[e]);
            out.print("]");
        }
        out.print("\n");
        out.print(space + "Name: " + bv.getName() + " (function="
            + bv.getFunction() + ")\n");

        // Children output

        if (bv.has_children()) {
            out.print(space + "Children: ");
            for (AbstractVariable child : bv.get_children()) {
                out.print(child + " ");
            }
            out.print("\n");
        }

        // RB1 output
        out.print(space + "RB1 = [");
        final int en = bv.get_evidence();
        if (en != noEvidence)
            out.format("..., (%d) %.4f, ...", en, getValue(logMode, bv
                .getRB1(en)));
        else
            for (int e = 0; e < bv.getNumberOfStates(); e++) {
                out.print((e == 0 ? "" : ","));
                out.print(getValue(logMode, bv.getRB1(e))
                    + (e == bv.get_evidence() ? "*" : ""));
            }
        out.println("]");

        // RB2 output
        out.print(space + "RB2 = [");
        if (en != noEvidence)
            out.format("..., (%d) %.4f, ...", en, getValue(logMode, bv
                .getRB2(en)));
        else
            for (int e = 0; e < bv.getNumberOfStates(); e++) {
                out.print((e == 0 ? "" : ","));
                out.print(getValue(logMode, bv.getRB2(e))
                    + (e == bv.get_evidence() ? "*" : ""));
            }
        out.println("]");
        out.print(space + "RB3 = " + getValue(logMode, bv.getRB3()) + "\n");

        // Conditional probabilities
        if (bv.hasParent()) {
            out.print(space + "P(v|obs.) = [");
            if (bv.has_evidence())
                out.format("..., (%d) %.4f, ...", en, getValue(logMode,
                    getValue(logMode, bv.getLogConditionalProbability(
                        en, null))));
            else
                for (int e = 0; e < bv.getNumberOfStates(); e++) {
                    out.print((e == 0 ? "" : ","));
                    try {
                        final double lcp = bv.getLogConditionalProbability(
                            e, null);
                        out.print(getValue(logMode, lcp));
                    }
                    catch (UnsupportedOperationException uoe) {
                        out.print("?");
                    }
                }
            out.println("]");
        }

        // BN Function output
        final CPAbstract bf = bv.getFunction();
        if (bf != null) {
            bf.print(out);
            // int N = bv.getNumberOfParents();
            // int[] ep = new int[N], mask = new int[N];
            // init_loop(bv, ep, mask);
            // do {
            // out.print(space + "f(v_i | v_p = [");
            // for (int i = 0; i < N; i++)
            // out.print((i != 0 ? ", " : "") + ep[i]);
            // out.print("]) = [");
            // for (int e = 0; e < bv.getNumberOfStates(); e++)
            // out.print((e == 0 ? "" : ", ")
            // + getValue(logMode, bv.log_probability(e, ep)));
            // out.print("]" + "\n");
            // } while (next(bv, ep, mask));
        }
        out.println();
    }

    public void print(PrintStream out) {
        print(out, false);
    }

    /** \name Probabilities */

    /** Returns the joint probability of the evidence in the network */
    public abstract double getLogLikelihood();

    /**
     * A network configuration along with its likelihood
     *
     * @author bpiwowar
     */
    public static final class Configuration {
        /** Variable evidence */
        public HashMap<AbstractVariable, Integer> evidence = new HashMap<AbstractVariable, Integer>();

        /** Likelihood */
        public double logLikelihood;
    }

    /** Returns the maximum likelihood configuration */
    public abstract Configuration getMaximumLikelihoodConfiguration();

    /**
     * Compute partials at point theta
     *
     * @param analytical Analytical mode
     */
    final public void computePartials(double weight, boolean analytical) {
        for (AbstractVariable v : variables) {
            computePartials(v, weight, analytical);
        }
    }

    /**
     * Compute partials for a given variable
     *
     * @param analytical Analytical mode
     */
    final public void computePartials(AbstractVariable v, double weight,
        boolean analytical) {
        // Compute partials only if function is mutable
        final CPAbstract f = v.getFunction();
        if (f == null || !f.isMutable())
            return;
        for (int e = v.initialize(); e != noEvidence; e = v.getNext(e)) {

            for (ParentConfiguration pcs = v.getParentConfigurations(e, true); pcs
                .hasCurrent(); pcs.next()) {
                final double lcp = Math.exp(v.getLogConditionalProbability(
                    e, pcs.current));

                if (logger.isDebugEnabled())
                    logger.debug(String.format("LCP(%s for %s,%d,%s)=%f", v, v
                            .getFunction(), e, Arrays.toString(pcs.current),
                        lcp));

                f.update_partials(analytical, v, e, pcs.current, lcp, weight);
            }
        }
    }

    /** Compute partials at point theta with custom a given error function */
    void compute_discriminant_partials(final ErrorFunction error_function) {
        throw new NotImplementedException("compute_discriminant_partials()");
    }

    /** Returns the joint probability of the evidence in the network */
    double getError(ErrorFunction error_function) {
        if (error_function.needs_parents())
            throw new IntegrityException(
                "Can't compute error if we need the parent's evidence");
        double error = 0.;
        int n = 0;
        for (AbstractVariable v : variables) {
            final double[] output = v.get_output();
            if (output != null) {
                // OK. We've got something to compute
                if (v.has_evidence())
                    continue;
                n++;

                for (int e = 0; e < v.getNumberOfStates(); e++) {
                    error += error_function.get_value(e, null, v, v.getRB2(),
                        output);
                }
            }
        }
        return error;
    }

    // /** Compute a conditional probability P(v_e,v_{pa(e)} | O) */
    // abstract double get_log_conditional_probability(final Variable v, int e,
    // final int[] parent_e);
    //
    // /** Compute a conditional probability P(v_e,v_{pa(e)} | X) */
    // abstract void get_log_conditional_probability_gradient(double[] gradient,
    // final Variable v, int e, final int[] parent_e);

    /**
     * @param v
     */
    public void remove_evidence(AbstractVariable v) {
        v.set_evidence(noEvidence);
    }

    /**
     * Invalidate the network cache Recompute all the probabilities next time
     */
    public void invalidate() {
        for (AbstractVariable v : variables) {
            v.invalidate(true, true);
        }
    }

    static void init_loop(final AbstractVariable v, int[] ep, int[] mask) {
        for (int i = 0; i < v.getNumberOfParents(); i++) {
            mask[i] = v.getParent(i).getNumberOfStates();
            ep[i] = 0;
        }
    }

    static boolean next(final AbstractVariable v, int[] ep, int[] mask) {
        assert (mask != null);
        for (int i = 0; i < v.getNumberOfParents(); i++) {
            if (mask[i] != 0)
                if ((ep[i] = ((ep[i] + 1) % mask[i])) != 0)
                    return true;
        }
        return false;
    }

    public Set<AbstractVariable> getVariables() {
        return variables;
    }

    /**
     * Get the network name
     */
    public String getName() {
        return name;
    }

    /**
     * Verify the integrity of the network (in particular, if functions are well defined for each of the network
     * variables)
     */
    public final boolean verify() {
        boolean ok = true;
        for (AbstractVariable v : variables) {
            try {
                v.verify();
            }
            catch (IntegrityException e) {
                System.err.println("Error with " + v + ": " + e);
                e.printStackTrace();
                ok = false;
            }
        }
        return ok;
    }

    public void write(DataOutput out,
        Map<Class<? extends AbstractVariable>, Integer> classMap,
        Map<CPAbstract, Integer> functions) throws IOException {
        out.writeInt(variables.size());
        // Set of processed variables
        HashMap<AbstractVariable, Integer> map = new HashMap<AbstractVariable, Integer>();
        for (AbstractVariable v : variables) {
            write(out, v, classMap, map, functions);
        }
    }

    private void write(DataOutput out, AbstractVariable v,
        Map<Class<? extends AbstractVariable>, Integer> classMap,
        Map<AbstractVariable, Integer> map,
        Map<CPAbstract, Integer> functions) throws IOException {
        if (map.containsKey(v))
            return;

        // Ensure parents are written before proceeding
        for (AbstractVariable p : v.getParents())
            write(out, p, classMap, map, functions);

        // --- Write ourselves [type, variable data, # of parents, list of
        // parents]
        map.put(v, map.size());

        // Write type
        final Integer type = classMap.get(v.getClass());
        if (type == null)
            throw new RuntimeException("Dictionnary does not contain class "
                + v.getClass());
        out.writeInt(type.intValue());

        // write variable data
        v.write(out, functions);

        // write the parents
        out.writeInt(v.getNumberOfParents());
        for (AbstractVariable p : v.getParents()) {
            out.writeInt(map.get(p));
        }

    }

    public void read(
        final DataInput in,
        final Map<Integer, Class<? extends AbstractVariable>> variableClasses,
        final Map<Integer, CPAbstract> functions) throws IOException,
        SecurityException, IllegalArgumentException,
        InstantiationException, IllegalAccessException {
        final int N = in.readInt();
        variables.clear();
        final AbstractVariable[] a = new AbstractVariable[N];
        for (int i = 0; i < N; i++) {
            // Variable = [type, variable info, parents]
            final int type = in.readInt();
            final Class<? extends AbstractVariable> c = variableClasses
                .get(type);
            if (c == null)
                throw new IOException("Class number " + type
                    + " cannot be found");
            a[i] = c.newInstance();

            // Read variable data and construct instance
            a[i].read(in, functions);
            variables.add(a[i]);
            // Read list of parents
            final int NP = in.readInt();
            for (int j = 0; j < NP; j++) {
                final int k = in.readInt();
                if (i <= k)
                    throw new RuntimeException("A parent cannot have an id ("
                        + k + ")less than the variable id (" + i + ")");
                a[i].addParent(a[k]);
            }

            add(a[i]);
        }
    }

}

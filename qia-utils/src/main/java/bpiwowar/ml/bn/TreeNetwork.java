package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import bpiwowar.ml.bn.BayesianNetworks.ErrorFunction;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static bpiwowar.ml.bn.BayesianNetworks.noEvidence;
import static java.lang.Math.exp;

/**
 * @author Benjamin Piwowarski
 */
public class TreeNetwork extends Network {
    AbstractVariable root;

    /**
     * @param name The name of this network
     */
    public TreeNetwork(String name) {
        super(name);
    }

    public TreeNetwork() {
        super();
    }

    /** Add a variable */
    public void add(AbstractVariable... newVariables) {
        super.add(newVariables);
        for (AbstractVariable v : newVariables) {
            if (v.getNumberOfParents() > 1)
                throw new IntegrityException(
                    "Variables in a tree cannot have more than one parent");

            if (!v.hasParent()) {
                if (root != null)
                    throw new IntegrityException(
                        "Root already defined in tree network");
                root = v;
            }
        }
    }

    /** Remove variable and all its descendants */
    void remove(AbstractVariable v) {
        super.remove(v);
        if (root == v)
            root = null;
    }

    /** Returns the joint probability of the evidence in the network */
    public double getLogLikelihood() {
        if (root == null)
            throw new IntegrityException("No root defined in a tree network");
        return root.getRB3();
    }

    /** Compute discriminant partials */
    @SuppressWarnings("null")
    void compute_discriminant_partials(final ErrorFunction error_function) {
        invalidate();

        // int[] evidences = new int[2];
        // int &e_j = evidences[0];
        // int &e_p = evidences[1];

        int e_j = 0;

        // Go on
        for (AbstractVariable v : variables) {
            final double[] output = v.get_output();
            if (output != null) {
                // std::cerr + "X" + output + "Y\n" +
                // v.get_output_ratio();
                // OK. We've got something to compute
                if (v.has_evidence()) {
                    continue;
                }

                AbstractVariable pa = null;

                int e = 0;
                if (v.hasParent() && error_function.needs_parents()) {
                    pa = v.getParent(0);
                    if (pa.has_evidence())
                        continue;
                }

                int ns = v.getNumberOfStates();
                double[] err = new double[v.getNumberOfStates()
                    * (pa != null ? pa.getNumberOfStates() : 1)];
                for (int e2 = 0; e2 < ns; e2++) {
                    if (pa != null) {
                        for (int pa_e2 = 0; pa_e2 < v.getNumberOfStates(); pa_e2++) {
                            err[e2 + pa_e2 * ns] = error_function.get_partial(
                                e2, new int[] {pa_e2}, v, v.getRB2(),
                                output);
                        }
                    }
                    else
                        err[e2] = error_function.get_partial(e2, null, v, v.getRB2(), output);
                }

                e = 0;
                int[] pa_e = {0};

                while (e < ns
                    && ((pa != null) || (pa_e[0] < pa
                    .getNumberOfStates()))) {
                    double error = pa != null ? err[e + pa_e[0] * ns] : err[e];
                    if (error != 0.) {
                        v.set_evidence(e);
                        if (pa != null)
                            pa.set_evidence(pa_e[0]);
                        // Browse all ancestors for contribution
                        for (AbstractVariable v_a = v; v_a != null; v_a = (v_a
                            .hasParent() ? v_a.getParent(0) : null)) {
                            if (!v_a.getFunction().isMutable())
                                continue;
                            if (v_a.hasParent()) {
                                AbstractVariable v_p = v_a.getParent(0);
                                for (e_j = v_a.initialize(); e_j != noEvidence; e_j = v_a
                                    .getNext(e_j)) {
                                    for (int[] e_p = new int[] {
                                        v_p
                                            .initialize()}; e_p[0] != noEvidence; e_p[0] = v_p
                                        .getNext(e_p[0])) {
                                        double logp = 0.;
                                        if (pa != null) {
                                            // Error function with parent
                                            // evidence
                                            if (v_a.equals(v))
                                                logp = v_p.getRB2(e_p[0]);
                                            else if (v_a == pa)
                                                logp = v.getFunction()
                                                    .log_probability(v, e,
                                                        pa_e)
                                                    + v_p.getRB2(e_p[0]);
                                            else
                                                logp = v.getFunction()
                                                    .log_probability(v, e,
                                                        pa_e)
                                                    + v_a.getRB1(e_j)
                                                    + v_p.getRB2(e_p[0]);
                                        }
                                        else
                                            logp = (v_a.equals(v) ? 0. : v_a
                                                .getRB1(e_j)) // Pr(v/v)
                                                // = 1
                                                + v_p.getRB2(e_p[0]);
                                        v_a.getFunction()
                                            .update_partials(false, v_a, e_j, e_p,
                                                exp(logp) * error, 1.);
                                    }
                                }
                            }
                            else {
                                for (e_j = v_a.initialize(); e_j != noEvidence; e_j = v_a
                                    .getNext(e_j)) {
                                    double logp = 0.;
                                    if (pa != null) {
                                        // error function with parent
                                        // evidence
                                        if (v_a == v)
                                            logp = 0.;
                                        else if (v_a == pa)
                                            logp = v
                                                .getFunction()
                                                .log_probability(v, e, pa_e);
                                        else
                                            logp = v
                                                .getFunction()
                                                .log_probability(v, e, pa_e)
                                                + v_a.getRB1(e_j);
                                    }
                                    else
                                        logp = (v_a == v ? 0. : v_a
                                            .getRB1(e_j)); // Pr(v/v)
                                    // = 1
                                    v_a.getFunction().update_partials(false, v_a,
                                        e_j, pa_e, exp(logp) * error, 1.);
                                }
                            }
                        }
                    }
                    e++;
                    if (e >= ns && pa != null) {
                        e = 0;
                        pa_e[0]++;
                    }
                } // end while
                // Remove evidence
                v.set_evidence(noEvidence);
                if (pa != null)
                    pa.set_evidence(noEvidence);
            }
        }
    }

    /** Get the error */
    double getError(final ErrorFunction error_function) {
        if (!error_function.needs_parents())
            return super.getError(error_function);

        double error = 0.;
        int n = 0;
        for (AbstractVariable v : variables) {
            final double[] output = v.get_output();
            if (output != null) {
                // OK. We've got something to compute
                if (v.has_evidence()) {
                    continue;
                }
                n++;

                for (int e = 0; e < v.getNumberOfStates(); e++) {
                    if (!v.hasParent())
                        error += error_function.get_value(e, null, v, v.getRB2(), output);
                    else {
                        final AbstractVariable pv = v.getParent(0);
                        for (int[] pe = {0}; pe[0] < pv
                            .getNumberOfStates(); pe[0]++) {
                            error += error_function.get_value(e, pe, v, v
                                .getRB2(), output);
                        }
                    }
                }
            }
        }
        return error;
    }

    /** Print the network */
    void print(PrintStream out, boolean logMode) {
        if (root != null) {
            super.print(out, logMode);
            return;
        }

        invalidate(); // just to be sure !
        out.print("(Tree) Bayesian network output (" + variables.size()
            + " variables)" + " P(X) = "
            + getValue(logMode, getLogLikelihood()) + "\n");
        out.print("==============================" + "\n" + "\n");
        print_variable(out, root, 0, true, logMode);
    }

    void print_variable(PrintStream out, AbstractVariable v, int tab_width,
        boolean recursive, boolean logMode) {
        super.print(out, v, tab_width, logMode);
        // RB4 output
        String space = "";
        for (int i = 0; i < tab_width; i++)
            space += ' ';
        if (v.hasParent()) {
            out.print(space + "RB4 (/pa) = [");
            final AbstractVariable pa = v.getParent(0);
            for (int e = 0; e < pa.getNumberOfStates(); e++) {
                out.print((e == 0 ? "" : ","));
                if (pa.has_evidence() && e != pa.get_evidence()) {
                    out.print("?");
                }
                else {
                    out.print(getValue(logMode, v.getRB4(new int[] {e})));
                }
            }
            out.print("]" + "n");// Derivate
            out.print(space + "P(v_e, v_pa(e) / X) = [");
            for (int pae = 0; pae < pa.getNumberOfStates(); pae++) {
                for (int e = 0; e < v.getNumberOfStates(); e++) {
                    if (pae != 0 || e != 0)
                        out.print(", ");
                    out.print(e + " " + pae + " . ");
                    if (v.compatible(e) && pa.compatible(pae)) {
                        out.print(getValue(logMode, v
                            .getLogConditionalProbability(e,
                                new int[] {pae})));
                    }
                    else {
                        out.print("*");
                    }
                }
            }
            out.print("]" + "n");
        }
        out.print("n");
        if (recursive)
            for (int i = 0; i < v.get_children().size(); i++)
                print_variable(out, v.get_child(i), tab_width + 2, recursive,
                    logMode);
    }

    Iterable<int[]> parentLoop(final AbstractVariable v) {
        return new Iterable<int[]>() {
            public Iterator<int[]> iterator() {
                return new Iterator<int[]>() {
                    int[] array = new int[1];

                    int e = v.initialize();

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    public int[] next() {
                        if (e == noEvidence)
                            throw new NoSuchElementException();
                        array[0] = e;
                        e = v.getNext(e);
                        return array;
                    }

                    public boolean hasNext() {
                        return e != noEvidence;
                    }

                };
            }
        };
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.Network#getMaximumLikelihoodConfiguration()
     */
    @Override
    public Configuration getMaximumLikelihoodConfiguration() {
        Configuration configuration = new Configuration();
        configuration.logLikelihood = root.getMaxConfiguration(configuration.evidence);
        return configuration;
    }

    public AbstractVariable getRoot() {
        return root;
    }

}

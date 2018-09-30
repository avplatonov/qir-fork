package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import bpiwowar.ml.OutOfBoundsException;
import bpiwowar.utils.maths.LogSumExpLog;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static bpiwowar.ml.bn.BayesianNetworks.LOG_ERROR_THRESHOLD;
import static bpiwowar.ml.bn.BayesianNetworks.MINLOGPROBA;
import static bpiwowar.ml.bn.BayesianNetworks.logNormalize;
import static bpiwowar.ml.bn.BayesianNetworks.noEvidence;
import static bpiwowar.ml.bn.BayesianNetworks.normalizeLogProbability;
import static bpiwowar.ml.bn.BayesianNetworks.stochasticEvidence;

public class Variable extends AbstractVariable {
    /** Number of values */
    int nbStates;

    /** Current evidence */
    int evidence = noEvidence;

    /** Our function */
    CPAbstract function;

    public Variable(int nbValues) {
        nbStates = nbValues;
    }

    public Variable() {
    }

    /**
     * @param string
     * @param i
     */
    public Variable(String name, int nbStates) {
        super(name);
        this.nbStates = nbStates;
    }

    /**
     * @param numberOfStates
     * @param chainType
     */
    public Variable(int numberOfStates, CPAbstract function) {
        this.nbStates = numberOfStates;
        this.function = function;
    }

    /**
     * @param name
     * @param numberOfStates
     * @param function
     */
    public Variable(String name, int numberOfStates, CPAbstract function) {
        super(name);
        this.nbStates = numberOfStates;
        this.function = function;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.AbstractVariable#getNumberOfStates()
     */
    @Override final public int getNumberOfStates() {
        return nbStates;
    }

    @Override final protected int getNumberOfPossibleStates() {
        return noEvidence == evidence ? nbStates : 1;
    }

    /** To set the bayesian function */
    public void setFunction(CPAbstract b) {
        function = b;
    }

    /** To get the bayesian function */
    @Override
    public CPAbstract getFunction() {
        return function;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.AbstractVariable#getIsMutable()
     */
    @Override
    public boolean getIsMutable() {
        return getFunction().isMutable();
    }

    /** Get the log probability */
    public double log_probability(int e, final int[] pa_e) {
        return getFunction().log_probability(this, e, pa_e);
    }

    /** Set evidence */
    public void set_evidence(int e) {
        // No action
        if (has_evidence()) {
            if (e == get_evidence())
                return;
        }
        else if (e == noEvidence)
            return;

        // Update
        if (evidence == stochasticEvidence && _RB2 != null) {
            _RB2 = null;
        }
        evidence = e;

        invalidate_up();
        invalidate_down();
    }

    /** Set stochastic evidence */
    void set_evidence(final double probabilities[]) {
        if (_RB2 != null)
            _RB2 = new double[getNumberOfStates()];
        double sum = 0;
        for (int e = 0; e < getNumberOfStates(); e++) {
            sum += (probabilities[e]);
            _RB2[e] = logNormalize(probabilities[e]);
        }
        if (Math.abs(sum - 1.) > 1e-30)
            throw new IntegrityException(
                "Stochastic evidence sum is not 1 (at 1e-30)");
        evidence = stochasticEvidence;
        invalidate_down();
        invalidate_up();
    }

    public int get_evidence() {
        return evidence;
    }

    /**
     * Iterates through all parent configurations
     *
     * @param noParentConfiguration If the variable has no parent, consider one null configuration
     */
    public ParentConfiguration getParentConfigurations(int evidence,
        final boolean noParentConfiguration) {
        return function.getParentConfigurations(this, evidence,
            noParentConfiguration);
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.AbstractVariable#destroy()
     */
    @Override
    void destroy() {
        super.destroy();

        // Delete cache probabilites
        _RB1 = _RB2 = _RB4 = null;
        _RB3 = -1;
    }

    /* ========= CACHE ========= */

    /**
     * RB1 is $P(O_{descendants(v)} | v = e) for $e \in X_v$ It is 1 if the variable has no descendants, or if all the
     * descendants do not bear evidence
     */
    double[] _RB1;

    /**
     * RB2(v_e) is - $P(v = e | O_{non-descendants(v)}) for e in O_v or a given probability if we have stochastic
     * evidence
     */
    double[] _RB2;

    /**
     * RB3 is the probability P(O_{v and descendants} | O_{nondesc}) For a tree, RB3 of the root is thus the likelihood
     * P(O)
     */
    private double _RB3 = 1;

    /**
     * RB4(e) is the probability of the observation restricted to the variable and its descendants, given the $P(O_{v
     * and descendants} | v_pa(e) ) for $v_{pa(e)} \in O_{pa(e)}$
     */
    double[] _RB4;

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.AbstractVariable#invalidate()
     */
    @Override
    public void invalidate(boolean up, boolean down) {
        if (down || up) {
            _RB3 = 1.;
        }

        if (up) {
            _RB1 = null;
            _RB4 = null;
        }

        if (down) {
            _RB2 = null;
        }

        super.invalidate(up, down);
    }

    @Override
    public double getRB4(final int[] pc) {
        if (getParents().isEmpty())
            throw new IntegrityException("Non sense");

        if (!has_children())
            return has_evidence() ? log_probability(get_evidence(), pc) : 0.;

        if (_RB4 == null) {
            final AbstractVariable pa = getParent(0);

            if (_RB4 == null)
                _RB4 = new double[pa.getNumberOfStates()];
            LogSumExpLog s = new LogSumExpLog();
            _RB4[0] = MINLOGPROBA; // value is cached
            boolean has_stochastic_evidence = has_stochastic_evidence();
            for (int[] pa_e = new int[] {pa.initialize()}; pa_e[0] != noEvidence; pa_e[0] = pa
                .getNext(pa_e[0])) {
                s.reset();
                for (int e = initialize(); e != noEvidence; e = getNext(e)) {
                    if (has_stochastic_evidence)
                        s.addLogValue(getRB1(e) + getRB2(e));
                    else
                        s.addLogValue(getRB1(e) + log_probability(e, pa_e));
                }
                _RB4[pa_e[0]] = normalizeLogProbability(s.getLogSum());
            }

        }

        return _RB4[getPosition(pc)];
    }

    @Override
    public double getRB2(int state) {
        if (!hasParent())
            return getFunction().get_a_priori()[state];

        final boolean flag = has_evidence();
        if (_RB2 == null) {
            // Initializations
            final AbstractVariable pa = getParent(0);
            if (_RB2 == null)
                _RB2 = new double[flag ? 1 : getNumberOfStates()];

            // Does parent have any evidence ???
            if (pa.has_evidence()) {
                int[] pa_e = {pa.get_evidence()};
                _RB2[0] = MINLOGPROBA; // Value is now "cached"
                for (int e = initialize(); e != noEvidence; e = getNext(e)) {
                    _RB2[flag ? 0 : e] = normalizeLogProbability(log_probability(e, pa_e));
                }
            }
            else if (pa.has_stochastic_evidence()) {
                // Does parent have stochastic evidence ?
                LogSumExpLog sum = new LogSumExpLog();
                for (int e = initialize(); e != noEvidence; e = getNext(e)) {
                    sum.reset();
                    for (int[] pa_e = {pa.initialize()}; pa_e[0] != noEvidence; pa_e[0] = pa
                        .getNext(pa_e[0])) {
                        sum.addLogValue(pa.getRB2(pa_e[0])
                            + log_probability(e, pa_e));
                    }
                    _RB2[flag ? 0 : e] = normalizeLogProbability(sum.getLogSum());
                }
            }
            else {

                // Other cases
                // Compute P(X_{v brothers and their descendants},
                // v_{parent} /
                // X_{non descendants of parent})
                // for each value of v_{parent} (RB7) and its sum (RB7_sum)

                double[] RB7 = new double[pa.getNumberOfStates()];
                LogSumExpLog s_RB7 = new LogSumExpLog();
                for (ParentConfiguration pc = getParentConfigurations(
                    noEvidence, false); pc.hasCurrent(); pc.next()) {
                    try {
                        int[] pa_e = pc.current;
                        if (Double.isInfinite(pa.getRB2(pa_e[0])))
                            RB7[pa_e[0]] = MINLOGPROBA;
                        else {
                            // FIXME: Assumes v is a tree variable!
                            if (Double.isInfinite(getRB4(pa_e))) {
                                if (!Double.isInfinite(pa.getRB1(pa_e[0])))
                                    throw new IntegrityException(
                                        "RB4 is 0 but not RB1 !!!");
                                // Ok. We compute it !
                                double p = 0.;
                                for (int i = 0; i < pa.getNumberOfChildren(); i++) {
                                    if (pa.get_child(i) == this)
                                        continue;
                                    // FIXME: Assumes v is a tree variable!
                                    p += pa.get_child(i).getRB4(pa_e);
                                }
                                RB7[pa_e[0]] = normalizeLogProbability(pa.getRB2(pa_e[0])
                                    + p);
                                // Compute as the product of RB4
                                // RB7[pa_e] = pa_RB2[pa_e]; // => RB2 or
                                // RB1 is -inf
                                // also
                            }
                            else
                                // FIXME: Assumes v is a tree variable!
                                RB7[pa_e[0]] = normalizeLogProbability(pa.getRB2(pa_e[0])
                                    + pa.getRB1(pa_e[0]) - getRB4(pa_e));
                        }
                        s_RB7.addLogValue(RB7[pa_e[0]]);
                    }
                    catch (RuntimeException e) {
                        // std::cerr + __LOCATION__ + ": "
                        // + pa_RB2[pa_e] + "," + pa.get_RB1(pa_e) + ", " +
                        // get_RB4(v,pa_e) + "\n";
                        throw e;
                    }

                }

                double RB7_sum = MINLOGPROBA;
                RB7_sum = normalizeLogProbability(s_RB7.getLogSum());

                // HYPINF : RB7 = 0 => RB2 equals conditional Sum_{v_pa(e)}
                // P(v_e /
                // v_pa(e)) x P(v_pa(e) / v_nd(e))
                // ------

                if (Double.isInfinite(RB7_sum)) {
                    LogSumExpLog s = new LogSumExpLog();
                    for (int e = initialize(); e != noEvidence; e = getNext(e)) {
                        s.reset();
                        for (ParentConfiguration pc = getParentConfigurations(
                            e, false); pc.hasCurrent(); pc.next())
                            s.addLogValue(log_probability(e, pc)
                                + pa.getRB2(pc.get(0)));
                        _RB2[flag ? 0 : e] = normalizeLogProbability(s.getLogSum());
                    }
                }
                else {
                    // -2.2- Compute RB2
                    for (int e = initialize(); e != noEvidence; e = getNext(e)) {
                        LogSumExpLog s = new LogSumExpLog();
                        for (ParentConfiguration pc = getParentConfigurations(
                            e, false); pc.hasCurrent(); pc.next()) {
                            s.addLogValue(log_probability(e, pc.current)
                                + RB7[pc.current[0]]);
                        }
                        _RB2[flag ? 0 : e] = normalizeLogProbability(s.getLogSum()
                            - RB7_sum);
                    }
                }
            }
        }

        return _RB2[flag ? 0 : state];
    }

    @Override
    public double getRB1(int state) {
        if (!has_children())
            return 0.;
        final boolean flag = has_evidence();

        if (_RB1 == null) {
            // (!has_children()) || );
            // -1- Computation

            if (_RB1 == null)
                _RB1 = new double[flag ? 1 : getNumberOfStates()];

            for (int e = initialize(); e != noEvidence; e = getNext(e)) {
                _RB1[flag ? 0 : e] = 0.;
                for (AbstractVariable child : get_children()) {
                    _RB1[flag ? 0 : e] += child.getRB4(new int[] {e});
                }
                _RB1[flag ? 0 : e] = normalizeLogProbability(_RB1[flag ? 0 : e]);
            }

        }

        return _RB1[flag ? 0 : state];
    }

    @Override
    public double getRB3() {
        if (_RB3 > 0.) {
            // no children
            if (!has_children()) {
                _RB3 = (has_evidence() ? getRB2(get_evidence()) : 0.);
            }
            else {
                LogSumExpLog s = new LogSumExpLog();
                for (int e = initialize(); e != BayesianNetworks.noEvidence; e = getNext(e)) {
                    s.addLogValue(getRB1(e) + getRB2(e));
                }
                _RB3 = s.getLogSum();
            }
        }
        return _RB3;
    }

    /**
     * Compute conditional probability of variable v and its parents being in a given state, given the current
     * observations
     */
    public double getLogConditionalProbability(int e, final int[] pa_e) {
        if (has_evidence() && e != get_evidence())
            throw new IntegrityException(String.format(
                "Incompatible evidence (%d vs %d) for %s", e,
                get_evidence(), this));

        // --- variable has no parent ---
        if (!hasParent() || pa_e == null) {
            if (has_evidence())
                return 0.;
            // --- Stochastic evidence ---
            if (has_stochastic_evidence())
                return getRB2(e);

            // HYPINF => Sum is 0 => part of the sum is also 0
            if (Double.isInfinite(getRB3())) {
                if (!(Double.isInfinite(getRB1(e)) || Double
                    .isInfinite(getRB2(e))))
                    throw new RuntimeException();
                return MINLOGPROBA;
            }

            try {
                return normalizeLogProbability(getRB1(e) + getRB2(e) - getRB3());
            }
            catch (OutOfBoundsException f) {
                System.err.format("Error for %s, (%d/%s): %f, %f, %f = %f%n", this, e, pa_e == null ? "" : Arrays.toString(pa_e),
                    getRB1(e), getRB2(e), getRB3(), getRB1(e) + getRB2(e) - getRB3());
                throw f;
            }
        }

        // --- some sanity checks ---

        final AbstractVariable pa = getParent(0);
        assert (pa_e != null);
        assert (pa_e[0] >= 0);
        assert (pa_e[0] < pa.getNumberOfStates());
        assert (e < getNumberOfStates());
        if (pa.has_evidence() && pa_e[0] != pa.get_evidence())
            throw new IntegrityException(String.format(
                "Incompatible evidence (%d but %d set) for %s", pa_e[0], pa
                    .get_evidence(), getParent(0)));

        // --- evidence is the same ---

        if (e == get_evidence())
            if (pa_e[0] == pa.get_evidence())
                return 0.;
            else
                return pa.getLogConditionalProbability(pa_e[0], null);

        // --- parent evidence is the same ---

        if (pa_e[0] == pa.get_evidence())
            return getLogConditionalProbability(e, null);

        // --- Worst case: Variable & parent have no evidence ---

        // --- infinite cases
        if (Double.isInfinite(getRB4(pa_e)) || Double.isInfinite(pa.getRB3()))
            return MINLOGPROBA;

        final double p1 = getRB1(e) + log_probability(e, pa_e) - getRB4(pa_e);
        final double p2 = pa.getRB1(pa_e[0]) + pa.getRB2(pa_e[0]) - pa.getRB3();

        try {
            // P(O_d | v=e) * P(v=e|pa=pa_e) * P(O_d(pa) | pa=pa_e) * P(pa=pa_e
            // | O_a(pa))
            // ---------------------------------------------------------------------------
            // P(O_ds(v) | pa=pa_e) * P(O_ds(pa) / O_a(pa))
            assert p1 < LOG_ERROR_THRESHOLD;
            assert p2 < LOG_ERROR_THRESHOLD;

            return normalizeLogProbability(p1 + p2);
        }
        catch (AssertionError err) {
            System.err
                .format(
                    "RB1=%f, LP=%f, RB4=%f : p1 = %f%nRB1_pa=%f, RB2_pa=%f, RB3=%f : p2=%f%n",
                    getRB1(e), log_probability(e, pa_e), getRB4(pa_e),
                    p1, pa.getRB1(pa_e[0]), pa.getRB2(pa_e[0]), pa
                        .getRB3(), p2);
            throw err;
        }
    }

    @Override
    public void get_log_conditional_probability_gradient(double[] gradient,
        int e, int[] parent_e) {
        invalidate_up();
        invalidate_down();

        int e_j = e;
        // Go on
        // std::cerr << "X" << output << "Y\n" << v.get_output_ratio();
        // OK. We've got something to compute

        set_evidence(e_j);
        if (parent_e != null)
            getParent(0).set_evidence(parent_e[0]);

        // Browse all ancestors for contribution
        for (AbstractVariable v_a = this; v_a != null; v_a = (v_a.hasParent() ? v_a
            .getParent(0)
            : null)) {

            if (!v_a.getIsMutable())
                continue;

            if (v_a.hasParent()) {
                AbstractVariable v_p = v_a.getParent(0);
                for (e_j = v_a.initialize(); e_j != noEvidence; e_j = v_a
                    .getNext(e_j)) {
                    for (ParentConfiguration pc = v_a.getParentConfigurations(
                        e_j, false); pc.hasCurrent(); pc.next()) {
                        double logp = (v_a == this ? 0. : v_a.getRB1(e_j)) // Pr(v/v)
                            // = 1
                            + v_p.getRB2(pc.current[0]);
                        ((Variable)v_a).getFunction().update_partials(false,
                            v_a, e_j, pc.current, Math.exp(logp), 1.);
                    }
                }
            }
            else {
                for (e_j = v_a.initialize(); e_j != noEvidence; e_j = v_a
                    .getNext(e_j)) {
                    double logp = (v_a == this ? 0. : v_a.getRB1(e_j));
                    ((Variable)v_a).getFunction().update_partials(false, v_a,
                        e_j, null, Math.exp(logp), 1.);
                }

            }
        }

        // Remove evidence
        set_evidence(noEvidence);

    }

    @Override
    public MaxConfiguration[] computeMaxConfiguration() {
        // --- Initialisations ---
        int n = 1;
        for (AbstractVariable parent : parents)
            n *= parent.getNumberOfStates();

        MaxConfiguration[] maxc = new MaxConfiguration[n];
        MaxConfiguration[][] cmaxc = new MaxConfiguration[getNumberOfChildren()][];
        for (int i = 0; i < children.size(); i++)
            cmaxc[i] = children.get(i).computeMaxConfiguration();

        // --- Loop over each parent configuration ---

        if (getNumberOfParents() > 1)
            throw new UnsupportedOperationException(
                "Compute maximum configuration with more than one parent");

        for (ParentConfiguration pc = getParentConfigurations(evidence, true); pc
            .hasCurrent(); pc.next()) {
            // Get the argmax over possible variable states
            double max = Double.NEGATIVE_INFINITY;
            int argmax = 0;
            for (int e : getEvidences(pc.current)) {
                double logp = getFunction().log_probability(this, e, pc);
                for (int i = 0; i < children.size(); i++) {
                    // System.err.format("cmax[%s][%d]%n",children.get(i),e);
                    logp += cmaxc[i][e].logProbability;
                }
                if (logp > max) {
                    argmax = e;
                    max = logp;
                }
            }

            // Add the argmax for this parent configuration

            final int noConf = hasParent() ? pc.current[0] : 0;
            // System.err.format("For parent configuration %s/%d, variable %s
            // best state is %d (%f)%n",
            // pc, noConf, this, argmax, Math.exp(max));
            final MaxConfiguration mc = (maxc[noConf] = new MaxConfiguration());
            mc.logProbability = max;
            mc.state = argmax;
            mc.variable = this;
            for (int i = 0; i < children.size(); i++)
                mc.subconfigurations.add(cmaxc[i][argmax]);

        }

        return maxc;
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.AbstractVariable#verify()
     */
    @Override
    public void verify() throws IntegrityException {
        getFunction().verify(this);
    }

    @Override
    public void read(DataInput in, Map<Integer, CPAbstract> functions) throws IOException {
        super.read(in, functions);
        int fno = in.readInt();
        function = functions.get(fno);
        if (function == null && fno >= 0)
            throw new RuntimeException("Cannot find function no " + fno);
        nbStates = in.readInt();
        evidence = in.readInt();
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.AbstractVariable#write(java.io.ObjectOutputStream, java.util.Map)
     */
    @Override
    public void write(DataOutput out, Map<CPAbstract, Integer> functions) throws IOException {
        super.write(out, functions);
        int fno = -1;
        if (function != null) {
            Integer i = functions.get(function);
            if (i == null)
                throw new RuntimeException("Cannot find function " + function + " index");
            fno = i;

        }
        out.writeInt(fno);
        out.writeInt(nbStates);
        out.writeInt(evidence);
    }
}

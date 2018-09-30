package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static bpiwowar.ml.bn.BayesianNetworks.noEvidence;
import static bpiwowar.ml.bn.BayesianNetworks.stochasticEvidence;

abstract public class AbstractVariable {
    /**
     * Variable name
     */
    private String name;

    /** The parents of the variable */
    protected List<AbstractVariable> parents = new ArrayList<AbstractVariable>();

    /**
     * List of children variables
     */
    protected List<AbstractVariable> children = new ArrayList<AbstractVariable>();

    public CPAbstract getFunction() {
        return null;
    }

    public AbstractVariable() {
    }

    public AbstractVariable(String name) {
        this.name = name;
    }

    /** To add a child variable */
    public void add_child(AbstractVariable b) {
        children.add(b);
        b.parents.add(this);
    }

    /** Get the parents */
    protected List<AbstractVariable> get_children() {
        return children;
    }

    /**
     * Get the i<sup>th</sup> child
     *
     * @return 0 if this child does not exist
     */
    AbstractVariable get_child(int i) {
        if ((i < 0) || (i >= children.size()))
            return null;

        return children.get(i);
    }

    /** Get number of children */
    protected int getNumberOfChildren() {
        return children.size();
    }

    /** Has parent */
    protected boolean has_children() {
        return !children.isEmpty();
    }

    /** Get the parents */
    public List<AbstractVariable> getParents() {
        return parents;
    }

    /** To get the number of parents */
    public int getNumberOfParents() {
        return parents.size();
    }

    /** To get the parent i */
    public AbstractVariable getParent(int i) {
        return parents.get(i);
    }

    /** To add a parent variable */
    public void addParent(AbstractVariable b) {
        parents.add(b);
        b.children.add(this);
    }

    /** Has parent */
    public boolean hasParent() {
        return !parents.isEmpty();
    }

    protected abstract int getNumberOfPossibleStates();

    /**
     * Returns the a value for the log probability table
     *
     * @param e
     * @param pa_e
     * @return
     */
    public abstract double log_probability(int e, final int[] pa_e);

    /** Get the variable number */
    abstract public int getNumberOfStates();

    abstract public void set_evidence(int e);

    final protected void invalidate_down() {
        invalidate(false, true);
        for (AbstractVariable child : children)
            child.invalidate_down();
    }

    final protected void invalidate_up() {
        invalidate(true, false);
        for (AbstractVariable parent : parents)
            parent.invalidate_up();
    }

    /** Get evidence */
    abstract public int get_evidence();

    /** Get evidence */
    public boolean has_evidence() {
        return get_evidence() != noEvidence
            && get_evidence() != stochasticEvidence;
    }

    protected boolean has_stochastic_evidence() {
        return get_evidence() == stochasticEvidence;
    }

    /**
     * Is evidence compatible \returns true if variable has no evidence or has the same evidence
     */
    boolean compatible(int e) {
        return !has_evidence() || e == get_evidence();
    }

    /** Initialize */
    public int initialize() {
        return has_evidence() ? get_evidence() : 0;
    }

    /** Initialize */
    public int getNext(int e) {
        return has_evidence() ? noEvidence
            : (e >= getNumberOfStates() - 1 ? noEvidence : e + 1);
    }

    // Discriminant learning
    private double[] output;

    void set_output_ratio(double ratio) {
        if (output == null)
            throw new IntegrityException(
                "Setting output ratio, but no output !!!");
        output[0] = ratio;
    }

    void set_output(double[] _output) {
        set_output(_output, 1.);
    }

    void set_output(double[] _output, double _ratio) {
        double sum = 0;
        if (output != null)
            output = new double[getNumberOfStates() + 1];
        output[0] = _ratio;
        for (int e = 0; e < getNumberOfStates(); e++) {
            sum += (_output[e]);
            output[e + 1] = _output[e];
        }
        if (Math.abs(sum - 1.) > 1e-30)
            throw new IntegrityException("Output sum is not 1 (at 1e-30)");
    }

    final double[] get_output() { /*
     * if (output != null) return output + 1;
     * return null;
     */
        if (output != null)
            throw new IntegrityException("behaviour is too c++");
        return null;
    }

    double get_output_ratio() {
        if (output != null)
            return output[0];
        return 0.;
    }

    /**
     * Get the variable name
     */
    public String getName() {
        return name;
    }

    void destroy() {
        // Detach parents
        invalidate_up();
        invalidate_down();
        for (AbstractVariable parent : parents)
            parent.children.remove(this);
        for (AbstractVariable child : children)
            child.children.remove(this);

        // Delete output
        output = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name != null ? name + " ("
            + Integer.toHexString(super.hashCode()) + ")" : super
            .toString();
    }

    /**
     * Iterates through all parent configurations
     *
     * @param noParentConfiguration If the variable has no parent, consider one null configuration
     */
    public abstract ParentConfiguration getParentConfigurations(int evidence,
        final boolean noParentConfiguration);

    public Iterable<Integer> getEvidences(final int[] pc) {
        return getFunction().getEvidences(this, pc);
    }

    /**
     * Returns log P(descendants evidence | v = state)
     *
     * @param state The variable evidence
     */
    public abstract double getRB1(int state);

    /**
     * Returns the log probability P(v = state | non descendants evidence)
     *
     * @param state The state of v (must be compatible with the evidence if any)
     */
    public abstract double getRB2(int state);

    final List<Double> getRB2() {
        return new AbstractList<Double>() {
            @Override
            public int size() {
                return getNumberOfStates();
            }

            @Override
            public Double get(int index) {
                return getRB2(index);
            }
        };
    }

    /**
     * @return log P(evidence of the variable and its descendants | evidence of non descendants)
     */
    public abstract double getRB3();

    /**
     * Returns log P(evidence of the variable and its descendants | v_pa = pa_e)
     */
    public abstract double getRB4(final int[] pa_e);

    abstract public double getLogConditionalProbability(int e,
        final int[] pa_e);

    abstract public void get_log_conditional_probability_gradient(
        double[] gradient, int e, int[] parent_e);

    /**
     * @param e
     * @param pc
     * @return
     */
    final public double log_probability(int e, ParentConfiguration pc) {
        return log_probability(e, pc.current);
    }

    public static class MaxConfiguration {
        public double logProbability;

        public AbstractVariable variable;

        public int state;

        public ArrayList<MaxConfiguration> subconfigurations = new ArrayList<MaxConfiguration>();
    }

    public double getMaxConfiguration(
        final Map<AbstractVariable, Integer> states) {
        final MaxConfiguration[] configurations = computeMaxConfiguration();
        MaxConfiguration argmax = null;

        for (MaxConfiguration c : configurations)
            if (argmax == null || c.logProbability > argmax.logProbability)
                argmax = c;

        assert argmax != null;

        addStates(states, argmax);
        return argmax.logProbability;
    }

    final private static void addStates(Map<AbstractVariable, Integer> states,
        MaxConfiguration configuration) {
        states.put(configuration.variable, configuration.state);
        for (MaxConfiguration c : configuration.subconfigurations)
            addStates(states, c);
    }

    abstract public MaxConfiguration[] computeMaxConfiguration();

    /**
     * Checks if the associated CP is mutable
     */
    abstract public boolean getIsMutable();

    /**
     * Invalidate all the cached values
     */
    public void invalidate(boolean up, boolean down) {
    }

    /**
     * Return an index for a given parent configuration
     *
     * @param parentConfiguration
     * @return
     */
    protected final int getPosition(int[] parentConfiguration) {
        assert (parentConfiguration != null);
        int i = 0;
        int k = 1;
        int position = 0;
        for (AbstractVariable parent : getParents()) {
            position = position + k * parentConfiguration[i];
            i++;
            k = parent.getNumberOfStates();
        }
        return position;
    }

    /**
     * Verify the integrity of the variable
     *
     * @return
     */
    abstract public void verify() throws IntegrityException;

    public void read(DataInput in, Map<Integer, CPAbstract> functions) throws IOException {
        name = in.readUTF();
    }

    /**
     * Serialize a variable
     *
     * @param out
     * @param functions
     * @throws IOException
     */
    public void write(DataOutput out, Map<CPAbstract, Integer> functions) throws IOException {
        out.writeUTF(name);
    }

}

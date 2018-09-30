package bpiwowar.ml.bn;

import bpiwowar.io.LoggerOutputStream;
import bpiwowar.ml.IntegrityException;
import bpiwowar.utils.maths.LogSumExpLog;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author bpiwowar
 */
public class CPTable extends CPAbstract {
    protected int numberOfStates;

    private final Logger logger = Logger.getLogger(CPTable.class);

    static protected final int FORBIDDEN_POSITION = -1;

    static protected final int ONLY_POSITION = -2;

    public CPTable(String name, int numberOfStates, int numberOfParentStates) {
        super(name, numberOfParentStates * numberOfStates);
        this.numberOfStates = numberOfStates;

        // Random initialisation
        Random r = new Random();
        for (int i = 0, N = getNumberOfParameters(); i < N; i += numberOfStates) {
            double s = 0;
            for (int j = 0; j < numberOfStates; j++) {
                final double d = r.nextDouble();
                parameters[i + j] = d;
                s += d;
            }

            for (int j = 0; j < numberOfStates; j++)
                parameters[i + j] = BayesianNetworks.logNormalize(parameters[i
                    + j]
                    / s);
        }
    }

    public CPTable(int numberOfStates, double[] parameters,
        boolean logParameters) {
        super(parameters);
        this.numberOfStates = numberOfStates;
        if (!logParameters)
            for (int i = 0; i < parameters.length; i++)
                parameters[i] = BayesianNetworks.logNormalize(parameters[i]);
    }

    public CPTable(AbstractVariable v) {
        this.numberOfStates = v.getNumberOfStates();

        int N = v.getNumberOfStates();
        for (AbstractVariable parent : v.getParents())
            N *= parent.getNumberOfStates();
        setNumberOfParameters(N);
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#setParameters(double[])
     */
    @Override
    public void setParameters(double[] ds) {
        super.setParameters(ds);
        for (int i = 0, N = getNumberOfParameters(); i < N; i += numberOfStates) {
            LogSumExpLog s = new LogSumExpLog();
            for (int j = 0; j < numberOfStates; j++)
                s.addLogValue(parameters[i + j]);
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
    public double log_probability(AbstractVariable variable, int value,
        int[] parents_value) {
        final int position = getPosition(variable, value, parents_value);

        if (position == FORBIDDEN_POSITION)
            return BayesianNetworks.MINLOGPROBA;
        if (position == ONLY_POSITION)
            return 0.;
        try {
            return parameters[position];
        }
        catch (java.lang.ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("For " + this + ": "
                + value + " " + Arrays.toString(parents_value) + ", pos. "
                + position + " vs " + parameters.length);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.Function#analytical_resolve(double[])
     */
    @Override
    protected void doAnalyticalResolve(int k, double[] theta) {
        if (partials == null)
            throw new IntegrityException("Partials are null");

        if (logger.isDebugEnabled()) {
            PrintStream out = new PrintStream(new LoggerOutputStream(logger, Level.DEBUG));
            out.println("Table CP partials (" + name + ")");
            for (int i = 0, N = getNumberOfParameters(); i < N; i += numberOfStates) {
                out.print("P(v/" + (i / numberOfStates) + ") = {");
                for (int j = 0; j < numberOfStates; j++) {
                    if (j > 0)
                        out.print(", ");
                    out.print(partials[i + j]);
                }
                out.println("}");

            }
        }

        // We just normalize for the different states of the variable
        for (int i = 0, N = getNumberOfParameters(); i < N; i += numberOfStates) {
            double s = 0;
            for (int j = 0; j < numberOfStates; j++)
                s += partials[i + j];
            if (s == 0)
                for (int j = 0; j < numberOfStates; j++)
                    theta[i + j + k] = BayesianNetworks
                        .logNormalize(1. / numberOfStates);
            else
                for (int j = 0; j < numberOfStates; j++)
                    theta[i + j + k] = BayesianNetworks.logNormalize(partials[i
                        + j]
                        / s);

        }

        partials = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.ConditionalProbability#update_partials(yrla.ml.bn.Variable,
     *      int, int[], double)
     */
    @Override
    protected void update_partials(boolean analytical, AbstractVariable bv,
        int evidence, int[] parentConfiguration, double pcond, double weight) {
        final int position = getPosition(bv, evidence, parentConfiguration);
        // Just add the observation
        if (position >= 0)
            partials[position] += weight * pcond;
        // System.err.println("UPDATE PARTIAL of " + bv + " by " + pcond);
    }

    /**
     * Returns the position of the variable
     *
     * @param variable
     * @param evidence
     * @param parentConfiguration
     * @return -2 if it is (by grammar) the only position for the parent configuration
     */
    protected int getPosition(final AbstractVariable variable, int evidence,
        int[] parentConfiguration) {
        int i = 0;
        int k = variable.getNumberOfStates();
        for (AbstractVariable parent : variable.getParents()) {
            evidence = evidence + k * parentConfiguration[i];
            i++;
            k = parent.getNumberOfStates();
        }
        return evidence;
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.ConditionalProbability#get_a_priori()
     */
    @Override
    protected double[] get_a_priori() {
        if (parameters.length == numberOfStates)
            return parameters;
        return super.get_a_priori();
    }

    @Override
    public void print(PrintStream out, final double[] theta) {
        out.println("Table CP (" + name + ")");
        for (int i = 0, N = getNumberOfParameters(); i < N; i += numberOfStates) {
            out.print("P(v/" + (i / numberOfStates) + ") = {");
            for (int j = 0; j < numberOfStates; j++) {
                if (j > 0)
                    out.print(", ");
                out.print(Math.exp(theta[i + j]));
            }
            out.println("}");

        }
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#verify(yrla.ml.bn.AbstractVariable)
     */
    @Override
    public void verify(AbstractVariable variable) throws IntegrityException {
        int N = 1;
        for (AbstractVariable p : variable.getParents())
            N *= p.getNumberOfStates();
        if (numberOfStates != variable.getNumberOfStates())
            throw new IntegrityException();
        if (numberOfStates * N != getNumberOfParameters())
            throw new IntegrityException();
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#write(java.io.ObjectOutputStream)
     */
    @Override
    public void write(DataOutput out) throws IOException {
        super.write(out);
        out.writeInt(numberOfStates);
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#read(java.io.ObjectInputStream)
     */
    @Override
    public void read(DataInput in) throws IOException {
        super.read(in);
        numberOfStates = in.readInt();

    }

}

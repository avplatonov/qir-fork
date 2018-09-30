/**
 *
 */
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
 * @author bpiwowar
 */
public class CPModulo extends CPAbstract {
    int nbMergedStates;

    int modulo;

    {
        setIsMutable(false);
    }

    /**
     * @param name The name of the variable
     * @param numberOfParentStates Number of states (must be a multiple of the modulo)
     * @param numberOfStates Number of states of the target variable (i.e. modulo)
     */
    public CPModulo(final String name, int numberOfParentStates,
        int numberOfStates) {
        super(name, numberOfStates);
        if (numberOfParentStates % numberOfStates != 0)
            throw new IllegalArgumentException(String.format(
                "The number of parent states (%d) must "
                    + "be a multiple of the modulo %d",
                numberOfParentStates, numberOfStates));
        this.nbMergedStates = numberOfParentStates / numberOfStates;
        this.modulo = numberOfStates;
    }

    public CPModulo() {
    }

    /**
     * @param name
     * @param v An example variable for this modulo (the parent must already be connected)
     */
    public CPModulo(String name, Variable v) {
        this(name, v.getParent(0).getNumberOfStates(), v.getNumberOfStates());
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#print(java.io.PrintStream, double[])
     */
    @Override
    public void print(PrintStream out, double[] theta) {
        out.format("Modulo %s (%d to %d)%n", name, nbMergedStates * modulo, modulo);
    }

    /*
     * (non-Javadoc)
     *
     * @see yrla.ml.bn.CPAbstract#log_probability(yrla.ml.bn.AbstractVariable,
     *      int, int[])
     */
    @Override
    public double log_probability(AbstractVariable v, int s, int[] pc) {
        if (pc[0] % modulo == s)
            return 0.;
        return BayesianNetworks.MINLOGPROBA;
    }

    public Iterable<Integer> getEvidences(final int[] pc) {
        return new Iterable<Integer>() {
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int e = pc[0] % modulo;

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    public Integer next() {
                        final int e2 = e;
                        e = noEvidence;
                        return e2;
                    }

                    public boolean hasNext() {
                        return e != noEvidence;
                    }

                };
            }

        };
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#getParentConfigurations(yrla.ml.bn.AbstractVariable, int, boolean)
     */
    @Override
    public ParentConfiguration getParentConfigurations(AbstractVariable v, final int evidence,
        boolean noParentConfiguration) {
        final int max = nbMergedStates * modulo;
        assert v.getParent(0).getNumberOfStates() == max;
        final AbstractVariable p = v.getParent(0);

        return new ParentConfiguration() {
            {
                current = new int[1];
                if (evidence != noEvidence) {
                    current[0] = evidence;
                }
                else {
                    current[0] = p.initialize();
                }
            }

            @Override
            public void next() {
                if (current == null)
                    throw new NoSuchElementException("");
                if (evidence != noEvidence)
                    current[0] += modulo;
                else
                    current[0]++;

                if (current[0] >= max)
                    current = null;
            }
        };
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.Variable#verify(Variable v)
     */
    @Override
    public void verify(final AbstractVariable v) throws IntegrityException {
        if (v.getParent(0).getNumberOfStates() != modulo * nbMergedStates)
            throw new IntegrityException();
        if (v.getNumberOfStates() != modulo)
            throw new IntegrityException();
    }

    /* (non-Javadoc)
     * @see yrla.ml.bn.CPAbstract#read(java.io.ObjectInputStream)
     */
    @Override
    public void read(DataInput in) throws IOException {
        super.read(in);
        nbMergedStates = in.readInt();
        modulo = in.readInt();
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeInt(nbMergedStates);
        out.writeInt(modulo);
    }
}

package bpiwowar.ml.bn;

import bpiwowar.ml.IntegrityException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * A list of bayesian functions In order to allow automatic loading/saving of functions (and their parameters), new
 * functions must be defined in loading/saving methods.
 */
public class Functions implements Iterable<CPAbstract> {
    final static Logger logger = Logger.getLogger(Functions.class);

    /** The number of parameters */
    int numberOfParameters;

    /**
     * indices of parameters List of pointer from function number to the rank in the temporary double vector Its value
     * is null
     */
    int[] listOfParameters;

    /** Functions */
    Map<CPAbstract, Integer> functionId = new HashMap<CPAbstract, Integer>();

    /** The list of bayesian functions */
    List<CPAbstract> functions = new ArrayList<CPAbstract>();

    /**
     * Default constructor
     */
    public Functions() {
    }

    /**
     * Construct functions from network
     *
     * @param network
     */
    public Functions(TreeNetwork network) {
        for (AbstractVariable v : network.getVariables()) {
            final CPAbstract f = v.getFunction();
            if (f != null)
                add(f);
        }
    }

    /** Update the index & the number of parameters */
    protected void update() {
        if (listOfParameters == null) {
            listOfParameters = new int[functions.size()];
            numberOfParameters = 0;
            // We compute the number of parameters
            for (int i = 0; i < size(); i++) {
                CPAbstract f = functions.get(i);
                listOfParameters[i] = numberOfParameters;
                if (f != null) {
                    numberOfParameters += f.getNumberOfParameters();
                }
                else {
                    logger.warn("Warning : function no" + i + " is not valid");
                }
            }
        }
    }

    /** Invalidate the valid state of listOfParameters */
    protected void invalidate() {
        if (listOfParameters != null) {
            listOfParameters = null;
        }
    }

    /**
     * @throws out_of_range if this function is not in the set
     */
    int getIndex(CPAbstract f) {
        Integer i = functionId.get(f);
        if (i == null)
            throw new IntegrityException("Index out of bounds in isValid");
        update();
        return listOfParameters[i];
    }

    /** Returns the number of functions */
    public int size() {
        return functions.size();
    }

    /** Returns the number of parameters */
    public int getNumberOfParameters() {
        update();
        return numberOfParameters;
    }

    /** Copy the parameters into a double vector (that <b>must</b> be allocated) */
    public void copyParameters(double[] d) {
        update();
        for (int i = 0; i < size(); i++) {
            CPAbstract f = functions.get(i);
            if (f != null) {
                final double[] parameters = f.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    d[listOfParameters[i] + j] = parameters[j];
                }
            }
        }
    }

    /** Copy parameters from a double array */
    public void setParameters(final double[] d) {
        update();
        for (int i = 0; i < functions.size(); i++) {
            functions.get(i).copy_parameters(listOfParameters[i], d);
        }
    }

    /** Prints the functions */
    public void print(PrintStream out) {
        out.println("Parameters");
        for (CPAbstract f : functions)
            if (f != null)
                f.print(out);
    }

    /** Add a bayesian function to this set (if it does not exist) */
    void add(final CPAbstract bf) {
        invalidate();
        if (bf == null)
            return;
        if (functionId.get(bf) != null)
            return;
        functionId.put(bf, functions.size());
        functions.add(bf);
        numberOfParameters += bf.getNumberOfParameters();
    }

    /* Return a specific function */
    CPAbstract getFunction(int i) {
        return functions.get(i);
    }

    /* Is a function valid */
    boolean isValid(int i) {
        return functions.get(i) != null;
    }

    /** \name Partials */
    public void init_partials(boolean analytical) {
        for (int j = 0; j < size(); j++) {
            CPAbstract f = functions.get(j);
            if (f != null && f.isMutable())
                f.initPartials(analytical);
        }
    }

    /**
     * End the computation of partials
     *
     * @param gradient A gradient vector to update (can be null)
     */
    public void end_partials(double[] gradient) {
        update();
        for (int j = 0; j < size(); j++) {
            CPAbstract f = getFunction(j);
            if (f != null && f.isMutable())
                f.end_partials(gradient, listOfParameters[j]);
        }
    }

    /**
     * Analytical resolve of the derivates
     *
     * @param theta the parameters (null if the function has to store the result directly)
     */
    public void analytical_resolve(double[] theta) {
        update();
        for (int j = 0; j < size(); j++) {
            CPAbstract f = getFunction(j);
            if (f.isMutable()) {
                f.analyticalResolve(listOfParameters[j], theta);
            }
        }
    }

    /**
     * @param functions A list of functions to add
     */
    public void add(CPAbstract... functions) {
        for (CPAbstract f : functions)
            add(f);
    }

    /**
     * Update partial with already computed ones
     *
     * @param analytical
     * @param values
     */
    public void updatePartials(boolean analytical, double[] values) {
        int size = 0;
        for (int j = 0; j < size(); j++) {
            CPAbstract f = getFunction(j);
            if (f.isMutable()) {
                f.updatePartials(analytical, size, values);
                size += f.partials.length;
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<CPAbstract> iterator() {
        return functions.iterator();
    }

    public int getPartialSize() {
        int size = 0;
        for (CPAbstract f : functions)
            if (f != null && f.partials != null)
                size += f.partials.length;
        return size;
    }

    /**
     * @param partials
     */
    public void copyPartials(double[] partials) {
        int size = 0;
        for (CPAbstract f : functions) {
            if (f != null && f.partials != null) {
                for (int i = 0; i < f.partials.length; i++)
                    partials[i + size] = f.partials[i];
                size += f.partials.length;
            }
        }
    }

}

/**
 *
 */
package bpiwowar.ml;

import java.util.List;

/**
 * @author bpiwowar
 */
abstract public class Function {
    /** Get the space dimension */
    public abstract int get_number_of_dimensions();

    /**
     * Update the gradient for a given example
     */
    public abstract void compute_gradient(final double[] x, double[] partials, final List<Example> examples, int N);

    /** Returns the value of the function for a given value & examples */
    public abstract double evaluate(final double[] x, final List<Example> examples, int N);

}

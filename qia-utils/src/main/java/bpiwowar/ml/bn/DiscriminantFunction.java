package bpiwowar.ml.bn;

import bpiwowar.ml.Example;
import bpiwowar.ml.bn.BayesianNetworks.ErrorFunction;
import java.util.List;

/**
 * @author Benjamin Piwowarski
 */

class DiscriminantFunction extends bpiwowar.ml.Function {
    Functions functions;

    ErrorFunction error_function;

    public DiscriminantFunction(ErrorFunction error_function) {
        this.error_function = error_function;
    }

    public double evaluate(final double[] x, final List<Example> examples,
        int wanted) {
        double[] old = new double[functions.getNumberOfParameters()];
        functions.copyParameters(old);
        functions.setParameters(x);
        int N = examples.size();
        double s = 0.;
        for (int i = 0; i < N; i++) {
            if (wanted < N && wanted != i)
                continue;
            final Network bn = (Network)examples.get(i);
            bn.invalidate();
            double error;
            error = bn.getError(error_function);
            // std::cerr + "Error of network " + &bn + " of size " + bn.size() +
            // " is " + x + " (" + y + ")" + "n";
            bn.invalidate();
            s += error;
        }
        functions.setParameters(old);
        return s;
    }

    public int get_number_of_dimensions() {
        return functions.getNumberOfParameters();
    }

    public void compute_gradient(final double[] x, double[] partials,
        final List<Example> examples, int wanted) {
        double[] old = new double[functions.getNumberOfParameters()];
        functions.copyParameters(old);
        functions.setParameters(x);

        int N = functions.getNumberOfParameters();
        for (int i = 0; i < N; i++)
            partials[i] = 0.;

        functions.init_partials(false);

        for (int i = 0; i < examples.size(); i++) {
            if (wanted < examples.size() && wanted != i)
                continue;
            final Network bn = (Network)examples.get(i);
            bn.invalidate();
            bn.compute_discriminant_partials(error_function);
        }

        functions.end_partials(partials);

        for (int i = 0; i < examples.size(); i++) {
            if (wanted < examples.size() && wanted != i)
                continue;
            final Network bn = (Network)examples.get(i);
            bn.invalidate();
        }
        functions.setParameters(old);
    }

    static class QuadraticError extends ErrorFunction {
        boolean needs_parents() {
            return false;
        }

        /* (non-Javadoc)
         * @see yrla.ml.bn.BayesianNetworks.ErrorFunction#get_partial(int, int[], yrla.ml.bn.AbstractVariable, java.util.List, double[])
         */
        @Override
        double get_partial(int e, int[] e_p, AbstractVariable v, List<Double> log_output, double[] wanted_output) {
            return v.get_output_ratio() * 2.
                * (Math.exp(log_output.get(e)) - wanted_output[e]);
        }

        /* (non-Javadoc)
         * @see yrla.ml.bn.BayesianNetworks.ErrorFunction#get_value(int, int[], yrla.ml.bn.AbstractVariable, java.util.List, double[])
         */
        @Override
        double get_value(int e, int[] e_p, AbstractVariable v, List<Double> log_output, double[] wanted_output) {
            return v.get_output_ratio()
                * sqr(Math.exp(log_output.get(e)) - wanted_output[e]);
        }
    }

    ;

    // Kullback-Leibler
    static class Kullback_Leibler extends ErrorFunction {
        boolean needs_parents() {
            return false;
        }

        double get_partial(int e, final int[] ev, final AbstractVariable v,
            List<Double> log_output, final double[] wanted_output) {
            return -v.get_output_ratio()
                * (wanted_output[e] / Math.exp(log_output.get(e)));
        }

        double get_value(int e, final int[] ev, final AbstractVariable v,
            final List<Double> log_output, final double[] wanted_output) {
            // std::cerr + "Wanted = " + wanted_output[e] + ", got " + exp(
            // log_output.get(e)) + "n";
            if (wanted_output[e] == 0.)
                return 0.;
            return -v.get_output_ratio() * (wanted_output[e] * log_output.get(e));
        }
    }

}

package bpiwowar.ml.bn;

import bpiwowar.ml.Example;
import bpiwowar.ml.bn.BayesianNetworks.ErrorFunction;
import java.util.List;

/**
 * A discriminant function with support for any (local) error function
 */
class GenericDiscriminantFunction extends bpiwowar.ml.Function {
    private Functions functions;

    static abstract class GenericExample extends Example {
        public abstract ErrorFunction get_error_function();

        public abstract Network get_network();
    }

    ;

    public void compute_gradient(final double[] x, double[] partials, final List<Example> examples, int wanted) {
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
            final GenericExample example = (GenericExample)examples.get(i);
            final Network bn = example.get_network();
            bn.invalidate();
            ErrorFunction errorf = example.get_error_function();
            bn.compute_discriminant_partials(errorf);
        }

        functions.end_partials(partials);

        for (int i = 0; i < examples.size(); i++) {
            if (wanted < examples.size() && wanted != i)
                continue;
            final GenericExample example = (GenericExample)examples.get(i);
            final Network bn = example.get_network();
            bn.invalidate();
        }
        functions.setParameters(old);
    }

    public double evaluate(final double[] x, final List<Example> examples, int wanted) {
        double[] old = new double[functions.getNumberOfParameters()];
        functions.copyParameters(old);
        functions.setParameters(x);
        int N = examples.size();
        double s = 0.;
        for (int i = 0; i < N; i++) {
            if (wanted < N && wanted != i)
                continue;
            final GenericExample example = (GenericExample)examples.get(i);
            final Network bn = example.get_network();
            bn.invalidate();
            double error;
            ErrorFunction errorf = example.get_error_function();
            error = bn.getError(errorf);
            bn.invalidate();
            s += error;
        }
        functions.setParameters(old);
        return s;
    }

    public int get_number_of_dimensions() {
        return functions.getNumberOfParameters();
    }

}



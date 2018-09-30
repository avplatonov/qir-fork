/**
 *
 */
package bpiwowar.ml.rankboost;

import java.io.PrintStream;

public class ThresholdClassifier<T extends NumericFeatures> implements Classifier<T> {
    private static final long serialVersionUID = -3352258149900209943L;
    private int index;
    private double qdef;
    private double theta;
    private String name;

    ThresholdClassifier(String name, int index, double qdef, double theta) {
        this.name = name;
        this.index = index;
        this.qdef = qdef;
        this.theta = theta;
    }

    public double value(T example) {
        double x = example.get(index);
        if (Double.isNaN(x))
            return qdef;
        if (x > theta)
            return 1;
        return 0;
    }

    /* (non-Javadoc)
     * @see bpiwowar.ml.rankboost.Classifier#print(java.io.PrintStream)
     */
    public void print(PrintStream out) {
        out.format("[[ f(%s) >  %f; %f ]]", name, theta, qdef);
    }
}

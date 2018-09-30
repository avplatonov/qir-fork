/**
 *
 */
package bpiwowar.ml.rankboost;

import java.io.PrintStream;
import java.io.Serializable;

class WeightedClassifier<T> implements Classifier<T>, Serializable {

    private static final long serialVersionUID = 7760835294258898562L;

    /** Alpha is a coefficient between 0 and 1 */
    double alpha;

    Classifier<T> classifier;

    WeightedClassifier(Classifier<T> c, double alpha) {
        this.classifier = c;
        this.alpha = alpha;
    }

    public double value(T example) {
        return alpha * classifier.value(example);
    }

    public void print(PrintStream out) {
        out.format("%f * ", alpha);
        classifier.print(out);
    }

}

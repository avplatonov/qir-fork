package bpiwowar.ml.rankboost;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;

public class WeightedSumClassifier<T> implements Classifier<T>, Serializable {
    private static final long serialVersionUID = -897958579594826292L;

    ArrayList<WeightedClassifier<T>> classifiers = new ArrayList<WeightedClassifier<T>>();

    double max = 0;
    double min = 0;

    void add(WeightedClassifier<T> classifier) {
        classifiers.add(classifier);
        if (classifier.alpha > 0)
            max += classifier.alpha;
        else
            min -= classifier.alpha;
    }

    public double value(T example) {
        double x = 0;
        for (Classifier<T> classifier : classifiers)
            x += classifier.value(example);
        return (x - min) / (max - min);
    }

    /* (non-Javadoc)
     * @see bpiwowar.ml.rankboost.Classifier#print(java.io.PrintStream)
     */
    public void print(PrintStream out) {
        int i = 1;
        for (Classifier<T> c : classifiers) {
            out.format("Classifier %d: ", i);
            c.print(out);
            out.println();
            i++;
        }
    }
}

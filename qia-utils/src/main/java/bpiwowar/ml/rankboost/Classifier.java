package bpiwowar.ml.rankboost;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * @author bpiwowar
 */
public interface Classifier<T> extends Serializable {

    /**
     * @param object
     * @return
     */
    double value(T example);

    /**
     * Print the classifier
     *
     * @param out
     */
    void print(PrintStream out);

}

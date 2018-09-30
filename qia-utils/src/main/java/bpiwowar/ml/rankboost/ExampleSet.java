/**
 *
 */
package bpiwowar.ml.rankboost;

/**
 * @author bpiwowar
 */
public interface ExampleSet<T> extends Iterable<T> {

    /**
     * @return
     */
    int size();

    /**
     * @param i
     * @return
     */
    T get(int i);

}

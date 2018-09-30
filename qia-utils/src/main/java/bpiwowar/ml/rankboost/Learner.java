/**
 *
 */
package bpiwowar.ml.rankboost;

/**
 * @author bpiwowar
 */
public interface Learner<T> {

    /**
     * @param set
     */
    void init(ExampleSet<T> set);

    /**
     * @param boostB
     * @return
     */
    Classifier<T> learn(PotentialDistribution potential);

}

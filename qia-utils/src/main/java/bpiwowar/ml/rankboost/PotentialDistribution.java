/**
 *
 */
package bpiwowar.ml.rankboost;

/**
 * @author bpiwowar
 */
public interface PotentialDistribution {
    public double getPotential(int index);

    /**
     * @return
     */
    public double getTotalPotential();
}

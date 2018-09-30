package bpiwowar.maths.eigen.selector;

import bpiwowar.log.Logger;
import bpiwowar.maths.eigen.RankOneUpdate.EigenList;

/**
 * Select the eigenvalues which are more than epsilon * max(value).
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class ThresholdSelector extends Selector {
    final static private Logger logger = Logger.getLogger();

    private static final long serialVersionUID = 1L;
    final static double DEFAULT_EPSILON = Math.pow(2.0, -52.0);
    boolean absolute = true;
    double epsilon;

    public ThresholdSelector() {
        this(DEFAULT_EPSILON, true);
    }

    public ThresholdSelector(double eps) {
        this(eps, true);
    }

    public ThresholdSelector(boolean absolute) {
        this(DEFAULT_EPSILON, absolute);
    }

    /**
     * @param epsilon
     * @param absolute If one wants to select based on the absolute value
     */
    public ThresholdSelector(double epsilon, boolean absolute) {
        this.epsilon = epsilon;
        this.absolute = absolute;
    }

    @Override
    public void selection(final EigenList eigenValues) {
        if (absolute) {
            // Remove based on absolute values
            final double max = Math.max(Math.abs(eigenValues.get(0)), Math
                .abs(eigenValues.get(eigenValues.size() - 1)));
            final double tolerance = (eigenValues.size() + 1d) * max * epsilon;

            for (int i = eigenValues.size(); --i >= 0; )
                if (Math.abs(eigenValues.get(i)) < tolerance)
                    eigenValues.remove(i);
        }
        else {
            // Remove based on real values
            final double max = Math.abs(eigenValues.get(0));
            final double tolerance = (eigenValues.size() + 1d) * max * epsilon;

            for (int i = eigenValues.size(); --i >= 0; )
                if (eigenValues.get(i) < tolerance) {
                    logger.debug("Removing %d with l=%e [< %e]", i, eigenValues.get(i), tolerance);
                    eigenValues.remove(i);
                }
        }

    }
}

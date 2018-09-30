package bpiwowar.maths.eigen.selector;

import bpiwowar.maths.eigen.RankOneUpdate.EigenList;
import java.io.Serializable;

/**
 * An eigenvalue selector
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public abstract class Selector implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * @param eigenValues The ordered list of eigenvalues
     */
    public abstract void selection(final EigenList eigenValues);
}

package bpiwowar.maths.eigen.selector;

import bpiwowar.maths.eigen.RankOneUpdate.EigenList;

/**
 * Performs a series of selections
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class ChainSelector extends Selector {
    private static final long serialVersionUID = 1L;

    private final Selector[] selectors;

    public ChainSelector(Selector... selectors) {
        this.selectors = selectors;
    }

    @Override
    public void selection(final EigenList eigenValues) {
        for (Selector s : selectors)
            s.selection(eigenValues);
    }

}

package bpiwowar.maths.eigen.selector;

import bpiwowar.collections.BoundedSizeHeap;
import bpiwowar.log.Logger;
import bpiwowar.maths.eigen.RankOneUpdate.EigenList;
import bpiwowar.utils.Heap;

/**
 * Select based on the maximum rank - will select the highest eigenvalues
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class MaximumRankSelector extends Selector {
    private static final long serialVersionUID = 1L;

    final static private Logger logger = Logger.getLogger();

    int maxRank;

    private boolean absolute = false;

    /**
     * Will select the highest eigenvalues
     *
     * @param maxRank
     */
    public MaximumRankSelector(int maxRank) {
        this(maxRank, false);
    }

    /**
     * Will select the highest (absolute) eigenvalues
     *
     * @param maxRank
     * @param absolute
     */
    public MaximumRankSelector(int maxRank, boolean absolute) {
        super();
        this.maxRank = maxRank;
        this.absolute = true;
    }

    static class Element extends
        Heap.DefaultElement<MaximumRankSelector.Element> {
        int index;
        double lambda;

        public Element(int index, double lambda) {
            super();
            this.index = index;
            this.lambda = lambda;
        }

        @Override
        public int compareTo(MaximumRankSelector.Element o) {
            return Double.compare(o.lambda, lambda);
        }

    }

    public void setMaximum(int maxRank) {
        this.maxRank = maxRank;
    }

    @Override
    public void selection(final EigenList eigenValues) {
        int delta = eigenValues.rank() - maxRank;
        if (delta <= 0) {
            logger
                .debug(
                    "Not removing eigenvalue since current rank (%d) <= max rank (%d)",
                    eigenValues.rank(), maxRank);
            return;
        }

        logger.debug("Removing %d eigenvalues out of %d [max rank is %d]",
            delta, eigenValues.rank(), maxRank);

        BoundedSizeHeap<MaximumRankSelector.Element> heap = BoundedSizeHeap
            .create(delta);
        for (int i = eigenValues.size(); --i >= 0; )
            if (eigenValues.isSelected(i)) {
                final double lambda = absolute ? Math.abs(eigenValues.get(i))
                    : eigenValues.get(i);
                logger.debug("Adding %g to the heap", lambda);
                heap.add(new Element(i, lambda));
            }

        for (MaximumRankSelector.Element e : heap) {
            logger.debug("Removing %d with l=%g", e.index, e.lambda);
            eigenValues.remove(e.index);
        }

    }

}

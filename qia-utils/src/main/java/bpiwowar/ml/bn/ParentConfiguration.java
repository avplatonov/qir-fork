package bpiwowar.ml.bn;

import java.util.Arrays;

/**
 * @author bpiwowar
 */
abstract public class ParentConfiguration {
    /**
     * Get the current configuration
     */
    public int[] current;

    /**
     * Has a current configuration
     */
    final public boolean hasCurrent() {
        return current != null;
    }

    /**
     * Advance
     */
    public abstract void next();

    /**
     * @param i
     * @return
     */
    final public int get(int i) {
        return current[i];
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Arrays.toString(current);
    }

}

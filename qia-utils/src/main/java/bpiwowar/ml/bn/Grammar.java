/**
 *
 */
package bpiwowar.ml.bn;

/**
 * A grammar
 *
 * @author bpiwowar
 */
interface Grammar {

    /**
     * Allow a peculiar configuration
     *
     * @param e the evidence of the node
     * @param pa_e the evidence of the parent nodes
     */
    boolean allow(int e, int[] pa_e);

}

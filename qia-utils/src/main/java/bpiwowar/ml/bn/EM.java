/**
 *
 */
package bpiwowar.ml.bn;

import bpiwowar.ml.Likelihoods;

/**
 * Implementation of EM for probabilistic models
 *
 * @author bpiwowar
 */
public class EM {

    Functions functions;
    double[] parameters;

    public EM(Functions functions) {
        this.functions = functions;
        parameters = new double[functions.getNumberOfParameters()];
    }

    /**
     * Learn from examples
     *
     * @param examples
     * @return the log likelihood
     */
    public Likelihoods learn(NetworkSet networks) {
        // Initialisation
        functions.init_partials(true);
        Likelihoods statistics = new Likelihoods();
        // Computation of partials
        networks.reset();
        while (networks.hasNext()) {
            Network network = networks.next();
            statistics.add(network);
            network.computePartials(network.get_weigth(), true);
        }

        // Analytical resolve
        functions.analytical_resolve(null);

        // Invalidate the networks
        networks.invalidate();

        // Return the likelihood
        return statistics;
    }

}

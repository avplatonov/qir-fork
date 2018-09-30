package bpiwowar.maths.ml;

import bpiwowar.log.Logger;
import bpiwowar.maths.Misc;
import bpiwowar.maths.random.MultivariateGaussianGenerator;
import bpiwowar.utils.holders.IntHolder;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.NotConvergedException;
import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomGenerator;

/**
 * Gaussian mixture
 *
 * @author Y. Moshfeghi <yashar@dcs.gla.ac.uk>
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class GaussianMixture {
    final static private Logger logger = Logger.getLogger();

    Gaussian[] gaussians;
    double[] pi;

    private int dimension;

    private int nbGaussians;

    /**
     * Build a Gaussian mixture with a given number of components
     *
     * @param dimension
     */
    public GaussianMixture(int nbGaussians, int dimension,
        RandomGenerator random, boolean randomCovariance) {
        this.nbGaussians = nbGaussians;
        this.dimension = dimension;
        randomise(random, randomCovariance);
    }

    /**
     * Random initialisation of a given number of Gaussian in a given dimension. Means and pi coefficients are generated
     * by random, covariance matrices are identity.
     *
     * @param dimension
     * @param nbGaussians
     * @param randomCovariance
     * @return
     */
    public void randomise(RandomGenerator random, boolean randomCovariance) {
        gaussians = new Gaussian[nbGaussians];
        pi = new double[nbGaussians];

        double sumPI = 0;
        for (int k = 0; k < nbGaussians; k++) {
            final Gaussian gc = (gaussians[k] = new Gaussian(dimension));

            sumPI += (pi[k] = random.nextDouble());

            for (int j = 0; j < dimension; j++)
                gc.mean.set(j, random.nextDouble() - 0.5);

            if (randomCovariance) {
                for (int i = 0; i < dimension; i++)
                    for (int j = i; j < dimension; j++) {
                        double r = random.nextDouble();
                        // We can have negative co-variance
                        if (i != j)
                            r = r - 0.5;
                        gc.covariance.set(i, j, r);
                    }
            }
            else
                for (int j = 0; j < dimension; j++)
                    gc.covariance.set(j, j, 1);

        }

        // Normalise the pi coefficients
        for (int k = 0; k < nbGaussians; k++)
            pi[k] /= sumPI;

    }

    /**
     * Initialise the gaussian mixtures setting everything to zero
     *
     * @param nbGaussians
     * @param dimension
     */
    public GaussianMixture(int nbGaussians, int dimension) {
        this.nbGaussians = nbGaussians;
        this.dimension = dimension;
        this.gaussians = new Gaussian[nbGaussians];
        pi = new double[nbGaussians];
        for (int i = 0; i < nbGaussians; i++)
            gaussians[i] = new Gaussian(dimension);
    }

    /**
     * Computes a series of gaussian mixtures and select the one that has the maximum likelihood on a validation set
     *
     * @param maxIterations
     * @param random
     */
    static public EMResult computeBestMixture(int maxGaussians, int dimension,
        Iterable<DenseVector> train, Iterable<DenseVector> validation,
        double epsilon, int maxIterations, MersenneTwister random) {
        EMResult argmax = null;
        if (random == null)
            random = new MersenneTwister();

        // Run EM for each possible dimension and select the best one on the
        // validation set
        for (int nbGaussians = 1; nbGaussians <= maxGaussians; nbGaussians++) {
            logger.info("Training with %d gaussians", nbGaussians);
            EMResult result = computeMLGaussianMixture(nbGaussians, dimension,
                train, epsilon, maxIterations, random);

            IntHolder count = new IntHolder();
            result.testLogLikelihood = result.mixture.computeLikelihood(
                validation, true, count);

            if (argmax == null
                || result.testLogLikelihood > argmax.testLogLikelihood)
                argmax = result;

            logger
                .info(
                    "The log-likelihood on the validation set was %g [%d infinites]",
                    result.testLogLikelihood, count.value);
        }

        return argmax;
    }

    @Override
    public String toString() {
        return String.format(
            "Mixture of gaussian with %d components (dimension %d)",
            nbGaussians, dimension);
    }

    /**
     * @param nbGaussians
     * @param GaussianComponents
     * @param samples
     * @param epsilon
     * @return
     */
    static private EMResult computeMLGaussianMixture(int nbGaussians,
        int dimension, Iterable<DenseVector> samples, double epsilon,
        int maxIterations, RandomGenerator random) {
        GaussianMixture mixture = new GaussianMixture(nbGaussians, dimension,
            random, false);

        double previousLikelihood = Double.NEGATIVE_INFINITY;
        EMResult result = new EMResult(mixture, previousLikelihood);

        int iter = 0;
        do {
            iter++;
            previousLikelihood = result.trainLogLikelihood;
            result = mixture.emStep(result.mixture, samples);
            logger.info("EM step: log-likelihood is %g [delta=%g/%g]",
                result.trainLogLikelihood, result.trainLogLikelihood
                    - previousLikelihood,
                (result.trainLogLikelihood - previousLikelihood)
                    / (double)result.trainingSetSize);
        }
        while (iter < maxIterations
            && (result.trainLogLikelihood - previousLikelihood)
            / (double)result.trainingSetSize > epsilon);

        return result;
    }

    static public class EMResult {
        GaussianMixture mixture;
        double trainLogLikelihood = Double.NaN;
        double testLogLikelihood = Double.NaN;
        public int trainingSetSize;

        public EMResult(GaussianMixture mixture, double logLikelihood) {
            super();
            this.mixture = mixture;
            this.trainLogLikelihood = logLikelihood;
        }
    }

    /**
     * Compute the log likelihood of a set of samples
     *
     * @param components
     * @param samples
     * @return
     */
    public double computeLikelihood(Iterable<DenseVector> samples) {
        return computeLikelihood(samples, false, null);
    }

    /**
     * Compute the log likelihood of a set of samples
     *
     * @param samples
     * @param components
     * @return
     */
    public double computeLikelihood(Iterable<DenseVector> samples,
        boolean removeInfinites, IntHolder count) {
        double logLikelihood = 0;

        if (removeInfinites)
            count.value = 0;

        final int nbGaussians = gaussians.length;
        for (DenseVector sample : samples) {
            double sum = 0;
            for (int k = 0; k < nbGaussians; k++) {
                final double d = pi[k] * gaussians[k].getDensity(sample);

                if (Double.isInfinite(d))
                    if (!removeInfinites)
                        return Double.NEGATIVE_INFINITY;
                    else
                        count.value++;
                else
                    sum += d;
            }

            logLikelihood += Math.log(sum);
        }

        return logLikelihood;
    }

    /**
     * EM algorithm
     *
     * @param mixture The current mixture of gaussian
     * @param trainingSet the training set
     * @return
     */
    private EMResult emStep(GaussianMixture mixture,
        Iterable<DenseVector> trainingSet) {

        // The new gaussian components
        EMResult result = new EMResult(new GaussianMixture(nbGaussians,
            dimension), 0);
        GaussianMixture r = result.mixture;

        double[] gammas = new double[nbGaussians];

        result.trainingSetSize = 0;

        // Go through all Samples
        for (DenseVector sample : trainingSet) {
            // Increase the size
            result.trainingSetSize++;

            // Preparation for Gamma
            double sum = 0;
            for (int k = 0; k < nbGaussians; k++) {
                gammas[k] = pi[k] * mixture.gaussians[k].getDensity(sample);
                sum += gammas[k];
            }

            if (Double.isInfinite(sum)) {
                double n = 0;
                for (int k = 0; k < nbGaussians; k++)
                    if (Double.isInfinite(gammas[k]))
                        n++;
                n = 1. / n;

                for (int k = 0; k < nbGaussians; k++)
                    if (Double.isInfinite(gammas[k]))
                        gammas[k] = n;
                    else
                        gammas[k] = 0;
            }
            if (sum > 0) {
                // update likelohood
                result.trainLogLikelihood += Math.log(sum);

                // normalize gamma
                for (int k = 0; k < nbGaussians; k++)
                    gammas[k] = gammas[k] / sum;
            }
            else
                for (int k = 0; k < nbGaussians; k++)
                    gammas[k] = 1. / (double)nbGaussians;

            // update statistics
            for (int k = 0; k < nbGaussians; k++) {
                // update PI
                r.pi[k] += gammas[k];

                // update Mean
                r.gaussians[k].mean.add(gammas[k], sample);

                // update Covariance
                r.gaussians[k].covariance.rank1(gammas[k], sample);
            }
        }

        // Complete the M-step
        for (int k = 0; k < nbGaussians; k++) {
            // 1 / Nk
            double invNk = 1. / (double)r.pi[k];

            // Mean
            r.gaussians[k].mean.scale(invNk);

            // Covariance
            r.gaussians[k].covariance.scale(invNk);
            r.gaussians[k].covariance.rank1(-1, r.gaussians[k].mean);

            // Check for singularities
            double maxVariance = 0;
            for (int i = 0; i < dimension; i++)
                maxVariance = Math.max(r.gaussians[k].covariance.get(i, i),
                    maxVariance);
            if (maxVariance == 0)
                maxVariance = 1;

            for (int i = 0; i < dimension; i++) {
                if (r.gaussians[k].covariance.get(i, i) == 0) {
                    r.gaussians[k].covariance.set(i, i, maxVariance
                        * Misc.DOUBLE_PRECISION);
                    logger
                        .debug(
                            "%dth gaussian is singular [component %d] - setting it to %e",
                            k, i, maxVariance * Misc.DOUBLE_PRECISION);
                }
            }

            // pi
            r.pi[k] /= (double)result.trainingSetSize;
        }

        return result;
    }

    public void print(PrintWriter out) {
        out.println("--- Mixture of gaussians ---");
        for (int i = 0; i < nbGaussians; i++) {
            out.format("# Component %d (%g)", i, pi[i]);
            out.println(gaussians[i].mean);
            out.println(gaussians[i].covariance);
        }
        out.flush();
    }

    /**
     * A mixture of Gaussians generator
     */
    static class Generator {
        MultivariateGaussianGenerator[] generators;
        double[] cumulativeProbabilities;
        private RandomGenerator random;

        /**
         * Creates a new generator from a set of gaussians and a random generator
         *
         * @throws NotConvergedException If something wrong happens during the Cholesky decomposition
         */
        public Generator(GaussianMixture mixture, RandomGenerator random)
            throws NotConvergedException {
            generators = new MultivariateGaussianGenerator[mixture.nbGaussians];
            cumulativeProbabilities = new double[mixture.dimension];
            this.random = random;
            double sum = 0;
            for (int k = 0; k < mixture.nbGaussians; k++) {
                cumulativeProbabilities[k] = (sum += mixture.pi[k]);
                generators[k] = new MultivariateGaussianGenerator(
                    mixture.gaussians[k].mean,
                    mixture.gaussians[k].covariance, random);
            }
        }

        /**
         * Draw a sample from the mixture
         */
        DenseVector nextSample() {
            double v = random.nextDouble();
            for (int i = 0; i < cumulativeProbabilities.length; i++) {
                if (v <= cumulativeProbabilities[i]) {
                    return generators[i].nextSample();
                }
            }

            assert false;
            return null;
        }
    }

    public static void main(String[] args) throws NotConvergedException,
        FileNotFoundException, IOException, ClassNotFoundException {

        if (args.length > 0) {
            long seed = Long.parseLong(args[0]);
            MersenneTwister random = new MersenneTwister(seed);

            // --- The two parameters are serialised files
            final ObjectInputStream inTrain = new ObjectInputStream(
                new FileInputStream(args[1]));
            Iterable<DenseVector> train = (Iterable<DenseVector>)inTrain
                .readObject();
            inTrain.close();

            final ObjectInputStream inTest = new ObjectInputStream(
                new FileInputStream(args[2]));
            Iterable<DenseVector> validation = (Iterable<DenseVector>)inTest
                .readObject();
            inTest.close();

            EMResult result = computeBestMixture(5, 2, train, validation, 1e-5,
                50, random);

            System.out.println("The best is " + result.mixture);
            result.mixture.print(new PrintWriter(System.out));
        }
        else {
            int nbGaussians = 1;
            int d = 2;
            int seed = -1;

            final MersenneTwister random = new MersenneTwister(seed);
            GaussianMixture mixture = new GaussianMixture(d, nbGaussians,
                random, false);
            Generator generator = new Generator(mixture, random);

            ArrayList<DenseVector> samples = new ArrayList<DenseVector>();
            for (int i = 0; i < 50000; i++) {
                final DenseVector sample = generator.nextSample();
                samples.add(sample);
                // System.out.println(sample);
            }
            System.out.println("--- Learning --- ");

            EMResult result = computeMLGaussianMixture(nbGaussians, d, samples,
                1e-3, 1000, random);

            System.out.println("--- Original --- ");
            for (int i = 0; i < nbGaussians; i++) {
                System.out
                    .println("Mean=\n" + result.mixture.gaussians[i].mean);
                System.out.println("Covariance=\n"
                    + result.mixture.gaussians[i].covariance);
            }

            System.out.println("--- Learned --- ");
            for (int i = 0; i < nbGaussians; i++) {
                System.out
                    .println("Mean=\n" + result.mixture.gaussians[i].mean);
                System.out.println("Covariance=\n"
                    + result.mixture.gaussians[i].covariance);
            }
        }
    }
}

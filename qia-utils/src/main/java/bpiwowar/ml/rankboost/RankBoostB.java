package bpiwowar.ml.rankboost;

/**
 * Binary RankBoost algorithm
 *
 * @author bpiwowar
 */
public class RankBoostB<T> implements PotentialDistribution {
    final double positiveWeight[];

    final double negativeWeight[];

    private Learner<T> learner;

    private ExampleSet<T> set;

    final private WeightedSumClassifier<T> classifier = new WeightedSumClassifier<T>();

    private ExampleSet<T> positive;

    private ExampleSet<T> negative;

    protected double[] initDistribution(final int N) {
        double x[] = new double[N];
        for (int i = 0; i < N; i++)
            x[i] = 1. / (double)N;
        return x;
    }

    public RankBoostB(final Learner<T> weakLearner,
        final ExampleSet<T> positive, final ExampleSet<T> negative) {

        // Initialise the algorithm
        positiveWeight = initDistribution(positive.size());
        negativeWeight = initDistribution(negative.size());
        this.learner = weakLearner;

        this.positive = positive;
        this.negative = negative;

        set = new UnionExampleSet<T>(positive, negative);
        System.err.println("Initialisation of the weak learner");
        weakLearner.init(set);
    }

    final static double EPSILON = 1e-15;

    public boolean learn() {
        // Learn with the new distribution
        Classifier<T> weakClassifier = learner.learn(this);
        if (weakClassifier == null)
            return false;

        // Find the corresponding alpha
        double r = 0;
        for (int i = 0, N = set.size(); i < N; i++) {
            r += getPotential(i) * weakClassifier.value(set.get(i));
//			System.err.format(" pi(x)=%.5f, h(x)=%.5f => r=%.5f%n",
//					getPotential(i), weakClassifier.value(set.get(i)),r);
        }
        if (r > 1 - EPSILON)
            r = 1 - EPSILON;
        else if (r < -1 + EPSILON)
            r = -1 + EPSILON;

        final double alpha = .5 * Math.log((1 + r) / (1 - r));
        final WeightedClassifier<T> singleClassifier = new WeightedClassifier<T>(
            weakClassifier, alpha);
        classifier.add(singleClassifier);

        // Update the distributions
        updateDistribution(1, singleClassifier, positive,
            positiveWeight);
        updateDistribution(-1, singleClassifier, negative,
            negativeWeight);
        return true;
    }

    private void updateDistribution(final double coefficient,
        WeightedClassifier<T> singleClassifier, ExampleSet<T> set,
        double[] weigths) {
        double z = 0;
        int index = 0;
        for (T example : set) {
            final double x = weigths[index]
                * Math.exp(-coefficient * singleClassifier.value(example));
            z += x;
            weigths[index] = x;
            index++;
        }

        for (int i = 0, N = weigths.length; i < N; i++)
            weigths[i] /= z;
    }

    /*
     * (non-Javadoc)
     *
     * @see bpiwowar.ml.rankboost.PotentialDistribution#getPotential(int)
     */
    public double getPotential(int index) {
        if (index < positiveWeight.length)
            return positiveWeight[index];
        return -negativeWeight[index - positiveWeight.length];
    }

    /* (non-Javadoc)
     * @see bpiwowar.ml.rankboost.PotentialDistribution#getTotalPotential()
     */
    public double getTotalPotential() {
        // Each set has a total potential of 1, but of different signs
        return 0;
    }

    /**
     * @return
     */
    public Classifier<T> getClassifier() {
        return classifier;
    }
}

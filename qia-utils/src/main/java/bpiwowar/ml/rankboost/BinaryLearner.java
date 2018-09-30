package bpiwowar.ml.rankboost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static java.lang.Math.abs;

/**
 * @author bpiwowar
 */
public class BinaryLearner<T extends NumericFeatures> implements Learner<T> {

    private ExampleSet<T> set;

    // Number of features
    private final int nbFeatures;

    int[][] orders;
    String[] names;
    boolean cumulative = true;

    public BinaryLearner(int nbFeatures) {
        this.nbFeatures = nbFeatures;
        names = new String[nbFeatures];
        for (int i = 0; i < nbFeatures; i++)
            names[i] = new Integer(i).toString();
        orders = new int[nbFeatures][];
    }

    /**
     * @param objects
     */
    public BinaryLearner(Object[] objects) {
        nbFeatures = objects.length;
        names = new String[nbFeatures];
        for (int i = 0; i < nbFeatures; i++)
            names[i] = objects[i].toString();
        orders = new int[nbFeatures][];
    }

    /*
     * (non-Javadoc)
     *
     * @see bpiwowar.ml.rankboost.WeakLearner#init(bpiwowar.ml.rankboost.ExampleSet)
     */
    public void init(final ExampleSet<T> set) {
        this.set = set;

        class C implements Comparator<Integer> {
            public int index;

            public int compare(Integer o1, Integer o2) {
                double x1 = BinaryLearner.this.set.get(o1).get(index);
                double x2 = BinaryLearner.this.set.get(o2).get(index);
                if (x1 > x2)
                    return -1;
                else if (x1 < x2)
                    return 1;
                return 0;
            }
        }
        ;
        C comparator = new C();

        // Order this set with respect to the different examples
        for (int index = 0; index < nbFeatures; index++) {
            comparator.index = index;
            ArrayList<Integer> list = new ArrayList<Integer>();
            int N = 0;
            for (T x : this.set) {
                if (!Double.isNaN(x.get(index))) {
                    list.add(new Integer(N));
                }
                N++;
            }

            // Sort by descending feature value
            Collections.sort(list, comparator);

            orders[index] = new int[list.size()];
            int[] order = orders[index];
            for (int i = 0; i < list.size(); i++) {
                order[i] = list.get(i);
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see bpiwowar.ml.rankboost.WeakLearner#learn(bpiwowar.ml.rankboost.PotentialDistribution)
     */
    public Classifier<T> learn(PotentialDistribution potential) {
        int[] order;
        double rmax = 0;

        double q;
        int imax = -1;
        double thetamax = 0;
        double qdef = 0;

        double l = 0;
        for (int i = 0; i < nbFeatures; i++) {
            l = 0;
            order = orders[i];
            final int N = order.length;
            if (order.length == 0)
                continue;
            double x = set.get(order[0]).get(i);

            // Compute total potential for the operator
            double totalPotential = 0;
            for (int j = 0; j < N; j++) {
                totalPotential += potential.getPotential(order[j]);
            }

            for (int j = 0; j < N; ) {
                // Update L (5)
                double theta = x;
                do {
                    l += potential.getPotential(order[j]);
                    x = set.get(order[j]).get(i);
//					System.err.format("%s (%d) j=%d=>%d, L=%f, R=%.3f, theta=%.3f / %.3f, rmax=%.3f%n",names[i],
//							i,j,order[j],l,totalPotential,theta,potential.getPotential(order[j]),rmax);
                    j++;
                    assert !Double.isNaN(x);
                }
                while ((j < N) && (theta == x));

                if (j == N)
                    theta = Double.NEGATIVE_INFINITY;
                else
                    theta = x;

                if (abs(l) > abs(l - totalPotential))
                    q = 0;
                else
                    q = 1;

//				System.err.format("%s (%d) L=%f, R=%.3f, theta=%.3f / rmax=%.3f vs %.4f%n",names[i],
//						i,l,totalPotential,theta,rmax,
//						l - q * totalPotential);

                if (abs(l - q * totalPotential) > abs(rmax) && (!cumulative || l > 0)) {
                    rmax = l - q * totalPotential;
                    imax = i;
                    thetamax = theta;
                    qdef = q;
//					System.err.format("# New best: %s with %f; qdef=%f%n", names[imax], rmax, qdef);
                }
            }
        }

        if (imax == -1)
            return null;
//		System.err.format("------------------------- %s%n%n",names[imax]);
        return new ThresholdClassifier<T>(names[imax], imax, qdef, thetamax);
    }

}

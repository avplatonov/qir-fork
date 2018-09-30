package bpiwowar.utils.maths;

import java.util.Random;

import static java.lang.Math.exp;
import static java.lang.Math.log;

/**
 * Dirichlet prior for probability estimation
 *
 * @author bpiwowar
 */
public class DirichletParameters {
    double[] p;
    LogSumExpLog[] estimations;

    static final Random random = new Random(0);

    public DirichletParameters(final double[] p) {
        this.p = p.clone();
        estimations = new LogSumExpLog[p.length];
    }

    public DirichletParameters(final int n) {
        p = new double[n];

        estimations = new LogSumExpLog[n];
        startEM();
        for (int i = 0; i < n; i++)
            estimations[i].addLogValue(random.nextGaussian() / 10f + 0.5);
        endEM();
    }

    public double getLogProbability(final int i) {
        return p[i];
    }

    public void addLogExpectaction(final int i, final double x) {
        estimations[i].addLogValue(x);
    }

    @Override
    public String toString() {
        String s = "<";
        for (int i = 0; i < p.length; i++)
            s += (i == 0 ? "" : ",") + String.format("%.3f", exp(p[i]));

//		s += " # ";
//		for(int i = 0; i < p.length; i++)
//			s += (i == 0 ? "" : ",") + exp(estimations[i].getLogSum());
        s += ">";
        return s;
    }

    public void startEM() {
        for (int i = 0; i < estimations.length; i++)
            estimations[i] = new LogSumExpLog();
    }

    public void endEM() {
        final LogSumExpLog sum = new LogSumExpLog();
        for (final LogSumExpLog x : estimations)
            sum.addLogValue(x.getLogSum());
        final double s = sum.getLogSum();
        if (Double.isInfinite(s) || Double.isNaN(s))
            for (int i = 0; i < p.length; i++)
                p[i] = Math.normaliseLog(-log(p.length));
        else
            for (int i = 0; i < p.length; i++)
                p[i] = Math.normaliseLog(estimations[i].getLogSum() - s);
    }

    /**
     * @return
     */
    public double getEntropy() {
        double entropy = 0;
        for (double element : p)
            if (element != Double.NEGATIVE_INFINITY)
                entropy += element * exp(element);
        return entropy;
    }

    /**
     * @return
     */
    public int getArgMax() {
        int argmax = 0;
        double max = getLogProbability(0);
        for (int x = 1; x < p.length; x++)
            if (max < getLogProbability(x)) {
                argmax = x;
                max = getLogProbability(x);
            }
        return argmax;
    }

    /**
     * @param i
     * @return
     */
    public double getLogExpectation(final int i) {
        return estimations[i].getLogSum();
    }
}

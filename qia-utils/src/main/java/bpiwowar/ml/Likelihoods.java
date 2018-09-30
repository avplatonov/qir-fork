/**
 *
 */
package bpiwowar.ml;

import bpiwowar.ml.bn.Network;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static java.lang.Math.exp;

public final class Likelihoods {
    double log_likelihood = 0.;
    double weighted_log_likelihood = 0d;

    double total = 0d;
    double weighted_total = 0d;

    public Likelihoods() {
    }

    public void read(DataInput in) throws IOException {
        log_likelihood = in.readDouble();
        weighted_log_likelihood = in.readDouble();
        total = in.readDouble();
        weighted_total = in.readDouble();
    }

    public void write(DataOutput out) throws IOException {
        out.writeDouble(log_likelihood);
        out.writeDouble(weighted_log_likelihood);
        out.writeDouble(total);
        out.writeDouble(weighted_total);
    }

    public double getLogLikelihood(boolean weighted) {
        return weighted ? weighted_log_likelihood : log_likelihood;
    }

    public double getAverageLogLikelihood(boolean weighted) {
        return weighted ? weighted_log_likelihood / weighted_total : log_likelihood / total;
    }

    public double getPerplexity(boolean weighted) {
        return exp(-getAverageLogLikelihood(weighted));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Statistics (%.2f): perplexity=%f, ll=%f, average l=%f (nw: %f)",
            weighted_total,
            exp(-weighted_log_likelihood / weighted_total),
            weighted_log_likelihood,
            exp(weighted_log_likelihood / weighted_total),
            exp(log_likelihood / total));
    }

    /**
     * Add statistics for a network
     *
     * @param network
     */
    public void add(Network network) {
        double lp = network.getLogLikelihood();
        log_likelihood += lp;
        weighted_log_likelihood += lp * network.get_weigth();

        total += 1.;
        weighted_total += network.get_weigth();

    }

    public void add(double weight, double logLikelihood) {
        log_likelihood += logLikelihood;
        weighted_log_likelihood += logLikelihood * weight;

        total += 1.;
        weighted_total += weight;
    }

    /**
     * @param statistics
     */
    public void add(Likelihoods o) {
        log_likelihood += o.log_likelihood;
        weighted_log_likelihood += o.weighted_log_likelihood;
        total += o.total;
        weighted_total += o.weighted_total;
    }
}

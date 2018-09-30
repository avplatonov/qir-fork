package bpiwowar.ml.bn;

import bpiwowar.ml.Likelihoods;
import org.junit.Assert;
import org.junit.Test;

public class CPPoissonsTest {

    @Test
    public void learn() {
        final TreeNetwork network = new TreeNetwork();
        final Variable[] vars = {new Variable(2), new Variable(20)};
        vars[1].addParent(vars[0]);
        vars[0].setFunction(new CPTable("prior", 2, 1));
        final CPPoissons poissons = new CPPoissons("psss", 2);
        vars[1].setFunction(poissons);

        for (Variable v : vars)
            network.add(v);

        Functions functions = new Functions(network);
        EM em = new EM(functions);
        final int[][] examples = new int[][] {
            {-1, 1},
            {-1, 19},
        };

        NetworkSet networks = new NetworkSet() {
            int i = 0;

            @Override
            public void reset() {
                i = 0;
            }

            @Override
            public Network next() {
                for (int j = 0; j < vars.length; j++)
                    vars[j].set_evidence(examples[i][j]);
                i++;
                return network;
            }

            @Override
            public boolean hasNext() {
                return i < examples.length;
            }

        };

        Likelihoods old = null, s = null;
        int i = 0;
        do {
            i++;
            em.learn(networks);
            old = s;
            s = networks.getStatistics();
        }
        while (old == null || i > 1000
            || Math.abs(s.getAverageLogLikelihood(true)
            - old.getAverageLogLikelihood(true)) > 1e-5);

        final double[] p = poissons.getParameters();
        Assert.assertTrue(Math.abs(p[0] - 19) < 1e-2 || Math.abs(p[0] - 1) < 1e-2);
        Assert.assertTrue(Math.abs(p[1] - 19) < 1e-2 || Math.abs(p[1] - 1) < 1e-2);

    }

    public static void main(String[] args) {
        new CPPoissonsTest().learn();
    }
}

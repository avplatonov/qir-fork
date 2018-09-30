package bpiwowar.ml.bn;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Math.exp;

/**
 * @author bpiwowar
 */
public class TreeNetworkTest {
    private static final double DELTA = 1e-5;
    TreeNetwork network;
    Variable v1, v2, v3;
    private CPTable f3;
    private CPTable f2;
    private CPTable f1;

    @Before
    public void constructNetwork() {
        network = new TreeNetwork();

        v1 = new Variable("v1", 2);
        v2 = new Variable("v2", 2);
        v3 = new Variable("v3", 2);

        v1.setFunction(f1 = new CPTable(2, new double[] {0.8, 0.2}, false));

        v2.setFunction(f2 = new CPTable(2, new double[] {0.4, 0.6, 0.6, 0.4}, false));
        v2.addParent(v1);

        v3.setFunction(f3 = new CPTable(2, new double[] {0.4, 0.6, 0.5, 0.5}, false));
        v3.addParent(v1);
        network.add(v1, v2, v3);
    }

    @Test
    public void inferenceTest() {
//		network.print(System.out);

        Assert.assertEquals("Likelihood with no observation is 1", 1, Math.exp(network.getLogLikelihood()), DELTA);
//		logger.debug("P(O)=" + Math.exp(network.getLogLikelihood()));

        // Exp: 0.4400
        v2.set_evidence(0);
        Assert.assertEquals("P(v2 = 0)", 0.44, Math.exp(network.getLogLikelihood()), DELTA);

        // Exp: 0.2520
        v3.set_evidence(1);
        Assert.assertEquals("P(v2=0 & v3=1)", 0.252, Math.exp(network.getLogLikelihood()), DELTA);

        // Exp: 0.5800
        network.remove_evidence(v2);
        Assert.assertEquals("P(v3=1)", 0.58, Math.exp(network.getLogLikelihood()), DELTA);

    }

    private NetworkSet getExamples(final Network network, final int[][] examples) {
        return new NetworkSet() {
            int i = 0;

            @Override
            public void reset() {
                i = 0;
            }

            public Network next() {
                int[] e = examples[i++];
                v1.set_evidence(e[0]);
                v2.set_evidence(e[1]);
                v3.set_evidence(e[2]);
                return network;
            }

            public boolean hasNext() {
                return i < examples.length;
            }

        };

    }

    @Test
    public void learningFullEvidence() {
        // EM Style
        final int[][] examples = {
            {0, 0, 0},
            {0, 1, 0},
            {1, 0, 1},
            {1, 0, 1},
            {1, 1, 1},
            {1, 1, 1},
            {0, 1, 0}
        };

        NetworkSet examplesIterable = getExamples(network, examples);
        Functions functions = new Functions(network);

        EM em = new EM(functions);
        em.learn(examplesIterable);

//		network.print(System.out);

        Assert.assertEquals("P(v1=0)", 3. / 7., exp(f1.log_probability(v1, 0, (int[])null)), DELTA);
        Assert.assertEquals("P(v1=1)", 4. / 7., exp(f1.log_probability(v1, 1, (int[])null)), DELTA);

        Assert.assertEquals("P(v2=0/v1=0)", 1. / 3., exp(f2.log_probability(v2, 0, new int[] {0})), DELTA);
        Assert.assertEquals("P(v2=0/v1=1)", 2. / 4., exp(f2.log_probability(v2, 0, new int[] {1})), DELTA);

        Assert.assertEquals("P(v3=0/v1=0)", 3. / 3., exp(f3.log_probability(v1, 0, new int[] {0})), DELTA);
    }

    @Test
    public void learningPartialEvidence() {
        // EM Style
        final int[][] examples = {
            {0, 0, 0},
            {0, 1, 0},
            {1, -1, 1},
            {1, 0, 1},
            {1, -1, 1},
            {1, 1, 1},
            {0, 1, 0}
        };

        NetworkSet examplesIterable = getExamples(network, examples);
        Functions functions = new Functions(network);

        EM em = new EM(functions);
        for (int i = 0; i < 20; i++)
            em.learn(examplesIterable);

//		network.print(System.out);

        Assert.assertEquals("P(v1=0)", 3. / 7., exp(f1.log_probability(v1, 0, (int[])null)), DELTA);
        Assert.assertEquals("P(v1=1)", 4. / 7., exp(f1.log_probability(v1, 1, (int[])null)), DELTA);

        Assert.assertEquals("P(v2=0/v1=0)", 1. / 3., exp(f2.log_probability(v2, 0, new int[] {0})), DELTA);
        Assert.assertEquals("P(v2=0/v1=1)", 2. / 4., exp(f2.log_probability(v2, 0, new int[] {1})), DELTA);

        Assert.assertEquals("P(v3=0/v1=0)", 3. / 3., exp(f3.log_probability(v1, 0, new int[] {0})), DELTA);
    }
}

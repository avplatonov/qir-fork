package bpiwowar.ml.bn;

import bpiwowar.utils.maths.LogSumExpLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Math.exp;

/**
 * Test for networks with "virtual" parents CP
 *
 * @author bpiwowar
 */
public class CPVirtualNetworkTest {
    private static final double DELTA = 1e-5;
    TreeNetwork network;
    Variable v1, v3, v4;
    Variable v2;

    private CPVirtualParent f4;
    private CPFirstVirtualParent f3;
    private CPTable f1;

    @Before
    public void constructNetwork() {
        network = new TreeNetwork();

        v1 = new Variable("v1", 6);
        v2 = new Variable("v2", 3);
        v3 = new Variable("v3", 6);
        v4 = new Variable("v4", 6);

        v1.setFunction(f1 = new CPTable(v1));

        v2.addParent(v1);
        v2.setFunction(new CPModulo("", v2));

        v3.setFunction(f3 = new CPFirstVirtualParent("f3", 2, 3));
        v3.addParent(v2);

        v4.setFunction(f4 = new CPVirtualParent("cp", 2, 2, 3));
        v4.addParent(v3);

        network.add(v1, v2, v3, v4);
        Assert.assertTrue(network.verify());
    }

    static double[] logify(final double[] a) {
        for (int i = 0; i < a.length; i++)
            a[i] = Math.log(a[i]);
        return a;
    }

    @Test
    public void ModuloTest() {
        Assert.assertEquals(1, exp(v2.log_probability(0, new int[] {0})), DELTA);
        Assert.assertEquals(1, exp(v2.log_probability(0, new int[] {3})), DELTA);
        Assert.assertEquals(0, exp(v2.log_probability(0, new int[] {2})), DELTA);
    }

    @Test
    public void inferenceTest() {
        // Priors give a p.d. for v2 of (.5, .3, .2)
        final double[] d1 = logify(new double[] {
            0.3, 0.1, 0.05,
            0.2, 0.2, 0.15});
        f1.setParameters(d1);

        // Parameters for f3 are distributions on the 2 states for each possible value of the parent state (6 possible values)
        final double[] d3 = logify(new double[] { /* 0 */ 0.2, 0.8, /* 1 */ 0.3, 0.7, /* 2 */ 0.5, 0.5});
        f3.setParameters(d3);

        // Parameters for f2 are distributions on the 2 states for each possible value of the parent (2) and the shared parent (3)
        final double[] d4 = logify(new double[] {
            /* (0,0) */ 0.2, 0.8, /* (0,1) */ 0.55, 0.35,
            /* (1,0) */ 0.1, 0., /* (1,1) */ 0.3, 0.7,
            /* (2,0) */ 0.45, 0.55, /* (2,1) */ 0.5, 0.5});
        f4.setParameters(d4);

        // P(no evidence = 1)
        Assert.assertEquals("Likelihood with no observation is 1", 1, exp(network.getLogLikelihood()), DELTA);

        // Other case
        v1.set_evidence(0);
        v3.set_evidence(0);
        v4.set_evidence(0);

        Assert.assertEquals("P(v1=0)", exp(LogSumExpLog.add(d1[0], d1[3])), exp(v2.getRB2(0)), Math.exp(DELTA));

        Assert.assertEquals("P(v3 = 0 | v1=v2=0)", exp(d3[0]), exp(v3.getRB2(0)), DELTA);

        Assert.assertEquals(exp(d4[0]), exp(f4.log_probability(v3, 0, new int[] {0})), DELTA);
        Assert.assertEquals(exp(d4[0]), exp(v4.getRB2(0)), DELTA);
        Assert.assertEquals(exp(d4[0]), exp(v4.getRB3()), DELTA);

        Assert.assertEquals(exp(d4[0] + d3[0]), exp(v3.getRB3()), DELTA);
        Assert.assertEquals("P(v1..v4 = 0)", exp(d1[0] + d3[0] + d4[0]), exp(network.getLogLikelihood()), DELTA);

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
                v3.set_evidence(e[1]);
                v4.set_evidence(e[2]);
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
            {1, 2, 1},
            {1, 2, 1},
            {4, 3, 1},
            {1, 3, 1},
            {0, 1, 0}
        };

        NetworkSet examplesIterable = getExamples(network, examples);
        Functions functions = new Functions(network);

        EM em = new EM(functions);
        em.learn(examplesIterable);

//		network.print(System.out);

        Assert.assertEquals("P(v1=0)", 3. / 7., exp(f1.log_probability(v1, 0, (int[])null)), DELTA);
        Assert.assertEquals("P(v1=1)", 3. / 7., exp(f1.log_probability(v1, 1, (int[])null)), DELTA);

        Assert.assertEquals("P(v3=0/v2=0)", 1. / 3., exp(f3.log_probability(v3, 0, new int[] {0})), DELTA);
        Assert.assertEquals("P(v3=2/v2=1)", 2. / 4., exp(f3.log_probability(v3, 2, new int[] {1})), DELTA);

        Assert.assertEquals("P(v4=0/v2=0, v3 = 1)", 0. / 2., exp(f4.log_probability(v4, 0, new int[] {2})), DELTA);
    }
}

package bpiwowar.maths.svd;

import bpiwowar.io.LoggerPrintWriter;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.maths.matrix.Multiply;
import bpiwowar.utils.arrays.ListAdaptator;
import cern.colt.function.IntIntDoubleFunction;
import cern.jet.math.Functions;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.lang.Math.sqrt;

@RunWith(Enclosed.class)
public final class IncrementalSVDTest {
    final static Logger logger = Logger.getLogger(IncrementalSVDTest.class);

    static {
//		logger.setLevel(Level.DEBUG);
    }

    final static double DELTA = 1e-5;

    public static class Test1 {
        @Test
        public void simpleTests() {

            IncrementalSVD ist = new IncrementalSVD(true, true);

            DoubleMatrix1D a = new DenseDoubleMatrix1D(new double[] {1, 0});

            // We add the vector a = (1, 0)^T: we should have one extra dimension
            ist.addColumnVector(a);
            DoubleMatrix2D U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 1;

            Assert.assertEquals(U.get(0, 0), 1, DELTA);
            Assert.assertEquals(U.get(1, 0), 0, DELTA);

            // We add the same vector a = (1, 0)^T: we should not move
            ist.addColumnVector(a);
            U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 1;

            if (U.get(0, 0) > 0) {
                Assert.assertEquals(U.get(0, 0), 1, DELTA);
                Assert.assertEquals(U.get(1, 0), 0, DELTA);
            }
            else {
                Assert.assertEquals(U.get(0, 0), -1, DELTA);
                Assert.assertEquals(U.get(1, 0), 0, DELTA);
            }

            // We add another vector a = (0, 1)^T: we should get an extra dimension
            a = new DenseDoubleMatrix1D(new double[] {0, 1});
            ist.addColumnVector(a);
            U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 2;

            Assert.assertEquals(U.get(0, 0), 1, DELTA);
            Assert.assertEquals(U.get(1, 0), 0, DELTA);
            Assert.assertEquals(U.get(0, 1), 0, DELTA);
            Assert.assertEquals(U.get(1, 1), 1, DELTA);

            // We add another vector a = (1, 1)^T: should not move!
            a = new DenseDoubleMatrix1D(new double[] {1, 1});
            ist.addColumnVector(a);
            U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 2;

            if (U.get(0, 0) > 0) {
                Assert.assertEquals(U.get(0, 0), 0.8506508, DELTA);
                Assert.assertEquals(U.get(1, 0), 0.5257311, DELTA);
            }
            else {
                Assert.assertEquals(U.get(0, 0), -0.8506508, DELTA);
                Assert.assertEquals(U.get(1, 0), -0.5257311, DELTA);
            }

            if (U.get(0, 1) > 0) {
                Assert.assertEquals(U.get(0, 1), 0.5257311, DELTA);
                Assert.assertEquals(U.get(1, 1), -0.8506508, DELTA);
            }
            else {
                Assert.assertEquals(U.get(0, 1), -0.5257311, DELTA);
                Assert.assertEquals(U.get(1, 1), 0.8506508, DELTA);

            }
        }

        @Test
        public void simpleTests2() {

            IncrementalSVD ist = new IncrementalSVD(true, true);

            // We add the vector a = (1, 1)^T: we should have one extra dimension
            DoubleMatrix1D a = new DenseDoubleMatrix1D(new double[] {1, 1});
            ist.addColumnVector(a);
            DoubleMatrix2D U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 1;

            final double ISQRT2 = 1 / sqrt(2.);
            Assert.assertEquals(U.get(0, 0), ISQRT2, DELTA);
            Assert.assertEquals(U.get(1, 0), ISQRT2, DELTA);

            // We add the vector a = (1, 1)^T
            a = new DenseDoubleMatrix1D(new double[] {2, 2});
            ist.addColumnVector(a);
            U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 1;

            if (U.get(0, 0) > 0)
                Assert.assertEquals(U.get(0, 0), ISQRT2, DELTA);
            else
                Assert.assertEquals(U.get(0, 0), -ISQRT2, DELTA);

            if (U.get(1, 0) > 0)
                Assert.assertEquals(U.get(1, 0), ISQRT2, DELTA);
            else
                Assert.assertEquals(U.get(1, 0), -ISQRT2, DELTA);

            // We add the same vector a = (1, 0)^T: we should not move
            a = new DenseDoubleMatrix1D(new double[] {1, 0});
            ist.addColumnVector(a);
            U = ist.computeU();

            assert U.rows() == 2;
            assert U.columns() == 2;

            if (U.get(0, 0) > 0) {
                Assert.assertEquals(U.get(0, 0), 0.7414525, DELTA);
                Assert.assertEquals(U.get(1, 0), 0.6710053, DELTA);
            }
            else {
                Assert.assertEquals(U.get(0, 0), -0.7414525, DELTA);
                Assert.assertEquals(U.get(1, 0), -0.6710053, DELTA);
            }

            if (U.get(0, 1) > 0) {
                Assert.assertEquals(U.get(0, 1), 0.6710053, DELTA);
                Assert.assertEquals(U.get(1, 1), -0.7414525, DELTA);
            }
            else {
                Assert.assertEquals(U.get(0, 1), -0.6710053, DELTA);
                Assert.assertEquals(U.get(1, 1), 0.7414525, DELTA);
            }
        }
    }

    // ************ RANDOM MATRICES SVD ******************

    @RunWith(Parameterized.class)
    public static class Test2 {
        @Parameterized.Parameters(name = "randomSeeds")
        public static Object[][] randomSeeds() {
            // Gives a random seed, plus the dimension of the matrix
            return new Object[][] {
                {
                    new Long(102), new Integer(5), new Integer(10),
                    new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 1.},

                {
                    new Long(540), new Integer(10), new Integer(100),
                    new int[] {1, 9, 10, 50, 100}, 1.},

            };
        }

        @Parameterized.Parameter(0)
        public long seed;
        @Parameterized.Parameter(1)
        public int numRows;
        @Parameterized.Parameter(2)
        public int numCols;
        @Parameterized.Parameter(3)
        public int[] checkpoints;
        @Parameterized.Parameter(4)
        public double ratio;

        @Test
        public void randomSVDTest() {
            // Generate the matrix
            Random r = new Random(seed);
            DenseDoubleMatrix2D X = new DenseDoubleMatrix2D(numRows, numCols);
            for (int i = 0; i < numRows; i++) {
                double weight = 1;
                for (int j = 0; j < numCols; j++) {
                    weight = weight * ratio;
                    X.set(i, j, weight * (r.nextDouble() - .5));
                }
            }

            // Add the data points
            Set<Integer> checkpointSet = new TreeSet<Integer>();
            checkpointSet.addAll(new ListAdaptator<Integer>(checkpoints));

            // Perform the incremental SVD
            IncrementalSVD iSVD = new IncrementalSVD(true, true);
            for (int j = 0; j < numCols; j++) {
                logger.debug(String.format("%n%n____ Iteration %d _____%n", j + 1));

                try {
                    iSVD.addColumnVector(X.viewColumn(j));
                }
                catch (RuntimeException e) {
                    logger.warn(String.format("Caught an exception while adding the vector %d for SVD: %s", j + 1, e));
                    throw e;
                }

                if (checkpointSet.contains(j + 1)) {
                    // Compare
                    DoubleMatrix2D U = iSVD.computeU();
                    DoubleMatrix2D S = iSVD.getSigma();
                    DoubleMatrix2D V = iSVD.computeV();
                    DoubleMatrix2D X2 = Multiply.multiply(U, Multiply.multiply(S,
                        V, null, 1, 0, false, true), null);

                    if (logger.isDebugEnabled()) {
                        LoggerPrintWriter out = new LoggerPrintWriter(logger,
                            Level.DEBUG);

                        out.println("____ U _____");
                        U.print(out);

                        out.println("____ S _____");
                        S.print(out);

                        out.println("____ V _____");
                        V.print(out);

                        out.println("____ X[j] _____");
                        X.viewPart(0, 0, numRows, j + 1).print(out);

                        out.println("____ USV^T _____");
                        X2.print(out);

                        out.println("____ X-USV^T _____");
                        final DoubleMatrix2D delta = X2.copy();
                        X.viewPart(0, 0, numRows, j + 1).forEachNonZero(new IntIntDoubleFunction() {
                            @Override
                            public double apply(int i, int j, double v) {
                                delta.set(i, j, delta.get(i, j) - v);
                                return v;
                            }
                        });
                        delta.print(out);

                        out.println("____ ||X-USV^T||_F^2 _____");
                        out.flush();
                    }

                    // Compute the error
                    double fNorm = X.viewPart(0, 0, numRows, j + 1).aggregate(0,
                        X2, Functions.plus,
                        Functions.chain(Functions.square, Functions.minus),
                        true);
                    logger.debug(String.format("%e", fNorm));

                    assert Math.abs(fNorm) < 1e-15 : String
                        .format(
                            "SVD decomposition has a too big error ||X-USV^T||^2_F=%e > 1e-15",
                            fNorm);
                }
            }
        }
    }
}

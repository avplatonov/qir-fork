package bpiwowar.maths.eigen;

import bpiwowar.io.LoggerPrintWriter;
import bpiwowar.log.Logger;
import bpiwowar.maths.eigen.RankOneUpdate.Result;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.maths.matrix.Multiply;
import bpiwowar.testng.CartesianProduct;
import bpiwowar.utils.arrays.ListAdaptator;
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.IntIntDoubleFunction;
import cern.jet.math.Functions;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Level;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public final class RankOneUpdateTest {
    private static final double EPSILON = 1e-15;
    final static private Logger logger = Logger.getLogger();
    final static double DELTA = 1e-5;

    // ************ Symmetric incremental rank one update ******************

    @RunWith(Parameterized.class)
    public static class Test1 {
        @Parameterized.Parameters
        public static Object[][] symmetricRandomSeeds() {
            // Gives a random seed, plus the dimension of the matrix
            return new Object[][] {
                {
                    new Long(102),
                    new Integer(5),
                    new Integer(10), new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
                },
            };
        }

        @Parameterized.Parameter(0)
        public long seed;

        @Parameterized.Parameter(1)
        public int numRows;

        @Parameterized.Parameter(2)
        public int numUpdates;

        @Parameterized.Parameter(3)
        public int[] checkpoints;

        @Test
        public void incrementalSymmEDTest() {
            // Generate the matrix
            Random r = new Random(seed);

            // The matrix
            DenseDoubleMatrix2D mA = new DenseDoubleMatrix2D(numRows, numRows);

            // Add the data points
            Set<Integer> checkpointSet = new TreeSet<Integer>();
            checkpointSet.addAll(new ListAdaptator<Integer>(checkpoints));

            // Perform the incremental SVD
            IncrementalSymmetricED ised = new IncrementalSymmetricED();
            for (int j = 0; j < numUpdates; j++) {
                logger.debug("%n%n____ Iteration %d _____%n", j + 1);

                // Creates the vector
                DenseDoubleMatrix1D v = new DenseDoubleMatrix1D(numRows);
                for (int i = 0; i < numRows; i++)
                    v.set(i, r.nextDouble() - .5);
                double rho = (r.nextBoolean() ? -1 : 1) * (r.nextDouble() + 1);

                try {
                    Multiply.rankOneUpdate(rho, mA, v, v);
                    ised.update(rho, v);
                }
                catch (RuntimeException e) {
                    logger
                        .warn(
                            "Caught an exception while adding the vector %d for SVD: %s",
                            j + 1, e);
                    throw e;
                }

                if (checkpointSet.contains(j + 1)) {
                    // Compare
                    DoubleMatrix2D U = ised.computeU();
                    DoubleMatrix2D S = ised.getSigma();
                    final DoubleMatrix2D SUT = Multiply.multiply(S, U, null, 1, 0,
                        false, true);
                    DoubleMatrix2D X2 = Multiply.multiply(U, SUT);

                    // Compute A - USU^T
                    if (logger.isDebugEnabled()) {
                        LoggerPrintWriter out = new LoggerPrintWriter(logger,
                            Level.DEBUG);
                        out.format("rho = %e%n", rho);

                        out.println("____ v _____");
                        v.print(out);

                        out.println("____ U _____");
                        U.print(out);

                        out.println("____ S _____");
                        S.print(out);

                        out.println("____ USU^T _____");
                        X2.print(out);

                        out.println("____ A _____");
                        mA.print(out);

                        out.println("____ A-USU^T _____");
                        final DoubleMatrix2D delta = X2.copy();
                        mA.forEachNonZero(new IntIntDoubleFunction() {
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
                    double error = mA.aggregate(0, X2, Functions.plus, Functions
                        .chain(Functions.square, Functions.minus), true);
                    double normA = mA.aggregate(0, Functions.plus,
                        Functions.square, true);
                    logger
                        .debug(
                            "||X-USV^T||_F^2=%e, ||A||^2=%e, eps=%e; threshold is %e (%b)",
                            error, normA, EPSILON, EPSILON * normA, Math
                                .abs(error) < EPSILON * normA);

                    // Check it is below a threshold
                    assert Math.abs(error) < EPSILON * normA : String
                        .format(
                            "SVD decomposition has a too big error ||A-USU^T||^2_F=%e > 1e-15 * ||A|| = %e",
                            error, EPSILON * normA);
                }
            }
        }
    }

    // ************ Rank one update of a diagonal matrix ******************

    @RunWith(Parameterized.class)
    public static class Test2 {
        @SuppressWarnings("unchecked")
        @Parameterized.Parameters(name = "diagonalRandomSeeds")
        public static Object[][] diagonalRandomSeeds() {
            return new Object[][] {
                {new FastRankOneUpdate(), 102l, 5, 0, 0, 1},
                {new FastRankOneUpdate(), 102l, 5, 0, 0, 2},
                {new FastRankOneUpdate(), 102l, 5, 0, 0, -1},
            };
        }

        @Parameterized.Parameter(0)
        public RankOneUpdate rou;
        @Parameterized.Parameter(1)
        public long seed;
        @Parameterized.Parameter(2)
        public int N;
        @Parameterized.Parameter(3)
        public int same;
        @Parameterized.Parameter(4)
        public int zero;
        @Parameterized.Parameter(5)
        public double rho;

        @Test
        public void rankOneUpdateTest() {
            logger.debug("random rank one test with %s (seed=%d, N=%d, same=%d, zero=%d, rho=%e)",
                rou.getClass(), seed, N, same, zero, rho);
            // Generate the matrix
            Random r = new Random(seed);
            DiagonalDoubleMatrix D = new DiagonalDoubleMatrix(N);
            // Set the diagonal matrix
            for (int i = 0; i < N; i++)
                D.set(i, i, r.nextDouble());

            Collections.sort(D.asList(), new Comparator<Double>() {
                @Override
                public int compare(Double o1, Double o2) {
                    return Double.compare(o2, o1);
                }
            });

            while (same-- > 0) {
                int i = r.nextInt(N - 1);
                D.set(i + 1, i + 1, D.get(i, i));
            }

            // Set the z vector
            DenseDoubleMatrix1D z = new DenseDoubleMatrix1D(N);
            for (int i = 0; i < N; i++)
                z.set(i, r.nextDouble() - .5);

            while (zero-- > 0)
                z.set(r.nextInt(N), 0);

            // Add the data points

            // Perform the incremental SVD
            long starts = System.currentTimeMillis();
            Result result = rou.rankOneUpdate(D, rho, z, true, null, true);
            long endts = System.currentTimeMillis();

            DoubleMatrix2D U = result.getEigenvectors();
            DoubleMatrix2D S = result.getEigenvalues();
            DoubleMatrix2D X2 = Multiply.multiply(U, Multiply.multiply(S, U, null,
                1, 0, false, true));

            DenseDoubleMatrix2D mA = new DenseDoubleMatrix2D(N, N);
            mA.assign(D);
            Multiply.rankOneUpdate(rho, mA, z, z);

            if (logger.isDebugEnabled()) {
                LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.DEBUG);

                out.println("____ D _____");
                D.print(out);

                out.println("____ z _____");
                z.print(out);

                out.println("____ D + rho * zz^t _____");
                mA.print(out);

                out.println("____ U _____");
                U.print(out);

                out.println("____ S _____");
                S.print(out);

                out.flush();
            }

            final DoubleDoubleFunction chain = Functions.chain(Functions.square,
                Functions.minus);

            double fNorm = mA.aggregate(0, X2.viewPart(0, 0, N, N), Functions.plus,
                chain, true);

            logger.debug("Decomposition took %dms and ||X-USV^T||_F^2 = %e", endts
                - starts, fNorm);
            assert Math.abs(fNorm) < 1e-15 : String
                .format(
                    "rank one update has a too big error ||X-USV^T||^2_F=%f > 1e-15",
                    fNorm);
        }
    }
}

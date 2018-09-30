package bpiwowar.maths.svd;

import bpiwowar.io.LoggerPrintWriter;
import bpiwowar.log.Logger;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.maths.matrix.Multiply;
import bpiwowar.maths.svd.BrokenArrowSVD.Result;
import bpiwowar.testng.CartesianProduct;
import bpiwowar.utils.arrays.ListAdaptator;
import cern.colt.function.DoubleDoubleFunction;
import cern.jet.math.Functions;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public final class BrokenArrowSVDTest {
    final static private Logger logger = Logger.getLogger();

    final static double DELTA = 1e-5;

    // ************ RANDOM MATRICES SVD ******************

    @SuppressWarnings("unchecked")
    @Parameterized.Parameters(name = "randomSeeds")
    public static Iterable<Object[]> randomSeeds() {
        ListAdaptator<Object[]> svds = new ListAdaptator<Object[]>(
            new Object[][] {
                {new FastBrokenArrowSVD()},
                {new StandardBrokenArrowSVD()},});
        ListAdaptator<Object[]> seeds = new ListAdaptator<Object[]>(
            new Object[][] {
                {102l, 5, 0, 0}, {102l, 5, 1, 0},
                {102l, 5, 0, 1}, {102l, 5, 1, 1},
                {102l, 25, 1, 1}, {540l, 15, 15, 0},
                {540l, 15, 0, 15},
                // { 54023l, 1000, 23, 10 },
            });

        return new CartesianProduct(svds, seeds);
    }

    @Parameterized.Parameter(0)
    public BrokenArrowSVD svd;
    @Parameterized.Parameter(1)
    public long seed;
    @Parameterized.Parameter(2)
    public int N;
    @Parameterized.Parameter(3)
    public int same;
    @Parameterized.Parameter(4)
    public int zero;

    @Test
    public void randomSVDTest() {
        logger.debug("randomSVDTest with %s (seed=%d, N=%d, same=%d, zero=%d)",
            svd.getClass(), seed, N, same, zero);
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
        DenseDoubleMatrix1D z = new DenseDoubleMatrix1D(N + 1);
        for (int i = 0; i < N + 1; i++)
            z.set(i, r.nextDouble() - .5);

        while (zero-- > 0)
            z.set(r.nextInt(N + 1), 0);

        // Add the data points

        // Perform the incremental SVD
        long starts = System.currentTimeMillis();
        Result result = svd.computeSVD(D, z, N + 1);
        long endts = System.currentTimeMillis();

        DoubleMatrix2D U = result.getU();
        DoubleMatrix2D V = result.getV();
        DoubleMatrix2D S = result.getS();
        DoubleMatrix2D X2 = Multiply.multiply(U, Multiply.multiply(S, V, null,
            1, 0, false, true));

        if (logger.isDebugEnabled()) {
            LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.DEBUG);

            out.println("____ D _____");
            D.print(out);

            out.println("____ z _____");
            z.print(out);

            out.println("____ U _____");
            U.print(out);

            out.println("____ S _____");
            S.print(out);

            out.println("____ V _____");
            V.print(out);

            out.flush();
        }

        final DoubleDoubleFunction chain = Functions.chain(Functions.square,
            Functions.minus);
        double fNorm = D.aggregate(0, X2.viewPart(0, 0, N, N), Functions.plus,
            chain, true);

        logger.debug("fnorm (diag) is %e", fNorm);
        fNorm += X2.viewRow(N).aggregate(0, z, Functions.plus, chain, true);

        logger.debug("Decomposition took %dms and ||X-USV^T||_F^2 = %e", endts
            - starts, fNorm);
        Assert.assertEquals(String.format("SVD decomposition has a too big error ||X-USV^T||^2_F=%f > 1e-15", fNorm),
            fNorm,
            0,
            1e-15);
    }
}

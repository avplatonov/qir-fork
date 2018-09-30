package bpiwowar.maths.matrix;

import bpiwowar.io.LoggerPrintWriter;
import bpiwowar.log.Logger;
import bpiwowar.maths.matrix.Multiply.RankOneUpdater;
import java.util.Random;
import org.apache.log4j.Level;
import org.junit.Test;

public class RankOneUpdateTest {
    final static private Logger logger = Logger.getLogger();

    final static private double DELTA = 1e-12;

    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        // Create the data
        Random random = new Random(0);
        LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.DEBUG);

        final int n = 5;
        final int m = 13;

        DenseDoubleMatrix2D mA = new DenseDoubleMatrix2D(n, m);
        DoubleMatrix2DTest.randomise(random, mA);
        DoubleMatrix2D mA2 = mA.like(n, m);
        mA2.assign(mA);

        DenseDoubleMatrix1D x = new DenseDoubleMatrix1D(n);
        DoubleMatrix1DTest.randomise(random, x);
        DenseDoubleMatrix1D y = new DenseDoubleMatrix1D(m);
        DoubleMatrix1DTest.randomise(random, y);

        // Update with standard methods
        DenseDoubleMatrix2D mx = new DenseDoubleMatrix2D(n, 1);
        mx.viewColumn(0).assign(x);
        DenseDoubleMatrix2D my = new DenseDoubleMatrix2D(1, m);
        my.viewRow(0).assign(y);

        if (out.isEnabled()) {
            out.println("A=");
            mA.print(out);
        }

        Multiply.multiply(mx, my, mA, .8, 1, false, false);

        if (out.isEnabled()) {
            out.println("A' + .8 * x * t(y)");
            mA.print(out);
            out.println("x");
            x.print(out);
            out.println("y");
            y.print(out);
        }

        // Test the different updaters
        for (RankOneUpdater updater : new RankOneUpdater[] {
            new DenseDoubleMatrix2D.DenseRankOneUpdater(),
            new Multiply.DefautRankOneUpdater()}) {
            DoubleMatrix2D mB = mA2.like(n, m);
            mB.assign(mA2);

            updater.update(.8, mB, x, y);

            if (out.isEnabled()) {
                out.println("A + .8 * x * t(y)");
                mB.print(out);
                out.flush();
            }

            // Test
            DoubleMatrix2DTest.assertEqual(mB, mA, DELTA);
        }

    }
}

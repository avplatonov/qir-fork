package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D.DenseInnerProduct;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D.BlasMultiplier2D;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import java.util.Random;

@TaskDescription(name = "matrix-performance", description = "Compute statistics on matrix performance", project = {"maths"})
public class MatrixMeasures extends AbstractTask {
    final static int[] DIMENSIONS = {1, 5, 10, 100, 1000, 2500};
    final static int[] VDIMENSIONS = {10, 100, 1000, 10000, 100000, 1000000};

    @Argument(name = "what", required = true)
    What what;

    static public enum What {
        MULT2D, MULT1D, INNER
    }

    static <T extends DoubleMatrix2D> T randomise(Random random, final T m) {
        for (int i = 0; i < m.rows(); i++)
            for (int j = 0; j < m.columns(); j++)
                m.set(i, j, random.nextDouble());
        return m;
    }

    static <T extends DoubleMatrix1D> T randomise(Random random, final T m) {
        for (int i = 0; i < m.size(); i++)
            m.set(i, random.nextDouble());
        return m;
    }

    @Override
    public int execute() throws Throwable {
        Random random = new Random();

        switch (what) {
            case MULT2D: {
                BlasMultiplier2D blasMult = new BlasMultiplier2D(true);
                BlasMultiplier2D stdMult = new BlasMultiplier2D(false);
                for (int r : DIMENSIONS)
                    for (int c : DIMENSIONS) {
                        DenseDoubleMatrix2D m1 = new DenseDoubleMatrix2D(r, c);
                        randomise(random, m1);

                        for (int k : DIMENSIONS) {
                            DenseDoubleMatrix2D m2 = new DenseDoubleMatrix2D(c, k);
                            randomise(random, m2);

                            long t1 = System.currentTimeMillis();
                            blasMult.multiply(m1, m2, null, 1, 0, false, false);
                            long t2 = System.currentTimeMillis();
                            stdMult.multiply(m1, m2, null, 1, 0, false, false);
                            long t3 = System.currentTimeMillis();

                            System.out.format("%d\t%d\t%d\t%d\t%d%n", r, c, k, t2
                                - t1, t3 - t2);
                        }
                    }
                break;
            }

            case INNER: {
                DenseInnerProduct stdInner = new DenseDoubleMatrix1D.DenseInnerProduct(
                    false);
                DenseInnerProduct blasInner = new DenseDoubleMatrix1D.DenseInnerProduct(
                    true);
                for (int r : VDIMENSIONS) {
                    DenseDoubleMatrix1D a = new DenseDoubleMatrix1D(r);
                    randomise(random, a);
                    DenseDoubleMatrix1D b = new DenseDoubleMatrix1D(r);
                    randomise(random, b);

                    long t1 = System.currentTimeMillis();
                    blasInner.innerProduct(a, b);
                    long t2 = System.currentTimeMillis();
                    stdInner.innerProduct(a, b);
                    long t3 = System.currentTimeMillis();

                    System.out.format("%d\t%d\t%d%n", r, t2 - t1, t3 - t2);
                }

                break;
            }
            case MULT1D:
                break;
        }
        return 0;
    }
}

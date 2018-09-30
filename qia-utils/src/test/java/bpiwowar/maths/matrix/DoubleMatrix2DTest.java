package bpiwowar.maths.matrix;

import bpiwowar.log.Logger;
import bpiwowar.maths.matrix.Multiply.Multiplier1D;
import bpiwowar.maths.matrix.Multiply.Multiplier2D;
import bpiwowar.maths.matrix.Multiply.TrieKey;
import bpiwowar.testng.CartesianProduct;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Pair;
import bpiwowar.utils.arrays.ListAdaptator;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
public class DoubleMatrix2DTest {
    final static private Logger logger = Logger.getLogger();

    // --- Custom factories

    static class DensePartViewFactory implements
        DoubleMatrix2DFactory<DenseDoubleMatrix2D> {
        private static final long serialVersionUID = 1L;

        @Override
        public DenseDoubleMatrix2D create(int rows, int cols) {
            // Creates a too large matrix and get a view of it
            return randomise(new Random(),
                new DenseDoubleMatrix2D(rows + 3, cols + 3)).getViewPart(2,
                2, rows, cols);
        }
    }

    static class PartViewFactory implements
        DoubleMatrix2DFactory<PartViewDoubleMatrix2D> {
        private static final long serialVersionUID = 1L;

        @Override
        public PartViewDoubleMatrix2D create(int rows, int cols) {
            return new PartViewDoubleMatrix2D(randomise(new Random(),
                new DenseDoubleMatrix2D(rows + 3, cols + 3)), 2, 2, rows,
                cols);
        }
    }

    static class DiceViewFactory implements
        DoubleMatrix2DFactory<DiceViewDoubleMatrix2D> {
        private static final long serialVersionUID = 1L;

        @Override
        public DiceViewDoubleMatrix2D create(int rows, int cols) {
            return new DiceViewDoubleMatrix2D(new DenseDoubleMatrix2D(cols,
                rows));
        }
    }

    // --- Some iterators
    @SuppressWarnings("unchecked")
    private static final ListAdaptator<DoubleMatrix2DFactory[]> MATRIX2D_TYPES = ListAdaptator
        .create(new DoubleMatrix2DFactory[][] {
            {DenseDoubleMatrix2D.FACTORY},
            {new DensePartViewFactory()}, {new PartViewFactory()},
            {new DiceViewFactory()},});

    @SuppressWarnings("unused")
    private static final ListAdaptator<Object[]> TRUE_FALSE = new ListAdaptator<Object[]>(
        new Object[][] {{true}, {false}});

    static enum TransposeMode {
        NONE, CONSTRUCTION, MULTIPLICATION
    }

    // 3 x 2 matrix
    static final double[][] A = new double[][] {{1, 2}, {3, 4}, {5, 6}};
    // 2 x 2 matrix
    static final double[][] B = new double[][] {{2, 3}, {2, 6}};
    static final double[][] C = new double[][] {{1, -2}, {-3, 4}, {-5, 6}};

    // Vector (3)
    static final double x[] = new double[] {.24, .12};


    @RunWith(Parameterized.class)
    public static class Test1 {
        @SuppressWarnings("unchecked")
        @Parameterized.Parameters(name = "matrixTypes")
        public static Iterable<DoubleMatrix2DFactory[]> vectorTypes() {
            return MATRIX2D_TYPES;
        }

        @Parameterized.Parameter
        public DoubleMatrix2DFactory<? extends DoubleMatrix2D> aClass;

        @Test
        public void assign()
            throws Throwable {
            final cern.colt.matrix.DoubleMatrix2D cA = create(A, false);
            DoubleMatrix2D mA = create(aClass, cA);
            assertEqual(cA, mA, 0);
        }
    }

    // ---
    // --- Multiplication (1D)
    // ---
    @RunWith(Parameterized.class)
    public static class Test2 {
        /**
         * Provides a set of configurations for multiplication
         */
        @Parameterized.Parameters(name = "multiply1D-configurations")
        @SuppressWarnings("unchecked")
        public static Iterable<Object[]> multiply1DConfigurations() {
            ArrayList<Object[]> keys = GenericHelper.newArrayList();
            for (Pair<TrieKey, Multiplier1D> x : Multiply.multipliers1D) {
                // For each key, get all the possible instances
                final Class<?>[] c = x.getFirst().classes;
                int found = 0;
                for (DoubleMatrix2DFactory<?>[] c0 : MATRIX2D_TYPES)
                    if (factoryIsCompatible(c[0], c0[0]))
                        for (DoubleMatrix1DFactory<?>[] c1 : DoubleMatrix1DTest.MATRIX1D_TYPES)
                            if (DoubleMatrix1DTest.factoryIsCompatible(c[1], c1[0]))
                                for (DoubleMatrix1DFactory<?>[] c2 : DoubleMatrix1DTest.MATRIX1D_TYPES)
                                    if (DoubleMatrix1DTest.factoryIsCompatible(
                                        c[2], c2[0])) {
                                        logger.debug(
                                            "Adding (1d mult) %s/%s%%s for %s (%s)",
                                            c0[0], c1[0], c2[0], c,
                                            x.getSecond());
                                        keys.add(new Object[] {
                                            x.getSecond(),
                                            c0[0], c1[0], c2[0]});
                                        found++;
                                    }

                if (found == 0)
                    logger.warn(
                        "Did not found any matching classes for multiplier %s",
                        x.getSecond().getClass());

            }

            final ListAdaptator<Object[]> trModes = ListAdaptator
                .create(new Object[][] {
                    {TransposeMode.NONE},
                    {TransposeMode.CONSTRUCTION},
                    // { TransposeMode.MULTIPLICATION }
                });
            return new CartesianProduct(keys, trModes);
        }

        @Parameterized.Parameter(0)
        public Multiplier1D<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> multiplier;
        @Parameterized.Parameter(1)
        public DoubleMatrix2DFactory<? extends DoubleMatrix2D> aFactory;
        @Parameterized.Parameter(2)
        public DoubleMatrix1DFactory<? extends DoubleMatrix1D> xFactory;
        @Parameterized.Parameter(3)
        public DoubleMatrix1DFactory<? extends DoubleMatrix1D> yFactory;
        @Parameterized.Parameter(4)
        public TransposeMode tA;

        @Test
        public void multiply1D() throws Throwable {
            assert tA != TransposeMode.MULTIPLICATION;
            logger.debug("Testing matrix 1D multiplication [%s] %s x %s ->  %s",
                multiplier.getClass().getName(), aFactory.getClass().getName(),
                xFactory.getClass().getName(), yFactory.getClass().getName());

            cern.colt.matrix.DoubleMatrix2D cA = create(A,
                tA == TransposeMode.CONSTRUCTION);
            DoubleMatrix2D mA = create(aFactory, cA);

            DoubleMatrix1D mB = DoubleMatrix1DTest.create(xFactory, x);
            cern.colt.matrix.DoubleMatrix1D cB = new cern.colt.matrix.impl.DenseDoubleMatrix1D(
                x);

            // Multiply
            cern.colt.matrix.DoubleMatrix1D cC = cA.zMult(cB, null, 1, 0,
                tA != TransposeMode.NONE);

            DoubleMatrix1D mC = yFactory.create(cC.size());
            multiplier.multiply(mA, mB, mC, 1, 0, tA != TransposeMode.NONE);
            DoubleMatrix1DTest.assertEqual(cC, mC);

            // C <- A x B - .2
            cC = cA.zMult(cB, cC, 1, -.2, tA != TransposeMode.NONE);

            multiplier.multiply(mA, mB, mC, 1, -.2, tA != TransposeMode.NONE);
            DoubleMatrix1DTest.assertEqual(cC, mC);

        }
    }

    // ---
    // --- Multiplication (2D)
    // ---

    @RunWith(Parameterized.class)
    public static class Test3 {
        /**
         * Provides a set of configurations for multiplication
         */
        @Parameterized.Parameters(name = "multiply2D-configurations")
        @SuppressWarnings("unchecked")
        public static Iterable<Object[]> multiply2DConfigurations() {
            ArrayList<Object[]> keys = GenericHelper.newArrayList();
            for (Pair<TrieKey, Multiplier2D> x : Multiply.multipliers2D) {
                // For each key, get all the possible instances
                final Class<?>[] c = x.getFirst().classes;
                int found = 0;
                for (DoubleMatrix2DFactory<?>[] c0 : MATRIX2D_TYPES)
                    if (factoryIsCompatible(c[0], c0[0]))
                        for (DoubleMatrix2DFactory<?>[] c1 : MATRIX2D_TYPES)
                            if (factoryIsCompatible(c[1], c1[0]))
                                for (DoubleMatrix2DFactory<?>[] c2 : MATRIX2D_TYPES)
                                    if (factoryIsCompatible(c[2], c2[0])) {
                                        logger.debug("Adding %s/%s%%s for %s (%s)",
                                            c0[0], c1[0], c2[0], c,
                                            x.getSecond());
                                        found++;
                                        keys.add(new Object[] {
                                            x.getSecond(),
                                            c0[0], c1[0], c2[0]});
                                    }

                if (found == 0)
                    logger.warn(
                        "Did not found any matching classes for multiplier %s",
                        x.getSecond().getClass());
            }

            final ListAdaptator<Object[]> trModes = ListAdaptator
                .create(new Object[][] {
                    {TransposeMode.NONE},
                    {TransposeMode.CONSTRUCTION},
                    // { TransposeMode.MULTIPLICATION }
                });
            return new CartesianProduct(keys, trModes, trModes);
        }

        @Parameterized.Parameter(0)
        public Multiplier2D<DoubleMatrix2D, DoubleMatrix2D, DoubleMatrix2D> multiplier;
        @Parameterized.Parameter(1)
        public DoubleMatrix2DFactory<? extends DoubleMatrix2D> aFactory;
        @Parameterized.Parameter(2)
        public DoubleMatrix2DFactory<? extends DoubleMatrix2D> bFactory;
        @Parameterized.Parameter(3)
        public DoubleMatrix2DFactory<? extends DoubleMatrix2D> cFactory;
        @Parameterized.Parameter(4)
        public TransposeMode tA;
        @Parameterized.Parameter(5)
        public TransposeMode tB;

        @Test
        public void multiply2D() throws Throwable {
            assert tA != TransposeMode.MULTIPLICATION;
            assert tB != TransposeMode.MULTIPLICATION;

            logger.debug("Testing matrix multiplication [%s] %s x %s ->  %s",
                multiplier.getClass().getName(), aFactory.getClass().getName(),
                bFactory.getClass().getName(), cFactory.getClass().getName());
            cern.colt.matrix.DoubleMatrix2D cA = create(A,
                tA == TransposeMode.CONSTRUCTION);
            DoubleMatrix2D mA = create(aFactory, cA);

            cern.colt.matrix.DoubleMatrix2D cB = create(B,
                tB == TransposeMode.CONSTRUCTION);
            DoubleMatrix2D mB = create(aFactory, cB);

            // Dice view if necessary
            if (tA == TransposeMode.MULTIPLICATION) {
                mA = mA.viewDice();
                cA = cA.viewDice();
            }
            if (tB == TransposeMode.MULTIPLICATION) {
                mB = mB.viewDice();
                cB = cB.viewDice();
            }

            // Multiply
            cern.colt.matrix.DoubleMatrix2D cC = cA.zMult(cB, null, 1, 0,
                tA != TransposeMode.NONE, tB != TransposeMode.NONE);

            DoubleMatrix2D mC = cFactory.create(A.length, B[0].length);
            multiplier.multiply(mA, mB, mC, 1, 0, tA != TransposeMode.NONE,
                tB != TransposeMode.NONE);
            assertEqual(cC, mC, 0);

            // C <- A x B - .2 C
            cC = cA.zMult(cB, cC, 1, -.2, tA != TransposeMode.NONE,
                tB != TransposeMode.NONE);

            multiplier.multiply(mA, mB, mC, 1, -.2, tA != TransposeMode.NONE,
                tB != TransposeMode.NONE);
            assertEqual(cC, mC, 0);

        }
    }

    // ----- Helper methods to create matrices

    static private cern.colt.matrix.DoubleMatrix2D create(double[][] m,
        boolean transpose) {
        if (transpose)
            return new cern.colt.matrix.impl.DenseDoubleMatrix2D(m).viewDice();
        return new cern.colt.matrix.impl.DenseDoubleMatrix2D(m);
    }

    /**
     * Creates a copy of a given matrix
     *
     * @param factory
     * @param A
     * @return
     * @throws Throwable
     */
    static private <T extends DoubleMatrix2D> T create(
        DoubleMatrix2DFactory<T> factory, cern.colt.matrix.DoubleMatrix2D A)
        throws Throwable {
        T m = factory.create(A.rows(), A.columns());

        for (int i = 0; i < m.rows(); i++)
            for (int j = 0; j < m.columns(); j++)
                if (m.zeroByConstruction(i, j))
                    A.set(i, j, 0);
                else
                    m.set(i, j, A.get(i, j));

        return m;
    }

    /**
     * Equality assertion
     *
     * @param cA
     * @param mA
     * @param delta The tolerated difference
     */
    static public void assertEqual(cern.colt.matrix.DoubleMatrix2D cA,
        DoubleMatrix2D mA, double delta) {
        for (int i = cA.rows(); --i >= 0; )
            for (int j = mA.columns(); --j >= 0; )
                Assert.assertEquals(String.format("Element (%d,%d) are different (%e and %e)", i,
                    j, mA.get(i, j), cA.get(i, j)), mA.get(i, j), cA.get(i, j), delta);
    }

    static public void assertEqual(DoubleMatrix2D mA, DoubleMatrix2D mB,
        double delta) {
        for (int i = mA.rows(); --i >= 0; )
            for (int j = mB.columns(); --j >= 0; )
                Assert.assertEquals(String.format("Element (%d,%d) are different (%e and %e)", i,
                    j, mB.get(i, j), mA.get(i, j)),
                    mB.get(i, j), mA.get(i, j), delta);
    }

    /**
     * Checks if a matrix factory can produce a given matrix class or subclass
     *
     * @param matrixClass
     * @param factory
     * @return
     */
    private static boolean factoryIsCompatible(final Class<?> matrixClass,
        DoubleMatrix2DFactory<?> factory) {
        final Type[] generatedClasses = GenericHelper.getActualTypeArguments(
            factory.getClass(), DoubleMatrix2DFactory.class);
        if (generatedClasses == null)
            throw new RuntimeException(String.format(
                "Cannot find the generic arguments for %s in %s",
                DoubleMatrix2DFactory.class, factory.getClass()));

        @SuppressWarnings("unchecked")
        Class<? extends DoubleMatrix2D> generatedClass = (Class<? extends DoubleMatrix2D>)generatedClasses[0];
        boolean f = matrixClass.isAssignableFrom(generatedClass);
        logger.debug("Checking factory %s (%s) for %s: %b", factory,
            generatedClass, matrixClass, f);
        return f;
    }

    /**
     * Randomise the matrix
     *
     * @param <T>
     * @param m
     * @return
     */
    static <T extends DoubleMatrix2D> T randomise(Random random, final T m) {
        for (int i = 0; i < m.rows; i++)
            for (int j = 0; j < m.columns; j++)
                m.set(i, j, random.nextDouble());
        return m;
    }

}

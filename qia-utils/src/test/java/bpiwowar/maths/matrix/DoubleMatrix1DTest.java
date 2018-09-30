package bpiwowar.maths.matrix;

import bpiwowar.log.Logger;
import bpiwowar.maths.matrix.Multiply.InnerProduct;
import bpiwowar.maths.matrix.Multiply.TrieKey;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Pair;
import bpiwowar.utils.arrays.ListAdaptator;
import cern.colt.function.IntDoubleProcedure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.lang.Math.abs;
import static java.lang.String.format;

@RunWith(Enclosed.class)
public class DoubleMatrix1DTest {
    final static private Logger logger = Logger.getLogger();

    /**
     * Creates a vector and assign values
     *
     * @param factory
     * @param x
     * @return
     * @throws Throwable
     */
    static <T extends DoubleMatrix1D> T create(
        DoubleMatrix1DFactory<T> factory, double... x) throws Throwable {
        T v = factory.create(x.length);
        for (int i = x.length; --i >= 0; )
            v.set(i, x[i]);
        return v;
    }

    static void assertEqual(DoubleMatrix1D v, double... x) {
        Assert.assertEquals("Sizes differ", v.size, x.length);

        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        v.print(pw);
        pw.flush();

        for (int i = x.length; --i >= 0; )
            Assert.assertEquals(String.format(
                "Component %d of vectors are different (%e and %e): %s", i,
                v.get(i), x[i], w.toString()), x[i], v.get(i), 0.01);
    }

    @RunWith(Parameterized.class)
    public static class Test1 {
        @SuppressWarnings("unchecked")
        @Parameterized.Parameters(name = "factories")
        public static Iterable<DoubleMatrix1DFactory[]> vectorTypes() {
            return MATRIX1D_TYPES;
        }

        @Parameterized.Parameter
        public DoubleMatrix1DFactory<? extends DoubleMatrix1D> factory;

        @Test
        public void assignTest()
            throws Throwable {
            DoubleMatrix1D v = factory.create(3);
            assertEqual(v, 0, 0, 0);

            v.set(0, 1);
            v.set(1, 2);
            v.set(2, 3);

            assertEqual(v, 1, 2, 3);
        }

        @Test
        public void aggregate1Test()
            throws Throwable {
            final DoubleMatrix1D v = create(factory, 1, 0, 3);
            v.forEachNonZero(new IntDoubleProcedure() {
                @Override
                public boolean apply(int i, double second) {
                    v.set(i, -second);
                    return true;
                }
            });
            assertEqual(v, -1, 0, -3);
        }
    }


    // --- Test of inner products
    @RunWith(Parameterized.class)
    public static class Test2 {
        /**
         * Provides a set of configurations for multiplication
         */
        @Parameterized.Parameters
        @SuppressWarnings("unchecked")
        public static Iterable<Object[]> innerProductConfigurations() {
            ArrayList<Object[]> keys = GenericHelper.newArrayList();

            for (Pair<TrieKey, InnerProduct> x : Multiply.innerProducts) {
                // For each key, get all the possible instances
                final Class<?>[] c = x.getFirst().classes;
                int found = 0;
                for (DoubleMatrix1DFactory<?>[] c0 : MATRIX1D_TYPES)
                    if (factoryIsCompatible(c[0], c0[0]))
                        for (DoubleMatrix1DFactory<?>[] c1 : DoubleMatrix1DTest.MATRIX1D_TYPES)
                            if (DoubleMatrix1DTest.factoryIsCompatible(c[1], c1[0])) {
                                logger
                                    .debug(
                                        "Adding inner product of %s and %s (%s, %s)",
                                        c0[0], c1[0], c, x.getSecond());
                                keys
                                    .add(new Object[] {
                                        x.getSecond(), c0[0],
                                        c1[0]});
                                found++;
                            }

                if (found == 0)
                    logger
                        .warn(
                            "Did not found any matching classes for inner product %s",
                            x.getSecond().getClass());

            }

            return keys;
        }

        @Parameterized.Parameter(0)
        public InnerProduct<DoubleMatrix1D, DoubleMatrix1D> inner;
        @Parameterized.Parameter(1)
        public DoubleMatrix1DFactory<? extends DoubleMatrix1D> xFactory;
        @Parameterized.Parameter(2)
        public DoubleMatrix1DFactory<? extends DoubleMatrix1D> yFactory;

        @Test
        public void inner()
            throws Throwable {
            final Random random = new Random(0);
            final int length = 10;

            DoubleMatrix1D x = xFactory.create(length);
            randomise(random, x);
            DoubleMatrix1D y = yFactory.create(length);
            randomise(random, y);

            double v = inner.innerProduct(x, y);

            // Matrix multiplication is checked in another part, we can rely on it
            DenseDoubleMatrix2D mX = new DenseDoubleMatrix2D(1, length);
            DenseDoubleMatrix2D mY = new DenseDoubleMatrix2D(length, 1);
            mX.viewRow(0).assign(x);
            mY.viewColumn(0).assign(y);

            DoubleMatrix2D res = Multiply.multiply(mX, mY);
            assert res.columns == 1;
            assert res.rows == 1;
            double expected = res.get(0, 0);

            assert Math.abs(v - expected) < 10e-10 : format("Error %g over the 10e-10 threshold", abs(v - expected));
        }
    }

    // -----
    // ----- DoubleMatrix1D factories

    /**
     * Randomise a vector
     */
    static <T extends DoubleMatrix1D> T randomise(Random random, final T m) {
        for (int i = 0; i < m.size; i++)
            m.set(i, random.nextDouble());
        return m;
    }

    static class DensePartViewFactory implements
        DoubleMatrix1DFactory<DenseDoubleMatrix1D> {
        private static final long serialVersionUID = 1L;

        @Override
        public DenseDoubleMatrix1D create(int size) {
            // Creates a too large matrix and get a view of it
            final DenseDoubleMatrix1D part = randomise(new Random(),
                new DenseDoubleMatrix1D(size + 5)).getViewPart(2, size);
            part.clear();
            return part;
        }
    }

    static class PartViewFactory implements
        DoubleMatrix1DFactory<PartViewDoubleMatrix1D> {
        private static final long serialVersionUID = 1L;

        @Override
        public PartViewDoubleMatrix1D create(int size) {
            final PartViewDoubleMatrix1D part = new PartViewDoubleMatrix1D(
                randomise(new Random(), new DenseDoubleMatrix1D(size + 5)),
                2, size);
            part.clear();
            return part;
        }
    }

    /**
     * Returns a part of a matrix
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static class DenseColumnFactory implements
        DoubleMatrix1DFactory<DenseDoubleMatrix1D> {
        private static final long serialVersionUID = 1L;

        @Override
        public DenseDoubleMatrix1D create(int size) {
            // Creates a too large matrix and get a column view of it
            DenseDoubleMatrix2D m = new DenseDoubleMatrix2D(size + 5, size + 5);
            DoubleMatrix2DTest.randomise(new Random(), m);
            final DenseDoubleMatrix1D part = m.viewColumn(2).getViewPart(2,
                size);
            part.clear();
            return part;
        }
    }

    static class DenseRowFactory implements
        DoubleMatrix1DFactory<DenseDoubleMatrix1D> {
        private static final long serialVersionUID = 1L;

        @Override
        public DenseDoubleMatrix1D create(int size) {
            // Creates a too large matrix and get a column view of it
            DenseDoubleMatrix2D m = new DenseDoubleMatrix2D(size + 5, size + 5);
            DoubleMatrix2DTest.randomise(new Random(), m);
            final DenseDoubleMatrix1D part = m.viewRow(2).getViewPart(2, size);
            part.clear();
            return part;
        }
    }

    @SuppressWarnings("unchecked")
    protected static final ListAdaptator<DoubleMatrix1DFactory[]> MATRIX1D_TYPES = ListAdaptator
        .create(new DoubleMatrix1DFactory[][] {
            {DenseDoubleMatrix1D.FACTORY},
            {new DenseColumnFactory()}, {new DenseRowFactory()},
            {new DensePartViewFactory()}, {new PartViewFactory()}, {SparseDoubleMatrix1D.FACTORY}});

    /**
     * Checks if a matrix factory can produce a given matrix class or subclass
     *
     * @param matrixClass
     * @param factory
     * @return
     */
    static boolean factoryIsCompatible(final Class<?> matrixClass,
        DoubleMatrix1DFactory<?> factory) {
        final Type[] generatedClasses = GenericHelper.getActualTypeArguments(
            factory.getClass(), DoubleMatrix1DFactory.class);
        if (generatedClasses == null)
            throw new RuntimeException(String.format(
                "Cannot find the generic arguments for %s in %s",
                DoubleMatrix1DFactory.class, factory.getClass()));

        @SuppressWarnings("unchecked")
        Class<? extends DoubleMatrix1D> generatedClass = (Class<? extends DoubleMatrix1D>)generatedClasses[0];
        boolean f = matrixClass.isAssignableFrom(generatedClass);
        logger.debug("Checking factory %s (%s) for %s: %b", factory,
            generatedClass, matrixClass, f);
        return f;
    }

    public static void assertEqual(cern.colt.matrix.DoubleMatrix1D x,
        DoubleMatrix1D y) {
        assert x.size() == y.size() : String.format(
            "Vector sizes differ (%d and %d)", x.size(), y.size());
        for (int i = 0; i < x.size(); i++)
            Assert.assertEquals("Vector values differ", x.get(i), y.get(i), 0.01);
    }
}

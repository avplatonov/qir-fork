package bpiwowar.maths.matrix;

import bpiwowar.collections.WeakComparable;
import bpiwowar.collections.WeakComparable.Status;
import bpiwowar.log.Logger;
import bpiwowar.ml.OutOfBoundsException;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Output;
import bpiwowar.utils.Pair;
import cern.colt.function.DoubleFunction;
import cern.colt.function.IntDoubleProcedure;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central class to multiply two matrices
 *
 * @author bpiwowar
 */
public class Multiply {

    final static private Logger logger = Logger.getLogger();

    /**
     * Type specific multiplier
     *
     * @param <T> The type of the first matrix
     * @param <U> The type of the second matrix
     * @param <R> The type of the result
     * @author bpiwowar
     */
    static public interface Multiplier2D<T extends DoubleMatrix2D, U extends DoubleMatrix2D, R extends DoubleMatrix2D> {
        R multiply(T A, U B, R C, double alpha, double beta,
            boolean transposeA, boolean transposeB);

        /**
         * Returns the complexity for the estimated complexity for the computation of the product of (m x n) matrix with
         * a (n x p) matrix
         *
         * @param m
         * @param n
         * @param p
         * @return The complexity
         */
        double complexity(int m, int n, int p);
    }

    /**
     * Compute alpha * mA * mB + beta * mC and put the result in mC
     *
     * @param mA
     * @param mC
     * @param mResult
     * @param alpha
     * @param beta
     * @param transposeA
     * @param transposeB
     * @return
     */
    static public DoubleMatrix2D multiply(DoubleMatrix2D A, DoubleMatrix2D B,
        DoubleMatrix2D C, double alpha, double beta, boolean transposeA,
        boolean transposeB) {

        logger.debug("Multiply %s [%b] with %s [%b] (result in %s)", A, B,
            transposeA, transposeB, C);

        // --- Check dimensions
        final int m = transposeA ? A.columns : A.rows;
        final int n = transposeA ? A.rows : A.columns;
        final int p = transposeB ? B.rows : B.columns;
        final int q = transposeB ? B.columns : B.rows;

        if (q != n)
            throw new IllegalArgumentException(
                String
                    .format(
                        "Cannot multiply incompatible matrices (%d x %d) and (%d x %d)",
                        m, n, q, p));
        if (C != null && (C.rows != m || C.columns != p))
            throw new IllegalArgumentException(
                String
                    .format(
                        "Result matrix (%d x %d) cannot hold the result of the multiplication of matrices (%d x %d) and (%d, %d)",
                        C.rows, C.columns, m, n, q, p));
        if (A == C || B == C)
            throw new IllegalArgumentException("Matrices must not be identical");

        // --- Find a suitable handler for the pair of matrices

        final Class<? extends DoubleMatrix2D> aClass = A.getClass();
        final Class<? extends DoubleMatrix2D> bClass = B.getClass();
        final Class<? extends DoubleMatrix2D> cClass = C != null ? C.getClass()
            : null;

        TrieKey key = new TrieKey(aClass, bClass, cClass);

        @SuppressWarnings("unchecked")
        Collection<Multiplier2D> list = getMatchingCandidates(key,
            multipliers2D, multipliers2DCache);

        @SuppressWarnings("unchecked")
        Multiplier2D multiplier = null;
        double minComplexity = Double.POSITIVE_INFINITY;

        for (Multiplier2D<?, ?, ?> candidate : list) {
            double complexity = candidate.complexity(m, n, p);
            logger.debug("Multiplier %s for %s has complexity %g", candidate
                .getClass(), key, complexity);
            if (complexity < minComplexity) {
                minComplexity = complexity;
                multiplier = candidate;
            }
        }

        if (multiplier != null) {
            logger.debug("Selected multiplier %s", multiplier.getClass());
            @SuppressWarnings("unchecked")
            DoubleMatrix2D r = multiplier.multiply(A, B, C, alpha, beta,
                transposeA, transposeB);
            return r;
        }

        // --- Should not happen
        throw new RuntimeException(String.format(
            "Did not find a suitable multiplication handler for %s and %s",
            aClass, bClass));

    }

    final public static class DefaultMultiplier2D implements
        Multiplier2D<DoubleMatrix2D, DoubleMatrix2D, DoubleMatrix2D> {

        @Override
        public double complexity(int m, int n, int p) {
            return (double)m * (double)n * (double)n * (double)p;
        }

        @Override
        public DoubleMatrix2D multiply(DoubleMatrix2D A, DoubleMatrix2D B,
            DoubleMatrix2D C, double alpha, double beta,
            boolean transposeA, boolean transposeB) {
            final int m = transposeA ? A.columns : A.rows;
            final int n = transposeA ? A.rows : A.columns;
            final int p = transposeB ? B.rows : B.columns;

            if (C == null)
                C = new DenseDoubleMatrix2D(m, p);

            if (transposeA)
                A = A.viewDice();
            if (transposeB)
                B = B.viewDice();

            MultAdd multAdd = new MultAdd();

            for (int j = p; --j >= 0; ) {
                for (int i = m; --i >= 0; ) {
                    double s = 0;
                    for (int k = n; --k >= 0; ) {
                        s += A.get(i, k) * B.get(k, j);
                    }
                    C.update(i, j, multAdd.set(beta, alpha * s));
                }
            }
            return C;
        }
    }

    final static class MultAdd implements DoubleFunction {
        double alpha;
        double x;

        @Override
        public double apply(double v) {
            return alpha * v + x;
        }

        public DoubleFunction set(double alpha, double x) {
            this.alpha = alpha;
            this.x = x;
            return this;
        }
    }

    final public static DoubleMatrix2D multiply(DoubleMatrix2D A,
        DoubleMatrix2D B) {
        return multiply(A, B, null, 1, 0, false, false);
    }

    final public static DoubleMatrix2D multiply(DoubleMatrix2D A,
        DoubleMatrix2D B, DoubleMatrix2D C) {
        return multiply(A, B, C, 1, 0, false, false);
    }

    // --------------------------------
    // -------------------------------- Rank one update
    // --------------------------------

    static public interface RankOneUpdater<T extends DoubleMatrix2D, U extends DoubleMatrix1D, R extends DoubleMatrix1D> {
        void update(double alpha, T mA, U x, R y);

        double complexity(int m, int n);
    }

    /**
     * Rank one update, computes mA + alpha * x * y^t and store it in mA
     *
     * @param alpha
     * @param mA The matrix
     * @param x
     * @param y
     */
    @SuppressWarnings("unchecked")
    static public void rankOneUpdate(double alpha, DoubleMatrix2D mA,
        DoubleMatrix1D x, DoubleMatrix1D y) {
        // --- Check dimensions

        if (x.size != mA.rows || y.size != mA.columns)
            throw new OutOfBoundsException(
                "Cannot do a rank one update of %s with %s * t(%s)", mA, x,
                y);

        // --- Find a suitable handler for the pair of matrices

        final Class<? extends DoubleMatrix2D> aClass = mA.getClass();
        final Class<? extends DoubleMatrix1D> bClass = x.getClass();
        final Class<? extends DoubleMatrix1D> cClass = y.getClass();

        TrieKey key = new TrieKey(aClass, bClass, cClass);

        Collection<RankOneUpdater> list = getMatchingCandidates(key,
            rankOneUpdaters, rankOneUpdatersCache);

        RankOneUpdater updater = null;
        double minComplexity = Double.POSITIVE_INFINITY;

        for (RankOneUpdater<?, ?, ?> candidate : list) {
            double complexity = candidate.complexity(x.size, y.size);
            logger.debug("Rank one updater %s for %s has complexity %g",
                candidate.getClass(), key, complexity);
            if (complexity < minComplexity) {
                minComplexity = complexity;
                updater = candidate;
            }
        }

        if (updater != null) {
            logger.debug("Selected updater %s", updater.getClass());
            updater.update(alpha, mA, x, y);
            return;
        }

        // --- Should not happen
        throw new RuntimeException(String.format(
            "Did not find a suitable multiplication handler for %s and %s",
            aClass, bClass));

    }

    final public static class DefautRankOneUpdater implements
        RankOneUpdater<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> {

        @Override
        public double complexity(int m, int n) {
            return (double)m * (double)n;
        }

        @Override
        public void update(final double alpha, final DoubleMatrix2D mA,
            final DoubleMatrix1D x, DoubleMatrix1D y) {
            y.forEachNonZero(new IntDoubleProcedure() {
                @Override
                public boolean apply(final int iy, final double vy) {
                    x.forEachNonZero(new IntDoubleProcedure() {
                        @Override
                        public boolean apply(int ix, double vx) {
                            mA.set(ix, iy, mA.get(ix, iy) + alpha * vx * vy);
                            return true;
                        }
                    });
                    return true;
                }
            });
        }
    }

    // --------------------------------
    // -------------------------------- 1D multiplication
    // -------------------------------- alpha * Ax + beta * x
    // --------------------------------

    /**
     * Type specific multiplier
     *
     * Compute alpha * Ax + beta * y and put the result in y
     *
     * @param <T>
     * @param <U>
     * @author bpiwowar
     */
    static public interface Multiplier1D<T extends DoubleMatrix2D, U extends DoubleMatrix1D, R extends DoubleMatrix1D> {
        /**
         * Computes alpha * Ax + beta * y (or alpha * Ax if y is null) and put the result in y
         *
         * @param A
         * @param x
         * @param y
         * @param alpha
         * @param beta
         * @param transposeA
         * @return y for convenience (if y was given) or a newly created matrix
         */
        R multiply(T A, U x, R y, double alpha, double beta, boolean transposeA);

        /**
         * Returns the complexity for the multiplication of a matrix of size m x n with a vector of size m
         *
         * @param m The number of rows in the matrix
         * @param n The size of the vector (or the number of columns of the matrix)
         * @return The complexity
         */
        double complexity(int m, int n);
    }

    /**
     * Computes alpha * A * x + beta * y (or alpha * Ax if y is null) and put the result in y
     *
     * @param A
     * @param x
     * @param y
     * @param alpha
     * @param beta
     * @param transposeA
     * @return y for convenience (if y was given) or a newly created matrix
     */
    public static DoubleMatrix1D multiply(DoubleMatrix2D A, DoubleMatrix1D x,
        DoubleMatrix1D y, double alpha, double beta, boolean transposeA) {

        // --- Check dimensions
        int m = transposeA ? A.columns : A.rows;
        int n = transposeA ? A.rows : A.columns;

        if (n != x.size)
            throw new IllegalArgumentException(
                String
                    .format(
                        "A %d x %d matrix and a vector of size %d cannot be multiplied",
                        m, n, x.size));

        if (y != null && y.size != m)
            throw new IllegalArgumentException(
                String
                    .format(
                        "The result of the multiplication of a %d x %d matrix with a %d vector cannot be put in a vector of size %d",
                        m, n, n, y.size));

        // --- Find a suitable handler for the pair of matrices

        final Class<? extends DoubleMatrix2D> aClass = A.getClass();
        final Class<? extends DoubleMatrix1D> bClass = x.getClass();
        final Class<? extends DoubleMatrix1D> cClass = y != null ? y.getClass()
            : null;

        TrieKey key = new TrieKey(aClass, bClass, cClass);
        @SuppressWarnings("unchecked")
        Collection<Multiplier1D> list = getMatchingCandidates(key,
            multipliers1D, multipliers1DCache);

        @SuppressWarnings("unchecked")
        Multiplier1D multiplier = null;
        double minComplexity = Double.POSITIVE_INFINITY;

        for (Multiplier1D<?, ?, ?> candidate : list) {
            double complexity = candidate.complexity(m, n);
            logger.debug("Multiplier %s for %s has complexity %g", candidate
                .getClass(), key, complexity);
            if (complexity < minComplexity) {
                minComplexity = complexity;
                multiplier = candidate;
            }
        }

        if (multiplier != null) {
            logger.debug("Selected multiplier %s", multiplier.getClass());

            @SuppressWarnings("unchecked")
            DoubleMatrix1D r = multiplier.multiply(A, x, y, alpha, beta,
                transposeA);
            return r;
        }

        // --- Should not happen
        throw new RuntimeException(String.format(
            "Did not find a suitable multiplication handler for %s and %s",
            aClass, bClass));

    }

    final public static class DefaultMultiplier1D implements
        Multiplier1D<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D> {

        @Override
        public double complexity(int m, int n) {
            return (double)m * (double)n * (double)n;
        }

        @Override
        public DoubleMatrix1D multiply(DoubleMatrix2D A, DoubleMatrix1D x,
            DoubleMatrix1D y, double alpha, double beta, boolean transposeA) {
            int m = transposeA ? A.columns : A.rows;
            int n = transposeA ? A.rows : A.columns;

            if (y == null)
                y = new DenseDoubleMatrix1D(m);

            for (int i = m; --i >= 0; ) {
                double s = 0;
                for (int j = n; --j >= 0; ) {
                    s += (transposeA ? A.get(j, i) : A.get(i, j)) * x.get(j);
                }
                y.set(i, alpha * s + beta * y.get(i));
            }

            return y;
        }

    }

    final public static DoubleMatrix1D multiply(DoubleMatrix2D A,
        DoubleMatrix1D x) {
        return multiply(A, x, null, 1, 0, false);
    }

    // --------------------------------
    // -------------------------------- Inner product
    // --------------------------------

    /**
     * Type specific multiplier
     *
     * Compute alpha * Ax + beta * y and put the result in y
     *
     * @param <T>
     * @param <U>
     * @author bpiwowar
     */
    static public interface InnerProduct<T extends DoubleMatrix1D, U extends DoubleMatrix1D> {
        /**
         * Computes the inner product of two vectors
         */
        public double innerProduct(T x, U y);

        /**
         * Returns the complexity for the multiplication of a matrix of size m x n with a vector of size m
         *
         * @param m The size of the vectors
         * @return The complexity
         */
        public double complexity(int m);
    }

    public static double innerProduct(DoubleMatrix1D x, DoubleMatrix1D y) {
        // --- Check dimensions
        int n = x.size();

        if (n != y.size)
            throw new IllegalArgumentException(String.format(
                "Cannot compute the inner product of %s and %s", x, y));

        // --- Find a suitable handler for the pair of matrices

        final Class<? extends DoubleMatrix1D> aClass = x.getClass();
        final Class<? extends DoubleMatrix1D> bClass = y.getClass();

        TrieKey key = new TrieKey(aClass, bClass);
        @SuppressWarnings("unchecked")
        Collection<InnerProduct> list = getMatchingCandidates(key,
            innerProducts, innerProductsCache);

        @SuppressWarnings("unchecked")
        InnerProduct handler = null;
        double minComplexity = Double.POSITIVE_INFINITY;

        for (InnerProduct<?, ?> candidate : list) {
            double complexity = candidate.complexity(n);
            logger.debug("Multiplier %s for %s has complexity %g", candidate
                .getClass(), key, complexity);
            if (complexity < minComplexity) {
                minComplexity = complexity;
                handler = candidate;
            }
        }

        if (handler != null) {
            logger.debug("Selected multiplier %s", handler.getClass());

            @SuppressWarnings("unchecked")
            double r = handler.innerProduct(x, y);
            return r;
        }

        // --- Should not happen
        throw new RuntimeException(String.format(
            "Did not find a suitable multiplication handler for %s and %s",
            aClass, bClass));

    }

    final public static class DefaultInnerProduct implements
        InnerProduct<DoubleMatrix1D, DoubleMatrix1D> {

        @Override
        public double complexity(int m) {
            return (double)m;
        }

        @Override
        public double innerProduct(DoubleMatrix1D x, DoubleMatrix1D y) {
            return staticInnerProduct(x, y);
        }

        static public double staticInnerProduct(DoubleMatrix1D x,
            DoubleMatrix1D y) {
            double s = 0;
            for (int i = x.size(); --i >= 0; )
                s += x.get(i) * y.get(i);
            return s;
        }

    }

    // --------------------------------
    // -------------------------------- Multiplier lists
    // --------------------------------

    /**
     * Get a matching multiplier using a cache
     *
     * @param <T>
     * @param key The key
     * @param candidates The potential candidates
     * @param cache The candidate cache
     * @return
     */
    private static <T> Collection<T> getMatchingCandidates(TrieKey key,
        List<Pair<TrieKey, T>> candidates, Map<TrieKey, List<T>> cache) {
        // Search into the cachs
        synchronized (cache) {
            List<T> list = cache.get(key);
            if (list != null)
                return list;

            list = GenericHelper.newArrayList();
            for (Pair<TrieKey, T> candidate : candidates) {
                Status compare = candidate.getFirst().compare(key);
                if (compare == Status.EQUAL || compare == Status.LESS) {
                    list.add(candidate.getSecond());
                    logger.debug("Added a multiplier for %s: %s", key,
                        candidate.getSecond().getClass());
                }
            }

            logger.debug("Added a cache for %s (of size %d)", key, list.size());
            cache.put(key, list);
            return list;
        }
    }

    /**
     * Our list of 2D multiplications
     */
    @SuppressWarnings("unchecked")
    static ArrayList<Pair<TrieKey, Multiplier2D>> multipliers2D = GenericHelper
        .newArrayList();
    @SuppressWarnings("unchecked")
    static HashMap<TrieKey, List<Multiplier2D>> multipliers2DCache = GenericHelper
        .newHashMap();

    /**
     * Our list of 1D multiplications
     */
    @SuppressWarnings("unchecked")
    static ArrayList<Pair<TrieKey, Multiplier1D>> multipliers1D = GenericHelper
        .newArrayList();
    @SuppressWarnings("unchecked")
    static HashMap<TrieKey, List<Multiplier1D>> multipliers1DCache = GenericHelper
        .newHashMap();

    /**
     * Our list of rank one updaters
     */
    @SuppressWarnings("unchecked")
    static ArrayList<Pair<TrieKey, RankOneUpdater>> rankOneUpdaters = GenericHelper
        .newArrayList();
    @SuppressWarnings("unchecked")
    static HashMap<TrieKey, List<RankOneUpdater>> rankOneUpdatersCache = GenericHelper
        .newHashMap();

    /**
     * Our list of inner product
     */
    @SuppressWarnings("unchecked")
    static ArrayList<Pair<TrieKey, InnerProduct>> innerProducts = GenericHelper
        .newArrayList();
    @SuppressWarnings("unchecked")
    static HashMap<TrieKey, List<InnerProduct>> innerProductsCache = GenericHelper
        .newHashMap();

    static final String MATRIX_MULTIPLY_CLASSES = "META-INF/services/bpiwowar.maths.matrix";

    @SuppressWarnings("unchecked")
    static void addClass(Class<?> aClass) throws InstantiationException,
        IllegalAccessException {
        logger.debug("Check class %s for matrix computation", aClass);

        for (Class<?> candidate : aClass.getDeclaredClasses()) {

            // 2D Multiplier
            if (Multiplier2D.class.isAssignableFrom(candidate)
                && !candidate.isInterface()) {
                logger.debug("Candidate is %s", candidate);
                Type[] targs = GenericHelper.getActualTypeArguments(candidate,
                    Multiplier2D.class);

                if (targs.length != 3)
                    throw new RuntimeException(
                        "Multiplier should only have two classes");
                TrieKey key = new TrieKey((Class)targs[0], (Class)targs[1],
                    (Class)targs[2]);
                logger.debug("Adding %s for %s", candidate, key);
                multipliers2D.add(Pair.create(key, (Multiplier2D)candidate
                    .newInstance()));

            }

            // Rank one updater
            else if (RankOneUpdater.class.isAssignableFrom(candidate)
                && !candidate.isInterface()) {
                logger.debug("Candidate is %s", candidate);
                Type[] targs = GenericHelper.getActualTypeArguments(candidate,
                    RankOneUpdater.class);
                if (targs.length != 3)
                    throw new RuntimeException(
                        "Rank one updater should only have three classes");
                TrieKey key = new TrieKey((Class)targs[0], (Class)targs[1],
                    (Class)targs[2]);
                logger.debug("Adding %s for %s", candidate, key);
                rankOneUpdaters.add(Pair.create(key, (RankOneUpdater)candidate
                    .newInstance()));
            }

            // 1D Multiplier
            else if (Multiplier1D.class.isAssignableFrom(candidate)
                && !candidate.isInterface()) {
                logger.debug("Candidate is %s", candidate);
                Type[] targs = GenericHelper.getActualTypeArguments(candidate,
                    Multiplier1D.class);
                if (targs.length != 3)
                    throw new RuntimeException(
                        "Multiplier should only have two classes");
                TrieKey key = new TrieKey((Class)targs[0], (Class)targs[1],
                    (Class)targs[2]);
                logger.debug("Adding %s for %s", candidate, key);
                multipliers1D.add(Pair.create(key, (Multiplier1D)candidate
                    .newInstance()));
            }

            // Inner product
            else if (InnerProduct.class.isAssignableFrom(candidate)
                && !candidate.isInterface()) {
                logger.debug("Candidate is %s", candidate);
                Type[] targs = GenericHelper.getActualTypeArguments(candidate,
                    InnerProduct.class);
                if (targs.length != 2)
                    throw new RuntimeException(
                        "Multiplier should only have two classes");
                TrieKey key = new TrieKey((Class)targs[0], (Class)targs[1]);
                logger.debug("Adding %s for %s", candidate, key);
                innerProducts.add(Pair.create(key, (InnerProduct)candidate
                    .newInstance()));
            }

        }
    }

    static {
        try {
            final ClassLoader classLoader = Multiply.class.getClassLoader();
            final Enumeration<URL> e = classLoader
                .getResources(MATRIX_MULTIPLY_CLASSES);
            addClass(Multiply.class);
            while (e.hasMoreElements()) {
                final URL url = e.nextElement();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                    url.openStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    String classname = line;
                    Class<?> aClass = classLoader.loadClass(classname);
                    addClass(aClass);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    // --------------------------------
    // -------------------------------- Trie helper methods
    // --------------------------------

    static public class TrieKey implements WeakComparable<TrieKey> {
        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(classes);
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            TrieKey other = (TrieKey)obj;
            if (!Arrays.equals(classes, other.classes)) {
                return false;
            }
            return true;
        }

        /**
         * Compare two classes
         *
         * @param a
         * @param b
         * @return
         */
        final static public WeakComparable.Status compare(Class<?> a, Class<?> b) {
            if (a == b)
                return Status.EQUAL;

            if (a == null)
                // a is null, but b is not (a is more specific)
                return Status.GREATER;

            // a is not null
            if (b == null)
                return Status.LESS;

            // both a and b are not null
            if (a.equals(b))
                return Status.EQUAL;

            // a and b are different classes
            if (a.isAssignableFrom(b))
                // a is a superclass: less specific
                return Status.LESS;

            if (b.isAssignableFrom(a))
                // a is a subclass: more specific
                return Status.GREATER;

            // Not the same branch
            return Status.NOT_COMPARABLE;
        }

        Class<?>[] classes = new Class<?>[3];

        public TrieKey(Class<?>... classes) {
            this.classes = classes;
        }

        @Override
        public String toString() {
            return "(" + Output.toString(",", classes) + ")";
        }

        @Override
        public WeakComparable.Status compare(TrieKey b) {
            if (b == this)
                return Status.EQUAL;

            Status compare = null;
            for (int i = 0; i < classes.length
                && compare != Status.NOT_COMPARABLE; i++) {
                Status c = compare(classes[i], b.classes[i]);

                if (compare == null || compare == Status.EQUAL)
                    compare = c;
                else if (c != Status.EQUAL && c != compare)
                    compare = Status.NOT_COMPARABLE;
            }

            logger.trace("Compared %s with %s: %s", this, b, compare);
            return compare;
        }

    }
}

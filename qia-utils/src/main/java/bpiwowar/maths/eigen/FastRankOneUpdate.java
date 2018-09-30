package bpiwowar.maths.eigen;

import bpiwowar.log.Logger;
import bpiwowar.maths.Misc;
import bpiwowar.maths.eigen.selector.Selector;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.maths.matrix.DoubleMatrix2DFactory;
import cern.jet.math.Mult;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import org.apache.log4j.Level;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.String.format;

/**
 * Computes the EVD of D + alpha * z * z^t where D is a diagonal matrix, alpha a real and z a vector
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class FastRankOneUpdate implements RankOneUpdate, Serializable {

    private static final long serialVersionUID = 1L;

    final static Logger logger = Logger.getLogger();

    private static final Comparator<IndexedValue> LAMBDA_COMPARATOR = new Comparator<IndexedValue>() {
        @Override
        public int compare(IndexedValue o1, IndexedValue o2) {
            // Special rule to order after the removed parts
            int z = (o1.isRemoved() ? 1 : 0) - (o2.isRemoved() ? 1 : 0);
            if (z != 0)
                return z;

            // We have to invert the order since we want biggest
            // values
            // first - that is, returns -1 if o1 > o2
            return Double.compare(o2.lambda, o1.lambda);
        }
    };

    private static final Comparator<IndexedValue> DIAGONAL_COMPARATOR = new Comparator<IndexedValue>() {
        @Override
        public int compare(IndexedValue o1, IndexedValue o2) {
            return Double.compare(o2.d, o1.d);
        }
    };

    /**
     * A list of eigenvalues
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static final class EigenValues implements EigenList {
        private final IndexedValue[] values;
        int minRemoved;
        int rank;

        EigenValues(IndexedValue[] values) {
            this.values = values;
            this.rank = values.length;
            this.minRemoved = values.length;
        }

        @Override
        public double get(int index) {
            return values[index].lambda;
        }

        @Override
        public void remove(int index) {
            if (!values[index].isRemoved()) {
                logger.debug("Removing eigenvalue %d (%e)", index,
                    values[index].lambda);
                minRemoved = Math.min(index, minRemoved);
                values[index].setRemoved(true);
                rank--;
            }
        }

        @Override
        public int size() {
            return values.length;
        }

        @Override
        public boolean isSelected(int i) {
            return !values[i].isRemoved();
        }

        @Override
        public int rank() {
            return rank;
        }

    }

    /**
     * Each value is indexed
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static final class IndexedValue {
        /**
         * The rank in the final matrix
         */
        int newPosition;

        /**
         * The new corresponding eigenvalue ( >= original)
         */
        double lambda;

        /**
         * The original eigenvalue
         */
        double d;

        /**
         * The corresponding z
         */
        double z;

        /**
         * The status (selection)
         */
        BitSet status = new BitSet(2);

        /**
         * Original position (-1 if not part of the original matrix)
         */
        int position = -1;

        /**
         * Set the selection status
         *
         * @param value
         */
        void setSelected(boolean value) {
            status.set(0, value);
        }

        /**
         * Was this eigenvalue removed from the final decomposition (by the selection algorithm)
         */
        boolean isRemoved() {
            return status.get(1);
        }

        /**
         * Set the removed value (see {@link #isRemoved()}
         */
        void setRemoved(boolean value) {
            status.set(1, value);
        }

        /**
         * Was this eigenvalue selected (otherwise it was deflated through rotation or zeroing)
         */
        boolean isSelected() {
            return status.get(0);
        }

        public IndexedValue(int position, double d, double z) {
            super();
            this.position = position;
            this.d = d;
            this.z = z;
            this.lambda = d;
        }

        @Override
        public String toString() {
            return String
                .format("(rank=%d, position=%d, lambda=%e, d=%e, z=%e, s=%b, r=%b)",
                    newPosition, position, lambda, d, z, isSelected(),
                    isRemoved());
        }
    }

    static final class Rotation {
        // The rotation matrix [ c, s; -s, c ]
        final double c, s;
        // Which is the singular value column we rotated with
        final IndexedValue vi, vj;

        public Rotation(double c, double s, IndexedValue vi, IndexedValue vj) {
            super();
            this.c = c;
            this.s = s;
            this.vi = vi;
            this.vj = vj;
        }
    }

    /**
     * Used for deflation
     */
    double gamma = 10;

    /**
     * Double precision
     */
    final static double EPSILON = Misc.DOUBLE_PRECISION;

    public FastRankOneUpdate() {
    }

    static final double sqr(double x) {
        return x * x;
    }

    DoubleMatrix2DFactory<?> uFactory = DenseDoubleMatrix2D.FACTORY,
        vFactory = DenseDoubleMatrix2D.FACTORY;

    /**
     * Set the factory for both U and V
     *
     * @param factory
     */
    public void setFactories(DoubleMatrix2DFactory<?> uFactory,
        DoubleMatrix2DFactory<?> vFactory) {
        this.uFactory = uFactory;
        this.vFactory = vFactory;
    }

    @Override
    public Result rankOneUpdate(DoubleMatrix2D D, double rho, DoubleMatrix1D z,
        boolean computeEigenvectors, Selector selector, boolean keep) {
        // ---
        // --- Deflate the matrix (see. G.W. Steward, p. 176)
        // ---

        // The deflated diagonal and corresponding z
        // The matrix we are working on is a (N + 1, N)
        final int N = z.size();
        final int rankD = D.columns();
        if (rankD > N)
            throw new bpiwowar.lang.RuntimeException(
                "D (%s) and z (%s) are not compatible in broken arrow SVD",
                D, z);

        logger.debug(
            "Initialisation for a rank-1 update of a %d x %1$d diagonal matrix (rho=%e, d(D)=%d)",
            N, rho, rankD);

        // The algorithm assumes that rho > 0, so that we keep track of this
        // here, and modify rho so that it is strictly positive
        final boolean negativeUpdate = rho < 0;
        if (negativeUpdate)
            rho = -rho;

        // The norm of ||D||
        double normD = 0;

        // Our store for singular values, D, and z
        final IndexedValue[] v = new IndexedValue[N];

        // Copy the diagonal entries (possibly inserting zeros if needed)
        int offset = N - rankD;
        // i' is the position on the D diagonal
        int iprime = rankD - 1;
        boolean foundNonNegative = false;
        boolean toSort = false;

        for (int i = N; --i >= 0; ) {
            double di = 0;

            // Check the current D[i', i'] if necessary
            if (iprime >= 0 && offset > 0 && !foundNonNegative)
                foundNonNegative = D.get(iprime, iprime) >= 0;

            int position = 0;
            final int index = negativeUpdate ? N - 1 - i : i;

            // If i' points on the first non negative
            double zpos = 0;
            if (iprime < 0 || (offset > 0 && foundNonNegative)) {
                // di is zero in that part
                // and zi comes from the end of the z vector
                offset--;
                position = z.size() + offset - (N - rankD);
                zpos = z.get(position);
                logger.debug("For v(%d), we have d=0 and z=z_%d=%e", index,
                    z.size() + offset - (N - rankD), zpos);
            }
            else {
                di = D.get(iprime, iprime);
                di = negativeUpdate ? -di : di;
                position = i - offset;
                zpos = z.get(position);
                logger.debug("For v(%d), we have d=d_%d=%e and z=z_%d=%e",
                    index, iprime, di, i - offset, zpos);
                iprime--;
            }

            normD += di * di;

            // Just checking
            final int previousIndex = negativeUpdate ? N - 1 - (i + 1) : i + 1;
            if (i != N - 1 && (negativeUpdate ^ v[previousIndex].d > di)) {
                logger.debug(
                    "Diagonal matrix is not sorted D[%d,%<d] = %e and D[%d,%<d]=%e: we will sort it latter",
                    previousIndex, v[previousIndex].d, index, di);
                toSort = true;
            }

            // If the update is negative, we have to reverse the order since
            // we take the opposite of diagonal entries
            v[index] = new IndexedValue(position, di, sqrt(rho) * zpos);
        }
        normD = sqrt(normD);

        // Sort if needed
        if (toSort) {
            logger.debug("Sorting the diagonal matrix representation");
            sortValues(v, 0, DIAGONAL_COMPARATOR);
        }

        // M is the dimension of the deflated diagonal matrix
        int M = 0;

        final double tau = gamma * EPSILON;
        final double tauM2 = tau * normD;
        logger.debug("taum2 = %e (%e)", tauM2, normD);
        double mzNorm = 0;

        // The list of rotations
        ArrayList<Rotation> rotations = new ArrayList<Rotation>();

        // Deflate the matrix and order the singular values
        IndexedValue last = null;
        for (int i = 0; i < N; i++) {
            final IndexedValue vi = v[i];
            final double zi = vi.z;

            logger.debug("Looking at column %d: %e, %e", i, abs(zi),
                M > 0 ? last.d - vi.d : Double.NaN);
            if (abs(zi) <= tauM2) {
                logger.debug("Deflating column %d (z_%d=%e close to 0)",
                    vi.position, i, zi);
            }
            else if (M > 0 && (last.d - vi.d <= tauM2)) {
                final double r = Math.sqrt(sqr(last.z) + sqr(zi));
                rotations.add(new Rotation(last.z / r, zi / r, last, vi));
                last.z = r;
                vi.z = 0;
                logger.debug(
                    "Deflating column %d with rotation with column %d",
                    vi.position, last.position);
            }
            else {
                // Else just copy the values
                last = vi;
                mzNorm += zi * zi;
                M++;
                vi.setSelected(true);
            }
        }

        logger.debug("Matrix deflation finished: %d to %d (%d rotations)", N,
            M, rotations.size());

        // Order the array v
        // so that the first M ones are within the
        // deflated matrix and the N-M last are outside
        // and preserving the order
        // last is the last non selected here
        int lastFree = -1;
        if (N != M)
            for (int i = 0; i < N; i++) {
                final IndexedValue vi = v[i];
                if (!vi.isSelected()) {
                    if (lastFree < 0)
                        lastFree = i;
                }
                else if (lastFree >= 0) {
                    // We have some room here
                    v[i] = v[lastFree];
                    v[lastFree] = vi;
                    if (lastFree + 1 < i)
                        lastFree++;
                    else
                        lastFree = i;
                }
            }

        if (logger.isTraceEnabled())
            logger.trace("v=%s", Arrays.toString(v));

        // ---
        // --- Search for singular values (solving the secular equation)
        // ---

        // if (LAPACK.register()) {
        // LAPACK.register();
        // logger.info("Computing with LAPACK");
        // double[] lapackZ = new double[M];
        // double[] lapackD = new double[M];
        // double[] lapackDelta = new double[M];
        //
        // double normZ = 0;
        // for (int j = 0; j < M; j++)
        // normZ += sqr(v[j].z);
        // normZ = sqrt(normZ);
        //
        // for (int j = 0; j < M; j++) {
        // final IndexedValue svj = v[j];
        // lapackZ[M - j - 1] = svj.z / normZ;
        // lapackD[M - j - 1] = svj.d;
        // }
        //
        // IntByReference info = new IntByReference();
        // DoubleByReference dj = new DoubleByReference();
        // IntByReference lapackM = new IntByReference(M);
        // DoubleByReference lapackRho = new DoubleByReference(rho * normZ *
        // normZ);
        //
        // for (int j = 0; j < M; j++) {
        // IntByReference lapackJ = new IntByReference(j + 1);
        // LAPACK.dlaed4(lapackM, lapackJ, lapackD, lapackZ, lapackDelta,
        // lapackRho, dj, info);
        // v[M - j - 1].lambda = dj.getValue();
        // logger.info(
        // "Output value for the %dth eigenvalue: %d (from %g to %g)",
        // j, info.getValue(), lapackD[j], dj.getValue());
        // }
        // } else {

        // For the stopping criterion
        final double e = gamma * EPSILON * M;

        logger.debug("Searching %d eigenvalues (bisection)", M);
        for (int j = 0; j < M; j++) {
            final IndexedValue svj = v[j];
            final double diagj = svj.d;

            double interval = (j == 0 ? mzNorm : v[j - 1].d - diagj) / 2;
            final double middle = diagj + interval;

            // Stopping criteria from Gu & Eisenstat
            double psi = 0;
            double phi = 0;
            logger.debug(
                "Searching for singular value between %e and %e (interval %e)",
                diagj, diagj + interval * 2, interval);

            double nu = -interval;
            double f = -1;
            do {
                // Update nu
                // TODO enhance the root finder by using a better
                // approximation
                final double oldnu = nu;
                if (f < 0)
                    nu += interval;
                else
                    nu -= interval;

                if (nu == oldnu) {
                    logger.debug("Stopping since we don't change f anymore");
                    break;
                }

                // Compute the new phi, psi and f
                psi = phi = 0;

                // lambda is between diagj and (diagj1 + diagj)/2
                for (int i = j; i < M; i++) {
                    final IndexedValue vi = v[i];
                    psi += sqr(vi.z) / (vi.d - middle - nu);
                }

                for (int i = 0; i < j; i++) {
                    final IndexedValue vi = v[i];
                    phi += sqr(vi.z) / (vi.d - middle - nu);
                }

                f = 1 + psi + phi;

                if (logger.isTraceEnabled())
                    logger.trace(
                        "phi=%e, psi=%e, nu is %e, f is %e, threshold is %e (interval %e)",
                        phi, psi, nu, f, (1 + abs(psi) + abs(phi)) * e,
                        interval);
                interval /= 2;
            }
            while (Double.isInfinite(f)
                || abs(f) > (1 + abs(psi) + abs(phi)) * e);

            // Done, store the eigen value
            // logger.info("LAPACK value = %g, computed value = %g", svj.lambda,
            // middle + nu);
            svj.lambda = middle + nu;
            logger.debug("Found %dth eigen value (%e) with f=%e", j + 1,
                svj.lambda, f);

            // Because of rounding errors, that can happen
            if (svj.lambda < diagj) {
                final double delta = diagj - svj.lambda;
                logger.log(
                    delta > 1e-14 ? Level.WARN : Level.DEBUG,
                    "lambda_%d (%e) inferior to d_%1$d (%e), delta=%e; increasing value to bound",
                    j, svj.lambda, diagj, delta);
                svj.lambda = diagj;
            }
            else {
                final double max = j == 0 ? mzNorm + diagj : v[j - 1].d;
                if (svj.lambda > max) {
                    final double delta = svj.lambda - max;
                    logger.log(
                        delta > 1e-14 ? Level.WARN : Level.DEBUG,
                        "lambda_%d (%e) superior to d_%d (%e), delta=%e; decreasing value to bound",
                        j, svj.lambda, j - 1, max, delta);
                    svj.lambda = max;
                }
            }
        }

        // }

        // ---
        // --- Compute the singular vectors
        // ---
        if (logger.isDebugEnabled())
            logger.debug("v=%s", Arrays.toString(v));

        logger.debug("Compute the new z");

        // First, recompute z to match the singular values we have

        final double lambda0 = v[0].lambda;

        for (int i = 0; i < M; i++) {
            final IndexedValue vi = v[i];
            double di = vi.d;
            double newz = computeZ(v, M, lambda0, i, vi, di, false);

            if (logger.isDebugEnabled() && Double.isNaN(newz))
                newz = computeZ(v, M, lambda0, i, vi, di, true);

            logger.debug("z_%d goes from %e to %e (delta=%e)", i, vi.z, newz,
                abs(vi.z - newz));

            // Remove z too close to 0
            if (abs(newz) < tauM2) {
                v[i].setSelected(false);
                logger.debug("z_%d has been removed from selection [too low]",
                    i);
            }
            vi.z = newz;
        }

        // --- Let's construct the result ---
        logger.debug("Construct the result");
        Result result = new Result();

        // --- First, take the opposite of eigenvalues if
        // --- we are doing a negative update
        if (negativeUpdate)
            for (IndexedValue iv : v) {
                iv.d = -iv.d;
                iv.lambda = -iv.lambda;
            }

        // --- Set eigen values (and the rank)

        // Select the eigenvalues if needed
        sortValues(v, 0, LAMBDA_COMPARATOR);

        int rank = v.length;
        if (selector != null) {
            final EigenValues list = new EigenValues(v);
            selector.selection(list);
            logger.debug(
                "After the selector was applied, our rank is %d (it was %d)",
                list.rank, rank);
            rank = list.rank;

            // Reorder if needed
            if (rank < N && (list.minRemoved != rank)) {
                logger.debug("Re-ordering since %d != %d", list.minRemoved,
                    rank);
                sortValues(v, list.minRemoved, LAMBDA_COMPARATOR);
                if (logger.isDebugEnabled())
                    logger.debug("v=%s", Arrays.toString(v));
            }

        }

        // then store them,
        int nbSelected = 0;
        int nbNaN = 0;
        result.mD = new DiagonalDoubleMatrix(rank);
        for (int i = rank - 1; i >= 0; i--) {
            v[i].newPosition = i;
            result.mD.set(i, i, v[i].lambda);
            if (v[i].isSelected())
                nbSelected++;
            if (Double.isNaN(v[i].lambda))
                nbNaN++;
        }

        if (nbNaN > 0)
            throw new ArithmeticException(format("We had %d eigen value(s) that is/are NaN", nbNaN));

        logger.debug("Number of selected values: %d out of %d", nbSelected,
            rank);

        // --- Compute V

        if (computeEigenvectors) {
            // Creates the matrix
            DoubleMatrix2D Q = result.Q = vFactory.create(N, rank);

            // Set the new values
            for (int j = 0; j < rank; j++) {
                final IndexedValue vj = v[j];
                if (!vj.isSelected()) {
                    Q.set(vj.position, j, 1);
                }
                else {
                    // Compute the new vector
                    double columnNorm = 0;
                    int iM = 0;
                    for (int i = 0; i < N && iM < M; i++) {
                        final IndexedValue vi = v[i];
                        if (vi.isSelected()) {
                            final double di = vi.d;
                            double x = vi.z / (di - vj.lambda);
                            columnNorm += x * x;
                            Q.set(vi.position, j, x);

                            if (Double.isNaN(x))
                                logger.warn(
                                    "Here we have a NaN (%d): %e, %e, %e, %e, %e",
                                    vi.position, vi.z, di, vj.lambda,
                                    (di - vj.lambda), (di + vj.lambda));
                            iM++;
                        }
                    }

                    // Normalize
                    Q.viewColumn(j).assign(Mult.div(sqrt(columnNorm)));
                }
            }

            // --- Rotate the vectors of U that need to be rotated
            for (int r = rotations.size(); --r >= 0; ) {
                final Rotation rot = rotations.get(r);
                DoubleMatrix1D vi = Q.viewRow(rot.vi.position);
                DoubleMatrix1D vj = Q.viewRow(rot.vj.position);

                // Rotation only affect the two rows i and j
                for (int col = 0; col < rank; col++) {
                    double x = vi.get(col);
                    double y = vj.get(col);
                    vi.set(col, x * rot.c - y * rot.s);
                    vj.set(col, x * rot.s + y * rot.c);
                }
            }

            if (!keep && rank < N)
                Q = Q.viewPart(0, 0, rank, rank);

        }

        return result;

    }

    /**
     * @param v
     */
    private void sortValues(final IndexedValue[] v, int from,
        Comparator<IndexedValue> comparator) {
        Arrays.sort(v, from, v.length, comparator);
    }

    /**
     * Compute the value of z
     *
     * @param v
     * @param M
     * @param lambda0
     * @param i
     * @param vi
     * @param di
     * @param newz
     * @return
     */
    private static double computeZ(final IndexedValue[] v, int M,
        final double lambda0, int i, final IndexedValue vi, double di,
        boolean debug) {
        double newz = -(di - lambda0);
        if (debug)
            logger.debug("[1] d_%d - l_%d = %e - %e = %e", i, 0, di, lambda0,
                newz);

        // lambda_j < di
        for (int j = i + 1; j < M; j++) {
            final IndexedValue vj = v[j];
            if (debug)
                logger.debug("[1/j=%d] %e * %e / %e -> %e", j, newz,
                    (di - vj.lambda), (di - vj.d), newz * (di - vj.lambda)
                        / (di - vj.d));
            newz *= (di - vj.lambda) / (di - vj.d);
        }

        for (int j = 1; j <= i; j++) {
            if (debug)
                logger.debug("[2/j=%d] %e * %e / %e -> %e", j, newz,
                    (di - v[j].lambda), (di - v[j - 1].d), newz
                        * (di - v[j].lambda) / (di - v[j - 1].d));
            newz *= (di - v[j].lambda) / (di - v[j - 1].d);
        }

        newz = Math.signum(vi.z) * sqrt(newz);

        return newz;
    }
}

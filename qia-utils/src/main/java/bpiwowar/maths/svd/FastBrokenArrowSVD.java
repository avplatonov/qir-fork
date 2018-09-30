package bpiwowar.maths.svd;

import bpiwowar.log.Logger;
import bpiwowar.maths.Misc;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
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

/**
 * Performs the SVD of a matrix of the form [D; z^t] where D is a diagonal square matrix of size (n x n) and z is vector
 * of size n. This is more general than
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class FastBrokenArrowSVD implements BrokenArrowSVD, Serializable {
    private static final long serialVersionUID = 1L;

    final static Logger logger = Logger.getLogger();

    static final class IndexedValue {
        /**
         * The rank in the final matrix
         */
        int rank;

        /**
         * The singular value
         */
        double omega;

        /**
         * The original singular value
         */
        double d;

        /**
         * The corresponding z
         */
        double z;

        /**
         * were we selected?
         */
        BitSet status = new BitSet(2);

        /**
         * Original position (-1 if not part of the original matrix)
         */
        int position = -1;

        void setSelected(boolean value) {
            status.set(0, value);
        }

        boolean isSelected() {
            return status.get(0);
        }

        public IndexedValue(int position, double d, double z) {
            super();
            this.position = position;
            this.d = d;
            this.z = z;
            this.omega = d;
        }

        @Override
        public String toString() {
            return String.format(
                "(rank=%d, position=%d, omega=%e, d=%e, z=%e, s=%b)", rank,
                position, omega, d, z, isSelected());
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

    private boolean computeV = true;

    private boolean computeU = true;

    public FastBrokenArrowSVD(boolean computeU, boolean computeV) {
        this.computeU = computeU;
        this.computeV = computeV;
    }

    public FastBrokenArrowSVD() {
    }

    /**
     * Returns x^2 - y^2
     *
     * @param x
     * @param y
     * @return x^2 - y^2
     */
    static final double diffSqr(double x, double y) {
        return (x + y) * (x - y);
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

    /*
     * (non-Javadoc)
     *
     * @see
     * bpiwowar.maths.svd.BrokenArrowSVD#computeSVD(cern.colt.matrix.DoubleMatrix2D
     * , cern.colt.matrix.DoubleMatrix1D, double, boolean)
     */
    public Result computeSVD(DoubleMatrix2D D, DoubleMatrix1D z, int maxRank) {
        // ---
        // --- Deflate the matrix (from Matrix algorithms volume 2, G.W.
        // --- Stewart, p.174-176) and Gu & Einsenstat, adapted
        // --- for the matrix of dimensions (n+1 x n)
        // ---
        // --- d_1
        // --- ...
        // --- d_n-1
        // --- 0 ... 0 0
        // --- z_1 ... z_n-1 z_n

        logger.debug("Initialisation");

        // The deflated diagonal and corresponding z
        // The matrix we are working on is a (N + 1, N)
        final int N = D.rows() + 1;
        if (N != z.size())
            throw new bpiwowar.lang.RuntimeException(
                "D (%s) and z (%s) are not compatible in broken arrow SVD",
                D, z);

        double normD = 0;

        // Our store for singular values, D, and z
        final IndexedValue[] v = new IndexedValue[N];

        // Copy the diagonal entries
        for (int i = N - 1; i >= 0; i--) {
            if (i < N - 1) {
                final double di = D.get(i, i);
                normD += di * di;
                if (v[i + 1].d > di)
                    throw new RuntimeException(String.format(
                        "D[%d,%<d] = %e > %e = D[%d,%<d]", i + 1,
                        v[i + 1].d, di, i));
                v[i] = new IndexedValue(i, di, z.get(i));
            }
            else
                v[i] = new IndexedValue(-1, 0, z.get(i));

        }

        // M is the dimension of the deflated diagonal matrix
        int M = 0;

        normD = sqrt(normD);
        final double tau = gamma * EPSILON;
        final double tauM2 = tau * normD;
        logger.debug("taum2 = %e (%e)", tauM2, normD);
        double mzNorm = 0;

        ArrayList<Rotation> rotations = new ArrayList<Rotation>();

        // Deflate the matrix and order the singular values
        IndexedValue last = null;
        for (int i = 0; i < N; i++) {
            final IndexedValue vi = v[i];
            final double zi = vi.z;

            logger.debug("Looking at column %d: %e, %e", i, abs(zi),
                M > 0 ? last.d - vi.d : Double.NaN);
            if (abs(zi) <= tauM2) {
                logger.debug("Deflating column %d", vi.position);
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
                continue;
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

        if (logger.isDebugEnabled())
            logger.debug("v=%s", Arrays.toString(v));

        // ---
        // --- Search for singular values
        // ---

        // For the stopping criterion
        final double e = gamma * EPSILON * M;

        logger.debug("Searching %d eigenvalues", M);
        for (int j = 0; j < M; j++) {
            final IndexedValue svj = v[j];
            final double diagj = svj.d;

            double interval = (j == 0 ? mzNorm : v[j - 1].d - diagj) / 2;
            final double middle = diagj + interval;

            // Stopping criteria from Gu & Eisenstat
            double psi = 0;
            double phi = 0;
            logger
                .debug(
                    "Searching for singular value between %e and %e (interval %e)",
                    diagj, diagj + interval * 2, interval);

            // Contrarily to Gu & Einsenstat,
            // We use a nu from the middle of the interval
            double nu = -interval;
            double f = -1;
            do {
                // Change mu
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

                // omega is between diagj and (diagj1 + diagj)/2
                for (int i = j; i < M; i++) {
                    final IndexedValue vi = v[i];
                    psi += sqr(vi.z)
                        / ((vi.d - middle - nu) * (vi.d + middle + nu));
                }

                for (int i = 0; i < j; i++) {
                    final IndexedValue vi = v[i];
                    phi += sqr(vi.z)
                        / ((vi.d - middle - nu) * (vi.d + middle + nu));
                }

                f = 1 + psi + phi;

                if (logger.isTraceEnabled())
                    logger
                        .trace(
                            "phi=%e, psi=%e, nu is %e, f is %e, threshold is %e (interval %e)",
                            phi, psi, nu, f, (1 + abs(psi) + abs(phi))
                                * e, interval);
                interval /= 2;
            }
            while (Double.isInfinite(f)
                || abs(f) > (1 + abs(psi) + abs(phi)) * e);

            // Done, store the singular value
            svj.omega = middle + nu;
            logger.debug("Found %dth singular value (%e) with f=%e", j + 1,
                svj.omega, f);

            // Because of rounding errors, that can happen
            if (svj.omega < diagj) {
                final double delta = svj.omega - diagj;
                logger
                    .log(
                        abs(delta / svj.omega) > 1e-10 ? Level.WARN
                            : Level.DEBUG,
                        "omega_%d (%e) inferior to d_%1$d (%e), delta=%e; increasing value to bound",
                        j, svj.omega, diagj, svj.omega - diagj);
                svj.omega = diagj;
            }
            else {
                final double max = j == 0 ? mzNorm + diagj : v[j - 1].d;
                if (svj.omega > max) {
                    final double delta = svj.omega - max;
                    logger
                        .log(
                            abs(delta / svj.omega) > 1e-10 ? Level.WARN
                                : Level.DEBUG,
                            "omega_%d (%e) superior to d_%d (%e), delta=%e; decreasing value to bound",
                            j, svj.omega, j - 1, max, delta);
                    svj.omega = max;
                }
            }
        }

        // ---
        // --- Compute the singular vectors
        // ---
        if (logger.isDebugEnabled())
            logger.debug("v=%s", Arrays.toString(v));

        logger.debug("Compute the new z");

        // First, recompute z to match the singular values we have
        // (lemma 2.2 of gu & eisenstat)
        // [note that values are ordered the other way in gu et al.]

        final double omega0 = v[0].omega;

        for (int i = 0; i < M; i++) {
            final IndexedValue vi = v[i];
            double di = vi.d;
            double newz = computeZ(v, M, omega0, i, vi, di, false);

            if (logger.isDebugEnabled() && Double.isNaN(newz))
                newz = computeZ(v, M, omega0, i, vi, di, true);

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

        // --- Set sigma values (and the rank)
        // first, sort all singular values (decreasing order)
        Arrays.sort(v, new Comparator<IndexedValue>() {
            @Override
            public int compare(IndexedValue o1, IndexedValue o2) {
                return Double.compare(o2.omega, o1.omega);
            }
        });

        // Compute the rank
        double eps = Math.pow(2.0, -52.0);
        double tolerance = (N + 1d) * v[0].omega * eps;
        int rank = 1;
        for (int i = 1; i < v.length && v[i].omega > tolerance; i++)
            rank++;
        rank = Math.min(rank, maxRank);
        logger.debug("New matrix rank: %d (tolerance %e)", rank, tolerance);

        // then store them,
        int nbSelected = 0;
        int nbNaN = 0;
        result.S = new DiagonalDoubleMatrix(rank);
        for (int i = rank - 1; i >= 0; i--) {
            v[i].rank = i;
            result.S.set(i, i, v[i].omega);
            if (v[i].isSelected())
                nbSelected++;
            if (Double.isNaN(v[i].omega))
                nbNaN++;
        }

        if (nbNaN > 0)
            logger.warn("We have %d singular value(s) that is/are NaN", nbNaN);

        logger.debug("Number of selected values: %d out of %d", nbSelected,
            rank);

        // --- Compute U
        if (computeU) {

            DoubleMatrix2D U = result.U = uFactory.create(N, rank);

            // Extra row
            DoubleMatrix1D Ue = new DenseDoubleMatrix1D(rank);

            // Set the new values
            for (int j = 0; j < rank; j++) {
                final IndexedValue vj = v[j];
                logger.debug(
                    "Constructing column %d of U (original position: %d)",
                    j, vj.position);
                if (!vj.isSelected()) {
                    final int s = vj.z < 0 ? -1 : 1;
                    if (vj.position >= 0)
                        U.set(vj.position, j, s);
                    else
                        Ue.set(j, s);
                }
                else {
                    // Compute the new vector (lemma 2.1, eq. 2.3)
                    U.set(N - 1, j, -1);
                    double columnNorm = 1;

                    int iM = 0;
                    for (int i = 0; i < N && iM < M - 1; i++) {
                        final IndexedValue vi = v[i];
                        if (vi.isSelected()) {
                            final double di = vi.d;
                            double x = vi.z * di
                                / ((di - vj.omega) * (di + vj.omega));
                            columnNorm += x * x;
                            if (vi.position >= 0)
                                U.set(vi.position, j, x);
                            else
                                Ue.set(j, x);
                            if (Double.isNaN(x))
                                logger
                                    .debug(
                                        "Here we have a NaN (%d): %e, %e, %e, %e, %e",
                                        vi.position, vi.z, di,
                                        vj.omega, (di - vj.omega),
                                        (di + vj.omega));
                            iM++;
                        }
                    }

                    // Normalize
                    U.viewColumn(j).assign(Mult.div(sqrt(columnNorm)));
                }

            }

            // Rotate the vectors of U that need to be rotated
            for (int r = rotations.size() - 1; r >= 0; r--) {
                final Rotation rot = rotations.get(r);
                DoubleMatrix1D ui = rot.vi.position >= 0 ? U
                    .viewRow(rot.vi.position) : Ue;
                DoubleMatrix1D uj = rot.vj.position >= 0 ? U
                    .viewRow(rot.vj.position) : Ue;

                logger.debug("Rotation (c=%e,s=%e) between %d and %d", rot.c,
                    rot.s, rot.vi.position, rot.vj.position);
                // The only non null indice in uj is
                // Rotation only affect the two rows i and j
                for (int col = 0; col < rank; col++) {
                    double ui_k = ui.get(col);
                    double uj_k = uj.get(col);
                    ui.set(col, ui_k * rot.c - uj_k * rot.s);
                    if (uj != Ue)
                        uj.set(col, uj_k * rot.c + ui_k * rot.s);
                }
            }
        }

        // --- Compute V

        if (computeV) {
            DoubleMatrix2D V = result.V = vFactory.create(N, rank);
            // Set the new values
            for (int j = 0; j < rank; j++) {
                final IndexedValue vj = v[j];
                if (!vj.isSelected()) {
                    int posj = vj.position >= 0 ? vj.position : N - 1;
                    V.set(posj, j, 1);
                }
                else {
                    // Compute the new vector (lemma 2.1, eq. 2.5)
                    double columnNorm = 0;
                    int iM = 0;
                    for (int i = 0; i < N && iM < M; i++) {
                        final IndexedValue vi = v[i];
                        if (vi.isSelected()) {
                            int posi = vi.position >= 0 ? vi.position : N - 1;
                            final double di = vi.d;
                            double x = vi.z
                                / ((di - vj.omega) * (di + vj.omega));
                            columnNorm += x * x;
                            V.set(posi, j, x);
                            if (Double.isNaN(x))
                                logger
                                    .warn(
                                        "Here we have a NaN (%d): %e, %e, %e, %e, %e",
                                        vi.position, vi.z, di,
                                        vj.omega, (di - vj.omega),
                                        (di + vj.omega));
                            iM++;
                        }
                    }

                    // Normalize
                    V.viewColumn(j).assign(Mult.div(sqrt(columnNorm)));
                }
            }

            // Rotate the vectors of U that need to be rotated
            for (int r = rotations.size() - 1; r >= 0; r--) {
                final Rotation rot = rotations.get(r);
                DoubleMatrix1D vi = rot.vi.position >= 0 ? V
                    .viewRow(rot.vi.position) : V.viewRow(N - 1);
                DoubleMatrix1D vj = rot.vj.position >= 0 ? V
                    .viewRow(rot.vj.position) : V.viewRow(N - 1);
                // Rotation only affect the two rows i and j
                for (int col = 0; col < rank; col++) {
                    double x = vi.get(col);
                    double y = vj.get(col);
                    vi.set(col, x * rot.c - y * rot.s);
                    vj.set(col, x * rot.s + y * rot.c);
                }
            }
        }

        return result;

    }

    /**
     * @param v
     * @param M
     * @param omega0
     * @param i
     * @param vi
     * @param di
     * @param newz
     * @return
     */
    private double computeZ(final IndexedValue[] v, int M, final double omega0,
        int i, final IndexedValue vi, double di, boolean debug) {
        double newz = diffSqr(omega0, di);
        if (debug)
            logger.debug("[1] w_%d - d_%d %e", i, 0, newz);

        // omega_j < di
        for (int j = i + 1; j < M; j++) {
            final IndexedValue vj = v[j];
            if (debug)
                logger.debug("[1/j=%d] %e * %e / %e -> %e", j, newz, diffSqr(
                    vj.omega, di), diffSqr(vj.d, di), newz
                    * diffSqr(vj.omega, di) / diffSqr(vj.d, di));
            newz *= diffSqr(vj.omega, di) / diffSqr(vj.d, di);
        }

        for (int j = 1; j <= i; j++) {
            if (debug)
                logger.debug("[2/j=%d] %e * %e / %e -> %e", j, newz, diffSqr(
                    v[j].omega, di), diffSqr(v[j - 1].d, di), newz
                    * diffSqr(v[j].omega, di) / diffSqr(v[j - 1].d, di));
            newz *= diffSqr(v[j].omega, di) / diffSqr(v[j - 1].d, di);
        }

        newz = vi.z <= 0 ? 0 - sqrt(newz) : sqrt(newz);

        return newz;
    }

    @Override
    public void setWantU(boolean wantU) {
        this.computeU = wantU;
    }

    @Override
    public void setWantV(boolean wantV) {
        this.computeV = wantV;
    }
}

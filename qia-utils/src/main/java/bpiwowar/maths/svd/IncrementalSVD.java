package bpiwowar.maths.svd;

import bpiwowar.NotImplementedException;
import bpiwowar.io.LoggerPrintWriter;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.maths.matrix.DoubleMatrix2DFactory;
import bpiwowar.maths.matrix.Multiply;
import cern.colt.function.DoubleDoubleFunction;
import cern.jet.math.Functions;
import java.io.Serializable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static bpiwowar.maths.matrix.Multiply.multiply;
import static bpiwowar.maths.matrix.Multiply.rankOneUpdate;

/**
 * Incremental SVD.
 *
 * <p>
 * Computes the left-singular vectors and the singular values, i.e. if<br/> A=U S V^T <br/> then the method keeps track
 * of U and S
 * </p>
 *
 * <p>
 * based on the work of M. Brand, "Fast low-rank modifications of the thin singular value decomposition" (DOI
 * 10.1016/j.laa.2005.07.021)
 * </p>
 *
 * <p>
 * The hypothesis is that there are much than columns are added one by one, whereas the rows of the matrix are all
 * known, i.e. it corresponds to find the SVD of A = [A c] where c is a new column when the SVD of A is already known.
 * </p>
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class IncrementalSVD implements Serializable {
    private static final long serialVersionUID = 2L;

    /**
     * Should we recycle memory (i.e. keep old matrices to store next results?)
     */
    boolean recycleMemory = true;

    /**
     * If we want matrix V to be computed
     */
    boolean wantV;

    /**
     * Factories for the different matrices
     */
    DoubleMatrix2DFactory<?> u1Factory, u2Factory, v1Factory, v2Factory;

    /**
     * Our broken arrow SVD handler
     */
    BrokenArrowSVD brokenArrowSVD;

    /**
     * The norm of the complete matrix
     */
    private double matrixSquaredNorm;

    /**
     * Set the matrix factories for all
     *
     * @param m
     */
    public void setFactory(DoubleMatrix2DFactory<?> m) {
        u1Factory = u2Factory = v1Factory = v2Factory = m;
    }

    /**
     * Set the matrix factories, specifying the matrix to use for each of the matrices used in incremental SVD
     *
     * @param u1Factory
     * @param u2Factory
     * @param v1Factory
     * @param v2Factory
     */
    public void setFactories(DoubleMatrix2DFactory<?> u1Factory,
        DoubleMatrix2DFactory<?> u2Factory,
        DoubleMatrix2DFactory<?> v1Factory,
        DoubleMatrix2DFactory<?> v2Factory) {
        this.u1Factory = u1Factory;
        this.u2Factory = u2Factory;
        this.v1Factory = v1Factory;
        this.v2Factory = v2Factory;
    }

    /**
     * Set the broken arrow SVD handler
     *
     * @param brokenArrowSVD
     */
    public void setBrokenArrowSVD(BrokenArrowSVD brokenArrowSVD) {
        this.brokenArrowSVD = brokenArrowSVD;
        brokenArrowSVD.setWantU(wantV);
    }

    /**
     * @param wantV True if the SVD should keep track of the row space
     */
    public IncrementalSVD(boolean wantU, boolean wantV) {
        super();
        this.wantV = wantV;
        u1Factory = u2Factory = v1Factory = v2Factory = DenseDoubleMatrix2D.FACTORY;
        brokenArrowSVD = new StandardBrokenArrowSVD(wantV, true);
    }

    /**
     * @param wantV True if the SVD should keep track of the row space
     */
    public IncrementalSVD(boolean wantU, boolean wantV,
        DoubleMatrix2DFactory<?> factory) {
        super();
        this.wantV = wantV;
        setFactory(factory);
        setBrokenArrowSVD(new StandardBrokenArrowSVD(wantV, true));
    }

    /**
     * Temporary matrices to avoid too much memory (de)allocation - should not be serialized
     */
    transient DoubleMatrix2D _U1, _U2;

    /**
     * p x r matrix containing a part of the information about U (U in Brand) with U = U1 x U2
     */
    DoubleMatrix2D U1;

    /**
     * r x r matrix containing a part of the information about U (U prime in Brand) with U = U1 x U2
     */
    DoubleMatrix2D U2;

    /**
     * V1 is a q x r matrix (V1 x V2 = V)
     */
    DoubleMatrix2D V1;

    /**
     * V' is a non singular rxr matrix
     */
    DoubleMatrix2D V2;

    /**
     * V'+ is V' pseudo-inverse transposed
     */
    DoubleMatrix2D V2PT;

    /** r x r matrix containing the singular values */
    DoubleMatrix2D S;

    /** Current number of rows */
    int nbRows = 0;

    /**
     * Number of columns (of the decomposed matrix), i.e. number of added columns
     */
    int q = 0;

    /** Current dimensionality */
    int r = 0;

    /** Maximum rank */
    private int maxRank = Integer.MAX_VALUE;

    private static final Logger logger = Logger.getLogger(IncrementalSVD.class);

    /**
     * Return the number of columns, i.e. the number of rows of V
     *
     * @return
     */
    public int getNumberOfColumns() {
        return q;
    }

    /**
     * Set the maximum rank of the decomposition
     *
     * @param maxRank The maximum rank
     */
    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

    /**
     * Add a new vector and updates the representation of the different matrices
     */
    public void addColumnVector(DoubleMatrix1D a) {
        logger.debug("*** New vector for incremental SVD ***");

        q++;
        if (logger.isDebugEnabled())
            logger.debug(String.format("Adding a vector of size %d", a.size()));

        final double aSqrNorm = Multiply.innerProduct(a, a);
        matrixSquaredNorm += aSqrNorm;

        // *** Special treatment: we have to initialise ***
        if (nbRows == 0 || r == 0) {
            r = 1;
            nbRows = a.size();

            // --- Initialize matrix U

            _U2 = U2 = null;

            if (U1 == null)
                U1 = u1Factory.create(a.size(), 1);
            else
                U1 = U1.resize(a.size(), 1);

            double aNorm = Math.sqrt(aSqrNorm);

            for (int i = 0; i < nbRows; i++)
                U1.set(i, 0, a.get(i) / aNorm);

            // --- We have only one singular value of value aNorm
            S = new DiagonalDoubleMatrix(new double[] {aNorm});

            // --- Initialize matrix V (there are scalars at the beginning,
            // since the row space is of dimension 1)
            if (wantV) {
                V1 = v1Factory.create(1, 1);
                V1.set(0, 0, 1);
                V2 = v2Factory.create(1, 1);
                V2.set(0, 0, 1);
                V2PT = v2Factory.create(1, 1);
                V2PT.set(0, 0, 1);
            }
            return;
        }

        // *** Normal flow ***

        // Adjust the size of U1 if the vector size has more rows
        if (a.size() > nbRows)
            U1 = U1.resize(a.size(), r);
        nbRows = U1.rows();

        // m <- U^T a = U2^T U1^T a
        DoubleMatrix1D mp = new DenseDoubleMatrix1D(r + 1);
        DoubleMatrix1D m = mp.viewPart(0, r);
        if (U2 != null) {
            final DoubleMatrix1D U1t_x_a = multiply(U1, a, null, 1, 0, true);
            multiply(U2, U1t_x_a, m, 1, 0, true);
        }
        else
            multiply(U1, a, m, 1, 0, true);

        // p <- a - U m = a - U1 U2 m = a - U1 U2 U2^T U1^T a
        // i.e. p is a residue (after the projection)
        final DoubleMatrix1D p = a.copy();
        multiply(U1, U2 != null ? multiply(U2, m) : m, p, -1, 1, false);
        final double pNorm = Math.sqrt(p.aggregate(0, Functions.plus,
            Functions.square, true));
        mp.set(r, pNorm);

        if (Double.isNaN(pNorm))
            debugPisNaN(a, mp);

        // Singular decomposition of K into C S D^T

        if (logger.isDebugEnabled())
            debugBrokenArrowSVD1(mp, p);

        BrokenArrowSVD.Result result = null;
        result = brokenArrowSVD.computeSVD(S, mp, Math.min(a.size(), q));
        final int newRank = result.rank();

        // The SVD is on the transpose, so C is V and D is U
        final DoubleMatrix2D C = result.getV();
        final DoubleMatrix2D D = wantV ? result.getU() : null;

        if (logger.isDebugEnabled())
            debugBrokenArrowSVD2(mp, result, newRank, C, D);

        // Set the new singular values
        S = result.getS();

        // *** The rank has not changed (or we don't want it to increase) ***
        final boolean goodReason = (r >= newRank) || (r >= nbRows) || (r >= q);
        if (r >= getMaxRank() || goodReason) {
            if (r > newRank) {
                if (wantV)
                    throw new NotImplementedException();
                logger.info(String.format("Rank is going down from %d to %d", r, newRank));
                U1 = multiply(U1, U2);
                U1.viewPart(0, 0, U1.rows(), newRank);
                U2 = null;
                r = newRank;
            }

            // U2 <- U2 C
            final DoubleMatrix2D Cr = C.viewPart(0, 0, r, r);
            if (U2 != null) {
                _U2 = _U2 != null ? _U2.resize(r, r, true, false) : u2Factory
                    .create(r, r);
                _U2 = multiply(U2, Cr, _U2);
                swapU2();
            }
            else {
                U2 = Cr;
            }

            if (S.columns() > r)
                S = S.viewPart(0, 0, r, r);

            // In case we don't have a higher rank because there is a fixed
            // limit,
            // we still have to update U1 by U1 <- U1 + p / || p || (U2 x)
            // where x is defined by C = (Cr b; x^t c)

            // Error is bounded by ||U2|| ||x|| = r sqrt(1 - c^2)
            // double c = C.get(r, r);
            // final double error = c * c * (double)r;
            // TODO use this to avoid update if not necessary

            if (!goodReason) {
                logger.debug("Rank one update of U1 is necessary");
                final DoubleMatrix1D x = C.viewRow(r).viewPart(0, r);
                DoubleMatrix1D U2a = multiply(U2, x);

                U1 = U1.resize(a.size(), r);
                rankOneUpdate(1. / pNorm, U1, p, U2a);
            }

            // --- Update V
            if (wantV) {
                // Decompose: D = [ W; w ]
                DoubleMatrix2D W = D.viewPart(0, 0, r, r);
                DoubleMatrix2D w = D.viewPart(r, 0, 1, r);

                // V2 <- V2(rxr) W(rxr)
                V2 = multiply(V2, W, v2Factory.create(V2.rows(), W.columns()));

                // V2P <- WP V2P
                // where WP = W^T + w^T / (1 - ||w||^2) (w W^T)

                // i.e. V2P^T <- V2P^T WP^T
                // where WP^T = W + W w^T w / (1 - ||w||^2) = (1/ (1 - ||w||^2))
                // * W (w^T w) + 1. * W
                double wSqNorm = w.aggregate(0, Functions.plus,
                    Functions.square, true);
                DoubleMatrix2D WPT = multiply(W, multiply(w, w, null, 1, 0,
                    true, false), v2Factory.create(W.rows(), W.columns()),
                    1. / (1. - wSqNorm), 0, false, false);

                for (int i = 0; i < r; i++)
                    for (int j = 0; j < r; j++)
                        WPT.set(i, j, WPT.get(i, j) + W.get(i, j));
                V2PT = multiply(V2PT, WPT, v2Factory.create(WPT.rows(), V2PT
                    .columns()));

                // V1 <- [ V1; w V2P] with V2P new
                int q = V1.rows();
                V1 = V1.resize(q + 1, r);
                DoubleMatrix2D V1LastRow = V1.viewPart(q, 0, 1, r);

                LoggerPrintWriter out = null;
                if (logger.isDebugEnabled())
                    out = printV1AndW(w, V1LastRow);

                multiply(w, V2PT, V1LastRow, 1, 0, false, true);

                if (logger.isDebugEnabled())
                    printV1(V1LastRow, out);
            }

        }

        // *** Dimensionality increases ***
        else {
            assert r + 1 == newRank;
            r = newRank;

            // U1 <- [U1 p/||p||]
            U1 = U1.resize(a.size(), r);
            for (int i = p.size() - 1; i >= 0; --i) {
                U1.set(i, r - 1, p.get(i) / pNorm);
            }

            // U2 <- [ U2 0, 0 1 ] C
            // C is of dimension r x r, and U2 is of dimension r-1 x r-1
            // result is of dimension r x r
            if (U2 != null)
                _U2 = IdentityAddedPostMultiplication(U2, C, u2Factory, _U2);
            else
                _U2 = C;

            // Swap U2 and _U2 so that U2 can be reused
            swapU2();

            // --- Update V
            if (wantV) {
                // V1 <- [ V1 0; 0 1 ]
                V1 = V1.resize(V1.rows() + 1, r);
                V1.set(r - 1, r - 1, 1);

                // V2 <- [ V2 0; 0 1 ] D
                V2 = IdentityAddedPostMultiplication(V2, D, v2Factory, null);

                // V2P <- D^T [ V2P 0; 0 1 ], with D^T=[D(r-1) a; b k] and V2P
                // i.e. V2P^T <- [V2P^T 0; 0 1] D
                V2PT = IdentityAddedPostMultiplication(V2PT, D, v2Factory, null);
            }
        }

    }

    /**
     * @param V1LastRow
     * @param out
     */
    private void printV1(DoubleMatrix2D V1LastRow, LoggerPrintWriter out) {
        {
            out.format("V1 = %s%n", V1);
            V1.print(out);
            out.format("V1LastRow = %s%n", V1LastRow);
            V1LastRow.print(out);
            out.flush();
        }
    }

    /**
     * @param w
     * @param V1LastRow
     * @return
     */
    private LoggerPrintWriter printV1AndW(DoubleMatrix2D w,
        DoubleMatrix2D V1LastRow) {
        LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.DEBUG);
        out.println("V1 last row computation");
        out.format("V1 = %s%n", V1);
        V1.print(out);
        out.format("V1LastRow = %s%n", V1LastRow);
        V1LastRow.print(out);
        out.format("w = %s%n", w);
        w.print(out);
        out.format("V2PT = %s%n", V2PT);
        V2PT.print(out);
        return out;
    }

    /**
     * @param a
     * @param mp
     */
    private void debugPisNaN(DoubleMatrix1D a, DoubleMatrix1D mp) {
        {
            LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.WARN);
            logger.warn(String.format("p-norm is NaN for the %dth vector", q));
            logger.warn(String.format("Matrix a: %s", a));
            a.print(out);
            out.flush();
            logger.warn(String.format("Matrix U1: %s", U1));
            U1.print(out);
            out.flush();
            if (U2 != null) {
                logger.warn(String.format("Matrix U2: %s", U2));
                U2.print(out);
                out.flush();
            }
            else
                logger.warn("U2 = I");
            logger.warn(String.format("Matrix mp: %s", mp));
            mp.print(out);
            out.flush();
        }
    }

    /**
     * @param mp
     * @param p
     */
    private void debugBrokenArrowSVD1(DoubleMatrix1D mp, DoubleMatrix1D p) {
        {
            LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.DEBUG);

            out.println("U1 = " + U1);
            U1.print(out);
            out.flush();
            out.println("U2 = " + U2);
            U1.print(out);
            out.flush();
            out.println("p = " + p);
            p.print(out);
            out.flush();
            out.println("S = " + S);
            S.print(out);
            out.flush();
            out.println("mp = " + mp);
            mp.print(out);
            out.flush();
        }
    }

    /**
     * @param mp
     * @param result
     * @param rank
     * @param C
     * @param D
     */
    private void debugBrokenArrowSVD2(DoubleMatrix1D mp,
        BrokenArrowSVD.Result result, final int rank,
        final DoubleMatrix2D C, final DoubleMatrix2D D) {
        LoggerPrintWriter out = new LoggerPrintWriter(logger, Level.DEBUG);
        logger.debug("initial rank = " + r);

        out.println("C = " + C);
        C.print(out);
        out.flush();

        out.println("S = " + S);
        S.print(out);
        out.flush();

        if (wantV) {
            out.println("D = " + D);
            D.print(out);
            out.flush();

            final DoubleMatrix2D SDT = multiply(result.getS(), D.viewDice());
            final DoubleMatrix2D M = multiply(C, SDT);
            out.println("CSD^T = " + M);
            M.print(out);
            out.flush();

            final DoubleDoubleFunction chain = Functions.chain(
                Functions.square, Functions.minus);
            double fNorm =
                // The S part
                M.viewPart(0, 0, S.rows(), S.columns()).aggregate(0, S,
                    Functions.plus, chain, true)
                    // plus the part on the below
                    + M.viewPart(S.rows(), 0, 1, S.columns()).aggregate(0,
                    Functions.plus, Functions.square, true)
                    // plus the mp part
                    + M.viewColumn(S.columns()).aggregate(0, mp,
                    Functions.plus, chain, true);

            logger.debug(String.format("Broken-arrows SVD fnorm is %e", fNorm));

        }

        logger.debug("Rank is " + rank);
    }

    /**
     * Swap U2 and _U2, and clear _U2 if we are not recycling memory
     */
    private void swapU2() {
        if (recycleMemory) {
            DoubleMatrix2D __U2 = U2;
            U2 = _U2;
            _U2 = __U2;
        }
        else {
            U2 = _U2;
            _U2 = null;
        }
    }

    /**
     * Performs [ A 0; 0 1 ] B with A(p x r) B(r+1 x q)
     *
     * @param factory
     * @param _A A matrix that can be used instead of using the factory
     * @return [ A 0; 0 1 ] x B of dimension (p+1) x q
     */
    private static DoubleMatrix2D IdentityAddedPostMultiplication(
        DoubleMatrix2D A, DoubleMatrix2D B,
        DoubleMatrix2DFactory<?> factory, DoubleMatrix2D _A) {
        // Decomposing B as [ B'] r x q
        // [ b ] 1 x q
        // we have [ A 0; 0 1 ] B = [ AB'; b ]
        final int p = A.rows();
        final int r = A.columns();
        final int q = B.columns();

        // Create matrix C (first trying to resize)
        DoubleMatrix2D C = null;
        if (_A != null) {
            // Resize but do not copy the values if a new structure is allocated
            C = _A.resize(p + 1, q, true, false);
            logger.debug(String.format("Resized old A (%s)", C));
        }
        else
            C = factory.create(p + 1, q);

        assert r + 1 == B.rows() : String.format("%d is different from %d",
            r + 1, B.rows());

        // C(pxq) <- A(pxr) * B(rxq)
        multiply(A, B.viewPart(0, 0, r, q), C.viewPart(0, 0, p, q));

        // Copy the row r+1 of B to the row p of C
        for (int j = q; --j >= 0; )
            C.set(p, j, B.get(r, j));

        return C;
    }

    /**
     * Compute U
     *
     * @return
     */
    public DoubleMatrix2D computeU() {
        if (U2 != null) {
            U1 = multiply(U1, U2);
            U2 = null;
        }
        return U1;
    }

    public DoubleMatrix1D computeU(int j) {
        if (U2 != null)
            return multiply(U1, U2.viewColumn(j));
        return U1.viewColumn(j);
    }

    /**
     * Compute V
     *
     * @return
     */
    public DoubleMatrix2D computeV() {
        return multiply(V1, V2);
    }

    public DiagonalDoubleMatrix getSigma() {
        return (DiagonalDoubleMatrix)S;
    }

    /**
     * Reduces as much as possible the internal matrices before storage
     */
    public void trimMatrices() {
        // Reduce matrix U1 to its size
        if (U2 != null)
            U1 = multiply(U1, U2);
        U2 = null;
        U1.trimToSize();

        if (wantV) {
            V1.trimToSize();
            V2.trimToSize();
            V2PT.trimToSize();
        }
    }

    /**
     * Change listener interface
     *
     * @author bpiwowar
     */
    public interface ChangeListener {
        /**
         * @param from The id of the index that is moved (or -1)
         * @param to The new index of from, or if from is -1, the deleted index
         */
        void change(int from, int to);
    }

    /**
     * Remove the all zeros rows of the column space (useful when some dimensions have disappeared due to a low rank
     * approximation)
     *
     * @param handler The handler is called everytime a row disappears and is replaced by another one. Note that the
     * implementation guarantees that a given row will only be removed once.
     * @param maxValue Maximum value for the sum of the square of the row (0 to remove zeros), exclusive. This should
     * normally be 0 since otherwise this can break the orthogonality of the column space.
     */
    public void removeRows(ChangeListener handler, double maxValue) {
        // Compute U
        if (U2 != null)
            U1 = multiply(U1, U2);
        U2 = null;

        // U1 is U since U2 is identity

        // The new p
        int newP = nbRows;

        for (int i = 0; i < newP; i++) {
            // (1) Search for a non zero row
            if (!rowSumExceeds(maxValue, i, U1, S)) {
                // We had a zero one, search for a non zero
                // from newP
                while (--newP > i) {
                    // Found it -- copy the row
                    if (rowSumExceeds(maxValue, newP, U1, S)) {
                        if (wantV) {
                            // TODO: handle the case where V is needed
                            throw new NotImplementedException();
                        }

                        U1.copyRow(newP, i);

                        // Notify the handler and go back to the search of a
                        // zero row
                        handler.change(newP, i);
                        break;
                    }
                    else
                        // newP is just removed since it is zero
                        handler.change(-1, newP);
                }

                // It means that row i (or newP) should be removed
                if (newP == i)
                    handler.change(-1, newP);
            }
        }

        logger.debug(String.format("Reduced the number of rows from %d to %d", nbRows, newP));
        nbRows = newP;
        U1 = U1.viewPart(0, 0, nbRows, r);
    }

    /**
     * Computes the square sum of a row and check if it is below a threshold
     *
     * The computed sum is sum_j S_{ii} U_{ij}^2
     *
     * @param maxValue The threshold
     * @param row The row
     * @param U The matrix for which the sum
     * @param S A diagonal matrix whose coefficients are multiplied to the corresponding columns values
     * @return True if the square
     */
    static private boolean rowSumExceeds(double maxValue, int row,
        final DoubleMatrix2D U, final DoubleMatrix2D S) {
        double sum = 0;
        final int columns = U.columns();
        for (int j = 0; j < columns; j++) {
            final double x = U.get(row, j);
            sum += S.get(j, j) * x * x;
            if (sum > maxValue)
                return true;
        }
        return false;
    }

    public DoubleMatrix2D getU1() {
        return U1;
    }

    public DoubleMatrix2D getU2() {
        return U2;
    }

    public DoubleMatrix2D getV1() {
        return V1;
    }

    public DoubleMatrix2D getV2() {
        return V2;
    }

    public int getRank() {
        return r;
    }

    final public double getSigma(int i) {
        return S.get(i, i);
    }

    /**
     * Return the squared sum of all the vectors
     */
    public double getMatrixSquaredNorm() {
        return matrixSquaredNorm;
    }

    /**
     * @return the maxRank
     */
    public int getMaxRank() {
        return maxRank;
    }

}

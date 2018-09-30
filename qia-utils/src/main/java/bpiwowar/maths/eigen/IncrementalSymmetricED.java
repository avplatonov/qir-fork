package bpiwowar.maths.eigen;

import bpiwowar.log.Logger;
import bpiwowar.maths.Misc;
import bpiwowar.maths.eigen.RankOneUpdate.Result;
import bpiwowar.maths.eigen.selector.Selector;
import bpiwowar.maths.matrix.DenseDoubleMatrix1D;
import bpiwowar.maths.matrix.DenseDoubleMatrix2D;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.maths.matrix.DoubleMatrix2DFactory;
import bpiwowar.maths.matrix.Multiply;
import java.io.Serializable;

import static bpiwowar.maths.matrix.Multiply.innerProduct;
import static bpiwowar.maths.matrix.Multiply.multiply;
import static bpiwowar.maths.matrix.Multiply.rankOneUpdate;
import static java.lang.Math.sqrt;

/**
 * Incremental eigendecomposition for symmetric rank one update.
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class IncrementalSymmetricED implements Serializable {
    private static final Logger logger = Logger.getLogger();
    // static {
    // logger.setLevel(Level.DEBUG);
    // }

    private static final long serialVersionUID = 2L;

    final static double EPSILON = Misc.DOUBLE_PRECISION * 10.;

    /**
     * We store U in the form U1 (n x p) * U2 (p x q). This ratio tells us when we should compute update U1 so that it
     * equals U1 * U2 and reset U2 to identity, in the case p / q < minRatio
     */
    double minRatio = 0.5;

    /**
     * Should we recycle memory (i.e. keep old matrices to store next results?)
     */
    boolean recycleMemory = true;

    /**
     * Factories for the different matrices
     */
    DoubleMatrix2DFactory<?> q1Factory, u2Factory;

    /**
     * Our broken arrow SVD handler
     */
    RankOneUpdate brokenArrowSVD;

    /**
     * Set the matrix factories for all
     *
     * @param m
     */
    public void setFactory(DoubleMatrix2DFactory<?> m) {
        q1Factory = u2Factory = m;
    }

    /**
     * Set the matrix factories, specifying the matrix to use for each of the matrices used in incremental SVD
     *
     * @param u1Factory
     * @param u2Factory
     */
    public void setFactories(DoubleMatrix2DFactory<?> u1Factory,
        DoubleMatrix2DFactory<?> u2Factory) {
        this.q1Factory = u1Factory;
        this.u2Factory = u2Factory;
    }

    /**
     * Set the broken arrow SVD handler
     *
     * @param brokenArrowSVD
     */
    public void setBrokenArrowSVD(RankOneUpdate brokenArrowSVD) {
        this.brokenArrowSVD = brokenArrowSVD;
    }

    /**
     * @param wantV True if the SVD should keep track of the row space
     */
    public IncrementalSymmetricED() {
        super();
        q1Factory = u2Factory = DenseDoubleMatrix2D.FACTORY;
        brokenArrowSVD = new FastRankOneUpdate();
    }

    /**
     * Temporary matrices to avoid too much memory (de)allocation - should not be serialized
     */
    transient DoubleMatrix2D _mQ2;

    /**
     * p x r matrix containing a part of the information about U (U in Brand) with U = U1 x U2
     */
    private DoubleMatrix2D mQ1;

    /**
     * r x r matrix containing a part of the information about U (U prime in Brand) with U = U1 x U2
     */
    private DoubleMatrix2D mQ2;

    /** r x r matrix containing the singular values */
    DoubleMatrix2D mS;

    /** Current number of rows */
    int nbRows = 0;

    /**
     * Number of updates
     */
    int numberOfUpdates = 0;

    /** Current rank */
    int rank = 0;

    /**
     * The provided selector
     */
    private Selector selector = null;

    /**
     * Set the eigenvalues selector
     *
     * @param selector
     */
    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public Selector getSelector() {
        return selector;
    }

    /**
     * Add a new vector and updates the representation of the different matrices, i.e. update the matrix by rho * a *
     * a^T
     *
     * @param rho The coefficient for the rank one update
     * @param a The vector to add
     */
    public void update(double rho, DoubleMatrix1D a) {
        logger.debug("*** New rank one update ***");

        numberOfUpdates++;
        if (logger.isDebugEnabled())
            logger.debug("Adding a vector of size %d", a.size());

        final double aSqrNorm = Multiply.innerProduct(a, a);

        // ---------------------------- First update

        if (rank == 0) {
            rank = 1;
            nbRows = a.size();

            // --- Initialize matrix U
            _mQ2 = mQ2 = null;
            if (mQ1 == null)
                mQ1 = q1Factory.create(a.size(), 1);
            else
                mQ1 = mQ1.resize(a.size(), 1);

            double aNorm = Math.sqrt(aSqrNorm);

            for (int i = 0; i < nbRows; i++)
                mQ1.set(i, 0, a.get(i) / aNorm);

            // --- We have only one singular value of value aNorm
            mS = new DiagonalDoubleMatrix(new double[] {rho * aSqrNorm});
            return;
        }

        // ---------------------------- Normal flow

        // Adjust the size of U1 if the vector size has more rows
        if (a.size() > nbRows)
            mQ1 = mQ1.resize(a.size(), mQ1.columns());
        nbRows = mQ1.rows();

        // m <- U^T a = U2^T U1^T a
        DoubleMatrix1D mp = new DenseDoubleMatrix1D(rank + 1);
        DoubleMatrix1D m = mp.viewPart(0, rank);
        if (mQ2 != null) {
            final DoubleMatrix1D U1t_x_a = multiply(mQ1, a, null, 1, 0, true);
            multiply(mQ2, U1t_x_a, m, 1, 0, true);
        }
        else
            multiply(mQ1, a, m, 1, 0, true);

        // p <- a - U m = a - U1 U2 m = a - U1 U2 U2^T U1^T a
        // i.e. p is a residue (after the projection)
        final DoubleMatrix1D p = a.copy();
        multiply(mQ1, mQ2 != null ? multiply(mQ2, m) : m, p, -1, 1, false);
        final double pNorm = sqrt(innerProduct(p, p));
        mp.set(rank, pNorm);

        // --- Rank one update of a diagonal matrix

        // First make sure rank cannot go higher if it shouldn't.
        final boolean extraDimension = nbRows > rank && numberOfUpdates > rank
            && (pNorm >= sqrt(aSqrNorm) * EPSILON);
        logger.debug("Extra dimension : %b (||p||=%e, ||a||*epsilon=%e)",
            extraDimension, pNorm, sqrt(aSqrNorm) * EPSILON);

        // The resulting Q matrix is n x n', where n is the old rank (possibly
        // +1), and n' is the new rank
        Result result = brokenArrowSVD.rankOneUpdate(mS, rho,
            extraDimension ? mp : m, true, selector, true);

        final int newRank = result.mD.rows();
        assert result.mD.rows() <= (extraDimension ? mp : m).size();

        final int eigenRank = result.Q.rows();

        logger.debug("Old rank is %d, eigen rank is %d, new rank is %d", rank,
            eigenRank, newRank);

        // The SVD is on the transpose, so C is V and D is U
        final DoubleMatrix2D mX = result.Q;

        // Set the new singular values
        mS = result.mD;

        // ---------------------------- Dimensionality does not increase
        if (rank >= newRank) {
            final DoubleMatrix2D mX1 = mX.viewPart(0, 0, rank, newRank);

            // --- Normal part of the update
            // Q2 <- Q2 * X1 [dimension: r x newRank]
            if (mQ2 != null) {
                _mQ2 = _mQ2 != null ? _mQ2.resize(mQ2.rows(), newRank, true,
                    false) : u2Factory.create(mQ2.rows(), newRank);
                _mQ2 = multiply(mQ2, mX1, _mQ2);
                swapQ2();
            }
            else {
                mQ2 = mX.viewPart(0, 0, mQ1.columns(), newRank);
            }

            // The case where we need to update Q1 is when
            // ||X_2|| > threshold
            if (extraDimension) {
                final DoubleMatrix1D x = mX.viewRow(eigenRank - 1).viewPart(0,
                    newRank);
                final double innerX = innerProduct(x, x);
                final double error = sqrt(innerX / (double)x.size());
                logger
                    .debug(
                        "Rank is going down from %d to %d [||x||=%e and error is %e]",
                        rank, newRank, sqrt(innerX), error);
                if (error > EPSILON) {
                    // Q1 <- Q1 * Q2 + Q'1 X2
                    mQ1 = multiply(mQ1, mQ2);
                    mQ2 = null;
                    rankOneUpdate(1. / pNorm, mQ1, p, x);
                }
            }
            else {
                double ratio = (double)mQ2.rows() / (double)mQ2.columns();
                if (ratio < minRatio) {
                    logger.debug(
                        "Ratio Q2.rows/Q2.columns = %e < %e, resetting Q2",
                        ratio, minRatio);
                    mQ1 = multiply(mQ1, mQ2);
                    mQ2 = null;
                }
            }

            // ---- Reduce the diagonal matrix
            if (newRank < eigenRank)
                mS = mS.viewPart(0, 0, newRank, newRank);

            rank = newRank;

        }

        // ---------------------------- Dimensionality increases

        else {
            assert rank + 1 == newRank;
            rank = newRank;

            // U1 <- [U1 p/||p||]
            mQ1 = mQ1.resize(a.size(), mQ1.columns() + 1);
            for (int i = p.size() - 1; i >= 0; --i) {
                mQ1.set(i, rank - 1, p.get(i) / pNorm);
            }

            // U2 <- [ U2 0, 0 1 ] C
            // C is of dimension r x r, and U2 is of dimension r-1 x r-1
            // result is of dimension r x r
            if (mQ2 != null)
                _mQ2 = IdentityAddedPostMultiplication(mQ2, mX, u2Factory, _mQ2);
            else
                _mQ2 = mX;

            // Swap U2 and _U2 so that U2 can be reused
            swapQ2();
        }

        logger.debug("End of update; S: %s, Q1: %s, Q2: %s", mS, mQ1, mQ2);

    }

    /**
     * Swap U2 and _U2, and clear _U2 if we are not recycling memory
     */
    private void swapQ2() {
        if (recycleMemory) {
            DoubleMatrix2D __U2 = mQ2;
            mQ2 = _mQ2;
            _mQ2 = __U2;
        }
        else {
            mQ2 = _mQ2;
            _mQ2 = null;
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
            logger.debug("Resized old A (%s)", C);
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
        if (mQ2 != null) {
            mQ1 = multiply(mQ1, mQ2);
            mQ2 = null;
        }
        return mQ1;
    }

    public DoubleMatrix1D computeU(int j) {
        if (mQ2 != null)
            return multiply(mQ1, mQ2.viewColumn(j));
        return mQ1.viewColumn(j);
    }

    public DiagonalDoubleMatrix getSigma() {
        return (DiagonalDoubleMatrix)mS;
    }

    /**
     * Reduces as much as possible the internal matrices before storage
     */
    public void trimMatrices() {
        // Reduce matrix U1 to its size
        if (mQ2 != null)
            mQ1 = multiply(mQ1, mQ2);
        mQ2 = null;
        mQ1.trimToSize();
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
     * Re-orthogonalise the basis vectors (by re-using an incremental EVD on the basis vectors)
     */
    public void reorthogonalise() {
        // Copy the S and X matrix
        DoubleMatrix2D local_mQ = computeU();
        DoubleMatrix2D local_mS = getSigma();

        logger.info("Reorthogonalise Q=%s and S=%s", local_mQ, local_mS);

        // Put everything to 0
        mQ1 = mQ2 = _mQ2 = mS = null;
        rank = 0;
        int numberOfUpdates = this.numberOfUpdates;

        // Add the vectors so we can a freshly re-orthogonalised set
        for (int i = 0; i < local_mS.columns(); i++)
            update(local_mS.get(i, i), local_mQ.viewColumn(i));

        // Restore the number of updtes
        this.numberOfUpdates = numberOfUpdates;
    }

    /**
     * Remove the all zeros rows of the column space (useful when some dimensions have disappeared due to a low rank
     * approximation)
     *
     * @param handler The handler is called everytime a row disappears and is replaced by another one. Note that the
     * implementation guarantees that a given row will only be removed once.
     * @param maxValue Maximum value for the sum of the square of the row (0 to remove zeros), exclusive. This should
     * normally be 0 since otherwise this can break the orthogonality of the column space. In the case it is superior to
     * 0, then it is advised to call {@linkplain #reorthogonalise()}
     */
    public void removeRows(ChangeListener handler, double maxValue) {
        // Compute U
        if (mQ2 != null)
            mQ1 = multiply(mQ1, mQ2);
        mQ2 = null;

        if (mQ1 == null)
            return;

        // U1 is U since U2 is identity

        // The new p
        int newP = nbRows;

        for (int i = 0; i < newP; i++) {
            // (1) Search for a non zero row
            if (!rowSumExceeds(maxValue, i, mQ1, mS)) {
                // We had a zero one, search for a non zero
                // from newP
                while (--newP > i) {
                    // Found it -- copy the row
                    if (rowSumExceeds(maxValue, newP, mQ1, mS)) {
                        mQ1.copyRow(newP, i);

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

        logger.debug("Reduced the number of rows from %d to %d", nbRows, newP);
        nbRows = newP;
        mQ1 = mQ1.viewPart(0, 0, nbRows, rank);
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
        return mQ1;
    }

    public DoubleMatrix2D getU2() {
        return mQ2;
    }

    public int getRank() {
        return rank;
    }

    /**
     * Set the rank (if superior to the current one, does nothing)
     *
     * @param argmaxRank
     */
    public void setRank(int newRank) {
        if (this.rank > newRank) {
            computeU();
            mQ1 = mQ1.viewPart(0, 0, mQ1.rows(), newRank);
            mS = mS.viewPart(0, 0, newRank, newRank);
            rank = newRank;
        }
    }

    final public double getSigma(int i) {
        return mS.get(i, i);
    }

}

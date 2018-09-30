package bpiwowar.maths.eigen;

import bpiwowar.maths.eigen.selector.Selector;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;

public interface RankOneUpdate {
    /**
     * Compute the eigendecomposition of D + rho * z z^t. The decomposition can be thin, in the sense that one can
     * specify a threshold on eigenvalues so as to discard (quasi)null eigenspace
     *
     * The result is in general of dimension p x n' where n >= n' and p is n or n'. In the case selector is null, then
     * n' = n. In the case selector removes some eigenvalues, then the diagonal matrix is of dimension n' (which depends
     * on the selector). Further, if keep is true, then the orthogonal matrix is of size n x n', otherwise it is n' x
     * n'.
     *
     * @param D is a n x n diagonal matrix (no check is performed to assert this) with decreasing positive values on its
     * diagonal. if the dimensionality of D is lower than that of z, we assume 0 values for the missing values.
     * @param z is a vector of size n
     * @param computeEigenvectors True if eigenvectors should be computed
     * @param selector Selector to remove some eigenvalues
     * @param keep In the case some eigenvalues are removed, this boolean dictates the geometry of the matrix Q
     * returned
     * @return A result composed of the orthonormal matrix and a diagonal matrix
     */
    public abstract Result rankOneUpdate(DoubleMatrix2D D, double rho,
        DoubleMatrix1D z, boolean computeEigenvectors, Selector selector,
        boolean keep);

    /**
     * A result from a rank one update
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static public class Result {
        DiagonalDoubleMatrix mD;
        DoubleMatrix2D Q;

        public DiagonalDoubleMatrix getEigenvalues() {
            return mD;
        }

        public DoubleMatrix2D getEigenvectors() {
            return Q;
        }
    }

    public interface EigenList {
        /**
         * Select an eigenvalue
         */
        double get(int i);

        /**
         * Remove this eigenvalue from the selection
         */
        void remove(int i);

        /**
         * The original number of eigenvalues
         */
        int size();

        /**
         * The current number of selected
         */
        int rank();

        /**
         * Check if an eigenvalue is currently selected or not
         */
        boolean isSelected(int i);
    }

}

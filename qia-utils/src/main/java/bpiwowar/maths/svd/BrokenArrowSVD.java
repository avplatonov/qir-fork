package bpiwowar.maths.svd;

import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;

/**
 * Performs the SVD of a matrix of the form [D; z^t] where D is a diagonal square matrix of size (n x n) and z is vector
 * of size n. This is more general than the rank one update D^2 + zz^t
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public interface BrokenArrowSVD {
    void setWantU(boolean wantU);

    void setWantV(boolean wantV);

    static public class Result {
        public DoubleMatrix2D U;
        public DoubleMatrix2D S;
        DoubleMatrix2D V;

        public DoubleMatrix2D getU() {
            return U;
        }

        public DoubleMatrix2D getS() {
            return S;
        }

        public DoubleMatrix2D getV() {
            return V;
        }

        public int rank() {
            return S.rows();
        }

    }

    /**
     * Computes the SVD of [D 0; z^t] where D is of dimension n x n and z of size n + 1
     *
     * @param D is a n x n diagonal matrix (no check is performed to assert this) with decreasing positive values on its
     * diagonal (i.e. the diagonal matrix of a previous SVD)
     * @param lastZ A double value (can be NaN)
     * @param z is a vector of size n + 1
     * @param maxRank The maximum rank of the decomposition
     * @param a is the last element of the last row (can be NaN if we should return the SVD of [D; z^t]
     */
    public abstract Result computeSVD(DoubleMatrix2D D, DoubleMatrix1D z,
        int maxRank);

}

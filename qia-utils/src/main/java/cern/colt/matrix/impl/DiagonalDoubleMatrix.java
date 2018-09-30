package cern.colt.matrix.impl;

import bpiwowar.utils.Output;
import bpiwowar.utils.arrays.ListAdaptator;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import java.util.List;
import javax.management.RuntimeErrorException;

public class DiagonalDoubleMatrix extends DoubleMatrix2D {
    private static final long serialVersionUID = 4444476703912381015L;

    double[] diagonal;

    public DiagonalDoubleMatrix(int size) {
        super();
        setUp(size, size);
        diagonal = new double[size];
    }

    /**
     * Construct a new diagonal matrix
     *
     * @param size
     * @param diag
     */
    public DiagonalDoubleMatrix(int size, final double[] diag) {
        super();
        setUp(size, size);
        diagonal = diag;
    }

    public DiagonalDoubleMatrix(double[] diag) {
        diagonal = diag;
        setUp(diag.length, diag.length);
    }

    @Override
    public double getQuick(int row, int column) {
        if (row != column)
            return 0;
        return diagonal[row];
    }

    @Override
    public DoubleMatrix2D like(int rows, int columns) {
        return new DiagonalDoubleMatrix(Math.min(rows, columns));
    }

    @Override
    public DoubleMatrix1D like1D(int size) {
        // Returns a dense vector, since it does not mean much to have the same
        // "type" for 1D
        return new DenseDoubleMatrix1D(size);
    }

    @Override
    protected DoubleMatrix1D like1D(int size, int zero, int stride) {
        throw new RuntimeErrorException(null, "Not sure what is needed here...");
    }

    @Override
    public void setQuick(int row, int column, double value) {
        if (row != column)
            throw new IndexOutOfBoundsException();
        diagonal[row] = value;
    }

    @Override
    protected DoubleMatrix2D viewSelectionLike(int[] rowOffsets,
        int[] columnOffsets) {
        throw new RuntimeErrorException(null, "Not implemented...");
    }

    /**
     * Linear algebraic matrix-vector multiplication;
     * <tt>z = alpha * A * y + beta*z</tt>.
     * <tt>z[i] = alpha*Sum(A[i,j] * y[j]) + beta*z[i], i=0..A.rows()-1, j=0..y.size()-1</tt>
     * . Where <tt>A == this</tt>. <br> Note: Matrix shape conformance is checked <i>after</i> potential
     * transpositions.
     *
     * @param y the source vector.
     * @param z the vector where results are to be stored. Set this parameter to <tt>null</tt> to indicate that a new
     * result vector shall be constructed.
     * @return z (for convenience only).
     * @throws IllegalArgumentException if <tt>A.columns() != y.size() || A.rows() > z.size())</tt>.
     */
    @Override
    public DoubleMatrix1D zMult(DoubleMatrix1D y, DoubleMatrix1D z,
        double alpha, double beta, boolean transposeA) {
        if (transposeA)
            return viewDice().zMult(y, z, alpha, beta, false);

        // boolean ignore = (z==null);
        if (z == null)
            z = y.like();

        if (columns != y.size() || rows > z.size())
            throw new IllegalArgumentException("Incompatible args: "
                + toStringShort() + ", " + y.toStringShort() + ", "
                + z.toStringShort());

        for (int i = rows; --i >= 0; ) {
            double s = 0;
            s += getQuick(i, i) * y.getQuick(i);
            z.setQuick(i, alpha * s + beta * z.getQuick(i));
        }
        return z;
    }

    @Override
    public String toString() {
        return "diag[" + rows + "](" + Output.toString(",", new ListAdaptator<Double>(diagonal, 0, rows)) + ")";
    }

    /**
     * Linear algebraic matrix-matrix multiplication;
     * <tt>C = alpha * A x B + beta*C</tt>.
     * <tt>C[i,j] = alpha*Sum(A[i,k] * B[k,j]) + beta*C[i,j], k=0..n-1</tt>. <br>
     * Matrix shapes: <tt>A(m x n), B(n x p), C(m x p)</tt>. <br> Note: Matrix shape conformance is checked <i>after</i>
     * potential transpositions.
     *
     * @param B the second source matrix.
     * @param C the matrix where results are to be stored. Set this parameter to <tt>null</tt> to indicate that a new
     * result matrix shall be constructed.
     * @return C (for convenience only).
     * @throws IllegalArgumentException if <tt>B.rows() != A.columns()</tt>.
     * @throws IllegalArgumentException if
     * <tt>C.rows() != A.rows() || C.columns() != B.columns()</tt>.
     * @throws IllegalArgumentException if <tt>A == C || B == C</tt>.
     */
    @Override
    public DoubleMatrix2D zMult(DoubleMatrix2D B, DoubleMatrix2D C,
        double alpha, double beta, boolean transposeA, boolean transposeB) {
        if (transposeA)
            return viewDice().zMult(B, C, alpha, beta, false, transposeB);
        if (transposeB)
            return this.zMult(B.viewDice(), C, alpha, beta, transposeA, false);

        int m = rows;
        int n = columns;
        int p = B.columns();

        if (C == null)
            C = new DenseDoubleMatrix2D(m, p);
        if (B.rows() != n)
            throw new IllegalArgumentException(
                "Matrix2D inner dimensions must agree:" + toStringShort()
                    + ", " + B.toStringShort());
        if (C.rows() != m || C.columns() != p)
            throw new IllegalArgumentException("Incompatibel result matrix: "
                + toStringShort() + ", " + B.toStringShort() + ", "
                + C.toStringShort());
        if (this == C || B == C)
            throw new IllegalArgumentException("Matrices must not be identical");

        // As we are a diagonal matrix, we just need to multiply each row of B
        // by a constant factor
        if (beta != 0) {
            for (int j = p; --j >= 0; ) {
                for (int i = m; --i >= 0; ) {
                    C.setQuick(i, j, alpha * getQuick(i, i) * B.getQuick(i, j)
                        + beta * C.getQuick(i, j));
                }
            }
        }
        else {
            for (int j = p; --j >= 0; ) {
                for (int i = m; --i >= 0; ) {
                    double s = getQuick(i, i) * B.getQuick(i, j);
                    C.setQuick(i, j, alpha * s);
                }
            }
        }

        return C;
    }

    public List<Double> diagonalAsList() {
        return new ListAdaptator<Double>(diagonal, 0, rows);
    }

}

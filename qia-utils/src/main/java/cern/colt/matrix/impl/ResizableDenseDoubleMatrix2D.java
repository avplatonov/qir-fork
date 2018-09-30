package cern.colt.matrix.impl;

import cern.colt.function.IntIntDoubleFunction;
import cern.colt.matrix.DoubleMatrix2D;
import java.io.Serializable;

/**
 * Use the dense double matrix implementation to have enough space for growing and shrinking
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class ResizableDenseDoubleMatrix2D extends DenseDoubleMatrix2D implements
    ResizableDoubleMatrix2D<ResizableDenseDoubleMatrix2D> {
    private static final long serialVersionUID = 267022600034161289L;

    // How much more space should we allow without complaining ?
    private int columnInc;
    private final int maxColumns;

    // How much more columms should we allow without complaining?
    private int rowInc;
    private final int maxRows;

    // [rowZero + row*rowStride + columnZero + column*columnStride];

    public ResizableDenseDoubleMatrix2D(int rows, int columns, int rowInc,
        int columnInc) {
        // We must have space
        // setUp(rows,columns,0,0,columns,1);
        super(rows, columns,
            new double[(rows + rowInc) * (columns + columnInc)],
            // row & column zero
            0, 0,
            // Number of elements between two rows/columns
            columns + columnInc, 1);

        // Set the rest
        this.columnInc = columnInc;
        this.rowInc = rowInc;

        this.maxColumns = columns + columnInc;
        this.maxRows = rows + rowInc;

        this.isNoView = true;
    }

    /**
     * Creates a new matrix with similar properties but all zeros
     *
     * @return
     */
    public ResizableDenseDoubleMatrix2D makeNew() {
        return new ResizableDenseDoubleMatrix2D(rows(), columns(), rowInc,
            columnInc);
    }

    /**
     * Adjust the matrix for a new size, returning either the same modified matrix or a new matrix if the new size does
     * not fit the allocated space
     *
     * @param rows
     * @param columns
     * @return The same matrix or a new one that will contain a deep copy
     */
    public ResizableDenseDoubleMatrix2D resize(int rows, int columns) {
        if (!isNoView)
            throw new RuntimeException("Cannot resize a view");

        if (rows > maxRows || columns > maxColumns) {
            // Just create a new matrix
            final ResizableDenseDoubleMatrix2D m = new ResizableDenseDoubleMatrix2D(
                rows, columns, rowInc, columnInc);
            forEachNonZero(new IntIntDoubleFunction() {
                @Override
                public double apply(int i, int j, double v) {
                    m.setQuick(i, j, v);
                    return v;
                }
            });
            return m;
        }

        // We just modify the internal parameters
        this.columns = columns;
        this.rows = rows;
        return this;
    }

    /**
     * Our factory
     *
     * @author bpiwowar
     */
    public static class Factory implements
        DoubleMatrix2DFactory<ResizableDenseDoubleMatrix2D>, Serializable {
        private static final long serialVersionUID = 1L;
        private int rowInc = 10;
        private int colInc = 10;

        public Factory(int rowInc, int colInc) {
            this.rowInc = rowInc;
            this.colInc = colInc;
        }

        @Override
        public ResizableDenseDoubleMatrix2D create(int rows, int cols) {
            return new ResizableDenseDoubleMatrix2D(rows, cols, rowInc, colInc);
        }

        @Override
        public ResizableDenseDoubleMatrix2D resize(DoubleMatrix2D matrix,
            int rows, int cols) {
            return ((ResizableDenseDoubleMatrix2D)matrix).resize(rows, cols);
        }
    }

    public DoubleMatrix2D _zMult(DoubleMatrix2D B, DoubleMatrix2D C,
        double alpha, double beta, boolean transposeA, boolean transposeB) {
        int m = rows;
        int n = columns;
        int p = B.columns;

        if (C == null)
            C = new DenseDoubleMatrix2D(m, p);
        if (B.rows != n)
            throw new IllegalArgumentException(
                "Matrix2D inner dimensions must agree:" + toStringShort()
                    + ", " + B.toStringShort());
        if (C.rows != m || C.columns != p)
            throw new IllegalArgumentException("Incompatibel result matrix: "
                + toStringShort() + ", " + B.toStringShort() + ", "
                + C.toStringShort());
        if (this == C || B == C)
            throw new IllegalArgumentException("Matrices must not be identical");

        for (int j = p; --j >= 0; ) {
            for (int i = m; --i >= 0; ) {
                double s = 0;
                for (int k = n; --k >= 0; ) {
                    s += getQuick(i, k) * B.getQuick(k, j);
                }
                C.setQuick(i, j, alpha * s + beta * C.getQuick(i, j));
            }
        }
        return C;
    }

    // Patch for zMult of dense double matrices
    public DoubleMatrix2D zMult(DoubleMatrix2D B, DoubleMatrix2D C,
        double alpha, double beta, boolean transposeA, boolean transposeB) {
        // overriden for performance only
        if (transposeA)
            return viewDice().zMult(B, C, alpha, beta, false, transposeB);
        if (transposeB)
            return this.zMult(B.viewDice(), C, alpha, beta, transposeA, false);

        if (B instanceof SparseDoubleMatrix2D || B instanceof RCDoubleMatrix2D
            || B instanceof ColumnCompressedDoubleMatrix2D) {
            // exploit quick sparse mult
            // A*B = (B' * A')'
            if (C == null) {
                return B.zMult(this, null, alpha, beta, !transposeB, true)
                    .viewDice();
            }
            else {
                B.zMult(this, C.viewDice(), alpha, beta, !transposeB, true);
                return C;
            }
        }
        if (transposeB)
            return this.zMult(B.viewDice(), C, alpha, beta, transposeA, false);

        if (!(B instanceof DenseDoubleMatrix2D && (C != null || C instanceof DenseDoubleMatrix2D)))
            return _zMult(B, C, alpha, beta, transposeA, transposeB);

        return super.zMult(B, C, alpha, beta, transposeA, transposeB);
    }

}

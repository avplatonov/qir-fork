package bpiwowar.maths.matrix;

import bpiwowar.log.Logger;
import bpiwowar.utils.Formatter;
import bpiwowar.utils.Output;
import bpiwowar.utils.arrays.ListAdaptator;
import cern.colt.function.IntIntDoubleFunction;
import java.io.PrintWriter;
import java.nio.DoubleBuffer;
import java.util.List;

public class DiagonalDoubleMatrix extends DoubleMatrix2D {
    @SuppressWarnings("unused") final static private Logger logger = Logger.getLogger();

    private static final long serialVersionUID = 4444476703912381015L;

    double[] diagonal;

    public DiagonalDoubleMatrix(int rows, int columns) {
        super(rows, columns);
        int n = Math.max(rows, columns);
        diagonal = new double[n];
    }

    public DiagonalDoubleMatrix(int n) {
        this(n, n);
    }

    /**
     * Construct a new diagonal matrix
     *
     * @param size
     * @param diag
     */
    public DiagonalDoubleMatrix(int rows, int columns, final double[] diagonal) {
        super(rows, columns);
        this.diagonal = diagonal;
    }

    public DiagonalDoubleMatrix(double[] diag) {
        super(diag.length, diag.length);
        diagonal = diag;
    }

    @Override
    public double get(int row, int column) {
        if (row != column)
            return 0;
        return diagonal[row];
    }

    @Override
    public void set(int row, int column, double value) {
        if (row != column)
            throw new IndexOutOfBoundsException();
        diagonal[row] = value;
    }

    @Override
    public DiagonalDoubleMatrix copy() {
        DiagonalDoubleMatrix copy = new DiagonalDoubleMatrix(rows, columns);
        int r = Math.min(rows, columns);
        System.arraycopy(diagonal, 0, copy.diagonal, 0, r);
        return copy;
    }

    @Override
    public DoubleMatrix2D forEachNonZero(IntIntDoubleFunction function) {
        for (int k = Math.min(rows, columns); --k >= 0; ) {
            final double value = diagonal[k];
            if (value != 0) {
                double r = function.apply(k, k, diagonal[k]);
                if (r != value)
                    diagonal[k] = r;
            }
        }
        return this;
    }

    @Override
    protected DoubleMatrix2D getViewPart(int i, int j, int height, int width) {
        if (i == j && i == 0)
            return new DiagonalDoubleMatrix(height, width, diagonal);
        // TODO: case i != j
        return super.getViewPart(i, j, height, width);
    }

    @Override
    public DoubleMatrix2D viewDice() {
        return this;
    }

    @Override
    public void print(PrintWriter out) {
        out.format("diag[%d](", rows);
        Output.print(out, ", ", new ListAdaptator<Double>(diagonal, 0, rows),
            new Formatter<Double>() {
                @Override
                public String format(Double t) {
                    return String.format("%10.2e", t);
                }
            });
        out.print(')');
        out.println();

    }

    @Override
    public void trimToSize() {
        final int r = Math.min(columns, rows);
        if (diagonal.length != r) {
            double[] newDiagonal = new double[r];
            for (int i = 0; i < r; i++)
                newDiagonal[i] = diagonal[i];
        }
    }

    /**
     * Return the diagonal as a vector
     *
     * @return
     */
    public DenseDoubleMatrix1D diagonal() {
        return new DenseDoubleMatrix1D(diagonal, 0, 1, Math.min(columns(),
            rows()));
    }

    //
    // Multiplication
    //

    /**
     * 1D Multiplication of the type Dx
     *
     * @author bpiwowar
     */
    static class Multiplier1D
        implements
        Multiply.Multiplier1D<DiagonalDoubleMatrix, DoubleMatrix1D, DoubleMatrix1D> {
        @Override
        public DoubleMatrix1D multiply(DiagonalDoubleMatrix A,
            DoubleMatrix1D y, DoubleMatrix1D z, double alpha, double beta,
            boolean transposeA) {
            if (z == null)
                z = y.like(y.size);

            if (A.columns != y.size() || A.rows > z.size())
                throw new IllegalArgumentException("Incompatible args");

            for (int i = A.rows; --i >= 0; ) {
                double s = 0;
                s += A.get(i, i) * y.get(i);
                z.set(i, alpha * s + beta * z.get(i));
            }
            return z;
        }

        @Override
        public double complexity(int m, int n) {
            return m;
        }
    }

    /**
     * 2D Multiplications of the type A x D
     *
     * @author bpiwowar
     */
    static class PostMultiplier2D
        implements
        Multiply.Multiplier2D<DoubleMatrix2D, DiagonalDoubleMatrix, DoubleMatrix2D> {
        @Override
        public double complexity(int m, int n, int p) {
            return m * n * p;
        }

        @Override
        public DoubleMatrix2D multiply(DoubleMatrix2D A,
            DiagonalDoubleMatrix B, DoubleMatrix2D C, double alpha,
            double beta, boolean transposeA, boolean transposeB) {
            return PreMultiplier2D._multiply(B, A, C, alpha, beta, !transposeB,
                false);
        }
    }

    /**
     * 2D Multiplications of the type D x A
     *
     * @author bpiwowar
     */
    static class PreMultiplier2D
        implements
        Multiply.Multiplier2D<DiagonalDoubleMatrix, DoubleMatrix2D, DoubleMatrix2D> {

        @Override
        public DoubleMatrix2D multiply(DiagonalDoubleMatrix A,
            DoubleMatrix2D B, DoubleMatrix2D C, double alpha, double beta,
            boolean transposeA, boolean transposeB) {
            return _multiply(A, B, C, alpha, beta, transposeA, transposeB);
        }

        final static public DoubleMatrix2D _multiply(DiagonalDoubleMatrix A,
            DoubleMatrix2D B, DoubleMatrix2D C, double alpha, double beta,
            boolean transposeA, boolean transposeB) {
            B = B.viewDice();

            int m = A.rows;
            int p = B.columns;

            if (C == null)
                C = B.like(m, p);

            // As we are a diagonal matrix, we just need to multiply each row of
            // B by a constant factor
            if (beta != 0) {
                for (int i = m; --i >= 0; ) {
                    final double d = A.get(i, i);
                    for (int j = p; --j >= 0; ) {
                        C.set(i, j, alpha * d * B.get(i, j) + beta
                            * C.get(i, j));
                    }
                }
            }
            else {
                for (int i = m; --i >= 0; ) {
                    final double d = A.get(i, i);
                    for (int j = p; --j >= 0; ) {
                        double s = d * B.get(i, j);
                        C.set(i, j, alpha * s);
                    }
                }
            }

            return C;
        }

        @Override
        public double complexity(int m, int n, int p) {
            return m * n * p;
        }
    }

    /**
     * Returns a vector view on the diagonal of this matrix
     */
    public DoubleMatrix1D asVector() {
        return new DenseDoubleMatrix1D(diagonal, 0, 1, rows);
    }

    /**
     * Returns a view on this matrix where the diagonal is seen as a list
     *
     * @return
     */
    public List<Double> asList() {
        return new ListAdaptator<Double>(diagonal, 0, rows);
    }

    /**
     * Returns the double buffer corresponding to our diagonal
     */
    public DoubleBuffer getDiagonalDoubleBuffer() {
        return DoubleBuffer.wrap(diagonal, 0, rows);
    }

}

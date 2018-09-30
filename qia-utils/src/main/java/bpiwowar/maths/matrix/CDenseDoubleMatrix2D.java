package bpiwowar.maths.matrix;

import bpiwowar.NotImplementedException;
import bpiwowar.log.Logger;
import bpiwowar.maths.matrix.Multiply.Multiplier1D;
import bpiwowar.maths.matrix.Multiply.Multiplier2D;
import bpiwowar.maths.matrix.Multiply.RankOneUpdater;
import bpiwowar.ml.OutOfBoundsException;
import cern.colt.function.DoubleFunction;
import java.nio.DoubleBuffer;
import java.util.Arrays;

/**
 * A dense matrix (column order storage to follow BLAS, i.e. column are stored one by one)
 */
public class CDenseDoubleMatrix2D extends DoubleMatrix2D {
    private static final long serialVersionUID = 2L;

    final static private Logger logger = Logger.getLogger();

    final static long DOUBLE_BYTES = Double.SIZE / 8;

    /**
     * Offset between two adjacent column elements (for the same row)
     */
    int colStride;

    /**
     * Offset to the first element
     */
    int offset;

    /**
     * Column major array
     */
    private double[] elements;

    /**
     * Creates a m by p matrix filled with zeros
     *
     * @param m
     * @param p
     */
    public CDenseDoubleMatrix2D(int m, int p) {
        super(m, p);
        colStride = m;
        elements = new double[m * p];
    }

    /**
     * Transforms a dense double matrix in row storage mode to a column storage mode. Warning: this modifies the
     * original array (or a part of the original array if the dense matrix is a view.
     *
     * @param original
     * @return
     */
    public static CDenseDoubleMatrix2D transform(DenseDoubleMatrix2D a) {
        CDenseDoubleMatrix2D c = new CDenseDoubleMatrix2D(a.rows, a.columns, a.elements, a.rows, a.offset);
        // Now, copy

//		 final int pos = offset + i * rowStride + j;
        throw new NotImplementedException();
//		 return c;
    }

    /**
     * Creates a m by p matrix by giving the full parameters
     *
     * @param m The number of rows
     * @param p The number of columns
     * @param elements A double array containing the matrix data
     * @param rowStride The distance between an element and the next row element (for the same column)
     * @param offset The offset from the start of the double array
     */
    public CDenseDoubleMatrix2D(int m, int p, double[] elements, int colStride,
        int offset) {
        super(m, p);
        this.elements = elements;
        this.colStride = colStride;
        this.offset = offset;
    }

    /**
     * Creates a matrix in row major mode
     *
     * @param rows
     * @param columns
     * @param elements
     */
    public CDenseDoubleMatrix2D(int rows, int columns, double... elements) {
        this(rows, columns, elements, columns, 0);
    }

    @Override
    public double get(int i, int j) {
        final int pos = offset + i + j * colStride;
        return elements[pos];
    }

    @Override
    public void set(int i, int j, double v) {
        final int pos = offset + i + j * colStride;
        elements[pos] = v;
    }

    @Override
    void update(int i, int j, DoubleFunction f) {
        final int pos = offset + j * colStride + i;
        elements[pos] = f.apply(elements[pos]);
    }

    @Override
    public String toString() {
        return String.format("%s (%d x %d out of %d x %d; +%d, *%d)",
            getClass(), rows, columns, colStride > 0 ? elements.length
                / colStride : -elements.length, colStride, offset,
            colStride);
    }

    /**
     * Get a double buffer pointing to the beginning of the storage of the matrix
     *
     * @return
     */
    DoubleBuffer getPointer() {
        final DoubleBuffer db = DoubleBuffer.wrap(elements, offset,
            elements.length - offset);
        return db.slice();
    }

    @Override
    protected CDenseDoubleMatrix2D getViewPart(int i, int j, int height,
        int width) {
        return new CDenseDoubleMatrix2D(height, width, elements, colStride,
            offset + i + j * colStride);
    }

    @Override
    public DenseDoubleMatrix1D viewRow(int i) {
        if (i < 0 || i >= rows)
            throw new OutOfBoundsException("Cannot view row %d of matrix %s",
                i, this);

        return new DenseDoubleMatrix1D(elements, offset + i, colStride, columns);
    }

    @Override
    public DenseDoubleMatrix1D viewColumn(int j) {
        if (j < 0 || j >= columns)
            throw new OutOfBoundsException(
                "Cannot view column %d of matrix %s", j, this);
        return new DenseDoubleMatrix1D(elements, offset + colStride * j, 1,
            rows);

    }

    /**
     * Get the new size for a growable array, which is given by the formula
     * <code>max(newCapacity, oldCapacity * 3/2 + 1)</code>
     *
     * @param oldCapacity
     * @param minCapacity
     * @return
     */
    public static final int grow(int oldCapacity, int minCapacity) {
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            return newCapacity;
        }
        return oldCapacity;
    }

    @Override
    public DoubleMatrix2D resize(int rows, int columns, boolean create,
        boolean copy) {
        // Starting column
        int rowStart = offset % colStride;
        // Number of available rows
        int availColumns = elements.length / colStride - offset / colStride;
        // Number of available columns
        int availRows = colStride - rowStart;

        // We have enough space: just update
        if (rows <= availRows && columns <= availColumns) {
            this.rows = rows;
            this.columns = columns;
            logger.debug("Resizing to %d x %d (available: %d x %d)", rows,
                columns, availRows, availColumns);
            return this;
        }

        // We need a new matrix
        if (!create)
            return null;

        final int newRows = grow(availRows, rows);
        final int newColumns = grow(availColumns, columns);
        logger.debug("Resize to %d x %d and take a view %d x %d", newRows,
            newColumns, rows, columns);

        DoubleMatrix2D m = like(newRows, newColumns).viewPart(0, 0, rows,
            columns);

        if (copy)
            m.viewPart(0, 0, Math.min(this.rows, rows),
                Math.min(this.columns, columns)).assign(this);
        return m;
    }

    @Override
    public void trimToSize() {
        // Check if we have something to do
        final int targetSize = rows * columns;

        if (targetSize != elements.length) {
            // Let's trim
            if (colStride == rows) {
                // Copy and set the offset to 0 if the row stride is already
                // adjusted
                elements = Arrays.copyOfRange(elements, offset, offset
                    + targetSize);
                offset = 0;
            }
            else {
                // We have to copy row by row
                final double[] array = new double[targetSize];
                for (int j = columns; --j >= 0; )
                    System.arraycopy(elements, offset + j * colStride, array, j
                        * rows, rows);

                // Set the parameters
                elements = array;
                colStride = rows;
                offset = 0;
            }
        }
    }

    /**
     * Multiplies two blas dense matrices and put the result into a new one
     *
     * @author bpiwowar
     */
    final public static class BlasMultiplier2D
        implements
        Multiplier2D<CDenseDoubleMatrix2D, CDenseDoubleMatrix2D, CDenseDoubleMatrix2D> {
        boolean useBlas;

        public BlasMultiplier2D() {
            this(true);
        }

        public BlasMultiplier2D(boolean useBlas) {
            this.useBlas = useBlas;
            if (useBlas && !Blas.register()) {
                // We will use something different
                useBlas = false;
            }
        }

        @Override
        public CDenseDoubleMatrix2D multiply(CDenseDoubleMatrix2D A,
            CDenseDoubleMatrix2D B, CDenseDoubleMatrix2D C, double alpha,
            double beta, boolean transposeA, boolean transposeB) {

            // Multiplies (m x p) by (p x n)
            int m = transposeA ? A.columns : A.rows;
            int p = transposeA ? A.rows : A.columns;
            int n = transposeB ? B.rows : B.columns;

            logger.debug("Multiplying %s [%b] with %s [%b] (%d,%d,%d) -> %s",
                A, transposeA, B, transposeB, m, n, transposeA ? A.rows
                    : A.columns, C);

            if (C == null) {
                C = new CDenseDoubleMatrix2D(m, n);
                logger.debug("Created C: " + C);
            }

            Blas.cblas_dgemm(Blas.CBLAS_ORDER.CblasColMajor,
                cblasTranspose(transposeA), cblasTranspose(transposeB), m,
                n, p, alpha, A.getPointer(), A.colStride, B.getPointer(),
                B.colStride, beta, C.getPointer(), C.colStride);
            //
            // if (1 + 1 == 1)
            //
            // if (useBlas) {
            // final int tA = transposeA ? Blas.CBLAS_TRANSPOSE.CblasTrans
            // : Blas.CBLAS_TRANSPOSE.CblasNoTrans;
            // final int tB = transposeB ? Blas.CBLAS_TRANSPOSE.CblasTrans
            // : Blas.CBLAS_TRANSPOSE.CblasNoTrans;
            //
            // Blas.cblas_dgemm(Blas.CBLAS_ORDER.CblasRowMajor, tA, tB, m,
            // n, p, alpha, A.getPointer(), A.rowStride,
            // B.getPointer(), B.rowStride, beta, C.getPointer(),
            // C.rowStride);
            // } else {
            //
            // // Semantic of p and n is not the same
            // final int _p = p;
            // p = n;
            // n = _p;
            //
            // /*
            // * From cern colt library
            // *
            // * A is blocked to hide memory latency xxxxxxx B xxxxxxx
            // * xxxxxxx A xxx xxxxxxx C xxx xxxxxxx --- ------- xxx
            // * xxxxxxx xxx xxxxxxx --- ------- xxx xxxxxxx
            // */
            // final int BLOCK_SIZE = 30000; // * 8 == Level 2 cache in
            // // bytes
            // // if (n+p == 0) return C;
            // // int m_optimal = (BLOCK_SIZE - n*p) / (n+p);
            // int m_optimal = (BLOCK_SIZE - n) / (n + 1);
            // if (m_optimal <= 0)
            // m_optimal = 1;
            // int blocks = m / m_optimal;
            // int rr = 0;
            // if (m % m_optimal != 0)
            // blocks++;
            //
            // // Strides
            // final int cA = transposeA ? A.rowStride : 1;
            // final int rA = transposeA ? 1 : A.rowStride;
            //
            // final int cB = transposeB ? B.rowStride : 1;
            // final int rB = transposeB ? 1 : B.rowStride;
            //
            // boolean transposeC = false;
            // final int cC = transposeC ? C.rowStride : 1;
            // final int rC = transposeC ? 1 : C.rowStride;
            //
            // // Block stride
            // for (; --blocks >= 0;) {
            // int jB = B.offset;
            // int indexA = A.offset + rr * rA;
            // int jC = C.offset + rr * rC;
            // rr += m_optimal;
            // if (blocks == 0)
            // m_optimal += m - rr;
            //
            // for (int j = p; --j >= 0;) {
            // int iA = indexA;
            // int iC = jC;
            // for (int i = m_optimal; --i >= 0;) {
            // int kA = iA;
            // int kB = jB;
            // double s = 0;
            //
            // // loop unrolled
            // kA -= cA;
            // kB -= rB;
            //
            // for (int k = n % 4; --k >= 0;) {
            // s += A.elements[kA += cA]
            // * B.elements[kB += rB];
            // }
            // for (int k = n / 4; --k >= 0;) {
            // s += A.elements[kA += cA]
            // * B.elements[kB += rB]
            // + A.elements[kA += cA]
            // * B.elements[kB += rB]
            // + A.elements[kA += cA]
            // * B.elements[kB += rB]
            // + A.elements[kA += cA]
            // * B.elements[kB += rB];
            // }
            //
            // C.elements[iC] = alpha * s + beta
            // * C.elements[iC];
            // iA += rA;
            // iC += rC;
            // }
            // jB += cB;
            // jC += cC;
            // }
            // }
            // }
            return C;
        }

        @Override
        public double complexity(int m, int n, int p) {
            return ((double)m * n * p) / 10.;
        }
    }

    /**
     * Multiplies two blas dense matrices and put the result into a new one
     *
     * @author bpiwowar
     */
    final static class BlasMultiplier1D
        implements
        Multiplier1D<CDenseDoubleMatrix2D, DenseDoubleMatrix1D, DenseDoubleMatrix1D> {

        public BlasMultiplier1D() {
            if (!Blas.register())
                throw new RuntimeException("Cannot initialise the BLAS library");
        }

        @Override
        public double complexity(int m, int n) {
            return ((double)m * n) / 10.;
        }

        @Override
        public DenseDoubleMatrix1D multiply(CDenseDoubleMatrix2D A,
            DenseDoubleMatrix1D x, DenseDoubleMatrix1D y, double alpha,
            double beta, boolean transposeA) {

            int m = transposeA ? A.columns : A.rows;
            int n = transposeA ? A.rows : A.columns;

            logger.debug("Multiplying %s [%b] with %s (%d,%d) -> %s", A,
                transposeA, x, m, n, y);
            if (y == null) {
                y = new DenseDoubleMatrix1D(m);
                logger.debug("Created y: " + y);
            }

            // BLAS.getInstance().dgemv(transposeA ? "T" : "N", A.rows,
            // A.columns,
            // alpha, A.getPointer(), A.colStride,
            // x.getPointer(), x.stride, beta,
            // y.getPointer(), y.stride);

            Blas.dgemv(Blas.CBLAS_ORDER.CblasColMajor,
                cblasTranspose(transposeA), A.rows, A.columns, alpha,
                A.getPointer(), A.colStride, x.getPointer(), x.stride,
                beta, y.getPointer(), y.stride);
            return y;
        }
    }

    static final int cblasTranspose(boolean transpose) {
        return transpose ? Blas.CBLAS_TRANSPOSE.CblasTrans
            : Blas.CBLAS_TRANSPOSE.CblasNoTrans;
    }

    // Our factory
    public static final DoubleMatrix2DFactory<?> FACTORY = new Factory();

    static public class Factory implements
        DoubleMatrix2DFactory<CDenseDoubleMatrix2D> {
        private static final long serialVersionUID = 1L;

        @Override
        public CDenseDoubleMatrix2D create(int rows, int cols) {
            return new CDenseDoubleMatrix2D(rows, cols);
        }
    }

    /**
     * Rank one updater for dense matrices using the BLAS library
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    static class DenseRankOneUpdater
        implements
        RankOneUpdater<CDenseDoubleMatrix2D, DenseDoubleMatrix1D, DenseDoubleMatrix1D> {

        public DenseRankOneUpdater() {
            if (!Blas.register())
                throw new RuntimeException("Cannot initialise the BLAS library");
        }

        @Override
        public double complexity(int m, int n) {
            return (double)m * (double)n * .1;
        }

        @Override
        public void update(double alpha, CDenseDoubleMatrix2D mA,
            DenseDoubleMatrix1D x, DenseDoubleMatrix1D y) {

            Blas.cblas_dger(Blas.CBLAS_ORDER.CblasColMajor, x.size, y.size,
                alpha, x.getPointer(), x.stride, y.getPointer(), y.stride,
                mA.getPointer(), mA.colStride);
        }

    }
}

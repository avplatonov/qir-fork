/*
Copyright � 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose
is hereby granted without fee, provided that the above copyright notice appear in all copies and
that both that copyright notice and this permission notice appear in supporting documentation.
CERN makes no representations about the suitability of this software for any purpose.
It is provided "as is" without expressed or implied warranty.
 */
package cern.colt.matrix.impl;

import bpiwowar.NotImplementedException;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import java.io.Serializable;

/**
 * Adapted from the row compressed representation
 *
 * @author wolfgang.hoschek@cern.ch
 * @author B. Piwowarski
 * @version 0.9, 04/14/2000
 */
public class ColumnCompressedDoubleMatrix2D extends WrapperDoubleMatrix2D
    implements ResizableDoubleMatrix2D<ColumnCompressedDoubleMatrix2D> {
    private static final long serialVersionUID = 1L;

    /*
     * The elements of the matrix.
     */
    protected IntArrayList rowIndexes;
    protected DoubleArrayList values;
    protected int[] starts;

    // protected int N;

    /**
     * Constructs a matrix with a copy of the given values. <tt>values</tt> is required to have the form
     * <tt>values[row][column]</tt> and have exactly the same number of columns in every row.
     * <p>
     * The values are copied. So subsequent changes in <tt>values</tt> are not reflected in the matrix, and vice-versa.
     *
     * @param values The values to be filled into the new matrix.
     * @throws IllegalArgumentException if
     * <tt>for any 1 &lt;= row &lt; values.length: values[row].length != values[row-1].length</tt>
     * .
     */
    public ColumnCompressedDoubleMatrix2D(double[][] values) {
        this(values.length, values.length == 0 ? 0 : values[0].length);
        assign(values);
    }

    /**
     * Constructs a matrix with a given number of rows and columns. All entries are initially <tt>0</tt>.
     *
     * @param rows the number of rows the matrix shall have.
     * @param columns the number of columns the matrix shall have.
     * @throws IllegalArgumentException if
     * <tt>rows<0 || columns<0 || (double)columns*rows > Integer.MAX_VALUE</tt>
     * .
     */
    public ColumnCompressedDoubleMatrix2D(int rows, int columns) {
        super(null);
        try {
            setUp(rows, columns);
        }
        catch (IllegalArgumentException exc) { // we can hold
            // rows*columns>Integer.MAX_VALUE
            // cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }
        rowIndexes = new IntArrayList();
        values = new DoubleArrayList();
        starts = new int[rows + 1];
    }

    /**
     * Sets all cells to the state specified by <tt>value</tt>.
     *
     * @param value the value to be filled into the cells.
     * @return <tt>this</tt> (for convenience only).
     */
    public DoubleMatrix2D assign(double value) {
        // overriden for performance only
        if (value == 0) {
            rowIndexes.clear();
            values.clear();
            for (int i = columns; --i >= 0; )
                starts[i] = 0;
        }
        else
            super.assign(value);
        return this;
    }

    public DoubleMatrix2D assign(
        final cern.colt.function.DoubleFunction function) {
        if (function instanceof cern.jet.math.Mult) { // x[i] = mult*x[i]
            final double alpha = ((cern.jet.math.Mult)function).multiplicator;
            if (alpha == 1)
                return this;
            if (alpha == 0)
                return assign(0);
            if (Double.isNaN(alpha))
                return assign(alpha); // the funny definition of isNaN(). This
            // should better not happen.

            double[] vals = values.elements();
            for (int j = values.size(); --j >= 0; ) {
                vals[j] *= alpha;
            }

            /*
             * forEachNonZero( new cern.colt.function.IntIntDoubleFunction() {
             * public double apply(int i, int j, double value) { return
             * function.apply(value); } } );
             */
        }
        else {
            super.assign(function);
        }
        return this;
    }

    /**
     * Replaces all cell values of the receiver with the values of another matrix. Both matrices must have the same
     * number of rows and columns. If both matrices share the same cells (as is the case if they are views derived from
     * the same matrix) and intersect in an ambiguous way, then replaces <i>as if</i> using an intermediate auxiliary
     * deep copy of
     * <tt>other</tt>.
     *
     * @param source the source matrix to copy from (may be identical to the receiver).
     * @return <tt>this</tt> (for convenience only).
     * @throws IllegalArgumentException if
     * <tt>columns() != source.columns() || rows() != source.rows()</tt>
     */
    public DoubleMatrix2D assign(DoubleMatrix2D source) {
        if (source == this)
            return this; // nothing to do
        checkShape(source);
        // overriden for performance only
        if (!(source instanceof ColumnCompressedDoubleMatrix2D)) {
            // return super.assign(source);

            assign(0);
            source
                .forEachNonZero(new cern.colt.function.IntIntDoubleFunction() {
                    public double apply(int i, int j, double value) {
                        setQuick(i, j, value);
                        return value;
                    }
                });
            /*
             * indexes.clear(); values.clear(); int nonZeros=0; for (int row=0;
             * row<rows; row++) { starts[row]=nonZeros; for (int column=0;
             * column<columns; column++) { double v =
             * source.getQuick(row,column); if (v!=0) { values.add(v);
             * indexes.add(column); nonZeros++; } } } starts[rows]=nonZeros;
             */
            return this;
        }

        // even quicker
        ColumnCompressedDoubleMatrix2D other = (ColumnCompressedDoubleMatrix2D)source;

        System.arraycopy(other.starts, 0, this.starts, 0, this.columns);
        int s = other.rowIndexes.size();
        this.rowIndexes.setSize(s);
        this.values.setSize(s);
        this.rowIndexes.replaceFromToWithFrom(0, s - 1, other.rowIndexes, 0);
        this.values.replaceFromToWithFrom(0, s - 1, other.values, 0);

        return this;
    }

    public DoubleMatrix2D assign(DoubleMatrix2D y,
        cern.colt.function.DoubleDoubleFunction function) {
        checkShape(y);

        if (function instanceof cern.jet.math.PlusMult) { // x[i] = x[i] +
            // alpha*y[i]
            final double alpha = ((cern.jet.math.PlusMult)function).multiplicator;
            if (alpha == 0)
                return this; // nothing to do
            y.forEachNonZero(new cern.colt.function.IntIntDoubleFunction() {
                public double apply(int i, int j, double value) {
                    setQuick(i, j, getQuick(i, j) + alpha * value);
                    return value;
                }
            });
            return this;
        }

        if (function == cern.jet.math.Functions.mult) { // x[i] = x[i] * y[i]
            int[] idx = rowIndexes.elements();
            double[] vals = values.elements();

            for (int j = columns - 1; --j >= 0; ) {
                int low = starts[j];
                for (int k = starts[j + 1]; --k >= low; ) {
                    int i = idx[k];
                    vals[k] *= y.getQuick(j, i);
                    if (vals[k] == 0)
                        remove(j, i);
                }
            }
            return this;
        }

        if (function == cern.jet.math.Functions.div) { // x[i] = x[i] / y[i]
            int[] idx = rowIndexes.elements();
            double[] vals = values.elements();

            for (int j = columns - 1; --j >= 0; ) {
                int low = starts[j];
                for (int k = starts[j + 1]; --k >= low; ) {
                    int i = idx[k];
                    vals[k] /= y.getQuick(j, i);
                    if (vals[k] == 0)
                        remove(j, i);
                }
            }
            return this;
        }

        return super.assign(y, function);
    }

    /*
     * (non-Javadoc)
     *
     * @seecern.colt.matrix.DoubleMatrix2D#forEachNonZero(cern.colt.function.
     * IntIntDoubleFunction)
     */
    @Override
    public DoubleMatrix2D forEachNonZero(
        final cern.colt.function.IntIntDoubleFunction function) {
        int[] idx = rowIndexes.elements();
        double[] vals = values.elements();

        for (int j = columns - 1; --j >= 0; ) {
            int low = starts[j];
            for (int k = starts[j + 1]; --k >= low; ) {
                int i = idx[k];
                double value = vals[k];
                double r = function.apply(i, j, value);
                if (r != value)
                    vals[k] = r;
            }
        }
        return this;
    }

    /**
     * Returns the content of this matrix if it is a wrapper; or <tt>this</tt> otherwise. Override this method in
     * wrappers.
     */
    protected DoubleMatrix2D getContent() {
        return this;
    }

    /**
     * Returns the matrix cell value at coordinate <tt>[row,column]</tt>.
     *
     * <p>
     * Provided with invalid parameters this method may return invalid objects without throwing any exception. <b>You
     * should only use this method when you are absolutely sure that the coordinate is within bounds.</b> Precondition
     * (unchecked):
     * <tt>0 &lt;= column &lt; columns() && 0 &lt;= row &lt; rows()</tt>.
     *
     * @param column the index of the row-coordinate.
     * @param row the index of the column-coordinate.
     * @return the value at the specified coordinate.
     */
    public double getQuick(int row, int column) {
        int k = rowIndexes.binarySearchFromTo(row, starts[column],
            starts[column + 1] - 1);
        double v = 0;
        if (k >= 0)
            v = values.getQuick(k);
        return v;
    }

    final protected void insert(int row, int column, int index, double value) {
        rowIndexes.beforeInsert(index, row);
        values.beforeInsert(index, value);
        for (int j = columns; --j > column; )
            starts[j]++;
    }

    /**
     * Construct and returns a new empty matrix <i>of the same dynamic type</i> as the receiver, having the specified
     * number of rows and columns. For example, if the receiver is an instance of type
     * <tt>DenseDoubleMatrix2D</tt> the new matrix must also be of type
     * <tt>DenseDoubleMatrix2D</tt>, if the receiver is an instance of type
     * <tt>SparseDoubleMatrix2D</tt> the new matrix must also be of type
     * <tt>SparseDoubleMatrix2D</tt>, etc. In general, the new matrix should
     * have internal parametrization as similar as possible.
     *
     * @param rows the number of rows the matrix shall have.
     * @param columns the number of columns the matrix shall have.
     * @return a new empty matrix of the same dynamic type.
     */
    public DoubleMatrix2D like(int rows, int columns) {
        return new ColumnCompressedDoubleMatrix2D(rows, columns);
    }

    /**
     * Construct and returns a new 1-d matrix <i>of the corresponding dynamic type</i>, entirelly independent of the
     * receiver. For example, if the receiver is an instance of type <tt>DenseDoubleMatrix2D</tt> the new matrix must be
     * of type <tt>DenseDoubleMatrix1D</tt>, if the receiver is an instance of type <tt>SparseDoubleMatrix2D</tt> the
     * new matrix must be of type <tt>SparseDoubleMatrix1D</tt>, etc.
     *
     * @param size the number of cells the matrix shall have.
     * @return a new matrix of the corresponding dynamic type.
     */
    public DoubleMatrix1D like1D(int size) {
        return new SparseDoubleMatrix1D(size);
    }

    protected void remove(int column, int index) {
        rowIndexes.remove(index);
        values.remove(index);
        for (int j = columns; --j > column; )
            starts[j]--;
    }

    /**
     * Sets the matrix cell at coordinate <tt>[row,column]</tt> to the specified value.
     *
     * <p>
     * Provided with invalid parameters this method may access illegal indexes without throwing any exception. <b>You
     * should only use this method when you are absolutely sure that the coordinate is within bounds.</b> Precondition
     * (unchecked):
     * <tt>0 &lt;= column &lt; columns() && 0 &lt;= row &lt; rows()</tt>.
     *
     * @param row the index of the row-coordinate.
     * @param column the index of the column-coordinate.
     * @param value the value to be filled into the specified cell.
     */
    public void setQuick(int row, int column, double value) {
        int k = rowIndexes.binarySearchFromTo(row, starts[column],
            starts[column + 1] - 1);
        if (k >= 0) { // found
            if (value == 0)
                remove(column, k);
            else
                values.setQuick(k, value);
            return;
        }

        // Insert if needed
        if (value != 0) {
            k = -k - 1;
            insert(row, column, k, value);
        }
    }

    public void trimToSize() {
        rowIndexes.trimToSize();
        values.trimToSize();
    }

    public DoubleMatrix1D zMult(DoubleMatrix1D y, DoubleMatrix1D z,
        double alpha, double beta, boolean transposeA) {
        int m = rows;
        int n = columns;
        if (transposeA) {
            m = columns;
            n = rows;
        }

        boolean ignore = (z == null || !transposeA);
        if (z == null)
            z = new DenseDoubleMatrix1D(m);

        if (!(y instanceof DenseDoubleMatrix1D && z instanceof DenseDoubleMatrix1D)) {
            return super.zMult(y, z, alpha, beta, transposeA);
        }

        if (n != y.size() || m > z.size())
            throw new IllegalArgumentException("Incompatible args: "
                + ((transposeA ? viewDice() : this).toStringShort()) + ", "
                + y.toStringShort() + ", " + z.toStringShort());

        DenseDoubleMatrix1D zz = (DenseDoubleMatrix1D)z;
        final double[] zElements = zz.elements;
        final int zStride = zz.stride;
        int zi = z.index(0);

        DenseDoubleMatrix1D yy = (DenseDoubleMatrix1D)y;
        final double[] yElements = yy.elements;
        final int yStride = yy.stride;
        final int yi = y.index(0);

        if (yElements == null || zElements == null)
            throw new InternalError();

        /*
         * forEachNonZero( new cern.colt.function.IntIntDoubleFunction() {
         * public double apply(int i, int j, double value) { zElements[zi +
         * zStride*i] += value * yElements[yi + yStride*j];
         * //z.setQuick(row,z.getQuick(row) + value * y.getQuick(column));
         * //System.out.println("["+i+","+j+"]-->"+value); return value; } } );
         */

        int[] idx = rowIndexes.elements();
        double[] vals = values.elements();
        final int s = columns - 1;

        if (transposeA) {
            // For each row (a column of A)
            for (int j = 0; j < s; j++) {
                int high = starts[j + 1];
                double sum = 0;
                for (int k = starts[j]; k < high; k++) {
                    int i = idx[k];
                    sum += vals[k] * yElements[yi + yStride * i];
                }

                zElements[zi] = alpha * sum + beta * zElements[zi];
                zi += zStride;
            }
        }
        else {
            if (!ignore)
                z.assign(cern.jet.math.Functions.mult(beta));
            for (int j = 0; j < s; j++) {
                int high = starts[j + 1];
                double yElem = alpha * yElements[yi + yStride * j];
                for (int k = starts[j]; k < high; k++) {
                    int i = idx[k];
                    zElements[zi + zStride * i] += vals[k] * yElem;
                }
            }
        }

        return z;
    }

    public DoubleMatrix2D zMult(DoubleMatrix2D B, DoubleMatrix2D C,
        final double alpha, double beta, boolean transposeA,
        boolean transposeB) {
        if (transposeB)
            B = B.viewDice();
        int m = rows;
        int n = columns;
        if (transposeA) {
            m = columns;
            n = rows;
        }
        int p = B.columns;
        boolean ignore = (C == null);
        if (C == null)
            C = new DenseDoubleMatrix2D(m, p);

        if (B.rows != n)
            throw new IllegalArgumentException(
                "Matrix2D inner dimensions must agree:" + toStringShort()
                    + ", "
                    + (transposeB ? B.viewDice() : B).toStringShort());
        if (C.rows != m || C.columns != p)
            throw new IllegalArgumentException("Incompatibel result matrix: "
                + toStringShort() + ", "
                + (transposeB ? B.viewDice() : B).toStringShort() + ", "
                + C.toStringShort());
        if (this == C || B == C)
            throw new IllegalArgumentException("Matrices must not be identical");

        if (!ignore)
            C.assign(cern.jet.math.Functions.mult(beta));

        // cache views
        final DoubleMatrix1D[] Brows = new DoubleMatrix1D[n];
        for (int i = n; --i >= 0; )
            Brows[i] = B.viewRow(i);
        final DoubleMatrix1D[] Crows = new DoubleMatrix1D[m];
        for (int i = m; --i >= 0; )
            Crows[i] = C.viewRow(i);

        final cern.jet.math.PlusMult fun = cern.jet.math.PlusMult.plusMult(0);

        int[] idx = rowIndexes.elements();
        double[] vals = values.elements();
        for (int j = columns - 1; --j >= 0; ) {
            int low = starts[j];
            for (int k = starts[j + 1]; --k >= low; ) {
                int i = idx[k];
                fun.multiplicator = vals[k] * alpha;
                if (!transposeA)
                    Crows[j].assign(Brows[i], fun);
                else
                    Crows[i].assign(Brows[j], fun);
            }
        }

        return C;
    }

    @Override
    public ColumnCompressedDoubleMatrix2D resize(int rows, int columns) {
        if (this.columns > columns)
            throw new NotImplementedException(
                "Cannot reduce the number of columns");
        if (this.rows > rows)
            throw new NotImplementedException(
                "Cannot reduce the number of rows");

        // Resize the starts and copy the last index
        starts = cern.colt.Arrays.ensureCapacity(starts, columns + 1);
        int v = starts[this.columns];
        for (int j = columns; j > this.columns; j--)
            starts[j] = v;

        // Set up new rows and columns
        try {
            setUp(rows, columns);
        }
        catch (IllegalArgumentException exc) { // we can hold
            // rows*columns>Integer.MAX_VALUE
            // cells !
            if (!"matrix too large".equals(exc.getMessage()))
                throw exc;
        }

        return this;
    }

    public DoubleMatrix2D viewPart(final int row, final int column, int height,
        int width) {
        checkBox(row, column, height, width);
        DoubleMatrix2D view = new PartialView(this, row, column, false);
        view.rows = height;
        view.columns = width;
        return view;
    }

    /**
     * A column compressed double matrix factory
     */
    static public final Factory FACTORY = new Factory();

    /**
     * A view on
     *
     * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
     */
    private final class PartialView extends WrapperDoubleMatrix2D {
        private final int rowOffset;
        private final int columnOffset;
        private boolean transposed;

        private static final long serialVersionUID = -548843628116769908L;

        private PartialView(DoubleMatrix2D newContent, int rowOffset,
            int columnOffset, boolean transposed) {
            super(newContent);
            this.rowOffset = rowOffset;
            this.columnOffset = columnOffset;
            this.transposed = transposed;
        }

        public double getQuick(int i, int j) {
            if (transposed)
                return content.get(columnOffset + j, rowOffset + i);
            else
                return content.get(rowOffset + i, columnOffset + j);
        }

        public void setQuick(int i, int j, double value) {
            if (transposed)
                content.set(columnOffset + j, rowOffset + i, value);
            else
                content.set(rowOffset + i, columnOffset + j, value);
        }

        @Override
        public String toStringShort() {
            if (!transposed)
                return rows() + " x " + columns() + " matrix";
            return columns() + " x " + rows() + " matrix";

        }

        @Override
        public DoubleMatrix2D zMult(DoubleMatrix2D B, DoubleMatrix2D C,
            double alpha, double beta, boolean transposeA,
            boolean transposeB) {
            if (transposeB)
                B = B.viewDice();
            int m = rows;
            int n = columns;
            if (transposeA) {
                m = columns;
                n = rows;
            }
            if (transposed)
                transposeA = !transposeA;

            int p = B.columns;
            boolean ignore = (C == null);
            if (C == null)
                C = new DenseDoubleMatrix2D(m, p);

            if (B.rows != n)
                throw new IllegalArgumentException(
                    "Matrix2D inner dimensions must agree (A x B): "
                        + toStringShort()
                        + ", "
                        + (transposeB ? B.viewDice() : B)
                        .toStringShort());
            if (C.rows != m || C.columns != p)
                throw new IllegalArgumentException(
                    "Incompatible result matrix: "
                        + toStringShort()
                        + ", "
                        + (transposeB ? B.viewDice() : B)
                        .toStringShort() + ", "
                        + C.toStringShort());
            if (this == C || B == C)
                throw new IllegalArgumentException(
                    "Matrices must not be identical");

            if (!ignore)
                C.assign(cern.jet.math.Functions.mult(beta));

            // cache views
            final DoubleMatrix1D[] Brows = new DoubleMatrix1D[n];
            for (int i = n; --i >= 0; )
                Brows[i] = B.viewRow(i);
            final DoubleMatrix1D[] Crows = new DoubleMatrix1D[m];
            for (int i = m; --i >= 0; )
                Crows[i] = C.viewRow(i);

            final cern.jet.math.PlusMult fun = cern.jet.math.PlusMult
                .plusMult(0);

            int[] idx = rowIndexes.elements();
            double[] vals = values.elements();
            for (int j = columns - 1; --j >= 0; ) {
                int low = starts[j + columnOffset];
                for (int k = starts[j + 1 + columnOffset]; --k >= low; ) {
                    int i = idx[k] - rowOffset;
                    if (i < 0)
                        continue;

                    fun.multiplicator = vals[k] * alpha;
                    if (!transposeA)
                        Crows[j].assign(Brows[i], fun);
                    else
                        Crows[i].assign(Brows[j], fun);
                }
            }

            return C;
        }

        @Override
        public DoubleMatrix2D viewDice() {
            DoubleMatrix2D view = new PartialView(this, columnOffset,
                rowOffset, !transposed);
            view.rows = columns;
            view.columns = rows;
            return view;
        }
    }

    public static class Factory implements
        DoubleMatrix2DFactory<ColumnCompressedDoubleMatrix2D>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public ColumnCompressedDoubleMatrix2D create(int rows, int cols) {
            return new ColumnCompressedDoubleMatrix2D(rows, cols);
        }

        @Override
        public ColumnCompressedDoubleMatrix2D resize(DoubleMatrix2D matrix,
            int rows, int cols) {
            return ((ColumnCompressedDoubleMatrix2D)matrix).resize(rows, cols);
        }
    }
}

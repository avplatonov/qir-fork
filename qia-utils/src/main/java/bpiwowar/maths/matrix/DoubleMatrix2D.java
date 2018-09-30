package bpiwowar.maths.matrix;

import bpiwowar.log.Logger;
import bpiwowar.ml.OutOfBoundsException;
import bpiwowar.utils.holders.DoubleHolder;
import bpiwowar.utils.holders.IntHolder;
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.DoubleFunction;
import cern.colt.function.IntIntDoubleFunction;
import cern.jet.math.Functions;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * A double matrix
 *
 * @author bpiwowar
 */
public abstract class DoubleMatrix2D implements Serializable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static Logger logger = Logger.getLogger();

    /**
     * The number of rows
     */
    int rows;

    /**
     * The number of columns
     */
    int columns;

    public DoubleMatrix2D() {
    }

    public DoubleMatrix2D(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    /**
     * Get the value of cell (i,j)
     *
     * @param i
     * @param j
     * @return
     */
    public abstract double get(int i, int j);

    /**
     * Set the value of cell (i,j)
     *
     * @param i
     * @param j
     */
    public abstract void set(int i, int j, double v);

    @Override
    public int hashCode() {
        final IntHolder hash = new IntHolder();
        forEachNonZero(new IntIntDoubleFunction() {
            @Override
            public double apply(int i, int j, double v) {
                hash.value = 31 * hash.value + i;
                hash.value = 31 * hash.value + j;
                hash.value = 31 * hash.value + Double.valueOf(v).hashCode();
                return v;
            }
        });

        return hash.value;
    }

    /**
     * Approximate hash-code
     *
     * @param context MathContext to round off results
     * @return
     */
    public int hashCode(final MathContext context) {
        final IntHolder hash = new IntHolder();

        forEachNonZero(new IntIntDoubleFunction() {
            @Override
            public double apply(int i, int j, double v) {
                hash.value = 31 * hash.value + i;
                hash.value = 31 * hash.value + j;
                hash.value = 31 * hash.value + new BigDecimal(v, context).hashCode();
                return v;
            }
        });

        return hash.value;
    }

    /**
     * Update the value of a single cell
     *
     * @param i
     * @param j
     * @param f
     */
    void update(int i, int j, DoubleFunction f) {
        set(i, j, f.apply(get(i, j)));
    }

    @Override
    public String toString() {
        return String.format("%s (%d x %d)", getClass(), rows, columns);
    }

    public void print(PrintWriter out) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (j != 0)
                    out.print(' ');
                out.format("%10.2e", get(i, j));
            }
            out.println();
        }
    }

    /**
     * Returns true if this coordinate is null by construction (e.g. a diagonal matrix will only store diagonal
     * elements). Returns false by default.
     *
     * @param i the row
     * @param j the column
     * @return True or false
     */
    boolean zeroByConstruction(int i, int j) {
        return false;
    }

    /**
     * Dice view of the matrix
     *
     * @return a view on the matrix
     */
    public DoubleMatrix2D viewDice() {
        return new DiceViewDoubleMatrix2D(this);
    }

    /**
     * Get a partial view of the matrix
     *
     * @param i
     * @param j
     * @param width
     * @param height
     * @return
     */
    public final DoubleMatrix2D viewPart(int i, int j, int height, int width) {
        // Identity?
        if (i == 0 && j == 0 && width == columns && height == rows)
            return this;

        // Basic checks
        if (j + width > columns || i + height > rows)
            throw new OutOfBoundsException(
                "The view (%d,%d) of dimensions %dx%d cannot be created for a %dx%d matrix",
                i, j, height, width, rows, columns);

        return getViewPart(i, j, height, width);
    }

    /**
     * Internal function to get a partial view of the matrix, where we can safely assume that all the bound checks have
     * been performed and that the view is really different from the original matrix
     *
     * @param i
     * @param j
     * @param width
     * @param height
     * @return
     */
    protected DoubleMatrix2D getViewPart(int i, int j, int height, int width) {
        return new PartViewDoubleMatrix2D(this, i, j, height, width);
    }

    /**
     * Simple assignation (without bound checks)
     *
     * @param m
     */
    public void assign(DoubleMatrix2D m) {
        for (int i = rows; --i >= 0; )
            for (int j = columns; --j >= 0; )
                set(i, j, m.get(i, j));
    }

    /**
     * Resize a matrix. Warning: if the matrix is a view, then the resizing operation can include old values
     *
     * @param rows
     * @param columns
     * @return
     */
    final public DoubleMatrix2D resize(int rows, int columns) {
        return resize(rows, columns, true, true);
    }

    /**
     * Resize a matrix. Warning: if the matrix is a view, then the resizing operation can include the old values
     *
     * @param rows
     * @param columns
     * @param create True if a new matrix should be created if it does not fit
     * @param True if the old values should be copied
     * @return The matrix itself (resized), a new matrix (if needed) with copied values (if asked for)
     */
    public DoubleMatrix2D resize(int rows, int columns, boolean create,
        boolean copy) {
        if (rows == this.rows && columns == this.columns)
            return this;
        if (!create)
            return null;

        DoubleMatrix2D m = like(rows, columns);
        int r = Math.min(this.rows, rows);
        int c = Math.min(this.columns, columns);
        if (copy)
            m.viewPart(0, 0, r, c).assign(viewPart(0, 0, r, c));
        return m;
    }

    public DoubleMatrix2D like(int rows, int columns) {
        try {
            Constructor<? extends DoubleMatrix2D> constructor = getClass()
                .getConstructor(Integer.TYPE, Integer.TYPE);
            DoubleMatrix2D m = constructor.newInstance(rows, columns);
            return m;
        }
        catch (Exception e) {
            throw new RuntimeException(
                String.format(
                    "Error while constructing a new instance of %s",
                    getClass()), e);
        }
    }

    /**
     * Copy the current matrix
     *
     * @return A new instance of a matrix of the same type (or underlying type in case of a view), with copied data
     */
    public DoubleMatrix2D copy() {
        DoubleMatrix2D like = like(rows(), columns());
        for (int i = rows; --i >= 0; )
            for (int j = columns; --j >= 0; )
                like.set(i, j, get(i, j));
        return like;
    }

    public int rows() {
        return rows;
    }

    public int columns() {
        return columns;
    }

    /**
     * Trim the matrix to the minimum size
     */
    public void trimToSize() {
    }

    public DoubleMatrix1D viewRow(final int i) {
        return new RowView(i);
    }

    public DoubleMatrix1D viewColumn(final int j) {
        return new ColumnView(j);
    }

    public void assign(double[][] u) {
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < columns; j++)
                set(i, j, u[i][j]);
    }

    /**
     * Aggregate values of a matrix by first applying the function f to the matrix entry and aggregating it with the
     * function aggr
     *
     * @param a The initial value for aggregation
     * @param aggr The aggregation function
     * @param f The function to apply
     * @param skipZeros Should zeros be skipped?
     * @return
     */
    public double aggregate(double a, final DoubleDoubleFunction aggr,
        final DoubleFunction f, boolean skipZeros) {
        if (skipZeros) {
            final DoubleHolder d = new DoubleHolder(a);
            forEachNonZero(new IntIntDoubleFunction() {
                @Override
                public double apply(int i, int j, double v) {
                    d.value = aggr.apply(d.value, f.apply(v));
                    return v;
                }
            });
            return d.value;
        }
        else {
            for (int row = rows; --row >= 0; ) {
                for (int column = columns; --column >= 0; ) {
                    final double x = get(row, column);
                    if (!skipZeros || x != 0)
                        a = aggr.apply(a, f.apply(x));
                }
            }
            return a;
        }
    }

    public DoubleMatrix2D forEachNonZero(final IntIntDoubleFunction function) {
        for (int row = rows; --row >= 0; ) {
            for (int column = columns; --column >= 0; ) {
                double value = get(row, column);
                if (value != 0) {
                    double r = function.apply(row, column, value);
                    if (r != value)
                        set(row, column, r);
                }
            }
        }
        return this;
    }

    /**
     * Apply a function to each non zero entry
     *
     * @param f
     */
    final public void forEachNonZero(final DoubleFunction f) {
        forEachNonZero(new IntIntDoubleFunction() {
            @Override
            public double apply(int i, int j, double v) {
                return f.apply(v);
            }
        });
    }

    /**
     * Normalise the matrix using the Froebenius norm (i.e. sum of squares)
     */
    public double normalise() {
        final double norm = Math.sqrt(aggregate(0, Functions.plus,
            Functions.square, true));

        if (norm != 1.)
            forEachNonZero(new IntIntDoubleFunction() {
                @Override
                public double apply(int i, int j, double v) {
                    return v / norm;
                }
            });

        return norm;
    }

    public double cardinality() {
        final IntHolder count = new IntHolder();
        forEachNonZero(new IntIntDoubleFunction() {
            @Override
            public double apply(int first, int second, double value) {
                count.value++;
                return value;
            }
        });
        return count.value;
    }

    /**
     * Returns the size (number of cells) of this matrix
     */
    final public double size() {
        return rows * columns;
    }

    /**
     * Aggregate values over two matrices
     *
     * @param a The starting value
     * @param other The other matrix
     * @param aggr The value to aggregate values
     * @param f The function applied on pairs of value
     * @param skipZeros Should we skip zeros?
     * @return The aggregated value
     */
    public double aggregate(double a, DoubleMatrix2D other,
        DoubleDoubleFunction aggr, DoubleDoubleFunction f, boolean skipZeros) {
        if (size() == 0)
            return Double.NaN;

        if (other.rows() != rows() || other.columns() != columns())
            throw new IllegalArgumentException(String.format(
                "%s and %s have different shapes and cannot be aggregated",
                this, other));

        for (int i = rows; --i >= 0; ) {
            for (int j = columns; --j >= 0; ) {
                final double x = get(i, j);
                final double y = other.get(i, j);
                if (!skipZeros || x != 0 || y != 0)
                    a = aggr.apply(a, f.apply(x, y));
            }
        }

        return a;
    }

    /**
     * The default column view
     *
     * @author bpiwowar
     */
    private final class ColumnView extends DoubleMatrix1D {
        private final int j;
        private static final long serialVersionUID = 1L;

        {
            size = rows();
        }

        private ColumnView(int j) {
            this.j = j;
        }

        @Override
        public void set(int i, double v) {
            DoubleMatrix2D.this.set(i, j, v);
        }

        @Override
        public double get(int i) {
            return DoubleMatrix2D.this.get(i, j);
        }
    }

    /**
     * The default row view
     *
     * @author bpiwowar
     */
    private final class RowView extends DoubleMatrix1D {
        private final int i;
        private static final long serialVersionUID = 1L;

        {
            size = DoubleMatrix2D.this.columns();
        }

        private RowView(int i) {
            this.i = i;
        }

        @Override
        public void set(int j, double v) {
            DoubleMatrix2D.this.set(i, j, v);
        }

        @Override
        public double get(int j) {
            return DoubleMatrix2D.this.get(i, j);
        }
    }

    /**
     * Copy a row to another one
     *
     * @param from
     * @param to
     */
    public void copyRow(int from, int to) {
        if (from != to)
            for (int j = columns; --j >= 0; )
                set(to, j, get(from, j));
    }

}

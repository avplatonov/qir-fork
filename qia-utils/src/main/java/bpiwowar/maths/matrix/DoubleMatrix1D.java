package bpiwowar.maths.matrix;

import bpiwowar.log.Logger;
import bpiwowar.ml.OutOfBoundsException;
import bpiwowar.utils.holders.DoubleHolder;
import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.DoubleFunction;
import cern.colt.function.IntDoubleFunction;
import cern.colt.function.IntDoubleProcedure;
import cern.jet.math.Functions;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;

/**
 * A double vector
 *
 * @author bpiwowar
 */
public abstract class DoubleMatrix1D implements Serializable {
    private static final long serialVersionUID = 1L;

    final static Logger logger = Logger.getLogger();

    /**
     * The number of rows
     */
    int size;

    public DoubleMatrix1D() {
    }

    public DoubleMatrix1D(int rows) {
        this.size = rows;
    }

    /**
     * Get the value of cell (i,j)
     *
     * @param i
     * @param j
     * @return
     */
    public abstract double get(int i);

    /**
     * Set the value of cell (i,j)
     *
     * @param i
     * @param j
     */
    public abstract void set(int i, double v);

    /**
     * Update the value of a single cell
     *
     * @param i
     * @param j
     * @param f
     */
    public void update(int i, DoubleFunction f) {
        set(i, f.apply(get(i)));
    }

    /**
     * Get a partial view of the matrix
     *
     * @param offset
     * @param width
     * @param height
     * @return
     */
    public final DoubleMatrix1D viewPart(int offset, int height) {
        // Identity?
        if (offset == 0 && height == size)
            return this;

        // Basic checks
        if (offset + height > size)
            throw new OutOfBoundsException(
                "The view (%d,%d) of dimensions %dx%d cannot be created for a %dx%d matrix",
                offset, height, size);

        return getViewPart(offset, height);
    }

    /**
     * Internal function to get a partial view of the matrix, where we can safely assume that all the bound checks have
     * been performed and that the view is really different from the original matrix
     *
     * @param offset
     * @param width
     * @param height
     * @return
     */
    protected DoubleMatrix1D getViewPart(int offset, int height) {
        return new PartViewDoubleMatrix1D(this, offset, height);
    }

    /**
     * Normalise the vector using the euclidian norm
     */
    public void normalise() {
        final double norm = Math.sqrt(aggregate(0, Functions.plus,
            Functions.square, true));

        if (norm != 1.)
            forEachNonZero(new IntDoubleProcedure() {
                @Override
                public boolean apply(int i, double v) {
                    set(i, v / norm);
                    return true;
                }
            });

    }

    /**
     * Set all the components to zero
     *
     * @return
     */
    public void clear() {
        for (int i = size; --i >= 0; )
            set(i, 0);
    }

    /**
     * Assign the values of another vector to this vector
     *
     * @param v
     */
    public void assign(DoubleMatrix1D v) {
        for (int i = 0; i < size; i++)
            set(i, v.get(i));
    }

    /**
     * Resize a vector
     *
     * @param newSize
     * @param newColumns
     * @return The same vector if the size requested is compatible, or a new vector
     */
    public DoubleMatrix1D resize(int newSize) {
        return resize(newSize, true);
    }

    /**
     * Resize a vector
     *
     * @param newSize The new size
     * @param copy Should the values be copied if a new vector is to be made
     * @return The same vector if the size requested is compatible, or a new vector
     */
    public DoubleMatrix1D resize(int newSize, boolean copy) {
        if (newSize == size)
            return this;

        try {
            DoubleMatrix1D m = like(newSize);
            if (copy) {
                int r = Math.min(newSize, size);
                m.viewPart(0, r).assign(viewPart(0, r));
            }
            return m;
        }
        catch (Exception e) {
            logger.warn("No int constructor found for %s", getClass());
            return null;
        }
    }

    /**
     * Creates a new vector of the same kind
     *
     * @param size
     * @return
     */
    DoubleMatrix1D like(int size) {
        try {
            if (getClass().isAnonymousClass())
                return new DenseDoubleMatrix1D(size);

            Constructor<? extends DoubleMatrix1D> constructor = getClass()
                .getConstructor(Integer.TYPE);
            DoubleMatrix1D m = constructor.newInstance(size);
            return m;
        }
        catch (Exception e) {
            throw new RuntimeException(
                "Could not create a new DoubleMatrix1D of class "
                    + getClass() + ": " + e);
        }
    }

    public int size() {
        return size;
    }

    /**
     * @param a
     * @param aggr
     * @param f
     * @param skipZeros
     * @return
     */
    public double aggregate(double a, final DoubleDoubleFunction aggr,
        final DoubleFunction f, boolean skipZeros) {
        if (size == 0)
            return a;

        if (skipZeros) {
            final DoubleHolder d = new DoubleHolder(a);
            forEachNonZero(new IntDoubleProcedure() {
                @Override
                public boolean apply(int i, double v) {
                    d.value = aggr.apply(d.value, f.apply(v));
                    return true;
                }
            });
            return d.value;
        }
        else {
            for (int i = size; --i >= 0; ) {
                a = aggr.apply(a, f.apply(get(i)));
            }
            return a;
        }
    }

    /**
     * @param a
     * @param aggr
     * @param f
     * @param skipZeros
     * @return
     */
    public double aggregate(double a, final DoubleDoubleFunction aggr,
        final IntDoubleFunction f, boolean skipZeros) {
        if (size == 0)
            return a;

        if (skipZeros) {
            final DoubleHolder d = new DoubleHolder(a);
            forEachNonZero(new IntDoubleProcedure() {
                @Override
                public boolean apply(int i, double v) {
                    d.value = aggr.apply(d.value, f.apply(i, v));
                    return true;
                }
            });
            return d.value;
        }
        else {
            for (int i = size; --i >= 0; ) {
                a = aggr.apply(a, f.apply(i, get(i)));
            }
            return a;
        }
    }

    /**
     * Assign a value given by a function to all non zero values
     *
     * @param f
     */
    public void assign(DoubleFunction f) {
        for (int i = 0; i < size; i++)
            update(i, f);
    }

    public DoubleMatrix1D copy() {
        try {
            DoubleMatrix1D like = like(this.size);
            like.assign(this);
            return like;
        }
        catch (Throwable e) {
            throw new RuntimeException("Could not create a copy of the vector "
                + getClass(), e);
        }
    }

    public void forEachNonZero(IntDoubleProcedure p) {
        for (int i = 0; i < size; i++) {
            final double v = get(i);
            if (v != 0)
                if (!p.apply(i, v))
                    break;
        }
    }

    /**
     * Applies f to each non null component, and replaces the value by the new one returned
     *
     * @param f
     */
    public void forEachNonZero(final IntDoubleFunction f) {
        forEachNonZero(new IntDoubleProcedure() {
            @Override
            public boolean apply(int i, double v) {
                double r = f.apply(i, v);
                if (r != v)
                    set(i, r);
                return true;
            }
        });
    }

    public void forEachNonZero(final DoubleFunction f) {
        forEachNonZero(new IntDoubleProcedure() {
            @Override
            public boolean apply(int i, double v) {
                double r = f.apply(v);
                if (r != v)
                    set(i, r);
                return true;
            }
        });
    }

    public void assign(double[] ds) {
        for (int i = size - 1; i >= 0; i--)
            set(i, ds[i]);
    }

    /**
     * Aggregate a pair of vectors. For each pair of values of same index of the two vectors, applies f and aggregates
     * with aggr.
     *
     * @param a The intial value
     * @param other The other vector
     * @param aggr The aggregation function
     * @param f The function used on pairs of vector components
     * @param skipZeros If zeros should be skipped (useful for optimisation if this method is overriden)
     * @return The aggregated value
     */
    public double aggregate(double a, DoubleMatrix1D other,
        DoubleDoubleFunction aggr, DoubleDoubleFunction f, boolean skipZeros) {
        if (size == 0)
            return Double.NaN;
        for (int i = size - 1; i >= 0; i--) {
            final double x = get(i);
            final double y = other.get(i);
            if (!skipZeros || x != 0 || y != 0)
                a = aggr.apply(a, f.apply(x, y));
        }
        return a;
    }

    public void print(PrintWriter w) {
        for (int i = 0; i < size; i++) {
            if (i != 0)
                w.print(' ');
            w.format("%10.2e", get(i));
        }
        w.println();
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", getClass(), size);
    }

    /**
     * Add an other vector
     * <pre>this = this + other</pre>
     *
     * @param other The vector to add
     */
    public void add(DoubleMatrix1D other) {
        add(1, other);
    }

    /**
     * Add an other vector
     * <pre>this = this + alpha * other</pre>
     *
     * @param alpha The coefficient
     * @param other The vector to add
     */
    public void add(final double alpha, DoubleMatrix1D other) {
        if (size != other.size)
            throw new OutOfBoundsException(
                "Cannot add two vectors of differents sizes (%d and %d)",
                size, other.size);

        other.forEachNonZero(new IntDoubleProcedure() {
            @Override
            public boolean apply(int i, double v) {
                set(i, get(i) + alpha * v);
                return true;
            }
        });

    }

    /**
     * Adjust the memory so that only the space taken by the matrix view is used
     */
    public void trim() {
    }
}

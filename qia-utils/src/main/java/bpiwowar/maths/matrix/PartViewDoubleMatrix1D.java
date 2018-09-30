package bpiwowar.maths.matrix;

import bpiwowar.ml.OutOfBoundsException;
import cern.colt.function.DoubleFunction;
import cern.colt.function.IntDoubleProcedure;

/**
 * A partial view on a matrix
 *
 * @author bpiwowar
 */
public class PartViewDoubleMatrix1D extends DoubleMatrix1D {
    private static final long serialVersionUID = 1L;

    final DoubleMatrix1D mOriginal;
    private int offset;

    public PartViewDoubleMatrix1D(DoubleMatrix1D mOriginal, int offset,
        int height) {
        this.mOriginal = mOriginal;
        if (offset + height > mOriginal.size)
            throw new OutOfBoundsException(
                "The view (%d) of dimensions %d cannot be created for a %d vector",
                offset, height, mOriginal.size);
        this.size = height;
        this.offset = offset;
    }

    /* (non-Javadoc)
     * @see bpiwowar.maths.matrix.DoubleMatrix1D#forEachNonZero(cern.colt.function.IntDoubleProcedure)
     *
     * Faster version in case we have a view which is a sparse matrix
     */
    @Override
    public void forEachNonZero(final IntDoubleProcedure f) {
        mOriginal.forEachNonZero(new IntDoubleProcedure() {
            @Override
            public boolean apply(int i, double v) {
                if (i >= offset && i < offset + size)
                    return f.apply(i - offset, v);
                return true;
            }
        });
    }

    @Override
    public void update(int i, DoubleFunction f) {
        mOriginal.update(i + offset, f);
    }

    @Override
    public double get(int i) {
        return mOriginal.get(i + offset);
    }

    @Override
    public void set(int i, double v) {
        mOriginal.set(i + offset, v);
    }

    @Override
    protected DoubleMatrix1D getViewPart(int offset, int height) {
        return new PartViewDoubleMatrix1D(mOriginal, this.offset + offset, height);
    }

    @Override
    public String toString() {
        return String.format("View[%d/%d] of %s", offset, size, mOriginal);
    }
}

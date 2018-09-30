package bpiwowar.maths.matrix;

import bpiwowar.ml.OutOfBoundsException;
import cern.colt.function.DoubleFunction;

/**
 * A partial view on a matrix
 *
 * @author bpiwowar
 */
public class PartViewDoubleMatrix2D extends DoubleMatrix2D {
    private static final long serialVersionUID = 1L;
    final DoubleMatrix2D mOriginal;
    private int columnOffset;
    private int rowOffset;

    public PartViewDoubleMatrix2D(DoubleMatrix2D mOriginal, int i, int j,
        int height, int width) {
        this.mOriginal = mOriginal;

        if (j + width > mOriginal.columns || i + height > mOriginal.rows)
            throw new OutOfBoundsException(
                "The view (%d,%d) of dimensions %dx%d cannot be created for a %dx%d matrix",
                i, j, height, width, mOriginal.rows, mOriginal.columns);

        this.rows = height;
        this.rowOffset = i;

        this.columns = width;
        this.columnOffset = j;
    }

    @Override
    void update(int i, int j, DoubleFunction f) {
        mOriginal.update(i + rowOffset, j + columnOffset, f);
    }

    @Override
    public double get(int i, int j) {
        return mOriginal.get(i + rowOffset, j + columnOffset);
    }

    @Override
    public void set(int i, int j, double v) {
        mOriginal.set(i + rowOffset, j + columnOffset, v);
    }

    @Override
    public DoubleMatrix2D like(int rows, int columns) {
        return mOriginal.like(rows, columns);
    }

    @Override
    public String toString() {
        return String.format("View[+(%d,%d)/(%d,%d)] of %s", rowOffset,
            columnOffset, rows, columns, mOriginal);
    }

}

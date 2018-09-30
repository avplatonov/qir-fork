package cern.colt.matrix.impl;

import cern.colt.matrix.DoubleMatrix2D;
import java.io.Serializable;

public class DenseDoubleMatrix2DFactory implements DoubleMatrix2DFactory<DenseDoubleMatrix2D>, Serializable {
    private static final long serialVersionUID = 1L;

    private DenseDoubleMatrix2DFactory() {
    }

    static final public DenseDoubleMatrix2DFactory INSTANCE = new DenseDoubleMatrix2DFactory();

    @Override
    public DenseDoubleMatrix2D create(final int rows, final int cols) {
        return new DenseDoubleMatrix2D(rows, cols);
    }

    @Override
    public DenseDoubleMatrix2D resize(final DoubleMatrix2D matrix, final int rows, final int cols) {
        final DenseDoubleMatrix2D m = create(rows, cols);
        m.viewPart(0, 0, matrix.rows(), matrix.columns()).assign(matrix);
        return m;
    }

}

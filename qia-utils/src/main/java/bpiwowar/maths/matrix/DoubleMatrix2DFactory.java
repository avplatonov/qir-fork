package bpiwowar.maths.matrix;

import java.io.Serializable;

public interface DoubleMatrix2DFactory<T extends DoubleMatrix2D> extends Serializable {
    /**
     * Creates a new matrix
     *
     * @param rows
     * @param cols
     * @return
     */
    public T create(int rows, int cols);
}

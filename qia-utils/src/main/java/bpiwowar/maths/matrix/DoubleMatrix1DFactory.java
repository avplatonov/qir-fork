package bpiwowar.maths.matrix;

import java.io.Serializable;

public interface DoubleMatrix1DFactory<T extends DoubleMatrix1D> extends Serializable {
    /**
     * Creates a new matrix
     *
     * @param rows
     * @param cols
     * @return
     */
    public T create(int length);
}

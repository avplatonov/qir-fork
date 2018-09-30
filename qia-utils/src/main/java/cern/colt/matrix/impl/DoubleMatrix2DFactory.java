/**
 *
 */
package cern.colt.matrix.impl;

import cern.colt.matrix.DoubleMatrix2D;

public interface DoubleMatrix2DFactory<T extends DoubleMatrix2D> {
    /**
     * Creates a new matrix
     *
     * @param rows
     * @param cols
     * @return
     */
    public T create(int rows, int cols);

    /**
     * Resize the matrix
     *
     * @param m The matrix to resize
     * @param rows
     * @param cols
     * @return Returns the resized matrix (or a new matrix, with the provided dimensions and the data from matrix)
     */
    public T resize(DoubleMatrix2D m, int rows, int cols);
}

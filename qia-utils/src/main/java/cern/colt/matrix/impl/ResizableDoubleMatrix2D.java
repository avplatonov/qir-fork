package cern.colt.matrix.impl;

public interface ResizableDoubleMatrix2D<T extends ResizableDoubleMatrix2D<?>> {
    /**
     * Set the new number of rows & columns
     *
     * @param rows
     * @param columns
     */
    T resize(int rows, int columns);
}

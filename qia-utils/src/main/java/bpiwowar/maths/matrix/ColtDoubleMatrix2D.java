package bpiwowar.maths.matrix;

public class ColtDoubleMatrix2D extends DoubleMatrix2D {
    private static final long serialVersionUID = 1L;

    private cern.colt.matrix.DoubleMatrix2D matrix;

    public ColtDoubleMatrix2D(cern.colt.matrix.DoubleMatrix2D matrix) {
        this.matrix = matrix;
    }

    @Override
    public double get(int i, int j) {
        return matrix.get(i, j);
    }

    @Override
    public void set(int i, int j, double v) {
        matrix.set(i, j, v);
    }

}

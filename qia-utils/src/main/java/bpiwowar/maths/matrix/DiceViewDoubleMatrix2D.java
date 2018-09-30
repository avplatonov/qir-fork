package bpiwowar.maths.matrix;

import cern.colt.function.DoubleFunction;

final public class DiceViewDoubleMatrix2D extends DoubleMatrix2D {
    private static final long serialVersionUID = 1L;

    final DoubleMatrix2D mOriginal;

    /**
     * @param mOriginal
     */
    public DiceViewDoubleMatrix2D(final DoubleMatrix2D mOriginal) {
        this.mOriginal = mOriginal;
        this.rows = mOriginal.columns;
        this.columns = mOriginal.rows;
    }

    @Override
    void update(final int i, final int j, final DoubleFunction f) {
        mOriginal.update(j, i, f);
    }

    @Override
    public double get(final int i, final int j) {
        return mOriginal.get(j, i);
    }

    @Override
    public void set(final int i, final int j, final double v) {
        mOriginal.set(j, i, v);
    }

    @Override
    public DoubleMatrix2D viewDice() {
        return mOriginal;
    }

}

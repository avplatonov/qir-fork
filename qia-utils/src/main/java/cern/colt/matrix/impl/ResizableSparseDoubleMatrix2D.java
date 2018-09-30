package cern.colt.matrix.impl;

import bpiwowar.NotImplementedException;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import it.unimi.dsi.fastutil.longs.Long2DoubleRBTreeMap;
import java.io.Serializable;

/**
 * A resizable sparse double matrix
 *
 * @author bpiwowar
 */
public class ResizableSparseDoubleMatrix2D extends WrapperDoubleMatrix2D implements
    ResizableDoubleMatrix2D<ResizableSparseDoubleMatrix2D> {
    private static final long serialVersionUID = 1L;

    /**
     * Our elements (from fastutils)
     */
    Long2DoubleRBTreeMap elements;

    @Override
    public DoubleMatrix2D viewDice() {
        WrapperDoubleMatrix2D wrapper = new WrapperDoubleMatrix2D(this);
        wrapper.rows = this.rows;
        wrapper.columns = this.columns;
        return wrapper;
    }

    public ResizableSparseDoubleMatrix2D(int rows, int columns) {
        super(null);
        resize(rows, columns);
        elements = new Long2DoubleRBTreeMap();
    }

    @Override
    public int cardinality() {
        return elements.size();
    }

    @Override
    public ResizableSparseDoubleMatrix2D resize(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        return this;
    }

    @Override
    public double getQuick(int row, int column) {
        final long index = (((long)row) << 32) + column;
        return elements.get(index);
    }

    @Override
    public void trimToSize() {
        // Nothing to do!
    }

    @Override
    public DoubleMatrix2D like(int rows, int columns) {
        return new ResizableSparseDoubleMatrix2D(rows, columns);
    }

    @Override
    public DoubleMatrix1D like1D(int size) {
        return new SparseDoubleMatrix1D(size);
    }

    @Override
    protected DoubleMatrix1D like1D(int size, int zero, int stride) {
        throw new NotImplementedException();
    }

    @Override
    public DoubleMatrix1D viewRow(final int row) {
        return new DoubleMatrix1D() {
            {
                this.size = columns();
            }

            @Override
            protected DoubleMatrix1D viewSelectionLike(int[] offsets) {
                throw new NotImplementedException();
            }

            @Override
            public void setQuick(int index, double value) {
                ResizableSparseDoubleMatrix2D.this.setQuick(row, index, value);
            }

            @Override
            public DoubleMatrix2D like2D(int rows, int columns) {
                throw new NotImplementedException();
            }

            @Override
            public DoubleMatrix1D like(int size) {
                throw new NotImplementedException();
            }

            @Override
            public double getQuick(int index) {
                return ResizableSparseDoubleMatrix2D.this.getQuick(row, index);
            }
        };
    }

    @Override
    public DoubleMatrix1D viewColumn(final int column) {
        return new DoubleMatrix1D() {
            {
                this.size = ResizableSparseDoubleMatrix2D.this.rows();
            }

            @Override
            protected DoubleMatrix1D viewSelectionLike(int[] offsets) {
                throw new NotImplementedException();
            }

            @Override
            public void setQuick(int index, double value) {
                ResizableSparseDoubleMatrix2D.this.setQuick(index, column,
                    value);
            }

            @Override
            public DoubleMatrix2D like2D(int rows, int columns) {
                throw new NotImplementedException();
            }

            @Override
            public DoubleMatrix1D like(int size) {
                throw new NotImplementedException();
            }

            @Override
            public double getQuick(int index) {
                return ResizableSparseDoubleMatrix2D.this.getQuick(index,
                    column);
            }
        };
    }

    @Override
    public void setQuick(int row, int column, double value) {
        final long index = (((long)row) << 32) + column;
        if (Math.abs(value) == 0)
            elements.remove(index);
        else
            elements.put(index, value);
    }

    @Override
    protected DoubleMatrix2D viewSelectionLike(int[] rowOffsets,
        int[] columnOffsets) {
        throw new NotImplementedException();
    }

    @Override
    public DoubleMatrix1D zMult(final DoubleMatrix1D y, DoubleMatrix1D z,
        double alpha, double beta, final boolean transposeA) {
        int m = rows;
        int n = columns;
        if (transposeA) {
            m = columns;
            n = rows;
        }

        boolean ignore = (z == null);
        if (z == null)
            z = new DenseDoubleMatrix1D(m);
        final DoubleMatrix1D z2 = z;

        if (!(this.isNoView && y instanceof DenseDoubleMatrix1D && z instanceof DenseDoubleMatrix1D)) {
            return super.zMult(y, z, alpha, beta, transposeA);
        }

        if (n != y.size() || m > z.size())
            throw new IllegalArgumentException("Incompatible args: "
                + ((transposeA ? viewDice() : this).toStringShort()) + ", "
                + y.toStringShort() + ", " + z.toStringShort());

        if (!ignore)
            z.assign(cern.jet.math.Functions.mult(beta / alpha));

        for (it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry a : elements
            .long2DoubleEntrySet()) {
            long key = a.getLongKey();
            int i = (int)(key >>> 32);
            int j = (int)key;
            if (transposeA)
                z2.setQuick(j, z2.getQuick(j) + a.getDoubleValue()
                    * y.getQuick(i));
            else
                z2.setQuick(i, z2.getQuick(i) + a.getDoubleValue()
                    * y.getQuick(j));
        }

        if (alpha != 1)
            z.assign(cern.jet.math.Functions.mult(alpha));
        return z;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * cern.colt.matrix.DoubleMatrix2D#zMult(cern.colt.matrix.DoubleMatrix2D,
     * cern.colt.matrix.DoubleMatrix2D, double, double, boolean, boolean)
     */
    @Override
    public DoubleMatrix2D zMult(DoubleMatrix2D B, DoubleMatrix2D C,
        final double alpha, double beta, final boolean transposeA,
        boolean transposeB) {
        if (!(this.isNoView)) {
            return super.zMult(B, C, alpha, beta, transposeA, transposeB);
        }
        if (transposeB)
            B = B.viewDice();
        int m = rows;
        int n = columns;
        if (transposeA) {
            m = columns;
            n = rows;
        }
        int p = B.columns();
        boolean ignore = (C == null);
        if (C == null)
            C = new DenseDoubleMatrix2D(m, p);

        if (B.rows() != n)
            throw new IllegalArgumentException(
                "Matrix2D inner dimensions must agree:" + toStringShort()
                    + ", "
                    + (transposeB ? B.viewDice() : B).toStringShort());
        if (C.rows() != m || C.columns() != p)
            throw new IllegalArgumentException("Incompatibel result matrix: "
                + toStringShort() + ", "
                + (transposeB ? B.viewDice() : B).toStringShort() + ", "
                + C.toStringShort());
        if (this == C || B == C)
            throw new IllegalArgumentException("Matrices must not be identical");

        if (!ignore)
            C.assign(cern.jet.math.Functions.mult(beta));

        // cache views
        final DoubleMatrix1D[] Brows = new DoubleMatrix1D[n];
        for (int i = n; --i >= 0; )
            Brows[i] = B.viewRow(i);
        final DoubleMatrix1D[] Crows = new DoubleMatrix1D[m];
        for (int i = m; --i >= 0; )
            Crows[i] = C.viewRow(i);

        final cern.jet.math.PlusMult fun = cern.jet.math.PlusMult.plusMult(0);

        for (it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry a : elements
            .long2DoubleEntrySet()) {
            long key = a.getLongKey();
            int i = (int)(key >>> 32);
            int j = (int)key;

            fun.multiplicator = a.getDoubleValue() * alpha;
            if (!transposeA)
                Crows[i].assign(Brows[j], fun);
            else
                Crows[j].assign(Brows[i], fun);
        }

        return C;
    }

    static public class Factory implements DoubleMatrix2DFactory, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public DoubleMatrix2D create(int rows, int cols) {
            return new ResizableSparseDoubleMatrix2D(rows, cols);
        }

        @Override
        public DoubleMatrix2D resize(DoubleMatrix2D matrix, int rows, int cols) {
            return (DoubleMatrix2D)((ResizableSparseDoubleMatrix2D)matrix)
                .resize(rows, cols);
        }

    }

    final static public Factory FACTORY = new Factory();

}

package bpiwowar.maths.matrix;

import cern.colt.function.DoubleFunction;
import java.nio.DoubleBuffer;

/**
 * A dense matrix operated by BLAS
 */
public class DenseDoubleMatrix1D extends DoubleMatrix1D {
    private static final long serialVersionUID = 1L;

    final static long DOUBLE_BYTES = Double.SIZE / 8;

    /**
     * Offset to the first element
     */
    protected int offset = 0;

    /**
     * The stride (useful when the vector is a extracted from a matrix), i.e. the difference in position in the array
     * between two adjacent elements of the vector
     */
    public int stride = 1;

    /**
     * Row major or row minor matrix
     */
    protected double[] elements;

    public DenseDoubleMatrix1D(int m) {
        super(m);
        elements = new double[m];
    }

    public DenseDoubleMatrix1D(double[] ds) {
        this(ds.length);
        assign(ds);
    }

    /**
     * Creates a 2D representation of a dense vector
     */
    public DenseDoubleMatrix2D as2D() {
        return new DenseDoubleMatrix2D(size, 1, elements, stride, offset);
    }

    /**
     * Construct a sub-vector
     *
     * @param x The initial vector
     * @param offset The offset within x
     * @param length The length of the vector
     */
    protected DenseDoubleMatrix1D(DenseDoubleMatrix1D x, int offset, int length) {
        super(length);
        this.offset = x.offset + offset;
        this.elements = x.elements;
        this.stride = x.stride;
    }

    public DenseDoubleMatrix1D(double[] elements, int offset, int stride,
        int length) {
        super(length);
        this.elements = elements;
        this.offset = offset;
        this.stride = stride;
    }

    @Override
    public void assign(DoubleMatrix1D v) {
        // Fast copy for two dense matrices with stride equal to 1
        if (v instanceof DenseDoubleMatrix1D) {
            final DenseDoubleMatrix1D dv = (DenseDoubleMatrix1D)v;
            if (dv.stride == 1 && stride == 1) {
                System.arraycopy(dv.elements, dv.offset, elements, offset, size);
                return;
            }
        }
        // Otherwise we use the default
        super.assign(v);
    }

    @Override
    public double get(int i) {
        return elements[offset + i * stride];
    }

    @Override
    public void set(int i, double v) {
        elements[offset + i * stride] = v;
    }

    @Override
    public void update(int i, DoubleFunction f) {
        final int pos = offset + i * stride;
        elements[pos] = f.apply(elements[pos]);
    }

    public DoubleBuffer getPointer() {
        return DoubleBuffer.wrap(elements, offset, elements.length - offset)
            .slice();
    }

    @Override
    protected DenseDoubleMatrix1D getViewPart(int offset, int height) {
        return new DenseDoubleMatrix1D(this, offset, height);
    }

    @Override
    public DenseDoubleMatrix1D resize(int newSize, boolean copy) {
        // Exactly the same size
        if (newSize == size)
            return this;

        // If we have enough space
        if ((elements.length - offset) / stride >= newSize) {
            size = newSize;
            return this;
        }

        // OK, let's expand
        int capacity = DenseDoubleMatrix2D.grow(size, newSize);
        DenseDoubleMatrix1D other = new DenseDoubleMatrix1D(
            new double[capacity], 0, 1, newSize);
        if (copy)
            other.viewPart(0, size).assign(this);
        return other;

    }

    // ---- Factory

    final static public Factory FACTORY = new Factory();

    static class Factory implements DoubleMatrix1DFactory<DenseDoubleMatrix1D> {
        private static final long serialVersionUID = 1L;

        @Override
        public DenseDoubleMatrix1D create(int length) {
            return new DenseDoubleMatrix1D(length);
        }
    }

    @Override
    public void trim() {
        if (size != elements.length) {
            double[] newElements = new double[size];
            for (int i = size; --i >= 0; )
                newElements[i] = elements[offset + stride * i];

            // Set up the new values
            elements = newElements;
            offset = 0;
            stride = 1;
        }
    }

    public static class DenseInnerProduct implements
        Multiply.InnerProduct<DenseDoubleMatrix1D, DenseDoubleMatrix1D> {

        boolean useBlas;

        public DenseInnerProduct() {
            this(false);
        }

        public DenseInnerProduct(boolean useBlas) {
            this.useBlas = useBlas;
            if (useBlas && !Blas.register()) {
                // We will use something different
                useBlas = false;
            }
        }

        @Override
        public double complexity(int m) {
            return ((double)m) / 10.;
        }

        @Override
        public double innerProduct(DenseDoubleMatrix1D x, DenseDoubleMatrix1D y) {
            if (!useBlas)
                return Multiply.DefaultInnerProduct.staticInnerProduct(x, y);

            final DoubleBuffer xPointer = x.getPointer();
            final DoubleBuffer yPointer = y.getPointer();
            double inner = Blas.cblas_ddot(x.size, xPointer, x.stride,
                yPointer, y.stride);
            return inner;
        }

    }

}

package bpiwowar.maths.matrix;

import bpiwowar.collections.IntersectionIterable;
import bpiwowar.maths.functions.Functions;
import bpiwowar.utils.Output;
import bpiwowar.utils.Pair;
import cern.colt.function.IntDoubleFunction;
import cern.colt.function.IntDoubleProcedure;
import it.unimi.dsi.fastutil.ints.Int2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import java.io.PrintWriter;

public class SparseDoubleMatrix1D extends DoubleMatrix1D {
    private static final long serialVersionUID = 1L;

    Int2DoubleLinkedOpenHashMap values = new Int2DoubleLinkedOpenHashMap();

    // Int2DoubleRBTreeMap values = new Int2DoubleRBTreeMap();

    public SparseDoubleMatrix1D(int n) {
        super(n);
    }

    @Override
    public double get(int i) {
        return values.get(i);
    }

    @Override
    public SparseDoubleMatrix1D resize(int newSize) {
        return resize(newSize, true);
    }

    @Override
    public SparseDoubleMatrix1D resize(int newSize, boolean copy) {
        this.size = newSize;
        return this;
    }

    @Override
    public void set(int i, double v) {
        if (v == 0)
            values.remove(i);
        else
            values.put(i, v);
    }

    /**
     * Resize the sparse vector to ensure we can add an element
     *
     * @param i
     */
    public void ensure(int i) {
        if (i >= size)
            size = i + 1;
    }

    @Override
    public void forEachNonZero(IntDoubleProcedure p) {
        for (Entry d : values.int2DoubleEntrySet())
            if (!p.apply(d.getIntKey(), d.getDoubleValue()))
                break;
    }

    @Override
    public void forEachNonZero(final IntDoubleFunction f) {
        ObjectBidirectionalIterator<Entry> iterator = values
            .int2DoubleEntrySet().iterator();
        while (iterator.hasNext()) {
            Entry d = iterator.next();
            final double v = d.getDoubleValue();
            final int i = d.getIntKey();
            double r = f.apply(i, v);
            if (r != v)
                if (r == 0)
                    iterator.remove();
                else
                    d.setValue(r);

        }
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public void print(PrintWriter w) {
        Output.print(w, ", ", values.entrySet());
    }

    /**
     * Inner product with two sparse vectors
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    public static class Sparse2InnerProduct implements
        Multiply.InnerProduct<SparseDoubleMatrix1D, SparseDoubleMatrix1D> {

        public Sparse2InnerProduct() {
        }

        @Override
        public double complexity(int m) {
            return m / 2;
        }

        @Override
        public double innerProduct(SparseDoubleMatrix1D x,
            SparseDoubleMatrix1D y) {
            double sum = 0;
            for (Pair<Integer, Double[]> v : IntersectionIterable.create(
                Double.class, x.values.int2DoubleEntrySet(),
                y.values.int2DoubleEntrySet())) {
                Double[] z = v.getSecond();
                sum += z[0] * z[1];
            }
            return sum;
        }
    }

    /**
     * Inner product with one sparse vector
     *
     * @author B. Piwowarski <benjamin@bpiwowar.net>
     */
    public static class SparseInnerProduct implements
        Multiply.InnerProduct<SparseDoubleMatrix1D, DoubleMatrix1D> {

        public SparseInnerProduct() {
        }

        @Override
        public double complexity(int m) {
            return m;
        }

        @Override
        public double innerProduct(SparseDoubleMatrix1D x,
            final DoubleMatrix1D y) {
            return x.aggregate(0, Functions.plus, new IntDoubleFunction() {
                @Override
                public double apply(int i, double x) {
                    return x != 0 ? y.get(i) * x : 0;
                }
            }, false);
        }

    }

    final static public Factory FACTORY = new Factory();

    static class Factory implements DoubleMatrix1DFactory<SparseDoubleMatrix1D> {
        private static final long serialVersionUID = 1L;

        @Override
        public SparseDoubleMatrix1D create(int length) {
            return new SparseDoubleMatrix1D(length);
        }
    }
}

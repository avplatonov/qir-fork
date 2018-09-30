package cern.colt.matrix.impl;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.DoubleFunction;
import cern.colt.function.IntDoubleProcedure;
import cern.colt.function.IntProcedure;
import cern.colt.map.AbstractIntDoubleMap;

/**
 * Same as {@see cern.colt.matrix.impl.SparseDoubleMatrix1D}, but with a way to access non zero elements only
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class BPSparseDoubleMatrix1D extends
    cern.colt.matrix.impl.SparseDoubleMatrix1D {

    public BPSparseDoubleMatrix1D(double[] values) {
        super(values);
    }

    public BPSparseDoubleMatrix1D(int size, AbstractIntDoubleMap elements,
        int offset, int stride) {
        super(size, elements, offset, stride);
    }

    public BPSparseDoubleMatrix1D(int size, int initialCapacity,
        double minLoadFactor, double maxLoadFactor) {
        super(size, initialCapacity, minLoadFactor, maxLoadFactor);
    }

    public BPSparseDoubleMatrix1D(int size) {
        super(size);
    }

    private static final long serialVersionUID = 1L;

    public void forEachNonZero(final IntDoubleProcedure procedure) {
        elements.forEachKey(new IntProcedure() {
            @Override
            public boolean apply(int index) {
                return procedure.apply(index, getQuick(index));
            }
        });
    }

    public void forEachNonZero(final IntProcedure procedure) {
        elements.forEachKey(procedure);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Vector of size ");
        sb.append(size());
        sb.append(" [");
        forEachNonZero(new IntDoubleProcedure() {
            @Override
            public boolean apply(int first, double second) {
                sb.append(String.format(" %d:%.3e", first, second));
                return true;
            }
        });
        sb.append("]");
        return sb.toString();
    }

    private final class Aggregator implements IntDoubleProcedure {
        double a = 0;
        DoubleDoubleFunction aggr;
        DoubleFunction f;

        public Aggregator(DoubleDoubleFunction aggr, DoubleFunction f) {
            super();
            this.aggr = aggr;
            this.f = f;
        }

        @Override
        public boolean apply(int first, double value) {
            a = aggr.apply(a, f.apply(value));
            return true;
        }
    }

    /**
     * Aggregate on non-null values (faster for big vectors)
     *
     * @param aggr
     * @param f
     * @return
     */
    public double nonNullAggregate(DoubleDoubleFunction aggr, DoubleFunction f) {
        if (size == 0)
            return Double.NaN;
        final Aggregator aggregator = new Aggregator(aggr, f);
        elements.forEachPair(aggregator);
        return aggregator.a;
    }
}

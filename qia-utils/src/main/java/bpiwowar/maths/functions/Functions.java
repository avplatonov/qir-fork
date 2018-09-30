package bpiwowar.maths.functions;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.DoubleFunction;

public class Functions {

    final public static DoubleFunction sqrt = new DoubleFunction() {
        @Override
        public double apply(double x) {
            return Math.sqrt(x);
        }
    };

    final public static DoubleFunction sqr = new DoubleFunction() {
        @Override
        public double apply(double x) {
            // cern.jet.math.Functions.plus()
            return x * x;
        }
    };

    final public static DoubleFunction neg = new DoubleFunction() {
        @Override
        public double apply(double x) {
            return -x;
        }
    };

    public static final DoubleFunction INCREMENT = new DoubleFunction() {
        @Override final public double apply(double x) {
            return x + 1;
        }
    };

    public static final DoubleDoubleFunction plus = new DoubleDoubleFunction() {
        @Override
        public double apply(double x, double y) {
            return x + y;
        }
    };

    public static final DoubleDoubleFunction minus = new DoubleDoubleFunction() {
        @Override
        public double apply(double x, double y) {
            return x - y;
        }
    };

    public static final DoubleFunction identity = new DoubleFunction() {
        @Override
        public double apply(double x) {
            return x;
        }
    };

    public final static DoubleFunction square = new DoubleFunction() {
        @Override
        public double apply(double x) {
            return x * x;
        }
    };

    public final static DoubleFunction abs = new DoubleFunction() {
        @Override
        public double apply(double x) {
            return Math.abs(x);
        }
    };

    public final static DoubleDoubleFunction min = new DoubleDoubleFunction() {

        @Override
        public double apply(double x, double y) {
            return Math.min(x, y);
        }
    };

    public final static DoubleDoubleFunction max = new DoubleDoubleFunction() {
        @Override
        public double apply(double x, double y) {
            return Math.max(x, y);
        }
    };

    /**
     * Returns the function f(x) = x + y
     */
    final public static DoubleFunction add(final double y) {
        return new DoubleFunction() {
            @Override
            public double apply(double x) {
                return x + y;
            }
        };
    }

    /**
     * Returns the function f(x) = alpha * x + y
     */
    final public static DoubleFunction add(final double alpha, final double y) {
        return new DoubleFunction() {
            @Override
            public double apply(double x) {
                return alpha * x + y;
            }
        };
    }

    public static DoubleFunction chain(final DoubleFunction... functions) {
        return new DoubleFunction() {
            @Override
            public double apply(double x) {
                for (int i = functions.length; --i >= 0; )
                    x = functions[i].apply(x);
                return x;
            }
        };
    }

    public static DoubleFunction div(final double norm) {
        return new DoubleFunction() {
            @Override
            public double apply(double x) {
                return x / norm;
            }

        };
    }

    public static DoubleDoubleFunction chain(final DoubleFunction f,
        final DoubleDoubleFunction g) {
        return new DoubleDoubleFunction() {
            @Override
            public double apply(double x, double y) {
                return f.apply(g.apply(x, y));
            }
        };
    }

}

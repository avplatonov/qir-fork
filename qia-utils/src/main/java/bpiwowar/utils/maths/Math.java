package bpiwowar.utils.maths;

import static java.lang.Math.log;

public class Math {
    final static double MINPROBA = 1e-30;

    final static double MAXPROBA = 1. - MINPROBA;

    final static double MINLOG = log(MINPROBA);

    final static double MAXLOG = log(MAXPROBA);

    public static double normaliseLog(final double x) {
        if (x < MINLOG)
            return MINLOG;
        if (x > MAXLOG)
            return MAXLOG;
        return x;
    }
}

package bpiwowar.maths;

public class Misc {
    public static final double DOUBLE_PRECISION = getDoublePrecision();

    /**
     * Return the precision of a double
     *
     * @return
     */
    private static double getDoublePrecision() {
        double p = 1;
        double s = 0;
        while ((s + p / 2) != s) {
            p = p / 2;
            s = s + p / 2;
        }
        return p;
    }
}

package bpiwowar.pipe;

/**
 * A piece of data, i.e. fields indexed by integers
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class Tuple {
    private Object[] values;

    Tuple(int length) {
        values = new Object[length];
    }

    Object getValue(int i) {
        return values[i];
    }

    void setValue(int i, Object object) {
        values[i] = object;
    }
}

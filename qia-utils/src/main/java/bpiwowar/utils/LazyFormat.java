package bpiwowar.utils;

/**
 * A lazy format object - used when we want to delay the formatting as much as possible (i.e. logging)
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
final public class LazyFormat {
    String string;
    String format;
    Object[] objects;

    public LazyFormat(final String format, final Object... objects) {
        this.format = format;
        this.objects = objects;
    }

    @Override final public String toString() {
        if (string == null) {
            // format
            string = String.format(format, objects);

            // release for GC
            format = null;
            objects = null;
        }
        return string;
    }
}

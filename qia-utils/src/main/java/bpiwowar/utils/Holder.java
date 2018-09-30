package bpiwowar.utils;

public class Holder<T> {
    T t;

    public T get() {
        return t;
    }

    public void set(T t) {
        this.t = t;
    }

    public static <T> Holder<T> create() {
        return new Holder<T>();
    }

    public static class Double {
        double x;

        public double get() {
            return x;
        }

        public void set(double x) {
            this.x = x;
        }
    }
}

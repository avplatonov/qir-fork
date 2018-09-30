package bpiwowar.utils;

import java.io.Serializable;

/**
 * @author bpiwowar
 */
public class Triple<T1, T2, T3> implements Serializable {
    private static final long serialVersionUID = 1;
    protected T1 first;
    protected T2 second;
    protected T3 third;

    public static <T1, T2, T3> Triple<T1, T2, T3> create(T1 a, T2 b, T3 c) {
        return new Triple<T1, T2, T3>(a, b, c);
    }

    public Triple() {
    }

    public Triple(final T1 x, final T2 y, final T3 z) {
        this.first = x;
        this.second = y;
        this.third = z;
    }

    public final T1 getFirst() {
        return first;
    }

    public final void setFirst(final T1 x) {
        this.first = x;
    }

    public final T2 getSecond() {
        return second;
    }

    public final void setSecond(final T2 y) {
        this.second = y;
    }

    public T3 getThird() {
        return third;
    }

    public void setThird(T3 third) {
        this.third = third;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("(%s,%s)", first, second);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        result = prime * result + ((third == null) ? 0 : third.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Triple other = (Triple)obj;
        if (first == null) {
            if (other.first != null) {
                return false;
            }
        }
        else if (!first.equals(other.first)) {
            return false;
        }
        if (second == null) {
            if (other.second != null) {
                return false;
            }
        }
        else if (!second.equals(other.second)) {
            return false;
        }
        if (third == null) {
            if (other.third != null) {
                return false;
            }
        }
        else if (!third.equals(other.third)) {
            return false;
        }
        return true;
    }

}

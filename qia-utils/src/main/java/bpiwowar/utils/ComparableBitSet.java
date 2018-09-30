package bpiwowar.utils;

import java.util.Arrays;

/**
 * A bitset
 *
 * @author bpiwowar
 */
public class ComparableBitSet implements Comparable<ComparableBitSet> {
    private static final long serialVersionUID = -1298245268897230820L;
    private byte[] data;
    private int length;

    /**
     * @param n size
     */
    public ComparableBitSet(final int n) {
        length = n;
        data = new byte[n / 8 + (n % 8 != 0 ? 1 : 0)];
    }

    public int compareTo(final ComparableBitSet o) {
        for (int i = 0, N = java.lang.Math.min(data.length, o.data.length); i < N; i++) {
            final int z = (0xFF & o.data[i]) - (0xFF & data[i]);
            if (z != 0)
                return z;
        }

        // equality so far
        return data.length - o.data.length;
    }

    public void set(final int i) {
        set(i, true);
    }

    public boolean get(final int i) {
        return (data[i / 8] & (byte)(0x80 >> i % 8)) != 0;
    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < length; i++)
            s += get(i) ? "1" : "0";

        return s;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + Arrays.hashCode(data);
        result = PRIME * result + length;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ComparableBitSet other = (ComparableBitSet)obj;
        if (length != other.length)
            return false;
        if (!Arrays.equals(data, other.data))
            return false;
        return true;
    }

    public void set(final int i, final boolean v) {
//		System.out.format("Set %d (%b): ", i, v);
//		System.out.format("%d (%s) => ",0xFF & data[i/8], toString());
        if (v)
            data[i / 8] |= (byte)(0x80 >> i % 8);
        else
            data[i / 8] &= (byte)(0x80 >> i % 8) ^ (byte)255;

        assert get(i) == v;

//		System.out.format("%d (%s)\n", 0xFF & data[i/8], toString());

    }

    /**
     * @return
     */
    public int size() {
        return length;
    }

}

package bpiwowar.utils;

import org.junit.Assert;
import org.junit.Test;

public class PagedByteArrayTest {

    @Test
    public void test() {
        PagedByteArray array = new PagedByteArray(3);
        final byte[] x = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        array.write(0, x, 0, 3);
        array.write(0, x, 0, 6);
        array.write(6, x, 6, 3);
        array.write(9, x, 9, 1);
        array.write(0, x, 0, 3);

        Assert.assertArrayEquals(x, array.get(0, 10));

        array = new PagedByteArray(3);
        array.write(0, x, 0, 2);
        array.write(2, x, 2, 4);
        array.write(6, x, 6, 1);
        array.write(7, x, 7, 3);
        Assert.assertArrayEquals(x, array.get(0, 10));

    }
}

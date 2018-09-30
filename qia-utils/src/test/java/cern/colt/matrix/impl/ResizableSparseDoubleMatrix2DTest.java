package cern.colt.matrix.impl;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class ResizableSparseDoubleMatrix2DTest {

    @Test
    public void test() {
        ResizableSparseDoubleMatrix2D m = new ResizableSparseDoubleMatrix2D(Integer.MAX_VALUE, Integer.MAX_VALUE);
        m.setQuick(Integer.MAX_VALUE, Integer.MAX_VALUE, 1e-3);
        Assert.assertEquals(m.getQuick(Integer.MAX_VALUE, Integer.MAX_VALUE), 1e-3, 0.001);

        // Random allocations
        double v = 0;
        Random r = new Random(0);
        final int N = 1000;
        for (int t = 0; t < N; t++) {
            int i = Math.abs(r.nextInt());
            int j = Math.abs(r.nextInt());
            m.setQuick(i, j, v);
            v++;
        }

        r.setSeed(0);
        v = 0;
        for (int t = 0; t < N; t++) {
            int i = Math.abs(r.nextInt());
            int j = Math.abs(r.nextInt());
            Assert.assertEquals(m.getQuick(i, j), v, 0.01);
            v++;
        }
    }
}

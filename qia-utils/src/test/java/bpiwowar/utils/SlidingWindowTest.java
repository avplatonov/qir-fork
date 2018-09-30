package bpiwowar.utils;

import org.junit.Test;

public class SlidingWindowTest {

    @Test
    public void testRelativePosition() {
        SlidingWindow<Integer> window = new SlidingWindow<Integer>(new Integer[3]);

        window.add(1);
        assert window.getRelative(0) == 1;
        assert window.getSize() == 1;

        window.add(2);
        assert window.getRelative(0) == 1;

        window.add(3);
        window.add(4);

        assert window.getRelative(0) == 2;
        assert window.getRelative(1) == 3;
        assert window.getRelative(2) == 4;

    }
}

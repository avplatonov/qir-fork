package bpiwowar.ml.bn;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static bpiwowar.ml.bn.VariableHelper.checkEvidences;
import static bpiwowar.ml.bn.VariableHelper.checkPC;

/**
 * @author bpiwowar
 */
public class CPFirstVirtualParentTest {
    Variable p, v;

    CPFirstVirtualParent cp;

    @Before
    public void init() {
        p = new Variable(4);
        v = new Variable(12);
        v.addParent(p);

        cp = new CPFirstVirtualParent("", 3, 4);
        v.setFunction(cp);
    }

    @Test
    public void constraintedParentConfiguration() {
        for (int e = 0; e < 3; e++)
            checkPC(v, false, e, new int[] {0});
        for (int e = 3; e < 6; e++)
            checkPC(v, false, e, new int[] {1});
        for (int e = 6; e < 9; e++)
            checkPC(v, false, e, new int[] {2});
        for (int e = 9; e < 12; e++)
            checkPC(v, false, e, new int[] {3});
    }

    @Test
    public void unconstraintedParentConfiguration() {
        checkPC(v, false, -1, new int[] {0, 1, 2, 3});
    }

    @Test
    public void unconstrainedEvidence() {
        checkEvidences(v, -1, new int[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
            12});
    }

    @Test
    public void constrainedEvidence() {
        checkEvidences(v, 0, new int[] {0, 1, 2});
        checkEvidences(v, 1, new int[] {3, 4, 5});
        checkEvidences(v, 2, new int[] {6, 7, 8});
        checkEvidences(v, 3, new int[] {9, 10, 11});
    }

    @Test
    public void positions() {
        // Array (state, parent state, position)
        final int[][] positions = new int[][] {
            {0, 1, -1}, {0, 0, 0},
            {1, 0, 1}, {2, 0, 2}, {3, 0, -1}, {3, 1, 3},
            {4, 1, 4}, {5, 1, 5}};

        for (int[] x : positions)
            Assert.assertEquals(cp.getPosition(v, x[0], new int[] {x[1]}),
                x[2]);
    }
}

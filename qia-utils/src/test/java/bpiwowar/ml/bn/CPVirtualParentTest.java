/**
 *
 */
package bpiwowar.ml.bn;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static bpiwowar.ml.bn.VariableHelper.checkEvidences;
import static bpiwowar.ml.bn.VariableHelper.checkPC;

/**
 * @author bpiwowar
 */
public class CPVirtualParentTest {

    Variable p, v;
    CPVirtualParent cp;

    @Before
    public void init() {
        // 4 * 4
        p = new Variable(16);
        v = new Variable(12);
        v.addParent(p);

        cp = new CPVirtualParent("", 3, 4, 4);
        v.setFunction(cp);
    }

    @Test
    public void constraintedParentConfiguration() {
        for (int e = 0; e < 3; e++)
            checkPC(v, false, e, new int[] {0, 1, 2, 3});
        for (int e = 3; e < 6; e++)
            checkPC(v, false, e, new int[] {4, 5, 6, 7});
        for (int e = 6; e < 9; e++)
            checkPC(v, false, e, new int[] {8, 9, 10, 11});
        for (int e = 9; e < 12; e++)
            checkPC(v, false, e, new int[] {12, 13, 14, 15});
    }

    @Test
    public void unconstraintedParentConfiguration() {
        checkPC(v, false, -1, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    }

    @Test
    public void constrainedEvidence() {
        for (int e = 0; e < 4; e++)
            checkEvidences(v, e, new int[] {0, 1, 2});
        for (int e = 4; e < 8; e++)
            checkEvidences(v, e, new int[] {3, 4, 5});
        for (int e = 8; e < 12; e++)
            checkEvidences(v, e, new int[] {6, 7, 8});
        for (int e = 12; e < 16; e++)
            checkEvidences(v, e, new int[] {9, 10, 11});
    }

    @Test
    public void positions() {
        // Array (state, parent state, position)
        final int[][] positions = new int[][] {
            {0, 0, 0}, {1, 0, 1}, {2, 0, 2},
            {0, 4, -1}, {3, 4, 12},
            {4, 6, 19},
            {7, 8, 25}
        };

        for (int[] x : positions)
            Assert.assertEquals(String.format("%d/%d", x[0], x[1]), x[2],
                cp.getPosition(v, x[0], new int[] {x[1]})
            );
    }

}

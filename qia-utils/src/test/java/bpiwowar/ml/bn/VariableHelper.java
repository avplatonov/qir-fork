/**
 *
 */
package bpiwowar.ml.bn;

import org.junit.Assert;

/**
 * @author bpiwowar
 */
public class VariableHelper {
    static public void checkEvidences(Variable v, int pc, int[] expected) {
        int i = 0;
        for (int e : v.getFunction().getEvidences(v, new int[] {pc})) {
            Assert.assertTrue(i < expected.length);
            Assert.assertEquals(String.format(
                "evidence no %d for pc [%d]", i + 1, pc),
                expected[i], e);
            i++;
        }
    }

    static public void checkPC(Variable v, boolean noPC, int evidence, int[] expected) {
        int i = 0;
        for (ParentConfiguration pc = v
            .getParentConfigurations(evidence, false); pc.hasCurrent(); pc
            .next(), i++) {
            Assert.assertTrue(i < expected.length);
            Assert.assertEquals(String.format(
                "configuration no %d for evidence %d", i + 1, evidence),
                expected[i], pc.current[0]);
        }
    }

}

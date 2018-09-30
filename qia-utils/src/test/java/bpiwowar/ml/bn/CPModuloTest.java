/**
 *
 */
package bpiwowar.ml.bn;

import org.junit.Test;

import static bpiwowar.ml.bn.VariableHelper.checkPC;

/**
 * @author bpiwowar
 */
public class CPModuloTest {

    @Test
    public void parentConfiguration() {
        Variable p = new Variable(12);
        Variable v = new Variable(4);
        v.addParent(p);
        CPModulo f = new CPModulo("", v);
        v.setFunction(f);

        checkPC(v, false, 0, new int[] {0, 4, 8});
        checkPC(v, false, 1, new int[] {1, 5, 9});
        checkPC(v, false, 2, new int[] {2, 6, 10});
        checkPC(v, false, 3, new int[] {3, 7, 11});
    }
}

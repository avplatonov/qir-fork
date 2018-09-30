package bpiwowar.maths.eigen;

import bpiwowar.NotImplementedException;
import bpiwowar.maths.eigen.selector.Selector;
import bpiwowar.maths.matrix.DiagonalDoubleMatrix;
import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;
import bpiwowar.utils.holders.IntHolder;
import java.nio.DoubleBuffer;

public class LapackRankOneUpdate implements RankOneUpdate {

    @Override
    public Result rankOneUpdate(DoubleMatrix2D D, double rho, DoubleMatrix1D z,
        boolean computeEigenvectors, Selector selector, boolean keep) {
        DoubleBuffer diag = null;
        if (D instanceof DiagonalDoubleMatrix)
            diag = ((DiagonalDoubleMatrix)D).getDiagonalDoubleBuffer();
        else {
            // TODO
            throw new NotImplementedException();
        }

        IntHolder n = new IntHolder(D.columns());
//		Blas.dlaed1(n, diag, q, ldq, indxq, rho, cutpnt, work, iwork, info);
        return null;
    }

}

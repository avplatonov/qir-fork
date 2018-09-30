package bpiwowar.maths.svd;

import bpiwowar.maths.matrix.DoubleMatrix1D;
import bpiwowar.maths.matrix.DoubleMatrix2D;

public class StandardBrokenArrowSVD implements BrokenArrowSVD {
    boolean wantU = true;
    boolean wantV = true;

    public StandardBrokenArrowSVD(boolean wantU, boolean wantV) {
        super();
        this.wantU = wantU;
        this.wantV = wantV;
    }

    public StandardBrokenArrowSVD() {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * bpiwowar.maths.svd.BrokenArrowSVD#computeSVD(cern.colt.matrix.DoubleMatrix2D
     * , cern.colt.matrix.DoubleMatrix1D, double, boolean)
     */
    @Override
    public Result computeSVD(DoubleMatrix2D D, DoubleMatrix1D z, int maxRank) {
        final int N = D.rows() + 1;
        if (N != z.size())
            throw new RuntimeException(String.format(
                "Sizes don't match (%d for D and %d and for z)", N, z
                    .size()));
        if (D.rows() != D.columns())
            throw new RuntimeException("D should be diagonal");

        // K = [D 0; z^T ] row by row, i.e. cell (i,j) is i * N + j
        final double[] K = new double[N * N];

        // Set the diagonal
        for (int i = N - 2; i >= 0; i--)
            K[i * (N + 1)] = D.get(i, i);

        // Set the last row
        for (int j = N - 1; j >= 0; j--) {
            K[(N - 1) * N + j] = z.get(j);
        }

        // Perform the SVD...
        SingularValueDecomposition svd = new SingularValueDecomposition(K, N,
            N, wantU, wantV);

        // ...and get the result
        Result r = new Result();

        final int rank = Math.min(svd.rank(), maxRank);
        r.S = svd.getS(rank);

        r.U = wantU ? svd.getU() : null;
        if (wantU && r.U.columns() > rank)
            r.U = r.U.viewPart(0, 0, r.U.rows(), rank);

        r.V = wantV ? svd.getV() : null;
        if (wantV && r.V.columns() > rank) {
            r.V = r.V.viewPart(0, 0, r.V.rows(), rank);
        }
        return r;
    }

    @Override
    public void setWantU(boolean wantU) {
        this.wantU = wantU;
    }

    @Override
    public void setWantV(boolean wantV) {
        this.wantV = wantV;
    }
}

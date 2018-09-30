package bpiwowar.maths.random;

import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.UpperTriangDenseMatrix;
import org.apache.commons.math.random.GaussianRandomGenerator;
import org.apache.commons.math.random.RandomGenerator;

/**
 * A multivariate gaussian generator
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class MultivariateGaussianGenerator {
    DenseVector mean;
    private UpperTriangDenseMatrix mA;
    private GaussianRandomGenerator gaussianGenerator;
    private int dimension;

    public MultivariateGaussianGenerator(DenseVector mean,
        Matrix covariance, RandomGenerator random) throws NotConvergedException {
        this.mean = mean;
        DenseCholesky factorize = DenseCholesky.factorize(covariance);

        if (!factorize.isSPD())
            throw new RuntimeException("Matrix was not SPD");

        mA = factorize.getU();

        gaussianGenerator = new GaussianRandomGenerator(random);
        dimension = mean.size();
    }

    public final DenseVector nextSample() {
        // Computes A x + mean
        DenseVector x = new DenseVector(dimension), y = new DenseVector(dimension);
        for (int k = dimension; --k >= 0; )
            x.set(k, gaussianGenerator.nextNormalizedDouble());

        mA.transMult(x, y);
        y.add(mean);
        return y;
    }

}

package bpiwowar.maths.ml;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SymmDenseEVD;
import no.uib.cipr.matrix.UpperSymmDenseMatrix;

/**
 * A Gaussian component from our mixture
 */
public class Gaussian {
    /**
     * Gaussian mean
     */
    DenseVector mean;

    /**
     * Co-variance matrix
     */
    UpperSymmDenseMatrix covariance;

    /**
     * Precision (computed if needed)
     */
    transient private DenseMatrix precision;

    /**
     * @return the covariance
     */
    public final UpperSymmDenseMatrix getCovariance() {
        return covariance;
    }

    /**
     * @param covariance the covariance to set
     */
    public final void setCovariance(UpperSymmDenseMatrix covariance) {
        this.covariance = covariance;
        this.precision = null;
    }

    /**
     * Initialise with matrices set to 0
     *
     * @param dimension The dimension of the multivariate gaussian
     */
    public Gaussian(int dimension) {
        mean = new DenseVector(dimension);
        covariance = new UpperSymmDenseMatrix(dimension);
    }

    /**
     * Get the density at the point x
     *
     * @param x The vector for which the density has to be computed
     * @return The probability density
     */
    public double getDensity(DenseVector x) {
        final int dimension = mean.size();
        assert x.size() == dimension;

        double a = Math.pow(2 * Math.PI, (double)dimension / 2.)
            * Math.sqrt(getDeterminant());

        x = x.copy();
        x.add(-1, this.mean);
        DenseVector multAdd = new DenseVector(dimension);
        if (precision == null) {
            DenseMatrix I = Matrices.identity(dimension);
            precision = I.copy();
            covariance.solve(I, precision);
        }
        precision.mult(1, x, multAdd);
        double dotProduct = multAdd.dot(x);

        double b = Math.exp(-0.5 * dotProduct);

        return b / a;
    }

    /**
     * Computes the determinant of the covariance matrix
     *
     * @return The determinant value
     */
    private double getDeterminant() {
        double determinant = 1;
        try {
            SymmDenseEVD sEVD = new SymmDenseEVD(mean.size(), true, false);
            SymmDenseEVD temp = sEVD.factor(covariance);
            double[] eigenvalues = temp.getEigenvalues();
            for (double d : eigenvalues)
                determinant *= d;
        }
        catch (NotConvergedException e) {
            throw new RuntimeException(e);
        }
        return determinant;
    }
}

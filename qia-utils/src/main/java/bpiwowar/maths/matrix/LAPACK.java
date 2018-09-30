package bpiwowar.maths.matrix;

import bpiwowar.argparser.utils.Output;
import bpiwowar.log.Logger;
import com.sun.jna.Native;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import java.nio.DoubleBuffer;
import java.util.ArrayList;

/**
 * Native LAPACK interface through JNA
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class LAPACK {
    final static private Logger LOGGER = Logger.getLogger();

    /**
     * The name of the property defining the library, if any
     */
    private static final String LIBRARY_NAME_LAPACK = "lapack";

    final static private Logger logger = Logger.getLogger();

    private static boolean registred = false;

    /**
     * Stores the list of library potential names
     */
    static ArrayList<String> libraryNames = new ArrayList<String>();

    static {
        // If the library.name.blas was defined, use this; otherwise, try atlas
        // and blas
        String name = System.getProperty(LIBRARY_NAME_LAPACK);
        if (name != null)
            libraryNames.add(name);
        else {
            libraryNames.add("lapack");
        }
    }

    private static boolean register(String names) {
        logger.info("Registering a LAPACK library");
        try {
            // Load pre-requesites
            String[] fields = names.split(":");
            if (fields.length > 1) {
                for (int i = 0; i < fields.length - 1; i++) {
                    logger.info("Loading library %s", fields[i]);
                    System.loadLibrary(fields[i]);
                }
            }

            // Now load the library
            final String name = fields[fields.length - 1];
            logger.info("Trying to register %s", name);
            Native.register(name);
            return true;
        }
        catch (UnsatisfiedLinkError e) {
            logger.info("Error while trying to register %s: %s", names, e);
        }
        catch (Throwable e) {
            logger.info("Error while trying to register %s: %s", names, e);
        }

        return false;
    }

    /**
     * Should be called before anything else! Note that library names can be changed through the system property
     *
     * @return
     */
    synchronized public static boolean register() {
        if (registred)
            return true;

        for (String name : libraryNames) {
            if (register(name)) {
                logger.info("Registred LAPACK library %s", name);
                registred = true;
                return true;
            }
        }

        logger.error(
            "Could not load the LAPACK library (nothing found among: %s)",
            Output.toString(", ", libraryNames));
        return false;
    }

    // --- Library methods ---

    /**
     * Computes the i<sup>th</sup> eigenvalue.
     *
     * @param n
     * @param i
     * @param d
     * @param z
     * @param delta
     * @param rho
     * @param di
     * @param info
     */
    public static native void dlaed4(IntByReference n, IntByReference i,
        double[] d, double[] z, double[] delta, DoubleByReference rho,
        DoubleByReference di, IntByReference info);

    /**
     * <p>
     * DLAED1 computes the updated eigensystem of a diagonal matrix after modification by a rank-one symmetric matrix.
     * This routine is used only for the eigenproblem which requires all eigenvalues and eigenvectors of a tridiagonal
     * matrix. DLAED7 handles the case in which eigenvalues only or eigenvalues and eigenvectors of a full symmetric
     * matrix (which was reduced to tridiagonal form) are desired.
     * </p>
     * <div>
     *
     * <pre>
     * T = Q(in) ( D(in) + RHO * Z*Z' ) Q'(in) = Q(out) * D(out) * Q'(out)
     * </pre>
     *
     * </div>
     * <p>
     * where Z = Q'u, u is a vector of length N with ones in the CUTPNT and CUTPNT + 1 th elements and zeros elsewhere.
     * </p>
     * <p>
     * The eigenvectors of the original matrix are stored in Q, and the eigenvalues are in D. The algorithm consists of
     * three stages:
     * </p>
     * <p>
     * The first stage consists of deflating the size of the problem when there are multiple eigenvalues or if there is
     * a zero in the Z vector. For each such occurence the dimension of the secular equation problem is reduced by one.
     * This stage is performed by the routine DLAED2.
     * </p>
     * <p>
     * The second stage consists of calculating the updated eigenvalues. This is done by finding the roots of the
     * secular equation via the routine DLAED4 (as called by DLAED3). This routine also calculates the eigenvectors of
     * the current problem.
     * </p>
     * <p>
     * The final stage consists of computing the updated eigenvectors directly using the updated eigenvalues. The
     * eigenvectors for the current problem are multiplied with the eigenvectors from the overall problem.
     * </p>
     *
     * @param n (input) INTEGER The dimension of the symmetric tridiagonal matrix. N >= 0.
     * @param d (input/output) DOUBLE PRECISION array, dimension (N) On entry, the eigenvalues of the rank-1-perturbed
     * matrix. On exit, the eigenvalues of the repaired matrix.
     * @param q (input/output) DOUBLE PRECISION array, dimension (LDQ,N) On entry, the eigenvectors of the
     * rank-1-perturbed matrix. On exit, the eigenvectors of the repaired tridiagonal matrix.
     * @param ldq (input) INTEGER The leading dimension of the array Q. LDQ >= max(1,N).
     * @param indxq (input/output) INTEGER array, dimension (N) On entry, the permutation which separately sorts the two
     * subproblems in D into ascending order. On exit, the permutation which will reintegrate the subproblems back into
     * sorted order, i.e. D( INDXQ( I = 1, N ) ) will be in ascending order.
     * @param rho (input) DOUBLE PRECISION The subdiagonal entry used to create the rank-1 modification.
     * @param cutpnt (input) INTEGER The location of the last eigenvalue in the leading sub-matrix. min(1,N) <= CUTPNT
     * <= N/2.
     * @param work (workspace) DOUBLE PRECISION array, dimension (4*N + N**2)
     * @param iwork (workspace) INTEGER array, dimension (4*N)
     * @param info (output) INTEGER = 0: successful exit. < 0: if INFO = -i, the i-th argument had an illegal value. >
     * 0: if INFO = 1, an eigenvalue did not converge
     */
    public native static void dlaed1(IntByReference n, DoubleBuffer d,
        DoubleBuffer q, IntByReference ldq, int[] indxq,
        DoubleByReference rho, IntByReference cutpnt, double[] work,
        int[] iwork, IntByReference info);

    /**
     * <p>
     * DLAED1 computes the updated eigensystem of a diagonal matrix after modification by a rank-one symmetric matrix.
     * This routine is used only for the eigenproblem which requires all eigenvalues and eigenvectors of a tridiagonal
     * matrix. DLAED7 handles the case in which eigenvalues only or eigenvalues and eigenvectors of a full symmetric
     * matrix (which was reduced to tridiagonal form) are desired.
     * </p>
     * <div>
     *
     * <pre>
     * T = Q(in) ( D(in) + RHO * Z*Z' ) Q'(in) = Q(out) * D(out) * Q'(out)
     * </pre>
     */
    public static void dlaed1(DenseDoubleMatrix2D mQ, DiagonalDoubleMatrix mD,
        double rho, DenseDoubleMatrix1D z) {
        int n = mD.columns();

        int[] indxq = new int[n];
        IntByReference _n = new IntByReference(n);
        IntByReference ldq = new IntByReference(mQ.rows());
        IntByReference cutpnt = new IntByReference(1);
        IntByReference info = new IntByReference();
        DoubleByReference _rho = new DoubleByReference(rho);

        dlaed1(_n, mD.getDiagonalDoubleBuffer(), mQ.getPointer(), ldq, indxq,
            _rho, cutpnt, new double[4 * n + n * n], new int[4 * n], info);

        LOGGER.info("DLAED returned info %d", info.getValue());
    }

    /*
    int dgesdd_(char *jobz, __CLPK_integer *m, __CLPK_integer *n, __CLPK_doublereal *
            a, __CLPK_integer *lda, __CLPK_doublereal *s, __CLPK_doublereal *u, __CLPK_integer *ldu,
            __CLPK_doublereal *vt, __CLPK_integer *ldvt, __CLPK_doublereal *work, __CLPK_integer *lwork,
            __CLPK_integer *iwork, __CLPK_integer *info) __OSX_AVAILABLE_STARTING(__MAC_10_2,__IPHONE_4_0);
     */
    public static native int dgesdd_(String jobz, IntByReference m,
        IntByReference n, double[] a, IntByReference lda, double[] s,
        double[] u, IntByReference ldu, double[] vt, IntByReference ldvt,
        double[] work, IntByReference lwork, int[] iwork,
        IntByReference info);

}

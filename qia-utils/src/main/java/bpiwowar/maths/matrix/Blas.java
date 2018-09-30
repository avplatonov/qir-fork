package bpiwowar.maths.matrix;

/*
 (from cblas)

 =================================================================================================
 Matrix shape and storage
 ========================
 Keeping the various matrix shape and storage parameters straight can be difficult.  The BLAS
 documentation generally makes a distinction between the concpetual "matrix" and the physical
 "array".  However there are a number of places where this becomes fuzzy because of the overall
 bias towards FORTRAN's column major storage.  The confusion is made worse by style differences
 between the level 2 and level 3 functions.  It is amplified further by the explicit choice of row
 or column major storage in the C interface.
 The storage order does not affect the actual computation that is performed.  That is, it does not
 affect the results other than where they appear in memory.  It does affect the values passed
 for so-called "leading dimension" parameters, such as lda in sgemv.  These are always the major
 stride in storage, allowing operations on rectangular subsets of larger matrices.  For row major
 storage this is the number of columns in the parent matrix, and for column major storage this is
 the number of rows in the parent matrix.
 For the level 2 functions, which deal with only a single matrix, the matrix shape parameters are
 always M and N.  These are the logical shape of the matrix, M rows by N columns.  The transpose
 parameter, such as transA in sgemv, defines whether the regular matrix or its transpose is used
 in the operation.  This affects the implicit length of the input and output vectors.  For example,
 if the regular matrix A is used in sgemv, the input vector X has length N, the number of columns
 of A, and the output vector Y has length M, the number of rows of A.  The length of the input and
 output vectors is not affected by the storage order of the matrix.
 The level 3 functions deal with 2 input matrices and one output matrix, the matrix shape parameters
 are M, N, and K.  The logical shape of the output matrix is always M by N, while K is the common
 dimension of the input matrices.  Like level 2, the transpose parameters, such as transA and transB
 in sgemm, define whether the regular input or its transpose is used in the operation.  However
 unlike level 2, in level 3 the transpose parameters affect the implicit shape of the input matrix.
 Consider sgemm, which computes "C = (alpha * A * B) + (beta * C)", where A and B might be regular
 or transposed.  The logical shape of C is always M rows by N columns.  The physical shape depends
 on the storage order parameter.  Using column major storage the declaration of C (the array) in C
 (the language) would be something like "float C[N][M]".  The logical shape of A without transposition
 is M by K, and B is K by N.  The one storage order parameter affects all three matrices.
 For those readers still wondering about the style differences between level 2 and level 3, they
 involve whether the input or output shapes are explicit.  For level 2, the input matrix shape is
 always M by N.  The input and output vector lengths are implicit and vary according to the
 transpose parameter.  For level 3, the output matrix shape is always M by N.  The input matrix
 shapes are implicit and vary according to the transpose parameters.
 =================================================================================================
 */

import bpiwowar.log.Logger;
import bpiwowar.utils.Output;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Native BLAS interface through JNA
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class Blas {
    /**
     * The name of the property defining the library, if any
     */
    private static final String LIBRARY_NAME_BLAS = "library.name.blas";

    final static private Logger logger = Logger.getLogger();
    private static boolean registred = false;

    /**
     * Stores the list of library potential names
     */
    static ArrayList<String> libraryNames = new ArrayList<String>();

    static {
        // If the library.name.blas was defined, use this; otherwise, try atlas
        // and blas
        String name = System.getProperty(LIBRARY_NAME_BLAS);
        if (name != null)
            libraryNames.add(name);
        else
            libraryNames.add("blas");
    }

    private static boolean register(String names) {
        logger.info("Registering a BLAS library");
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
        catch (Throwable e) {
            logger.debug("Error while trying to register %s: %s", names, e);
            return false;
        }
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
                logger.info("Registred BLAS library %s", name);
                registred = true;
                return true;
            }
        }

        logger.error(
            "Could not load the BLAS library (nothing found among: %s)",
            Output.toString(", ", libraryNames));
        return false;
    }

    // --- Library methods ---

    /**
     * <i>native declaration : include/cblas.h:5</i><br>
     * enum values
     */
    public static interface CBLAS_ORDER {
        public static final int CblasRowMajor = 101;
        public static final int CblasColMajor = 102;
    }

    /**
     * <i>native declaration : include/cblas.h:6</i><br>
     * enum values
     */
    public static interface CBLAS_TRANSPOSE {
        public static final int CblasNoTrans = 111;
        public static final int CblasTrans = 112;
        public static final int CblasConjTrans = 113;
        public static final int AtlasConj = 114;
    }

    // ---- Dense double matrix-vector multiplication

    /**
     * The _GEMV subprograms compute a matrix-vector product for either a general matrix or its transpose: y =
     * alpha*op(A) x + beta*y where op is either the identity or transpose operation.
     *
     * @param order The order of the matrix {@see CBLAS_ORDER}
     * @param transposeA If A has to be transposed {@see CBLAS_TRANSPOSE}
     * @param m The number of rows of matrix A
     * @param n The number of columns of the matrix A
     * @param alpha
     * @param A
     * @param lda
     * @param X
     * @param incX The stride for matrix A
     * @param beta
     * @param Y
     * @param incY The stride for vector y
     */

    private native static void cblas_dgemv(int order, int transposeA, int m,
        int n, double alpha, DoubleBuffer A, int lda, DoubleBuffer X,
        int incX, double beta, DoubleBuffer Y, int incY);

    static void dgemv(int order, int transposeA, int m, int n, double alpha,
        DoubleBuffer A, int lda, DoubleBuffer X, int incX, double beta,
        DoubleBuffer Y, int incY) {
        cblas_dgemv(order, transposeA, m, n, alpha, A, lda, X, incX, beta, Y,
            incY);
    }

    native static void cblas_dgemm(int order, int TransA, int TransB, int M,
        int N, int K, double alpha, DoubleBuffer A, int lda,
        DoubleBuffer B, int ldb, double beta, DoubleBuffer C, int ldc);

    native static void cblas_dgemm(int order, int TransA, int TransB, int M,
        int N, int K, double alpha, Pointer A, int lda, Pointer B, int ldb,
        double beta, Pointer C, int ldc);

    public native static void cblas_sgemm(int order, int TransA, int TransB,
        int M, int N, int K, float alpha, float[] a, int lda, Pointer B,
        int ldb, float beta, float[] c, int ldc);

    public native static void cblas_sgemm(int order, int TransA, int TransB,
        int M, int N, int K, float alpha, float[] A, int lda, float[] B,
        int ldb, float beta, float[] C, int ldc);

    public native static void cblas_sgemm(int cblasrowmajor, int cblasnotrans,
        int cblasnotrans2, int m, int n, int k, float alpha,
        FloatBuffer wrap, int lda, FloatBuffer wrap2, int ldb, float beta,
        FloatBuffer wrap3, int ldc);

    /**
     * perform a rank-one update of a real general matrix: A = alpha*x*transp(y) + A
     *
     * @param m The number of rows of the matrix A
     * @param n The number of columns of the matrix A;
     * @param alpha The scalar alpha
     * @param x a one-dimensional array X of length at least (1+(m-1)*|incx|)
     * @param incx The increment for the elements of X
     * @param y A one-dimensional array of length at least (1+(n-1)*|incy|)
     * @param incy The increment for the elements of Y; incy must not equal zero
     * @param A a two-dimensional array with dimensions lda by n
     * @param lda the first dimension of A
     */
    public native static void cblas_dger(int order, int m, int n, double alpha,
        DoubleBuffer x, int incx, DoubleBuffer y, int incy, DoubleBuffer A,
        int lda);

    public native static void cblas_dger(int order, int m, int n, double alpha,
        double[] x, int incx, double[] y, int incy, double[] A, int lda);

    /**
     * Inner product of two vectors with doubles
     *
     * @param size
     * @param x
     * @param strideX
     * @param y
     * @param strideY
     * @return
     */
    public native static double cblas_ddot(int size, DoubleBuffer x,
        int strideX, DoubleBuffer y, int strideY);

}

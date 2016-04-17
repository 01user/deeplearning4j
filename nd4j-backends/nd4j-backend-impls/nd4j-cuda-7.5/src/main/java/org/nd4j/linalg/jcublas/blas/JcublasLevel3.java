package org.nd4j.linalg.jcublas.blas;


import jcuda.jcublas.JCublas2;
import jcuda.jcublas.cublasHandle;
import org.nd4j.jita.allocator.Allocator;
import org.nd4j.jita.allocator.impl.AtomicAllocator;
import org.nd4j.linalg.api.blas.impl.BaseLevel3;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.DataTypeValidation;
import org.nd4j.linalg.jcublas.CublasPointer;
import org.nd4j.linalg.jcublas.context.CudaContext;
import org.nd4j.nativeblas.Nd4jBlas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Level 3 implementation of matrix matrix operations
 *
 * @author Adam Gibson
 */
public class JcublasLevel3 extends BaseLevel3 {
    private Allocator allocator = AtomicAllocator.getInstance();
    private Nd4jBlas nd4jBlas = new Nd4jBlas();
    private static Logger log = LoggerFactory.getLogger(JcublasLevel3.class);

    @Override
    protected void sgemm(char Order, char TransA, char TransB, int M, int N, int K, float alpha, INDArray A, int lda, INDArray B, int ldb, float beta, INDArray C, int ldc) {
        A = Shape.toOffsetZero(A);
        B = Shape.toOffsetZero(B);
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer cAPointer = new CublasPointer(A, ctx);
        CublasPointer cBPointer = new CublasPointer(B, ctx);
        CublasPointer cCPointer = new CublasPointer(C, ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.sgemm(
                    new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    TransA,
                    TransB,
                    M,
                    N,
                    K,
                    alpha,
                    cAPointer.getDevicePointer().getNativePointer(),
                    lda,
                    cBPointer.getDevicePointer().getNativePointer(),
                    ldb,
                    beta,
                    cCPointer.getDevicePointer().getNativePointer(),
                    ldc);
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void ssymm(char Order, char Side, char Uplo, int M, int N, float alpha, INDArray A, int lda, INDArray B, int ldb, float beta, INDArray C, int ldc) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A, ctx);
        CublasPointer bPointer = new CublasPointer(B, ctx);
        CublasPointer cPointer = new CublasPointer(C, ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.ssymm(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Side,
                    Uplo,
                    M, N,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda, bPointer.getDevicePointer().getNativePointer(),
                    ldb,
                    beta,
                    cPointer.getDevicePointer().getNativePointer(),
                    ldc);
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void ssyrk(char Order, char Uplo, char Trans, int N, int K, float alpha, INDArray A, int lda, float beta, INDArray C, int ldc) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer cPointer = new CublasPointer(C,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.ssyrk(new long[]{ctx.getHandle().getNativePointer()}, Order, Uplo, Trans, N, K, alpha, aPointer.getDevicePointer().getNativePointer(), lda, beta, cPointer.getDevicePointer().getNativePointer(), ldc);
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void ssyr2k(char Order, char Uplo, char Trans, int N, int K, float alpha, INDArray A, int lda, INDArray B, int ldb, float beta, INDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void strmm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, float alpha, INDArray A, int lda, INDArray B, int ldb) {
        throw new UnsupportedOperationException();}

    @Override
    protected void strsm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, float alpha, INDArray A, int lda, INDArray B, int ldb) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer bPointer = new CublasPointer(B,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.strsm(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Side,
                    Uplo,
                    TransA,
                    Diag,
                    M,
                    N,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda,
                    bPointer.getDevicePointer().getNativePointer(),
                    ldb);
        }

        allocator.tickDeviceWrite(B);
    }

    @Override
    protected void dgemm(char Order, char TransA, char TransB, int M, int N, int K, double alpha, INDArray A, int lda, INDArray B, int ldb, double beta, INDArray C, int ldc) {
        A = Shape.toOffsetZero(A);
        B = Shape.toOffsetZero(B);
        CudaContext ctx = CudaContext.getBlasContext();

        DataTypeValidation.assertDouble(A, B, C);

        CublasPointer cAPointer = new CublasPointer(A,ctx);
        CublasPointer cBPointer = new CublasPointer(B,ctx);
        CublasPointer cCPointer = new CublasPointer(C,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.dgemm(new long[]{ctx.getHandle().getNativePointer()}, Order, TransA, TransB, M, N, K, alpha, cAPointer.getDevicePointer().getNativePointer(), lda, cBPointer.getDevicePointer().getNativePointer(), ldb, beta, cCPointer.getDevicePointer().getNativePointer(), ldc);
            ctx.syncOldStream();
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void dsymm(char Order, char Side, char Uplo, int M, int N, double alpha, INDArray A, int lda, INDArray B, int ldb, double beta, INDArray C, int ldc) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer bPointer = new CublasPointer(B,ctx);
        CublasPointer cPointer = new CublasPointer(C,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.dsymm(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Side,
                    Uplo,
                    M,
                    N,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda,
                    bPointer.getDevicePointer().getNativePointer(),
                    ldb,
                    beta,
                    cPointer.getDevicePointer().getNativePointer(),
                    ldc);
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void dsyrk(char Order, char Uplo, char Trans, int N, int K, double alpha, INDArray A, int lda, double beta, INDArray C, int ldc) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer cPointer = new CublasPointer(C,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.dsyrk(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Uplo,
                    Trans,
                    N,
                    K,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda,
                    beta, cPointer.getDevicePointer().getNativePointer(),
                    ldc);
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void dsyr2k(char Order, char Uplo, char Trans, int N, int K, double alpha, INDArray A, int lda, INDArray B, int ldb, double beta, INDArray C, int ldc) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer bPointer = new CublasPointer(B,ctx);
        CublasPointer cPointer = new CublasPointer(C,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.dsyr2k(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Uplo,
                    Trans,
                    N,
                    K,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda,
                    bPointer.getDevicePointer().getNativePointer(),
                    ldb,
                    beta,
                    cPointer.getDevicePointer().getNativePointer(),
                    ldc);
        }

        allocator.tickDeviceWrite(C);
    }

    @Override
    protected void dtrmm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, double alpha, INDArray A, int lda, INDArray B, int ldb) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer bPointer = new CublasPointer(B,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.dtrmm(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Side,
                    Uplo,
                    TransA,
                    Diag,
                    M,
                    N,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda,
                    bPointer.getDevicePointer().getNativePointer(),
                    ldb);
        }

        allocator.tickDeviceWrite(B);
    }

    @Override
    protected void dtrsm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, double alpha, INDArray A, int lda, INDArray B, int ldb) {
        CudaContext ctx = CudaContext.getBlasContext();

        CublasPointer aPointer = new CublasPointer(A,ctx);
        CublasPointer bPointer = new CublasPointer(B,ctx);

        cublasHandle handle = ctx.getHandle();
        synchronized (handle) {
            JCublas2.cublasSetStream(handle, ctx.getOldStream());

            nd4jBlas.dtrsm(new long[]{ctx.getHandle().getNativePointer()},
                    Order,
                    Side,
                    Uplo,
                    TransA,
                    Diag,
                    M,
                    N,
                    alpha,
                    aPointer.getDevicePointer().getNativePointer(),
                    lda,
                    bPointer.getDevicePointer().getNativePointer(),
                    ldb);
        }

        allocator.tickDeviceWrite(B);
    }

    @Override
    protected void cgemm(char Order, char TransA, char TransB, int M, int N, int K, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void csymm(char Order, char Side, char Uplo, int M, int N, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void csyrk(char Order, char Uplo, char Trans, int N, int K, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void csyr2k(char Order, char Uplo, char Trans, int N, int K, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void ctrmm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void ctrsm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb) {

    }

    @Override
    protected void zgemm(char Order, char TransA, char TransB, int M, int N, int K, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zsymm(char Order, char Side, char Uplo, int M, int N, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zsyrk(char Order, char Uplo, char Trans, int N, int K, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();


    }

    @Override
    protected void zsyr2k(char Order, char Uplo, char Trans, int N, int K, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void ztrmm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();


    }

    @Override
    protected void ztrsm(char Order, char Side, char Uplo, char TransA, char Diag, int M, int N, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void chemm(char Order, char Side, char Uplo, int M, int N, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void cherk(char Order, char Uplo, char Trans, int N, int K, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();


    }

    @Override
    protected void cher2k(char Order, char Uplo, char Trans, int N, int K, IComplexFloat alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexFloat beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();



    }

    @Override
    protected void zhemm(char Order, char Side, char Uplo, int M, int N, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();


    }

    @Override
    protected void zherk(char Order, char Uplo, char Trans, int N, int K, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void zher2k(char Order, char Uplo, char Trans, int N, int K, IComplexDouble alpha, IComplexNDArray A, int lda, IComplexNDArray B, int ldb, IComplexDouble beta, IComplexNDArray C, int ldc) {
        throw new UnsupportedOperationException();


    }
}

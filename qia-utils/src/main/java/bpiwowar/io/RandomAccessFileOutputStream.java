package bpiwowar.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * This will write to a RandomAccessFile in the filesystem and keep track of the position it is writing to and the
 * length of the stream.
 *
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * @version $Revision: 1.6 $
 */
public class RandomAccessFileOutputStream extends OutputStream {
    private RandomAccessFile file;

    /**
     * Constructor to create an output stream that will write to the end of a random access file.
     *
     * @param raf The file to write to.
     * @throws IOException If there is a problem accessing the raf.
     */
    public RandomAccessFileOutputStream(RandomAccessFile raf)
        throws IOException {
        file = raf;
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b, int offset, int length) throws IOException {
        file.write(b, offset, length);

    }

    /**
     * {@inheritDoc}
     */
    public void write(int b) throws IOException {
        file.write(b);
    }

}

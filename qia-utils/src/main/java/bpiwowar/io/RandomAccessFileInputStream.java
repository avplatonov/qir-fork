package bpiwowar.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * This will read from a RandomAccessFile without keeping track of the position
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class RandomAccessFileInputStream extends InputStream {
    private RandomAccessFile file;

    /**
     * Constructor to create an input stream that will write to the end of a random access file.
     *
     * @param raf The file to write to.
     * @throws IOException If there is a problem accessing the raf.
     */
    public RandomAccessFileInputStream(RandomAccessFile raf)
        throws IOException {
        file = raf;
    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        return file.read(b, offset, length);

    }

    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return file.read();
    }

}

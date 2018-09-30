package bpiwowar.utils;

import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * A series of memory blocks
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class PagedByteArray {
    private final static Logger logger = Logger.getLogger(PagedByteArray.class);

    /**
     * The default block size (1 ko)
     */
    int blockSize = 1024;

    /**
     * The current size of the byte array
     */
    int size;

    /**
     * Our blocks
     */
    private ArrayList<byte[]> blocks = new ArrayList<byte[]>();

    /**
     * Default initialisation
     */
    public PagedByteArray() {
    }

    public int size() {
        return size;
    }

    /**
     * Initialise with a set block size
     *
     * @param blockSize the block size (in bytes)
     */
    public PagedByteArray(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * Write a part of of a byte array in memory
     *
     * @param idx Where should the data be written in the byte array
     * @param src The array from which the data is copied
     * @param srcIdx The position within the array given in parameters
     * @param size The number of bytes that should be copied
     */
    public void write(int idx, byte[] src, int srcIdx, int size) {
        // --- Check if we need to allocate more memory
        int left = blocks.size() * blockSize - (size + idx);
        logger.debug(String.format("%d bytes are left in this paged byte array", left));
        if (left < 0) {
            int needed = (-left - 1) / blockSize + 1;
            while (needed > 0) {
                needed--;
                blocks.add(new byte[blockSize]);
            }
        }

        // Update the array length
        this.size = Math.max(this.size, idx + size);

        // --- Write
        int blockNo = idx / blockSize;
        int inBlockFrom = idx % blockSize;
        while (size > 0) {
            int partLength = Math.min(size, blockSize - inBlockFrom);
            System.arraycopy(src, srcIdx, blocks.get(blockNo), inBlockFrom,
                partLength);
            inBlockFrom = 0;
            srcIdx += partLength;
            size -= partLength;
            blockNo++;
        }

    }

    /**
     * Get a single byte
     *
     * @param position
     * @return
     */
    public byte get(int position) {
        if (position >= this.size)
            throw new IndexOutOfBoundsException(String.format(
                "Get byte %d in an array of size %d", position, this.size));
        return blocks.get(position / blockSize)[(position % blockSize)];
    }

    /**
     * Get some bytes from the array
     *
     * @param idx The index within the byte array
     * @param dest
     * @param destIdx
     * @param size
     */
    public void get(int idx, byte[] dest, int destIdx, int size) {
        // Check bounds
        if (idx + size > this.size)
            throw new IndexOutOfBoundsException(
                String
                    .format(
                        "Asking for %d bytes from position %d in an array of size %d",
                        size, idx, this.size));

        // Read
        int blockNo = idx / blockSize;
        int inBlockFrom = idx % blockSize;
        while (size > 0) {
            int partLength = Math.min(size, blockSize - inBlockFrom);
            System.arraycopy(blocks.get(blockNo), inBlockFrom, dest, destIdx,
                partLength);
            blockNo++;
            inBlockFrom = 0;
            destIdx += partLength;
            size -= partLength;
        }
    }

    /**
     * Get a sub-array
     *
     * @param position
     * @param size
     * @return The subarrray
     */
    public byte[] get(int position, int size) {
        byte[] array = new byte[size];
        get(position, array, 0, size);
        return array;
    }

}

package bpiwowar.collections;

import bpiwowar.io.ByteSerializable;
import bpiwowar.io.ByteSerializer;
import bpiwowar.utils.PagedByteArray;
import java.io.IOException;
import java.util.AbstractList;

/**
 * An array which is represented by a block of memory. The elements should be byte serializable
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class PagedArray<E extends ByteSerializable> extends AbstractList<E> {
    /**
     * Internal representation
     */
    private PagedByteArray bytes;
    /**
     * The number
     */
    private final ByteSerializer<E> serializer;
    /**
     * The number of bytes per item
     */
    private final int itemSize;

    /**
     * Paged array
     *
     * @param serializer The byte serializer for this type of objects
     */
    public PagedArray(ByteSerializer<E> serializer) {
        this.serializer = serializer;
        bytes = new PagedByteArray();
        itemSize = serializer.size();
    }

    /**
     * Paged array initialisation
     *
     * @param serializer The byte serializer for this type of objects
     * @param blockSize The block size
     */
    public PagedArray(ByteSerializer<E> serializer, int blockSize) {
        this.serializer = serializer;
        bytes = new PagedByteArray(blockSize);
        itemSize = serializer.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractList#get(int)
     */
    @Override
    public E get(int index) {
        try {
            return serializer
                .deserialize(bytes.get(itemSize * index, itemSize));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.AbstractCollection#size()
     */
    @Override
    public int size() {
        return serializer.size() / itemSize;
    }

}

package bpiwowar.io;

public interface Bindable {
    /** Serialise the object as an array of bytes */
    byte[] toBytes();

    /**
     * Deserialise the object
     */
    void construct(final byte[] data);

}

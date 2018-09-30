package bpiwowar.io;

import java.io.IOException;

/**
 * Fixed width byte serializable objects
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public interface ByteSerializable {
    public byte[] serialize() throws IOException;

    public void deserialize(byte[] data) throws IOException;

    int size();
}

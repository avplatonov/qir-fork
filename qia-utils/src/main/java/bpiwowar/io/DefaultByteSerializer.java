package bpiwowar.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Utility to transform a serializable to a byte array and vice-versa
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class DefaultByteSerializer {
    /**
     * Return a byte array corresponding to the serialization of the object given in the parameters
     *
     * @param o The object to serialize
     * @param size The initial buffer size
     * @return A byte array
     * @throws IOException
     */
    static public byte[] serialize(Object o, int size) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(size);
        ObjectOutputStream out = new ObjectOutputStream(buffer);
        out.writeObject(o);
        out.close();
        return buffer.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static public <T> T deserialize(byte[] data) throws IOException,
        ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(
            data));
        return (T)in.readObject();
    }

}

package bpiwowar.io;

import java.io.IOException;

public interface ByteSerializer<E> {
    int size();

    public byte[] serialize(E e) throws IOException;

    public E deserialize(byte[] data) throws IOException;
}

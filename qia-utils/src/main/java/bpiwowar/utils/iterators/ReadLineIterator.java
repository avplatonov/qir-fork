package bpiwowar.utils.iterators;

import bpiwowar.pipe.CloseableIterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

public class ReadLineIterator implements Iterable<String>,
    CloseableIterator<String> {

    private BufferedReader reader;
    private boolean used = false;
    String current = null;

    public ReadLineIterator(final BufferedReader reader) {
        this.reader = reader;
        next();
    }

    public ReadLineIterator(Reader in) {
        this(in instanceof BufferedReader ? (BufferedReader)in : new BufferedReader(in));
    }

    public ReadLineIterator(InputStream in) {
        this(new BufferedReader(new InputStreamReader(in)));
    }

    public ReadLineIterator(File file) throws FileNotFoundException {
        this(new FileInputStream(file));
    }

    public boolean hasNext() {
        return current != null;
    }

    public String next() {
        final String s = current;
        try {
            current = reader.readLine();
        }
        catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return s;
    }

    public void remove() {
        throw new RuntimeException("Read only iterator");
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<String> iterator() {
        if (used)
            throw new RuntimeException("Cannot iterate two times over a stream");
        used = false;
        return this;
    }

    public void close() {
        try {
            reader.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ReadLineIterator iterable(File file) throws FileNotFoundException {
        return new ReadLineIterator(file);
    }

}

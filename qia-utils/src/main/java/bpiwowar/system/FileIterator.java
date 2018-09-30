/**
 *
 */
package bpiwowar.system;

import bpiwowar.pipe.BufferedProducer;
import bpiwowar.utils.Pair;
import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;
import org.apache.log4j.Logger;

/**
 * Parse a directory searching for files
 *
 * @author Benjamin Piwowarski
 */
public class FileIterator extends BufferedProducer<File> {
    final static Logger logger = Logger.getLogger(FileIterator.class);

    /**
     * The filter used to decide if a file should be output or not
     */
    private FileFilter filter;

    /**
     * Holds the current directory list to parse
     */
    private final LinkedList<Pair<File, Integer>> directoryStack = new LinkedList<Pair<File, Integer>>();

    /**
     * Current depth (used by the filter to process directories)
     */
    private int currentDepth;

    /**
     * Constructs a new file iterator
     *
     * @param directory The base directory (a string)
     * @param recursive Depth of the recursion (-1 for infinity)
     * @param filefilter
     */
    public FileIterator(final String directory, final int depth,
        final FileFilter filefilter) {
        this(new File(directory), depth, filefilter);
    }

    /**
     * Constructs a new file iterator
     *
     * @param directory The base directory
     * @param depth The depth of the recursion (-1 infinite, 0 no recursion)
     * @param filefilter The file filter (null if none)
     */
    public FileIterator(final File directory, int depth,
        final FileFilter filefilter) {
        super(true);

        // Creates the file filter
        this.filter = new FileFilter() {
            public boolean accept(File file) {
                // Add this directory if we are in recursive mode
                if ((currentDepth != 0) && file.isDirectory()) {
                    directoryStack.add(new Pair<File, Integer>(file,
                        new Integer(currentDepth >= 0 ? currentDepth - 1
                            : -1)));
                }

                // Add the file if it matches
                if (filefilter == null || filefilter.accept(file))
                    try {
                        produce(file);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                return false;
            }

        };

        // add the base directory to start the process
        directoryStack.add(new Pair<File, Integer>(directory, depth));

        synchronized (this) {
            wait = false;
            notifyAll();
        }
    }

    @Override
    public void produce() throws InterruptedException {
        while (!directoryStack.isEmpty()) {
            // Get the next directory to explore
            final Pair<File, Integer> current = directoryStack.getFirst();

            // Remove it from the stack
            directoryStack.removeFirst();

            // Get the files from this directory
            currentDepth = current.getSecond();
            try {
                current.getFirst().listFiles(filter);
            }
            catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause != null && cause instanceof InterruptedException) {
                    throw (InterruptedException)cause;
                }
                throw e;
            }
        }
    }

    /** Test */
    static public void main(final String[] args) {
        if (args.length != 3 && args.length != 2) {
            System.err.println("FileIterator <directory> <ext> <skipRegExp>");
            System.exit(1);
        }
        final Iterator<File> i = new FileIterator(args[0], -1,
            FileSystem.newRegexpFileFilter(args[1],
                args.length == 3 ? args[2] : null));
        while (i.hasNext()) {
            final Object x = i.next();
            System.out.println(x);
        }
    }

}

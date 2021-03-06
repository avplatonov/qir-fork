package bpiwowar.system;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

/**
 * Static methods for files
 *
 * @author B. Piwowarski
 */
public class FileSystem {
    private static final Logger logger = Logger.getLogger(FileSystem.class);

    /**
     * A filter for directories
     */
    final public static FileFilter DIRECTORY_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    /**
     * A filter for files
     */
    final public static FileFilter FILE_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isFile();
        }
    };

    /**
     * Returns the number of files in a given directory
     *
     * @param dirOrFilenames name of the directory to explore
     * @param getFilesInSubDirs true if recursive process
     * @param extFilter The extension of the matching files
     * @return a number of files
     */
    public static int getNumberOfFiles(final String dirOrFilenames,
        final boolean getFilesInSubDirs, final String extFilter) {
        final FileIterator iterator = new FileIterator(dirOrFilenames,
            getFilesInSubDirs ? -1 : 0,
            newRegexpFileFilter(extFilter, null));
        int n = 0;
        while (iterator.hasNext()) {
            iterator.next();
            n++;
        }
        return n;
    }

    /**
     * Get a file filter
     *
     * @param extFilter
     * @param skipRegExp
     * @return
     */
    public static FileFilter newRegexpFileFilter(final String extFilter,
        final String skipRegExp) {
        return new FileFilter() {
            final Pattern pattern = skipRegExp != null ? Pattern
                .compile(skipRegExp) : null;

            public boolean accept(File file) {
                return (file.getName().endsWith(extFilter) && (pattern == null || !pattern
                    .matcher(file.getName()).find()));
            }
        };
    }

    /**
     * Create a new file object from a list of names
     *
     * @param names A list of strings
     */
    public static File createFileFromPath(String... names) {
        return createFileFromPath(null, names);
    }

    /**
     * Creates a file from a list of strings and a base directory
     *
     * @param baseDirectory
     * @param names
     * @return
     */
    public static File createFileFromPath(File baseDirectory, String... names) {
        for (String name : names)
            if (baseDirectory == null)
                baseDirectory = new File(name);
            else
                baseDirectory = new File(baseDirectory, name);
        return baseDirectory;
    }

    /**
     * Delete everything recursively
     *
     * @param path
     */
    static public void recursiveDelete(File path) {

        logger.debug("Deleting " + path);
        for (File entry : path.listFiles()) {
            logger.debug("Considering " + entry);

            if (entry.isDirectory())
                recursiveDelete(entry);
            else {
                if (!entry.delete())
                    logger.warn("Could not delete file " + entry);
                else
                    logger.debug("Deleted file " + entry);
            }

        }

        // Deleting self
        if (!path.delete())
            logger.warn("Could not delete " + path);
        else
            logger.debug("Deleted " + path);

    }

}

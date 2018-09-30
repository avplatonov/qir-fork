package bpiwowar.db;

import bpiwowar.io.LoggerPrintStream;
import bpiwowar.io.RandomAccessFileInputStream;
import bpiwowar.io.RandomAccessFileOutputStream;
import bpiwowar.log.Logger;
import bpiwowar.log.TaskTimer;
import bpiwowar.pipe.CloseableIterator;
import bpiwowar.system.FileIterator;
import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.iterators.AbstractIterator;
import bpiwowar.utils.iterators.SimpleEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseNotFoundException;
import com.sleepycat.je.Environment;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.IncompatibleClassException;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PrimaryKey;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.log4j.Level;

/**
 * A storage database for big objects, where objects are serialised in a series of files. The index is managed by the
 * Berkeley database
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class BatchFileStorage<K, W extends bpiwowar.db.BatchFileStorage.Location<K>, V>
    implements Iterable<Entry<K, V>> {

    private static final Logger logger = Logger.getLogger();

    /**
     * Size limit (1.5GB)
     */
    long sizeLimit = 1024l * 1024l * 1024l * 3l / 2;

    /**
     * Home file for keyword storage
     */
    final private File home;

    /**
     * Locations of the stored objects
     */
    private PrimaryIndex<K, W> locations;

    // Our constructor
    private Constructor<W> constructor;

    /**
     * The entity store for locations
     */
    protected EntityStore locationStore;

    /**
     * Current batch index
     */
    private int currentBatchIndex;

    /**
     * Our database name
     */
    private final String dbname;

    /**
     * Should the objects be gzipped
     */
    final boolean compressed;

    private Class<K> keyClass;

    private final Class<W> entityClass;

    private boolean readOnly;

    /**
     * Creates a new batch file storage
     *
     * @param dbenv The database environment to store the locations
     * @param dbname The database name for locations
     * @param home The home directory for files (default to {@link Environment#getHome()})
     * @param compress If objects should be compressed when stored and uncompressed when loaded
     * @param destroy
     * @throws IncompatibleClassException
     * @throws DatabaseException
     */
    public BatchFileStorage(Environment dbenv, final String dbname, File home,
        Class<W> entityClass, final boolean compress, final boolean destroy)
        throws IncompatibleClassException, DatabaseException {
        // Open the database
        this.dbname = dbname;
        this.entityClass = entityClass;
        this.compressed = compress;

        this.home = (home = home != null ? home : dbenv.getHome());
        logger.info("Home directory is %s", this.home);

        namePattern = Pattern.compile("^" + dbname + "-(\\d+).obj$");

        // --- Destroy if needed
        if (destroy) {
            String prefix = "persist#" + dbname + '#';
            for (Object oName : dbenv.getDatabaseNames()) {
                String name = (String)oName;
                if (name.startsWith(prefix)) {
                    try {
                        logger.warn("Destroying database %s", dbname);
                        dbenv.removeDatabase(null, name);
                    }
                    catch (DatabaseNotFoundException e) {
                    }
                }
            }

            dbenv.sync();
        }

        int maxBatch = -1;
        int nbBatch = 0;
        FileIterator fileIterator = new FileIterator(home, 0,
            new RegexFileFilter(namePattern));
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            if (destroy) {
                logger.info("Deleting batch file %s", file);
                file.delete();
            }
            else {
                nbBatch++;
                Matcher matcher = namePattern.matcher(file.getName());
                matcher.find();
                maxBatch = Math
                    .max(Integer.valueOf(matcher.group(1)), maxBatch);
            }
        }

        // --- Our current batch is by default the next one
        currentBatchIndex = maxBatch;
        if (!destroy)
            logger
                .info(
                    "%d batch file(s) were already created [max=%d], next batch index is %d",
                    nbBatch, maxBatch, getCurrentBatchIndex() + 1);

        // Statistics
        StoreConfig storeConfig = new StoreConfig();
        this.readOnly = dbenv.getConfig().getReadOnly();
        if (dbenv.getConfig().getReadOnly()) {
            storeConfig.setReadOnly(true);
        }
        else {
            storeConfig.setDeferredWrite(true);
            storeConfig.setAllowCreate(true);
        }

        // Open the keyword location db
        locationStore = new EntityStore(dbenv, dbname, storeConfig);

        Type[] types = GenericHelper.getActualTypeArguments(entityClass,
            Location.class);
        keyClass = (Class<K>)types[0];
        try {
            constructor = entityClass.getConstructor(keyClass);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        locations = locationStore.getPrimaryIndex(keyClass, entityClass);
        // types = GenericHelper.getActualTypeArguments(this.getClass(),
        // BatchFileStorage.class);
        logger.info("Initialised batch file storage \"%s\" with %d entries",
            dbname, locations.count());
    }

    /**
     * Returns the home directory for object storage
     */
    public File getHome() {
        return home;
    }

    @Override
    public String toString() {
        return String.format("Database %s to %s (%s)", keyClass, entityClass,
            home);
    }

    boolean closed = false;

    /**
     * Close the underlying resources
     */
    public void close() throws Throwable {
        if (!closed) {
            closed = true;
            closeOpenedBatches(true);
            locationStore.close();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * Rewrite everything in the last batch
     *
     * @param threshold The minimum ratio for a batch file to be kept (1 means that files should be completly dense)
     * @throws DatabaseException
     * @throws IOException
     */
    public void cleanup(TaskTimer timer, double threshold)
        throws DatabaseException, IOException {
        // Do not touch the database if read-only
        if (readOnly) {
            logger.warn("Cannot cleanup a read-only database");
            return;
        }

        // OK, let's go

        BatchInformation currentInfo = getBatchInformation(getCurrentBatchIndex());
        if (currentInfo.statistics.getUsedPercentage() < threshold) {
            logger.info("Creating a new batch for cleanup");
            nextBatch();
            currentInfo = getBatchInformation(getCurrentBatchIndex());
        }

        File batchFile = getBatchFile(getCurrentBatchIndex());
        logger.info("Writing all the subspaces to the last batch (%s)",
            batchFile);
        currentInfo.raf.seek(currentInfo.raf.length());

        Map<Integer, Boolean> keepMap = GenericHelper.newTreeMap();
        keepMap.put(getCurrentBatchIndex(), true);

        // 16Kb buffer to write
        byte[] buffer = new byte[16 * 1024];

        final EntityCursor<W> cursor = locations.entities();

        for (W kl = cursor.next(); kl != null; kl = cursor.next()) {
            Boolean keep = keepMap.get(kl.batchIndex);

            if ((keep == null || !keep)
                && kl.batchIndex != getCurrentBatchIndex()) {
                BatchInformation info = getBatchInformation(kl.batchIndex);
                if (keep == null) {
                    keep = info.statistics.getUsedPercentage() > threshold;
                    keepMap.put(kl.batchIndex, keep);
                    logger
                        .info(
                            "We will %skeep batch %d since the used percentage (%.2f) is %s the threshold (%.2f)",
                            keep ? "" : "not ", kl.batchIndex,
                            info.statistics.getUsedPercentage(),
                            keep ? "above" : "below", threshold);
                    if (keep)
                        continue;
                }

                logger
                    .debug(
                        "Rewriting subspace %s from batch %d (%d/%d) to batch %d",
                        kl.getId(), kl.batchIndex, kl.position,
                        kl.size, getCurrentBatchIndex(),
                        currentInfo.raf.getFilePointer());

                info.raf.getChannel().position(kl.position);
                kl.batchIndex = getCurrentBatchIndex();
                kl.position = currentInfo.raf.getChannel().position();

                // Copy (byte copy)
                int toRead = kl.size;
                while (toRead > 0) {
                    int read = info.raf.read(buffer, 0, Math.min(toRead,
                        buffer.length));
                    currentInfo.raf.write(buffer, 0, read);
                    toRead -= read;
                }

                // Put the new location information in the database
                info.statistics.remove(kl.size);
                currentInfo.statistics.add(kl.size);

                cursor.update(kl);
            }
        }

        cursor.close();
        closeOpenedBatches(false);

        // Remove old files
        logger.info("Removing old files");
        FileIterator fileIterator = new FileIterator(home, 0,
            new RegexFileFilter(namePattern));
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            Matcher matcher = namePattern.matcher(file.getName());
            matcher.find();
            final Integer batchIndex = Integer.valueOf(matcher.group(1));
            Boolean keep = keepMap.get(batchIndex);
            logger.debug("Considering batch " + batchIndex + ": " + keep);
            if (keep == null || !keep) {
                BatchInformation info = getBatchInformation(batchIndex);
                logger.warn("Deleting batch file %s (used %.1f%%)", file,
                    info.statistics.getUsedPercentage() * 100);
                batchFiles.remove(batchIndex);
                file.delete();
            }
        }
    }

    private File getBatchFile(int batchIndex) {
        return new File(home, String.format("%s-%05d.obj", dbname, batchIndex));
    }

    static class BatchInformation {
        BatchStatistics statistics;
        RandomAccessFile raf;
    }

    final Map<Integer, BatchInformation> batchFiles = GenericHelper
        .newTreeMap();

    final private Pattern namePattern;

    /**
     * Close all the opened batch files
     */
    protected synchronized void closeOpenedBatches(boolean closeCurrentBatch) {
        logger.debug("Close opened batches");
        Entry<Integer, BatchInformation> current = null;

        for (Entry<Integer, BatchInformation> entry : batchFiles.entrySet())
            try {
                final BatchInformation info = entry.getValue();
                final Integer batchNo = entry.getKey();

                logger.debug("Closing batch %d (%b) %s", batchNo,
                    info.statistics.modified, info.statistics);

                // Write if modified
                if (info.statistics.modified) {
                    logger.debug("Writing new statistics for batch %d: %s",
                        batchNo, info.statistics);
                    RandomAccessFile raf = new RandomAccessFile(
                        getBatchFile(batchNo), "rw");
                    raf.getChannel().position(0);
                    info.statistics.write(raf);
                }

                if (currentBatchIndex == batchNo && !closeCurrentBatch)
                    current = entry;
                else
                    info.raf.close();

            }
            catch (IOException e) {
                logger.error(e);
            }

        batchFiles.clear();
        if (current != null)
            batchFiles.put(current.getKey(), current.getValue());
    }

    /**
     * Get the information about a batch
     *
     * @param batchIndex
     * @return
     * @throws IOException
     */
    private BatchInformation getBatchInformation(int batchIndex)
        throws IOException {
        BatchInformation info = batchFiles.get(batchIndex);

        if (info == null) {
            // No cached information: we load it
            info = new BatchInformation();
            final File batchFile = getBatchFile(batchIndex);
            final boolean batchFileExists = batchFile.exists();

            try {
                boolean readOnly = locationStore.getEnvironment().getConfig()
                    .getReadOnly();
                info.raf = new RandomAccessFile(batchFile, readOnly ? "r"
                    : "rw");
            }
            catch (DatabaseException e) {
                throw new RuntimeException(e);
            }
            info.statistics = new BatchStatistics();
            if (batchFileExists) {
                info.raf.getChannel().position(0);
                info.statistics.read(info.raf);
                // Next write will be at the end of the file
                info.statistics.position = info.raf.length();
                logger.debug("Loaded batch file %d: %s", batchIndex,
                    info.statistics.toString());
            }
            else {
                info.statistics.write(info.raf);
                info.statistics.position = info.raf.getFilePointer();
            }

            batchFiles.put(batchIndex, info);
            logger.debug("Read batch %d statistics: %s", batchIndex,
                info.statistics);
        }
        return info;
    }

    /**
     * Returns the number of stored objects See {@linkplain com.sleepycat.persist.BasicIndex#count()}
     *
     * @throws DatabaseException
     */
    public long count() throws DatabaseException {
        return locations.count();
    }

    /**
     * Get a stored object
     *
     * @param location The location of the object
     * @return The object or null if it does not exist
     * @throws IOException
     * @throws DatabaseException
     */
    public V getStoredObject(final K key) throws IOException, DatabaseException {
        Location<K> location = getLocation(key);
        if (location == null)
            return null;
        return getStoredObject(location);
    }

    public boolean hasObject(final K key) throws DatabaseException {
        Location<K> location = getLocation(key);
        return location != null;
    }

    public int getObjectSize(final K key) throws DatabaseException {
        Location<K> location = getLocation(key);
        return location != null ? location.size : 0;
    }

    public boolean isCompressed() {
        return compressed;
    }

    /**
     * Get a stored object stream (the stream is not uncompressed automatically if necessary)
     *
     * @param location The location of the object
     * @throws IOException
     * @throws DatabaseException
     */
    public void outputObject(OutputStream out, final K key) throws IOException,
        DatabaseException {
        Location<K> location = getLocation(key);
        if (location == null) {
            logger.warn("Location %s does not exist", key);
            return;
        }
        synchronized (this) {
            BatchInformation info = getBatchInformation(location.batchIndex);
            final RandomAccessFile raf = info.raf;
            raf.seek(location.position);
            InputStream in = new RandomAccessFileInputStream(raf);
            final byte[] buffer = new byte[8192];
            int toRead = location.size;
            while (toRead > 0) {
                int len = Math.min(toRead, buffer.length);
                len = in.read(buffer, 0, len);
                if (len <= 0)
                    throw new RuntimeException("should not happen");
                out.write(buffer, 0, len);
                toRead -= len;
            }

        }
    }

    /**
     * @param key
     * @return
     * @throws DatabaseException
     */
    protected W getLocation(final K key) throws DatabaseException {
        return locations.get(key);
    }

    synchronized protected V getStoredObject(final Location<K> location)
        throws IOException, DatabaseException {
        BatchInformation info = getBatchInformation(location.batchIndex);
        final RandomAccessFile raf = info.raf;
        raf.seek(location.position);
        logger
            .debug(
                "Loading the object %s from batch %d (offset=%d, size=%d, gz=%b): channel in position %d on %d",
                location.getId(), location.batchIndex,
                location.position, location.size, compressed, raf
                    .getFilePointer(), raf.length());

        InputStream in = new RandomAccessFileInputStream(raf);
        if (compressed)
            in = new GZIPInputStream(in);

        if (logger.isTraceEnabled()) {
            byte[] buffer = new byte[location.size];
            in.read(buffer);
            char[] chars = Hex.encodeHex(buffer);
            PrintStream out = new LoggerPrintStream(logger, Level.TRACE);
            for (int i = 0; i < chars.length; i++) {
                if (i % 32 == 0) {
                    if (i > 0)
                        out.flush();
                }
                else if (i % 2 == 0)
                    out.print(' ');
                out.print(chars[i]);
            }
            out.flush();

            raf.seek(location.position);
            in = new RandomAccessFileInputStream(raf);
            if (compressed)
                in = new GZIPInputStream(in);
        }

        final V t = readObject(in);
        return t;
    }

    protected void nextBatch() {
        currentBatchIndex++;
        logger.info("Current batch index is %d", currentBatchIndex);
    }

    protected void writeObject(OutputStream out, V object) throws IOException {
        BufferedOutputStream bout = new BufferedOutputStream(out);
        ObjectOutputStream oOut = new ObjectOutputStream(bout);
        oOut.writeObject(object);
        oOut.flush();
        bout.flush();
    }

    /**
     * Read an object
     *
     * @param in The input stream
     * @return
     * @throws IOException
     */
    protected V readObject(InputStream in) throws IOException {
        ObjectInputStream oIn = new ObjectInputStream(in);
        try {
            @SuppressWarnings("unchecked")
            V t = (V)oIn.readObject();
            return t;
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    interface Writer {
        void write(OutputStream out) throws IOException;
    }

    /**
     * Store the object
     *
     * @param key The index of the object
     * @param object The object to store
     * @throws IOException If an I/O error occurs
     * @throws DatabaseException
     */
    public final void storeObject(K key, V object) throws IOException,
        DatabaseException {
        storeObject(key, locations.get(key), object);
    }

    public final void storeObject(K key, final byte[] data, final int offset,
        final int length) throws IOException, DatabaseException {
        storeObject(key, locations.get(key), new Writer() {
            @Override
            public void write(OutputStream out) throws IOException {
                out.write(data, offset, length);
            }
        });

    }

    /**
     * Store an object
     *
     * @param key The id of the object
     * @param oldLocation The old location (must be given if it exists, otherwise statistics will be corrupted)
     * @param object The object to store
     * @throws IOException
     * @throws DatabaseException
     */
    protected synchronized final void storeObject(K key,
        Location<K> oldLocation, final V object) throws IOException,
        DatabaseException {
        storeObject(key, oldLocation, new Writer() {
            @Override
            public void write(OutputStream out) throws IOException {
                writeObject(out, object);
            }
        });
    }

    protected synchronized final void storeObject(K key,
        Location<K> oldLocation, Writer writer) throws IOException,
        DatabaseException {
        // Select the current batch (or the next one is we exceed in size)
        if (currentBatchIndex == -1)
            currentBatchIndex = 0;
        BatchInformation info = getBatchInformation(currentBatchIndex);
        if (info.statistics.position >= sizeLimit) {
            logger
                .info(
                    "Current batch position (%d) is over the size limit (%d) - creating a new one",
                    info.statistics.position, sizeLimit);
            nextBatch();
            info = getBatchInformation(currentBatchIndex);
        }

        // Store
        W location;
        try {
            location = constructor.newInstance(key);
        }
        catch (RuntimeException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
        location.batchIndex = currentBatchIndex;
        location.position = info.statistics.position;
        info.raf.seek(location.position);

        OutputStream out = new RandomAccessFileOutputStream(info.raf);
        if (compressed) {
            GZIPOutputStream gzout = new GZIPOutputStream(out);
            writer.write(gzout);
            gzout.finish();
        }
        else
            writer.write(out);

        out.flush();

        info.statistics.position = info.raf.getFilePointer();
        location.size = (int)(info.statistics.position - location.position);
        info.statistics.add(location.size);
        locations.put(location);

        // and now... we can destroy if necessary
        if (oldLocation != null) {
            info = getBatchInformation(oldLocation.batchIndex);
            info.statistics.remove(oldLocation.size);
        }

        logger.debug("Wrote object with key %s in batch %d (%d/%d), gz=%b",
            key, location.batchIndex, location.position, location.size,
            compressed);

    }

    public Iterable<K> keys() throws DatabaseException {
        return new Iterable<K>() {
            @Override
            public Iterator<K> iterator() {
                try {
                    return keyIterator();
                }
                catch (DatabaseException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Get an iterator on the keys
     */
    public CloseableIterator<K> keyIterator() throws DatabaseException {
        final EntityCursor<? extends Location<K>> cursor = locations.entities();

        logger.debug("We have %d subspaces in store", locations.count());
        return new AbstractIterator<K>() {
            @Override
            public void close() {
                try {
                    cursor.close();
                }
                catch (DatabaseException e) {
                    logger.error(e);
                }
                closeOpenedBatches(false);
            }

            ;

            @Override
            protected boolean storeNext() {
                try {
                    Location<K> location = cursor.next();
                    logger.debug("Next key id %s", location != null ? location
                        .getId() : "?");
                    if (location == null) {
                        close();
                        return false;
                    }
                    value = location.getId();
                    return true;
                }
                catch (DatabaseException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    /**
     * Get an iterator on the entries
     *
     * @return
     */
    public CloseableIterator<Entry<K, V>> iterator() {
        try {
            logger.debug("We have %d subspaces in store", locations.count());

            return new AbstractIterator<Entry<K, V>>() {
                EntityCursor<W> cursor = locations.entities();

                @Override
                protected void finalize() throws Throwable {
                    if (cursor != null)
                        logger
                            .warn(
                                "Iterator over database %s has not been closed",
                                BatchFileStorage.this);
                    close();
                }

                @Override
                public void close() {
                    try {
                        if (cursor != null) {
                            cursor.close();
                            cursor = null;
                        }
                    }
                    catch (DatabaseException e) {
                        logger.error(e);
                    }
                    closeOpenedBatches(false);
                }

                ;

                @Override
                protected boolean storeNext() {
                    try {
                        Location<K> location = cursor.next();
                        logger.debug("Next subspace id %s",
                            location != null ? location.getId() : -1);
                        if (location == null) {
                            close();
                            return false;
                        }
                        try {
                            value = SimpleEntry.create(location.getId(),
                                getStoredObject(location));
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    }
                    catch (DatabaseException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the currentBatchIndex
     */
    public int getCurrentBatchIndex() {
        return currentBatchIndex;
    }

    @Persistent
    public static abstract class Location<K> {
        abstract public K getId();

        public Location() {
        }

        public Location(int batchIndex, long position, int size) {
            super();
            this.batchIndex = batchIndex;
            this.position = position;
            this.size = size;
        }

        /**
         * File index
         */
        public int batchIndex;

        /**
         * Position in the file index
         */
        public long position;

        /**
         * Size of the entry
         */
        public int size;
    }

    @Entity final public static class IntegerLocation extends Location<Integer> {
        @PrimaryKey
        int id;

        public IntegerLocation() {
            super();
        }

        public IntegerLocation(Integer id) {
            this.id = id;
        }

        @Override
        public Integer getId() {
            return id;
        }
    }

    @Entity final public static class StringLocation extends Location<String> {
        @PrimaryKey
        String id;

        public StringLocation() {
            super();
        }

        public StringLocation(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    /**
     * Statistics on a batch file
     *
     * @author bpiwowar
     */
    static class BatchStatistics implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Current position
         */
        long position;

        /**
         * Total number of active entries
         */
        int used;

        /**
         * Total number of unactive entries
         */
        int unused;

        /**
         * Size
         */
        long size;

        /**
         * Unused space
         */
        long freeSize;

        transient boolean modified = false;

        public void add(long size) {
            modified = true;
            this.size += size;
            used++;
        }

        public void remove(long size) {
            modified = true;
            used--;
            this.size -= size;
            freeSize += size;
            unused++;
        }

        @Override
        public String toString() {
            return String
                .format(
                    "Batch has %d objects and %d bytes (unused: %d and %d), pos: %d",
                    used, size, unused, freeSize, position);
        }

        void read(DataInput in) throws IOException {
            used = in.readInt();
            unused = in.readInt();
            size = in.readLong();
            freeSize = in.readLong();
        }

        void write(DataOutput out) throws IOException {
            out.writeInt(used);
            out.writeInt(unused);
            out.writeLong(size);
            out.writeLong(freeSize);
        }

        public double getUsedPercentage() {
            return 1. - (double)freeSize / (double)(freeSize + size);
        }
    }

}

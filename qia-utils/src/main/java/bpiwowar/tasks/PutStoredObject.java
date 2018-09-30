package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.argparser.checkers.IOChecker.ValidDirectory;
import bpiwowar.db.BatchFileStorage;
import bpiwowar.db.BatchFileStorage.Location;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.log.Logger;
import bpiwowar.system.FileIterator;
import bpiwowar.system.FileSystem;
import bpiwowar.utils.GenericHelper;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.ClassMetadata;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.model.PrimaryKeyMetadata;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;

@TaskDescription(name = "put-stored-object", description = "Put an object stored in a BatchFileStorage", project = {"db"})
public class PutStoredObject extends AbstractTask {
    final static private Logger logger = Logger.getLogger();

    @Argument(name = "dir", help = "Database directory", required = true, checkers = ValidDirectory.class)
    private File dbdir;

    @Argument(name = "name", help = "Database name", required = true)
    private String dbname;

    @Argument(name = "key", help = "The key", required = false)
    String key;

    @Argument(name = "indir", help = "Output dir (in case key is not given)", checkers = IOChecker.ValidDirectory.class)
    File indir;

    @Override
    public int execute() throws Throwable {
        // Initialisations
        EnvironmentConfig dbconf = new EnvironmentConfig();
        dbconf.setReadOnly(false);
        dbconf.setAllowCreate(false);
        Environment dbenv = new Environment(dbdir, dbconf);

        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setReadOnly(true);
        EntityStore locationStore = new EntityStore(dbenv, dbname, storeConfig);

        // Discover the class of the key
        Class<?> keyClass = null;
        Type keyTClass = null;
        EntityModel model = locationStore.getModel();
        for (String x : model.getKnownClasses()) {
            ClassMetadata m = model.getClassMetadata(x);
            if (m != null) {
                final PrimaryKeyMetadata pKey = m.getPrimaryKey();
                final String className = pKey.getDeclaringClassName();
                keyClass = getClass().getClassLoader().loadClass(className);
                Type[] types = GenericHelper.getActualTypeArguments(keyClass,
                    Location.class);
                if (types != null) {
                    keyTClass = types[0];
                }
            }
        }

        if (keyTClass == null)
            throw new RuntimeException("Did not detected the key");

        // Convert the command line given key into an appropriate instance
        logger.info("Detected key: %s", keyTClass);
        @SuppressWarnings("unchecked")
        BatchFileStorage storage = new BatchFileStorage(dbenv, dbname, null,
            keyClass, true, false);

        if (key != null) {
            Object tkey = null;
            if (keyTClass == Integer.class) {
                tkey = Integer.parseInt(key);
            }
            else if (keyTClass == String.class) {
                tkey = key;
            }
            else {
                logger.error("Cannot handle key of type %s", keyTClass);
                return 1;
            }

            // Output the stored value

            byte[] data = new byte[8192];
            int l, length = 0;
            while ((l = System.in.read(data, length, data.length - length)) > 0) {
                length += l;
                data = ByteArrays.grow(data, length + 1);
            }

            storage.storeObject(tkey, data, 0, length);
        }
        else if (indir == null) {
            ObjectInputStream oin = new ObjectInputStream(System.in);
            byte[] bytes = new byte[8192];
            while (oin.available() > 0) {
                Object key = oin.readObject();
                int len = oin.readInt();
                bytes = ByteArrays.grow(bytes, len);
                oin.read(bytes, 0, len);
                storage.storeObject(key, bytes, 0, len);
            }
        }
        else {
            byte[] bytes = new byte[8192];
            FileIterator iterator = new FileIterator(indir, 0,
                FileSystem.FILE_FILTER);
            while (iterator.hasNext()) {
                File file = iterator.next();
                logger.debug("Processing %s", file);
                DataInputStream in = new DataInputStream(new FileInputStream(
                    file));
                // read the key
                Object key;
                if (keyTClass == Integer.class) {
                    key = (Integer)in.readInt();
                }
                else if (keyTClass == String.class) {
                    key = in.readUTF();
                }
                else {
                    logger.error("Cannot handle key of type %s", keyTClass);
                    return 1;
                }
                // read the object
                int read;
                int length = 0;
                while ((read = in.read(bytes, length, bytes.length - length)) > 0) {
                    length += read;
                    bytes = ByteArrays.grow(bytes, length + 1);
                }

                // Store the object
                storage.storeObject(key, bytes, 0, length);
                in.close();
            }
        }

        storage.close();
        return 0;
    }
}

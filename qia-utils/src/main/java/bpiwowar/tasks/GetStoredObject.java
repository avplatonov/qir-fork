package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.argparser.checkers.IOChecker.ValidDirectory;
import bpiwowar.db.BatchFileStorage;
import bpiwowar.db.BatchFileStorage.Location;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.log.Logger;
import bpiwowar.utils.GenericHelper;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.ClassMetadata;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.model.PrimaryKeyMetadata;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.zip.GZIPInputStream;

@TaskDescription(name = "get-stored-object", description = "Get an object stored in a BatchFileStorage", project = {"db"})
public class GetStoredObject extends AbstractTask {
    final static private Logger logger = Logger.getLogger();

    @Argument(name = "dir", help = "Database directory", required = true, checkers = ValidDirectory.class)
    private File dbdir;

    @Argument(name = "name", help = "Database name", required = true)
    private String dbname;

    @Argument(name = "key", help = "The key (if not present, outputs all the objects)")
    String key;

    @Argument(name = "outdir", help = "Output dir (in case key is not given)", checkers = IOChecker.ValidDirectory.class)
    File outdir;

    @SuppressWarnings("unchecked")
    @Override
    public int execute() throws Throwable {
        // Initialisations
        EnvironmentConfig dbconf = new EnvironmentConfig();
        dbconf.setReadOnly(true);
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
            throw new RuntimeException("Did not detect the key class");

        // Convert the command line given key into an appropriate instance
        logger.info("Detected key: %s", keyTClass);

        @SuppressWarnings({"rawtypes"})
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
            logger.info("Outputing value");
            storage.outputObject(System.out, tkey);
            System.out.flush();
        }
        else {
            // We output everything as an object stream

            ObjectOutputStream objOut = outdir == null ? new ObjectOutputStream(
                System.out)
                : null;

            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            for (Object key : storage.keys()) {
                byteArray.reset();
                storage.outputObject(byteArray, key);

                if (storage.isCompressed()) {
                    ByteArrayInputStream byteIn = new ByteArrayInputStream(
                        byteArray.toByteArray());
                    GZIPInputStream gzin = new GZIPInputStream(byteIn);
                    byteArray.reset();
                    byte[] buffer = new byte[8192];
                    int len = 0;
                    while ((len = gzin.read(buffer, 0, buffer.length)) > 0)
                        byteArray.write(buffer, 0, len);
                }

                // Output
                byte[] bytes = byteArray.toByteArray();
                if (outdir != null) {
                    DataOutputStream out = new DataOutputStream(
                        new FileOutputStream(new File(outdir, key
                            .toString())));
                    if (keyTClass == Integer.class) {
                        out.writeInt((Integer)key);
                    }
                    else if (keyTClass == String.class) {
                        out.writeUTF((String)key);
                    }
                    else {
                        logger.error("Cannot handle key of type %s", keyTClass);
                        return 1;
                    }
                    out.write(bytes);
                    out.close();
                }
                else {
                    objOut.writeObject(key);
                    objOut.writeInt(bytes.length);
                    objOut.write(bytes);
                }

            }
            if (objOut != null)
                objOut.close();
        }
        return 0;
    }
}

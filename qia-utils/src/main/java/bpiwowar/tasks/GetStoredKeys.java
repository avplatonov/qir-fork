package bpiwowar.tasks;

import bpiwowar.argparser.Argument;
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
import java.io.File;
import java.lang.reflect.Type;

@TaskDescription(name = "get-stored-keys", description = "Get an object stored in a BatchFileStorage", project = {"db"})
public class GetStoredKeys extends AbstractTask {
    final static private Logger logger = Logger.getLogger();

    @Argument(name = "dir", help = "Database directory", required = true, checkers = ValidDirectory.class)
    private File dbdir;

    @Argument(name = "name", help = "Database name", required = true)
    private String dbname;

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
            throw new RuntimeException("Did not detected the key");

        // Convert the command line given key into an appropriate instance
        logger.info("Detected key: %s", keyTClass);

        @SuppressWarnings({"rawtypes", "unchecked"})
        BatchFileStorage storage = new BatchFileStorage(dbenv, dbname, null,
            keyClass, true, false);
        for (Object x : storage.keys())
            System.out.format("%s%n", x);
        storage.close();
        return 0;
    }
}

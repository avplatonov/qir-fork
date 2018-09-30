package bpiwowar.pipe.io;

import bpiwowar.pipe.Processor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.apache.log4j.Logger;

public class File2InputStream implements Processor<File, InputStream> {
    static final Logger logger = Logger.getLogger(File2InputStream.class);

    public InputStream process(File input) {
        try {
            logger.debug(String.format("Processing %s", logger));
            return new FileInputStream(input);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}

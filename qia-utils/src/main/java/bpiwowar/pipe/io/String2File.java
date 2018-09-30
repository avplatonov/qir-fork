package bpiwowar.pipe.io;

import bpiwowar.pipe.Processor;
import java.io.File;
import org.apache.log4j.Logger;

public class String2File implements Processor<String, File> {
    static final Logger logger = Logger.getLogger(String2File.class);

    public File process(String input) {
        return new File(input);
    }

}

package bpiwowar.xml;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class XStreamHelper {

    /**
     * Serialise an object into a file
     *
     * @param file The file where the object should be serialised
     * @param object The object to serialise
     * @throws IOException
     */
    public static void serialise(Object object, File file) throws IOException {
        // --- Write to file
        FileOutputStream os = new FileOutputStream(file);
        new XStream().toXML(object, os);
        os.close();
    }

    public static void serialise(Object object, PrintStream out) {
        new XStream().toXML(object, System.out);
        System.out.flush();
    }

}

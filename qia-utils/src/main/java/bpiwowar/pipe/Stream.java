package bpiwowar.pipe;

import java.util.Hashtable;

/**
 * This class represents a stream, where each object of the stream is a fixed width array of objects of a given type
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class Stream {
    static private class Information {
        /**
         * Information about the loop number
         */
        public int loop;

        public Producer<?> producer;

    }

    /**
     * Association between streams and producers
     */
    Hashtable<String, Information> table;
}

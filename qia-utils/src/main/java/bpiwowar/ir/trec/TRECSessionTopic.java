/**
 * $Author:$ $Id:$ $Rev:$
 */

package bpiwowar.ir.trec;

import bpiwowar.ir.query.DefaultQuerySet;
import bpiwowar.ir.query.Query;
import bpiwowar.ir.query.QuerySet;
import bpiwowar.ir.query.StringQuery;
import bpiwowar.ir.query.Topic;
import bpiwowar.log.Logger;
import bpiwowar.utils.arrays.ListAdaptator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles topics as used in the 2010 TREC Session track. A topic contains
 * a topic ID, a query and a subsequent query reformulation.
 * @author <a href="mailto:ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 *
 */
public class TRECSessionTopic implements Topic {
    final static private Logger logger = Logger.getLogger();

    /** Topic ID */
    private String id = null;

    /** First query */
    private String query1 = null;

    /** Second query, the reformulation */
    private String query2 = null;

    /** The list of types in the topic description */
    final static List<String> typeList = ListAdaptator.create(new String[] {
        "query1", "query2"});

    /**
     * Constructor of class
     * @param id the topic ID
     * @param query1 the original query
     * @param query2 the query reformulation
     */
    public TRECSessionTopic(String id, String query1, String query2) {
        this.id = id;
        this.query1 = query1;
        this.query2 = query2;
    }

    /*
    /* (non-Javadoc)
     * @see bpiwowar.ir.query.Topic#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Returns the topic part as a {@link Query}. Currently available topic
     * parts/types are <em>query1</em> for the original query and
     * <em>query2</em> for the query reformulation.
     * @see bpiwowar.ir.query.Topic#getTopicPart(java.lang.String)
     */
    @Override
    public Query getTopicPart(String type) {
        if (type.equals("query1"))
            return new StringQuery(query1);
        if (type.equals("query2"))
            return new StringQuery(query2);
        return null;
    }

    /* (non-Javadoc)
     * @see bpiwowar.ir.query.Topic#getTypes()
     */
    @Override
    public List<String> getTypes() {
        return typeList;
    }

    /**
     * Reads a set of TREC Session Track Topics and returns a {@link QuerySet}
     * consisting of {@link Topic} objects. The reader must deliver the
     * required information in the TREC 2010 Session Track format, for instance
     * <pre>
     * 107:the music man:the music man songs
     * </pre>
     * where the entries (id, query1, query2) are separated by a colon.
     * @param reader the reader
     * @return a query set
     * @throws IOException
     */
    static public QuerySet readTopics(BufferedReader reader)
        throws IOException {
        final DefaultQuerySet querySet = new DefaultQuerySet();
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split(":");
            querySet.put(fields[0],
                new TRECSessionTopic(fields[0], fields[1], fields[2]));
        }
        reader.close();
        return querySet;
    }

    /**
     * Reads a set of TREC Session Track Topics from a file
     * and returns a {@link QuerySet} consisting of {@link Topic} objects. The
     * file must contain the
     * required information in the TREC 2010 Session Track format, for instance
     * <pre>
     * 107:the music man:the music man songs
     * </pre>
     * where the entries (id, query1, query2) are separated by a colon.
     * @param filename the topic file
     * @return the query set
     * @throws IOException
     */
    static public QuerySet readTopics(String filename) throws IOException {
        logger.debug("Reading TREC session track topics from " + filename);
        return readTopics(new BufferedReader(new FileReader(filename)));
    }

    /**
     * Reads a TREC Session Track topic file and outputs its topics
     * @param args first argument is the topic file
     */
    public static void main(String[] args) {
        try {
            QuerySet qSet = readTopics(args[0]);
            Map<String, ? extends Topic> queries = qSet.queries();
            Iterator<String> it = queries.keySet().iterator();
            int n = 0;
            while (it.hasNext()) {
                n++;
                String topicID = (String)it.next();
                Topic topic = queries.get(topicID);
                System.out.println("Topic " + topic.getId());
                List<String> types = topic.getTypes();
                Iterator<String> in2 = types.iterator();
                while (in2.hasNext()) {
                    String type = in2.next();
                    System.out.println(type + ": "
                        + ((StringQuery)topic.getTopicPart(type)).getQuery());
                }
                System.out.println();
            }
            System.out.println("Read " + n + " topics.");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}

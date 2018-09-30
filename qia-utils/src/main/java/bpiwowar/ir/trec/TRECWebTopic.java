/**
 *
 */
package bpiwowar.ir.trec;

import bpiwowar.ir.query.DefaultQuerySet;
import bpiwowar.ir.query.Query;
import bpiwowar.ir.query.QuerySet;
import bpiwowar.ir.query.StringQuery;
import bpiwowar.ir.query.Topic;
import bpiwowar.log.Logger;
import bpiwowar.utils.arrays.ListAdaptator;
import it.unimi.dsi.fastutil.chars.CharArrays;
import it.unimi.dsi.lang.MutableString;
import it.unimi.dsi.parser.Attribute;
import it.unimi.dsi.parser.BulletParser;
import it.unimi.dsi.parser.Element;
import it.unimi.dsi.parser.callback.DefaultCallback;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_DESCRIPTION;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_QUERY;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_SUBTOPIC;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_TOPIC;

/**
 * <p>
 * This class implements a TREC Web Topic, as used by recent TREC Web tracks supporting diversity (e.g. TREC 2009). A
 * topic can have subtopics. While this class implements the {@link Topic} interface, typecasting it to TRECWebTopic
 * makes available the {@link #getSubtopics} method which returns a {@link QuerySet} consisting of this topic's
 * subtopic.
 * </p>
 * <p>
 * As usual, use {@link #readTopic} to read a TREC topic file and get a {@link QuerySet} consisting of TRECWebTopic
 * objects including subtopics.
 *
 * @author <a mailto="ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
 */
public class TRECWebTopic implements Topic {
    final static private Logger logger = Logger.getLogger();

    /** The topic id */
    private String id;

    /** The topic type (e.g., faceted, ambiguous (default)) */
    private String topicType = "ambiguous";

    /** The list of types in the topic description */
    private final static List<String> typeList = ListAdaptator
        .create(new String[] {
            "query", "description"});

    /** the query type */
    private String query;

    /** the description type */
    private String description;

    /** the subtopics as query set */
    private final DefaultQuerySet subtopics = new DefaultQuerySet();

    /* (non-Javadoc)
     * @see bpiwowar.ir.query.Topic#getId()
     */
    @Override
    public String getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see bpiwowar.ir.query.Topic#getTopicPart(java.lang.String)
     */
    @Override
    public Query getTopicPart(String type) {
        if (type.equals("query"))
            return new StringQuery(query);
        if (type.equals("description"))
            return new StringQuery(description);
        return null;
    }

    /**
     * Returns the topic type (ambiguous|faceted|other)
     *
     * @return the topic type
     */
    public String getTopicType() {
        return topicType;
    }

    /* (non-Javadoc)
     * @see bpiwowar.ir.query.Topic#getTypes()
     */
    @Override
    public List<String> getTypes() {
        return typeList;
    }

    /**
     * Returns the subtopics as a {@link QuerySet}
     *
     * @return the subtopics
     */
    public QuerySet getSubtopics() {
        return subtopics;
    }

    /**
     * Reads a TREC 2009/2010 Web track topic file and returns a {@link QuerySet} consisting of {@link Topic} objects.
     * This objects must be typecast to TRECWebTopic objects in order to access the subtopics via {@link
     * #getSubtopics}.
     *
     * @param reader the reader
     * @return the query set
     * @throws IOException
     */
    static public QuerySet readTopics(BufferedReader reader) throws IOException {
        final DefaultQuerySet querySet = new DefaultQuerySet();
        TRECParsingFactory parsingFactory = TRECParsingFactory.INSTANCE;
        BulletParser bulletParser = new BulletParser(parsingFactory);

        bulletParser.setCallback(new DefaultCallback() {
            TRECWebTopic topic = null;
            MutableString curText = new MutableString();
            Subtopic curSubtopic;

            @Override
            public boolean characters(char[] text, int offset, int length,
                boolean flowBroken) {
                curText.append(text, offset, length);
                return true;
            }

            @Override
            public boolean startElement(Element element,
                Map<Attribute, MutableString> attrMap) {
                curText = new MutableString();
                if (element == ELEMENT_TOPIC) {
                    topic = new TRECWebTopic();
                    // parse and set attributes
                    for (Attribute attr : attrMap.keySet()) {
                        if (attr.name.equals("number"))
                            topic.id = attrMap.get(attr).toString();
                        else if (attr.name.equals("type"))
                            topic.topicType = attrMap.get(attr).toString();
                    }
                }
                else if (element == ELEMENT_SUBTOPIC) {
                    curSubtopic = new Subtopic();
                    // parse and set attributes
                    for (Attribute attr : attrMap.keySet()) {
                        if (attr.name.equals("number"))
                            curSubtopic.id = attrMap.get(attr).toString();
                        else if (attr.name.equals("type"))
                            curSubtopic.topicType = attrMap.get(attr).toString();
                    }
                }
                return true;
            }

            @Override
            public boolean endElement(Element element) {
                if (element == ELEMENT_TOPIC) {
                    // save topic to query set
                    querySet.put(topic.id, topic);
                    topic = null;
                }
                else if (element == ELEMENT_SUBTOPIC) {
                    // save subtopic (incl text) to topic
                    curSubtopic.query = curText.toString();
                    topic.subtopics.put(curSubtopic.id, curSubtopic);
                    curSubtopic = null;
                }
                else if (element == ELEMENT_DESCRIPTION) {
                    topic.description = curText.toString();
                }
                else if (element == ELEMENT_QUERY) {
                    topic.query = curText.toString();
                }

                curText = new MutableString();
                return true;
            }
        });

        // Read & parse the stream
        char text[] = new char[8192];
        int offset = 0, l;
        while ((l = reader.read(text, offset, text.length - offset)) > 0) {
            offset += l;
            text = CharArrays.grow(text, offset + 1);
        }

        bulletParser.parseText(true);
        bulletParser.parseCDATA(true);
        bulletParser.parseTags(true);
        bulletParser.parseAttributes(true);
        bulletParser.parseAttribute(
            parsingFactory.getAttribute(new MutableString("number")));
        bulletParser.parseAttribute(
            parsingFactory.getAttribute(new MutableString("type")));
        bulletParser.parse(text, 0, offset);
        return querySet;
    }

    /**
     * This subclass implements a subtopic belonging to a Web topic.
     *
     * @author <a mailto="ingo@dcs.gla.ac.uk">Ingo Frommholz</a>
     */
    public static class Subtopic implements Topic {

        /** The subtopic id **/
        public String id;

        /** The subtopic query */
        public String query;

        /** The type of the subtopic (e.g., nav, inf (default)) */
        public String topicType = "inf";

        /** The list of types in the topic description */
        final static List<String> typeList = ListAdaptator.create(new String[] {
            "query"});

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Query getTopicPart(String type) {
            if (type.equals("query"))
                return new StringQuery(query);
            return null;
        }

        @Override
        public List<String> getTypes() {
            return typeList;
        }

        /**
         * The topic type of the subquery (nav|inf)
         *
         * @return the topic type
         */
        public String getTopicType() {
            return topicType;
        }
    }

}

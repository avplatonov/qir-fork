package bpiwowar.ir.trec;

import bpiwowar.ir.query.DefaultQuerySet;
import bpiwowar.ir.query.Query;
import bpiwowar.ir.query.QuerySet;
import bpiwowar.ir.query.StringQuery;
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

import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_CON;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_DEF;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_DESC;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_NARR;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_NUM;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_SMRY;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_TITLE;
import static bpiwowar.ir.trec.TRECParsingFactory.ELEMENT_TOP;

/**
 * A TREC topic
 *
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 */
public class TRECTopic implements bpiwowar.ir.query.Topic {
    final static private Logger logger = Logger.getLogger();

    protected String id;

    protected String narrative;

    protected String description;

    protected String title;

    protected String concepts;

    protected String definitions;

    protected String summary;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getNarrative() {
        return narrative;
    }

    public String getSummary() {
        return summary;
    }

    public String getConcepts() {
        return concepts;
    }

    public String getDefinitions() {
        return definitions;
    }

    /**
     * Read a set of TREC topics
     *
     * @param reader
     * @param quoteCommas Should we quote word separated by commas?
     * @param useDescription Should we use the description part of the topic
     * @param wordRecogniser Used to disambiguate dash
     * @return
     * @throws IOException
     */
    static public QuerySet readTopics(BufferedReader reader,
        final boolean quoteCommas) throws IOException {
        logger.debug("Reading a topic file");
        final DefaultQuerySet querySet = new DefaultQuerySet();

        BulletParser bulletParser = new BulletParser(
            TRECParsingFactory.INSTANCE);

        bulletParser.setCallback(new DefaultCallback() {
            TRECTopic topic = null;
            MutableString curText = new MutableString();
            Element curElement;

            @Override
            public boolean characters(char[] text, int offset, int length,
                boolean flowBroken) {
                curText.append(text, offset, length);
                return true;
            }

            @Override
            public boolean startElement(Element element,
                Map<Attribute, MutableString> attrMapUnused) {

                // --- New tag
                if (topic != null)
                    process();

                // ---
                if (element == ELEMENT_TOP) {
                    topic = new TRECTopic();
                }
                curElement = element;
                return true;
            }

            void removePrefix(String prefix, MutableString text) {
                if (text.startsWith(prefix))
                    text.delete(0, prefix.length());

            }

            private void process() {
                curText.trim();
                curText.replace('\n', ' ');
                curText.squeezeSpaces(false);

                if (curElement == ELEMENT_TITLE) {
                    removePrefix("Topic: ", curText);
                    if (quoteCommas) {
                        StringBuilder builder = new StringBuilder();
                        boolean first = true;
                        for (String part : curText.toString()
                            .split("\\s*,\\s*")) {
                            if (first)
                                first = false;
                            else
                                builder.append(' ');

                            if (part.indexOf(' ') >= 0) {
                                builder.append('"');
                                builder.append(part);
                                builder.append('"');
                            }
                            else
                                builder.append(part);
                        }

                        topic.title = builder.toString();
                    }
                    else
                        topic.title = curText.toString();
                }
                else if (curElement == ELEMENT_NUM) {
                    removePrefix("Number: ", curText);
                    // Normalise the number
                    topic.id = new Integer(curText.toString()).toString();
                }
                else if (curElement == ELEMENT_DESC) {
                    removePrefix("Description: ", curText);
                    topic.description = curText.toString();
                }
                else if (curElement == ELEMENT_NARR) {
                    // TREC
                    removePrefix("Narrative: ", curText);
                    topic.narrative = curText.toString();
                }
                else if (curElement == ELEMENT_SMRY) {
                    removePrefix("Summary: ", curText);
                    topic.summary = curText.toString();
                }
                else if (curElement == ELEMENT_CON) {
                    // TREC 1
                    removePrefix("Concepts: ", curText);
                    topic.concepts = curText.toString();
                }
                else if (curElement == ELEMENT_DEF) {
                    // TREC 1
                    removePrefix("Definition(s): ", curText);
                    removePrefix("Definition: ", curText);
                    topic.definitions = curText.toString();
                }
                curElement = null;
                curText.delete(0, curText.length());
            }

            @Override
            public boolean endElement(Element element) {
                if (topic != null)
                    process();

                if (element == ELEMENT_TOP) {
                    if (topic.id == null) {
                        logger.warn("Topic had no identifier - skipping");
                    }
                    else {
                        logger.debug("Adding topic %s with title [%s]",
                            topic.id, topic.title);

                        querySet.put(topic.id, topic);
                    }
                    topic = null;
                }
                return true;
            }
        });

        // Read the file & parse
        char text[] = new char[8192];
        int offset = 0, l;
        while ((l = reader.read(text, offset, text.length - offset)) > 0) {
            offset += l;
            text = CharArrays.grow(text, offset + 1);
        }

        bulletParser.parseText(true);
        bulletParser.parseCDATA(true);
        bulletParser.parseTags(true);
        bulletParser.parse(text, 0, offset);

        return querySet;

    }

    @Override
    public String getId() {
        return id;
    }

    final static List<String> list = ListAdaptator.create(new String[] {
        "title", "desc"});

    @Override
    public Query getTopicPart(String type) {
        if ("title".equals(type))
            return new StringQuery(title);

        if ("desc".equals(type))
            return new StringQuery(description);

        return null;
    }

    @Override
    public List<String> getTypes() {
        return list;
    }

}
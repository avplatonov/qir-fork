package bpiwowar.ir.inex;

import bpiwowar.utils.GenericHelper;
import bpiwowar.utils.Output;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A set of topics
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class Topics implements Iterable<Topic> {
    final static Logger logger = Logger.getLogger(Topics.class);

    /**
     * Our topics
     */
    private Map<String, Topic> topics = GenericHelper.newTreeMap();

    /**
     * Add a new set of topics
     *
     * @param is The input stream
     * @throws Exception
     */
    public void add(InputStream is) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        if (factory.isValidating())
            factory
                .setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        DocumentBuilder parser = factory.newDocumentBuilder();
        Document document = parser.parse(is);
        XPath xpath = XPathFactory.newInstance().newXPath();

        Element root = document.getDocumentElement();
        NodeList topics = (NodeList)xpath.evaluate("//topic", root,
            XPathConstants.NODESET);

        // Parse all the //topic elements in the file
        for (int i = 0; i < topics.getLength(); i++) {
            Element item = (Element)topics.item(i);
            Topic topic = new Topic(item);
            this.topics.put(topic.getId(), topic);
        }
    }

    /**
     * Get all the terms of all the topics
     *
     * @return
     */
    Set<String> getAllTerms() {
        Set<String> terms = new TreeSet<String>();
        for (Topic topic : topics.values())
            topic.addAllTerms(terms);
        return terms;
    }

    public static void main(String[] args) throws Exception {
        // Parse the file in args
        Topics topics = new Topics();
        for (String filename : args) {
            logger.info(String.format("=== Parsing file %s", filename));
            topics.add(new FileInputStream(filename));
        }

        // Output everything
        Output.print(System.out, "\n", topics.getAllTerms());
    }

    @Override
    public Iterator<Topic> iterator() {
        return topics.values().iterator();
    }

    public Map<String, ? extends bpiwowar.ir.query.Topic> getMap() {
        return topics;
    }

    final public int size() {
        return topics.size();
    }
}

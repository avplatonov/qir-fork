package bpiwowar.ir.inex;

import bpiwowar.ir.query.COQuery;
import bpiwowar.ir.query.Query;
import bpiwowar.utils.arrays.ListAdaptator;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * An INEX topic
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class Topic implements bpiwowar.ir.query.Topic {
    /**
     * A CO query
     */
    COQuery coTitle;

    /**
     * The queries for this topic
     */
    public Query castitle;

    private String id;

    final static Logger logger = Logger.getLogger(Topic.class);

    /**
     * Parse an input stream
     *
     * @param is
     * @return The document element
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    static private Element parse(InputStream is)
        throws ParserConfigurationException, SAXException, IOException {
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		factory
//				.setFeature(
//						"http://apache.org/xml/features/nonvalidating/load-external-dtd",
//						false);
//		DocumentBuilder parser = factory.newDocumentBuilder();
//		Document document = parser.parse(is);
//
//		return document.getDocumentElement();
        return null;
    }

    public Topic(InputStream is) throws Exception {
        this(parse(is));
    }

    public Topic(Element root) throws Exception {
//		XPath xpath = XPathFactory.newInstance().newXPath();
//
//		 --- Get the ID
//		id = (String) xpath.evaluate("@id", root, XPathConstants.STRING);
//
//		 --- Get the title
//		String titleString = (String) xpath.evaluate("string(title)", root,
//				XPathConstants.STRING);
//		logger.debug("Parsing <title> %s", titleString);
//		coTitle = (COQuery) new Nexi(new StringReader(titleString)).start();
//		logger.debug("Title is <<<%s>>>", coTitle);
//
//		 --- Get the CAS title
//		String castitleString = (String) xpath.evaluate("string(castitle)",
//				root, XPathConstants.STRING);
//		logger.debug("Parsing <title> %s", castitleString);
//		castitle = new Nexi(new StringReader(castitleString)).start();
//		logger.debug("Cas-Title is <<<%s>>>", castitle);
    }

    public static void main(String[] args) throws Exception {
        // Parse the file in args
        for (String filename : args) {
            logger.info(String.format("=== Parsing file %s", filename));
            final Topic topic = new Topic(new FileInputStream(filename));
            for (String type : topic.getTypes())
                logger.info(String.format("[%s] %s", type, topic.getTopicPart(type)));

        }
    }

    public void addAllTerms(Set<String> terms) {
        coTitle.addTerms(terms);
        castitle.addTerms(terms);
    }

    public String getId() {
        return id;
    }

    @Override
    public Query getTopicPart(String type) {
        if ("CO".equals(type))
            return coTitle;
        else if ("CAS".equals(type))
            return castitle;
        return null;
    }

    @Override
    public List<String> getTypes() {
        return new ListAdaptator<String>(new String[] {"CO", "CAS"});
    }

}

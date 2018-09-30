package bpiwowar.ir.tasks;

import bpiwowar.argparser.Argument;
import bpiwowar.argparser.checkers.IOChecker;
import bpiwowar.experiments.AbstractTask;
import bpiwowar.experiments.TaskDescription;
import bpiwowar.ir.query.QuerySet;
import bpiwowar.ir.query.Topic;
import bpiwowar.ir.trec.TRECTopic;
import bpiwowar.log.Logger;
import bpiwowar.utils.GenericHelper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@TaskDescription(name = "trec-topics-to-xml", project = {"ir", "trec"}, description = "Read a trec topic file and output an XML version of it")
public class TRECTopicToXML extends AbstractTask {
    final static private Logger LOGGER = Logger.getLogger();

    @Argument(name = "topic-file", checkers = IOChecker.ValidFile.class, required = true)
    File topicFile;

    @Argument(name = "merge", help = "Merge with another XML file on topic id", checkers = IOChecker.ValidFile.class)
    File mergeFile;

    @Argument(name = "merge-element", help = "Element for a given topic id (Should contain %s for id substitution)")
    String mergeElementPath = "//topic[@id = %s]";

    @Argument(name = "merge-paths", help = "Sets of paths to merge (relative to the element)")
    Set<String> mergePaths = GenericHelper.newTreeSet();

    @Override
    public int execute() throws Throwable {
        // Read the TREC topics
        QuerySet topics = TRECTopic.readTopics(new BufferedReader(
            new FileReader(topicFile)), false);

        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(
            System.out, "UTF-8");

        XPath xpath = XPathFactory.newInstance().newXPath();
        Document document = null;
        if (mergeFile != null) {
            LOGGER.info("Reading file to merge: %s", mergeFile);
            DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            document = parser.parse(mergeFile);
        }

        writer.writeStartDocument();
        writer.writeStartElement("topics");
        for (Topic topic : topics.queries().values()) {
            TRECTopic trecTopic = (TRECTopic)topic;
            writer.writeStartElement("topic");
            final String id = trecTopic.getId();
            writer.writeAttribute("id", id);

            write(writer, "title", trecTopic.getTitle());

            write(writer, "description", trecTopic.getDescription());
            write(writer, "narrative", trecTopic.getNarrative());
            write(writer, "summary", trecTopic.getSummary());

            // --- Add merge
            if (document != null) {
                // Find the right
                final String xpathExpression = String.format(
                    mergeElementPath, id);
                Node element = (Element)xpath.evaluate(xpathExpression, document.getDocumentElement(),
                    XPathConstants.NODE);
                if (element == null) {
                    LOGGER.info("No topic found for %s", id);
                }
                else {
                    for (String path : mergePaths) {
                        NodeList elements = (NodeList)xpath.evaluate(path,
                            element, XPathConstants.NODESET);
                        for (int i = 0; i < elements.getLength(); i++) {
                            Element item = (Element)elements.item(i);
                            write(writer, item);
                        }
                    }
                }
            }

            writer.writeEndElement();
        }
        writer.writeEndDocument();
        writer.flush();
        return 0;
    }

    private void write(XMLStreamWriter writer, Node node) throws DOMException, XMLStreamException {
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                writer.writeStartElement(node.getNodeName());
                NodeList nodes = node.getChildNodes();
                for (int i = 0; i < nodes.getLength(); i++) {
                    write(writer, nodes.item(i));
                }
                writer.writeEndElement();
                break;

            case Node.TEXT_NODE:
                writer.writeCharacters(node.getNodeValue());
                break;

            default:
                LOGGER.warn("Cannot handle node type %d", node.getNodeType());
        }
    }

    private void write(XMLStreamWriter writer, String name, String text)
        throws XMLStreamException {
        if (text == null || text.isEmpty())
            return;

        writer.writeStartElement(name);
        writer.writeCharacters(text);
        writer.writeEndElement();
    }
}

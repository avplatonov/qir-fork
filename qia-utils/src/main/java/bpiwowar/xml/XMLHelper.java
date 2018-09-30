/*
 * Created on 16-Aug-2004
 *
 */
package bpiwowar.xml;

// IO

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Create XMLReader with given ContentHandler
 *
 * @author Arjen P. de Vries
 * @author Benjamin Piwowarski
 * @version 1.0
 */

public class XMLHelper {

    private static Logger logger = Logger.getLogger(XMLHelper.class);

    /**
     * Initialises XMLReader with default settings
     */
    public static XMLReader createXMLReader() throws SAXException {
        XMLReader xr = XMLReaderFactory.createXMLReader();
        // TODO Q: do we care about validation? should it be optional?
        // Note: IBM jre needs it turned off
        try {
            xr.setFeature("http://xml.org/sax/features/validation", false);
        }
        catch (SAXException e) {
            logger.info("Cannot de-activate validation.");
        }
        // TODO Q: do we really have to turn off namespaces?
        try {
            xr.setFeature("http://xml.org/sax/features/namespaces", false);
        }
        catch (SAXException e) {
            logger.info("Cannot de-activate namespaces.");
        }
        return xr;
    }

    /**
     * Initialises XMLReader with default settings
     */
    public static XMLReader createXMLReader(ContentHandler handler)
        throws SAXException {
        XMLReader xr = createXMLReader();
        xr.setContentHandler(handler);
        return xr;
    }

    /**
     * Initialises a validating XML Reader with default settings
     *
     * @param dtdFile The DTD URL to validate against.
     */
    public static XMLReader createValidatingReader(URL url)
        throws SAXException {
        XMLReader xr = createXMLReader();

        if (url != null) {
            // An INEXArticleReader must support entity-resolver2 interface
            try {
                xr.setFeature(
                    "http://xml.org/sax/features/use-entity-resolver2",
                    true);
            }
            catch (SAXException e) {
                logger
                    .fatal("Parser does not support entity-resolver2 feature.");
                throw new RuntimeException("Cannot create INEXArticleReader");
            }
            FixedDTDResolver r = new FixedDTDResolver(url.toString());
            xr.setEntityResolver(r);
        }
        xr.setFeature("http://xml.org/sax/features/validation", true);

        return xr;
    }

    /**
     * Creates InputSource from a file; sets systemId such that relative URLs are set correctly
     */
    public static InputSource getInputSource(File f) throws IOException {
        InputSource is = new InputSource(new FileInputStream(f));
        is.setSystemId(f.toURI().toString());
        return is;
    }

    public static XMLReader createFISXMLReader() {
        return new com.sun.xml.fastinfoset.sax.SAXDocumentParser();
    }
}

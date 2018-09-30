/*
 * Created on 16-Aug-2004
 *
 */
package bpiwowar.xml;

/**
 * @author Arjen P. de Vries
 *
 *  MyResolver2 pretends, using the EntityResolver2 interface, that
 *    <!DOCTYPE article PUBLIC "-//IEEE-CS//DTD Article//EN">
 *  has been encountered in the article XML document.
 *
 *  Originally, these were to be called from the volume as
 *   <!DOCTYPE books PUBLIC "-//LBIN//DTD IEEE Magazines//EN" "xmlarticle.dtd"
 *     [<!ENTITY C1001 SYSTEM "c1001.xml">]>
 *   <books>...&C1001;...</books>
 */

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class FixedDTDResolver implements EntityResolver2 {

    private static Logger logger = Logger.getLogger(FixedDTDResolver.class);
    private static String ourBaseURI = null;

    public FixedDTDResolver(String baseURI) {
        super();
        ourBaseURI = baseURI;
    }

    public InputSource getExternalSubset(java.lang.String name, java.lang.String baseURI)
        throws SAXException, java.io.IOException {
        String theDTD = ourBaseURI;
        InputSource s = new InputSource(theDTD);
        s.setSystemId(theDTD);
        return s;
    }

    public InputSource resolveEntity(java.lang.String publicId, java.lang.String systemId)
        throws SAXException, java.io.IOException {

        if (logger.isDebugEnabled())
            logger.debug("resolveEntity (EntityResolver)\npublicId = " + publicId + " systemId = " + systemId + "\n");
        return null;
    }

    public InputSource resolveEntity(java.lang.String name, java.lang.String publicId, java.lang.String baseURI,
        java.lang.String systemId)
        throws SAXException, java.io.IOException {
        if (baseURI != null && baseURI.equals("http://collection.inex.org/")) {
            logger.debug("HELP");
        }
        return this.resolveEntity(publicId, systemId);
    }

}

package bpiwowar.xml;

import java.io.IOException;
import java.io.Reader;
import org.w3c.dom.Document;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 * From an XML document, get a stream of text (i.e. the text without the tags)
 *
 * @author Benjamin Piwowarski <benjamin@bpiwowar.net>
 */
public class DocumentInputStream extends Reader {
    private NodeIterator iterator;
    private char[] content;
    private int contentOffset;

    public DocumentInputStream(Document document) {
        DocumentTraversal traversable = (DocumentTraversal)document;
        iterator = traversable.createNodeIterator(document,
            NodeFilter.SHOW_TEXT, null, true);

    }

    @Override
    public void close() throws IOException {
        iterator = null;
        content = null;
    }

    /**
     * Read characters into a portion of an array. This method will block until some input is available, an I/O error
     * occurs, or the end of the stream is reached.
     *
     * @param cbuf Destination buffer
     * @param off Offset at which to start storing characters
     * @param len Maximum number of characters to read
     * @return The number of characters read, or -1 if the end of the stream has been reached
     * @throws IOException If an I/O error occurs
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        int read = 0;

        // EOF
        if (iterator == null)
            return -1;

        // While we still have something to read, and we need to read
        while (read < len && (content == null && iterator != null)) {
            // Get a string into the current buffer
            if (content == null) {
                Text text = (Text)iterator.nextNode();
                if (text == null) {
                    iterator = null;
                    break;
                }
                content = text.getWholeText().toCharArray();
                contentOffset = 0;
            }

            // Fill up the array
            while (contentOffset < content.length && read < len) {
                cbuf[off + read] = content[contentOffset];
                read++;
                contentOffset++;
            }

            // Set content to null if we read everything
            if (contentOffset == content.length)
                content = null;
        }

        if (read == 0)
            return -1;
        return read;
    }

}

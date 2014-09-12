package it.unitn.disi.smatch.loaders.context.handlers;

import it.unitn.disi.smatch.data.trees.IBaseContext;
import it.unitn.disi.smatch.data.trees.IBaseNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Base content handler class for XML loaders.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public abstract class BaseXMLContentHandler<E extends IBaseContext<? extends IBaseNode>> extends DefaultHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseXMLContentHandler.class);

    protected int nodesParsed;

    // variables used in parsing
    // context being loaded
    protected E ctx;
    // to collect all content in case parser processes element content in several passes
    protected StringBuilder content;

    protected final boolean uniqueStrings;
    protected final Map<String, String> unique = new HashMap<>();

    public BaseXMLContentHandler() {
        this(false);
    }

    /**
     * @param uniqueStrings whether to make node names unique.
     */
    public BaseXMLContentHandler(boolean uniqueStrings) {
        this.uniqueStrings = uniqueStrings;
    }

    /**
     * Whether to make node names unique.
     * Saves memory in case of contexts with highly repetitive node names.
     *
     * @return true if to make node names unique.
     */
    public boolean isUniqueStrings() {
        return uniqueStrings;
    }

    public E getContext() {
        return ctx;
    }

    //org.xml.sax.ContentHandler methods re-implementation start
    @SuppressWarnings("unchecked")
    public void startDocument() {
        unique.clear();
        nodesParsed = 0;
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    public void endDocument() {
        log.info("Parsed nodes: " + nodesParsed);
        unique.clear();
    }

    //org.xml.sax.ContentHandler methods re-implementation end

    protected String makeUnique(String s) {
        if (uniqueStrings) {
            String result = unique.get(s);
            if (null == result) {
                unique.put(s, s);
                result = s;
            }
            return result;
        } else {
            return s;
        }
    }
}
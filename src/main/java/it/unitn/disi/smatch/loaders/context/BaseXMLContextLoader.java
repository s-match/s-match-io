package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.data.trees.IBaseContext;
import it.unitn.disi.smatch.data.trees.IBaseNode;
import it.unitn.disi.smatch.loaders.ILoader;
import it.unitn.disi.smatch.loaders.ParseInterruptedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for XML loaders. Works as a loader and a content handler for XML parser.
 * When instantiated as loader, the class uses a copy of itself as a content handler for thread safety.
 * When instantiated as a task (null != this.location), the class uses itself as a content handler,
 * because task is inherently single threaded and single use.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public abstract class BaseXMLContextLoader<E extends IBaseContext<T>, T extends IBaseNode> extends BaseFileContextLoader<E, T>
        implements IBaseContextLoader<E, T>, ContentHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseXMLContextLoader.class);

    private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    protected final XMLReader parser;
    protected final boolean uniqueStrings;

    // content handler variables
    // variables used in parsing
    // context being loaded
    protected E ctx;
    // to collect all content in case parser processes element content in several passes
    protected StringBuilder content;
    protected final Map<String, String> unique = new HashMap<>();

    public BaseXMLContextLoader() {
        this(false);
    }

    /**
     * @param uniqueStrings whether to make node names unique.
     */
    public BaseXMLContextLoader(boolean uniqueStrings) {
        this.uniqueStrings = uniqueStrings;
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setProperty("http://apache.org/xml/properties/input-buffer-size", 8196);
        } catch (SAXException e) {
            throw new IllegalStateException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * @param uniqueStrings whether to make node names unique.
     */
    public BaseXMLContextLoader(boolean uniqueStrings, String location) {
        super(location);
        this.uniqueStrings = uniqueStrings;
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setProperty("http://apache.org/xml/properties/input-buffer-size", 8196);
        } catch (SAXException e) {
            throw new IllegalStateException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
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

    @Override
    protected void createIds(E result) {
        //ids should be already in XML
    }

    @Override
    protected E process(BufferedReader input) throws IOException, ContextLoaderException {
        try {
            InputSource is = new InputSource(input);
            parser.setContentHandler(this);
            parser.parse(is);
        } catch (ParseInterruptedException e) {
            ctx = null;
        } catch (SAXException | FileNotFoundException | UnsupportedEncodingException e) {
            throw new ContextLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
        return ctx;
    }

    public String getDescription() {
        return ILoader.XML_FILES;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
        unique.clear();
        setProgress(0);
    }

    @Override
    public void endDocument() throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
        log.info("Parsed nodes: " + getProgress());
        unique.clear();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
        content.append(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
    }

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
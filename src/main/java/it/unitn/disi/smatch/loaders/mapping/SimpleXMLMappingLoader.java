package it.unitn.disi.smatch.loaders.mapping;

import it.unitn.disi.smatch.async.AsyncTask;
import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.mappings.IMappingFactory;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
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
import java.util.Iterator;

/**
 * Loads mappings in SimpleXML format as rendered by SimpleXMLMappingRenderer.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLMappingLoader extends BaseFileMappingLoader implements IAsyncMappingLoader, ContentHandler {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLMappingLoader.class);

    private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    private final XMLReader parser;
    private IContextMapping<INode> mapping;
    // hashes node id -> node
    private HashMap<String, INode> sNodes;
    private HashMap<String, INode> tNodes;

    public SimpleXMLMappingLoader(IMappingFactory mappingFactory) {
        super(mappingFactory);
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setProperty("http://apache.org/xml/properties/input-buffer-size", 8196);
        } catch (SAXException e) {
            throw new IllegalStateException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public SimpleXMLMappingLoader(IMappingFactory mappingFactory, IContext source, IContext target, String fileName) {
        super(mappingFactory, source, target, fileName);
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setProperty("http://apache.org/xml/properties/input-buffer-size", 8196);
        } catch (SAXException e) {
            throw new IllegalStateException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public AsyncTask<IContextMapping<INode>, IMappingElement<INode>> asyncLoad(IContext source, IContext target, String fileName) {
        return new SimpleXMLMappingLoader(mappingFactory, source, target, fileName);
    }

    @Override
    protected IContextMapping<INode> process(IContext source, IContext target, BufferedReader reader) throws IOException, MappingLoaderException {
        mapping = mappingFactory.getContextMappingInstance(source, target);
        try {
            InputSource is = new InputSource(reader);
            parser.setContentHandler(this);
            parser.parse(is);
        } catch (ParseInterruptedException e) {
            mapping = null;
        } catch (SAXException | FileNotFoundException | UnsupportedEncodingException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
        return mapping;
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
        setProgress(0);
        sNodes = createHash(source);
        tNodes = createHash(target);
    }

    @Override
    public void endDocument() throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            throw new ParseInterruptedException();
        }
        log.info("Parsed nodes: " + getProgress());
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
        if ("mapping".equals(localName)) {
            mapping.setSimilarity(Double.parseDouble(atts.getValue("similarity")));
        } else if ("link".equals(localName)) {
            final String sourceId = atts.getValue("source-id");
            INode source = sNodes.get(sourceId);
            final String targetId = atts.getValue("target-id");
            INode target = tNodes.get(targetId);
            char rel = atts.getValue("relation").charAt(0);
            if ((null != source) && (null != target)) {
                mapping.setRelation(source, target, rel);
                progress();
            } else {
                if (log.isWarnEnabled()) {
                    if (null == source) {
                        log.warn("Could not find source node for id: " + sourceId);
                    }
                    if (null == target) {
                        log.warn("Could not find target node for id: " + targetId);
                    }
                }
            }

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

    /**
     * Creates hash map for nodes which contains path from root to node for each node.
     *
     * @param context a context
     * @return a hash table which contains path from root to node for each node
     */
    private HashMap<String, INode> createHash(IContext context) {
        HashMap<String, INode> result = new HashMap<>();

        int nodeCount = 0;
        for (Iterator<INode> i = context.nodeIterator(); i.hasNext(); ) {
            INode node = i.next();
            result.put(node.nodeData().getId(), node);
            nodeCount++;
        }

        if (log.isInfoEnabled()) {
            log.info("Created hash for " + nodeCount + " nodes...");
        }

        return result;
    }
}
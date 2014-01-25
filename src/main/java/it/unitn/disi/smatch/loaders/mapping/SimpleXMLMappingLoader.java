package it.unitn.disi.smatch.loaders.mapping;

import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.loaders.ILoader;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Loads mappings in SimpleXML format as rendered by SimpleXMLMappingRenderer.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLMappingLoader extends BaseFileMappingLoader implements ContentHandler {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLMappingLoader.class);

    private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
    private XMLReader parser;

    // hashes node id -> node
    private HashMap<String, INode> sNodes;
    private HashMap<String, INode> tNodes;

    private IContextMapping<INode> mapping;

    public SimpleXMLMappingLoader() throws MappingLoaderException {
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setContentHandler(this);
            parser.setProperty("http://apache.org/xml/properties/input-buffer-size", 8196);
        } catch (SAXException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    protected void process(IContextMapping<INode> mapping, IContext source, IContext target, BufferedReader reader) throws IOException, MappingLoaderException {
        sNodes = createHash(source);
        tNodes = createHash(target);
        this.mapping = mapping;

        try {
            InputSource is = new InputSource(reader);
            parser.parse(is);
        } catch (SAXException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } finally {
            sNodes.clear();
            tNodes.clear();
        }
    }

    /**
     * Creates hash map for nodes which contains path from root to node for each node.
     *
     * @param context a context
     * @return a hash table which contains path from root to node for each node
     */
    protected HashMap<String, INode> createHash(IContext context) {
        HashMap<String, INode> result = new HashMap<String, INode>();

        int nodeCount = 0;
        for (INode node : context.getNodesList()) {
            result.put(node.getNodeData().getId(), node);
            nodeCount++;
        }

        if (log.isInfoEnabled()) {
            log.info("Created hash for " + nodeCount + " nodes...");
        }

        return result;
    }

    //org.xml.sax.ContentHandler methods re-implementation start

    public void startDocument() {
        //nop
    }

    public void startElement(String namespace, String localName, String qName, Attributes atts) {
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
                countRelation(rel);
                reportProgress();
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

    public void endElement(String uri, String localName, String qName) {
        //nop
    }

    public void characters(char[] ch, int start, int length) {
        //nop
    }

    public void setDocumentLocator(Locator locator) {
        //nop
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        //nop
    }

    public void processingInstruction(String target, String data) throws SAXException {
        //nop
    }

    public void skippedEntity(String name) throws SAXException {
        //nop
    }

    public void endDocument() {
        //nop
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        //nop
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        //nop
    }

    //org.xml.sax.ContentHandler methods re-implementation end

    public String getDescription() {
        return ILoader.XML_FILES;
    }
}


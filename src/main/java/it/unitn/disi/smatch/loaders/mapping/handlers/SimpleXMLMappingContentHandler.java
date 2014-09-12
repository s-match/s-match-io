package it.unitn.disi.smatch.loaders.mapping.handlers;

import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.data.util.MappingProgressContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashMap;

/**
 * Content handler class for SimpleXML mapping loader.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLMappingContentHandler extends DefaultHandler {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLMappingContentHandler.class);

    // hashes node id -> node
    private final HashMap<String, INode> sNodes;
    private final HashMap<String, INode> tNodes;

    private final IContextMapping<INode> mapping;

    private final MappingProgressContainer progressContainer;

    public SimpleXMLMappingContentHandler(IContextMapping<INode> mapping, IContext source, IContext target, MappingProgressContainer progressContainer) {
        this.mapping = mapping;
        this.progressContainer = progressContainer;
        sNodes = createHash(source);
        tNodes = createHash(target);
    }

    //DefaultHandler methods re-implementation start

    @Override
    public void startDocument() {
    }

    @Override
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
                progressContainer.countRelation(rel);
                progressContainer.progress();
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

    //DefaultHandler methods re-implementation end

    /**
     * Creates hash map for nodes which contains path from root to node for each node.
     *
     * @param context a context
     * @return a hash table which contains path from root to node for each node
     */
    protected HashMap<String, INode> createHash(IContext context) {
        HashMap<String, INode> result = new HashMap<>();

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
}
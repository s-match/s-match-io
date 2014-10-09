package it.unitn.disi.smatch.renderers.context;

import it.unitn.disi.smatch.async.AsyncTask;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.data.trees.INodeData;
import it.unitn.disi.smatch.data.trees.Node;
import it.unitn.disi.smatch.loaders.ILoader;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Renders a context into an OWL file. Created for OAEI webdirs track export, therefore takes into account
 * features of AlignAPI, like putting # before each class and not at the base.
 * <p/>
 * Needs parameters:
 * <p/>
 * datasetURI which will be used as a base for the ontology.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class OWLContextRenderer extends BaseXMLContextRenderer<IContext, INode> implements IAsyncBaseContextRenderer<IContext, INode> {

    private final String datasetURI;

    public OWLContextRenderer(String datasetURI) {
        super();
        this.datasetURI = datasetURI;
    }

    public OWLContextRenderer(String datasetURI, boolean sort) {
        super(sort);
        this.datasetURI = datasetURI;
    }

    public OWLContextRenderer(String datasetURI, String location, IContext context) {
        super(location, context);
        this.datasetURI = datasetURI;
    }

    public OWLContextRenderer(String datasetURI, String location, IContext context, boolean sort) {
        super(location, context, sort);
        this.datasetURI = datasetURI;
    }

    @Override
    protected void process(IContext context, BufferedWriter out) throws IOException, ContextRendererException {
        try {
            StreamResult streamResult = new StreamResult(out);
            SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler hd = tf.newTransformerHandler();
            Transformer serializer = hd.getTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            hd.setResult(streamResult);
            hd.startDocument();
            hd.startPrefixMapping("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            hd.endPrefixMapping("rdf");
            hd.startPrefixMapping("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
            hd.endPrefixMapping("rdfs");
            hd.startPrefixMapping("owl", "http://www.w3.org/2002/07/owl#");
            hd.endPrefixMapping("owl");
            hd.startPrefixMapping("dc", "http://purl.org/dc/elements/1.1/");
            hd.endPrefixMapping("dc");
            final String base = datasetURI;
            hd.startPrefixMapping("", base);
            hd.endPrefixMapping("");

            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "", "xml:base", "CDATA", base);
            atts.addAttribute("", "", "xml:lang", "CDATA", "en");
            hd.startElement("", "", "rdf:RDF", atts);

            String comment = "Class names are numeric, because it happens in classifications that names repeat";
            hd.comment(comment.toCharArray(), 0, comment.length());
            comment = "Therefore the classes are made unique and their names are put into the human-readable labels";
            hd.comment(comment.toCharArray(), 0, comment.length());

            atts = new AttributesImpl();
            atts.addAttribute("", "", "rdf:about", "CDATA", "");
            hd.startElement("", "", "owl:Ontology", atts);
            renderString(hd, new AttributesImpl(), "dc:creator", "S-Match");
            hd.endElement("", "", "owl:Ontology");

            renderNode(hd, context.getRoot());

            hd.endElement("", "", "rdf:RDF");
            hd.endDocument();
        } catch (SAXException | TransformerConfigurationException e) {
            throw new ContextRendererException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private void renderNode(TransformerHandler hd, INode curNode) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        // render current node
        INodeData curNodeData = curNode.nodeData();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", "rdf:about", "CDATA", "#" + curNodeData.getId());
        hd.startElement("", "", "owl:Class", atts);
        renderString(hd, atts, "rdfs:label", curNodeData.getName());
        if (curNode.hasParent()) {
            atts = new AttributesImpl();
            atts.addAttribute("", "", "rdf:resource", "CDATA", "#" + curNode.getParent().nodeData().getId());
            hd.startElement("", "", "rdfs:subClassOf", atts);
            hd.endElement("", "", "rdfs:subClassOf");
        }
        hd.endElement("", "", "owl:Class");

        // render children
        if (0 < curNode.getChildCount()) {
            Iterator<INode> children;
            if (sort) {
                ArrayList<INode> childrenList = new ArrayList<>(curNode.getChildren());
                Collections.sort(childrenList, Node.NODE_NAME_COMPARATOR);
                children = childrenList.iterator();
            } else {
                children = curNode.childrenIterator();
            }
            while (children.hasNext()) {
                renderNode(hd, children.next());
            }
        }

        progress();
    }

    public String getDescription() {
        return ILoader.OWL_FILES;
    }

    @Override
    public AsyncTask<Void, INode> asyncRender(IContext context, String location) {
        return new OWLContextRenderer(datasetURI, location, context, sort);
    }
}
package it.unitn.disi.smatch.renderers.context;

import it.unitn.disi.smatch.data.trees.IBaseContext;
import it.unitn.disi.smatch.data.trees.IBaseNode;
import it.unitn.disi.smatch.data.trees.IBaseNodeData;
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
 * Base renderer for XML.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public abstract class BaseXMLContextRenderer<E extends IBaseContext<T>, T extends IBaseNode> extends BaseFileContextRenderer<E, T> implements IBaseContextRenderer<E, T> {

    protected BaseXMLContextRenderer() {
        super();
    }

    protected BaseXMLContextRenderer(boolean sort) {
        super(sort);
    }

    protected BaseXMLContextRenderer(String location, E context) {
        super(location, context);
    }

    protected BaseXMLContextRenderer(String location, E context, boolean sort) {
        super(location, context, sort);
    }

    protected static void renderString(TransformerHandler hd, AttributesImpl atts, final String tagName, final String tagValue) throws SAXException {
        if (null != tagValue && 0 < tagValue.length()) {
            hd.startElement("", "", tagName, atts);
            hd.characters(tagValue.toCharArray(), 0, tagValue.length());
            hd.endElement("", "", tagName);
        }
    }

    protected static void renderAttribute(AttributesImpl atts, String tag, String text) {
        if (null != text) {
            atts.addAttribute("", "", tag, "CDATA", text);
        }
    }

    protected void process(E context, BufferedWriter out) throws IOException, ContextRendererException {
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
            hd.startElement("", "", "context", new AttributesImpl());

            if (null == context.getRoot()) {
                throw new ContextRendererException("Cannot render context without root node");
            }

            renderNode(hd, context.getRoot());

            hd.endElement("", "", "context");
            hd.endDocument();
        } catch (SAXException | TransformerConfigurationException e) {
            throw new ContextRendererException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void renderNode(TransformerHandler hd, IBaseNode curNode) throws SAXException {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        // render current node
        IBaseNodeData curNodeData = curNode.nodeData();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", "id", "CDATA", curNodeData.getId());
        if (curNode.hasParent()) {
            atts.addAttribute("", "", "parent-id", "CDATA", curNode.getParent().nodeData().getId());
        }
        renderNodeAttributes(curNode, atts);
        hd.startElement("", "", "node", atts);

        renderString(hd, new AttributesImpl(), "name", curNodeData.getName());

        renderNodeContents(curNode, hd);

        if (0 < curNode.getChildCount()) {
            hd.startElement("", "", "children", new AttributesImpl());
            Iterator<IBaseNode> children;
            if (sort) {
                ArrayList<IBaseNode> childrenList = new ArrayList<>(curNode.getChildren());
                Collections.sort(childrenList, Node.NODE_NAME_COMPARATOR);
                children = childrenList.iterator();
            } else {
                children = curNode.childrenIterator();
            }
            while (children.hasNext()) {
                renderNode(hd, children.next());
            }
            hd.endElement("", "", "children");
        }

        hd.endElement("", "", "node");
        progress();
    }

    protected void renderNodeContents(IBaseNode curNode, TransformerHandler hd) throws SAXException {
    }

    protected void renderNodeAttributes(IBaseNode curNode, AttributesImpl atts) {
    }

    public String getDescription() {
        return ILoader.XML_FILES;
    }
}

package it.unitn.disi.smatch.renderers.context;

import it.unitn.disi.smatch.async.AsyncTask;
import it.unitn.disi.smatch.data.ling.IAtomicConceptOfLabel;
import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.trees.IBaseNode;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.data.trees.INodeData;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.TransformerHandler;

/**
 * Renders a context into an XML file.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLContextRenderer extends BaseXMLContextRenderer<IContext, INode> implements IContextRenderer, IAsyncContextRenderer {

    private final static String preprocessedFlag = Boolean.toString(true);

    public SimpleXMLContextRenderer() {
        super();
    }

    public SimpleXMLContextRenderer(boolean sort) {
        super(sort);
    }

    public SimpleXMLContextRenderer(String location, IContext context) {
        super(location, context);
    }

    public SimpleXMLContextRenderer(String location, IContext context, boolean sort) {
        super(location, context, sort);
    }

    protected void renderNodeAttributes(IBaseNode curNode, AttributesImpl atts) {
        INodeData curNodeData = ((INode) curNode).nodeData();
        if (curNodeData.getIsPreprocessed()) {
            atts.addAttribute("", "", "preprocessed", "CDATA", preprocessedFlag);
        }
    }

    protected void renderNodeContents(IBaseNode curNode, TransformerHandler hd) throws SAXException {
        INodeData curNodeData = ((INode) curNode).nodeData();
        renderString(hd, new AttributesImpl(), "label-formula", curNodeData.getLabelFormula());
        renderString(hd, new AttributesImpl(), "node-formula", curNodeData.getNodeFormula());
        renderString(hd, new AttributesImpl(), "provenance", curNodeData.getProvenance());

        // senses
        if (!curNodeData.getConcepts().isEmpty()) {
            hd.startElement("", "", "tokens", new AttributesImpl());
            for (IAtomicConceptOfLabel acol : curNodeData.getConcepts()) {
                AttributesImpl atts = new AttributesImpl();
                atts.addAttribute("", "", "id", "CDATA", Integer.toString(acol.getId()));
                hd.startElement("", "", "token", atts);

                renderString(hd, new AttributesImpl(), "text", acol.getToken());
                renderString(hd, new AttributesImpl(), "lemma", acol.getLemma());

                hd.startElement("", "", "senses", new AttributesImpl());
                for (ISense sense : acol.getSenses()) {
                    atts = new AttributesImpl();
                    atts.addAttribute("", "", "id", "CDATA", sense.getId());
                    hd.startElement("", "", "sense", atts);
                    hd.endElement("", "", "sense");
                }
                hd.endElement("", "", "senses");

                hd.endElement("", "", "token");
            }
            hd.endElement("", "", "tokens");
        }
    }

    @Override
    public AsyncTask<Void, INode> asyncRender(IContext context, String location) {
        return new SimpleXMLContextRenderer(location, context, sort);
    }
}
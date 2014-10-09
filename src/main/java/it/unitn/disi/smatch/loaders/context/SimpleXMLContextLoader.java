package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.async.AsyncTask;
import it.unitn.disi.smatch.data.ling.IAtomicConceptOfLabel;
import it.unitn.disi.smatch.data.trees.Context;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Loader for SimpleXML format.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLContextLoader extends BaseXMLContextLoader<IContext, INode> implements IContextLoader, IAsyncContextLoader {

    protected final ILinguisticOracle oracle;

    // content handler vars
    // path to the root node
    protected final Deque<INode> pathToRoot = new ArrayDeque<>();

    // atomic concept being read
    private IAtomicConceptOfLabel acol;

    public SimpleXMLContextLoader(ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super();
        this.oracle = linguisticOracle;
    }

    public SimpleXMLContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle) {
        super(uniqueStrings);
        this.oracle = linguisticOracle;
    }

    public SimpleXMLContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle, String location) {
        super(uniqueStrings, location);
        this.oracle = linguisticOracle;
    }

    @Override
    public AsyncTask<IContext, INode> asyncLoad(String location) {
        return new SimpleXMLContextLoader(isUniqueStrings(), oracle, location);
    }

    // content handler methods
    @Override
    public void startDocument() throws SAXException {
        super.startDocument();
        ctx = new Context();
        pathToRoot.clear();
    }

    @Override
    public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(namespace, localName, qName, atts);
        switch (localName) {
            case "node":
                INode node;
                if (null == ctx.getRoot()) {
                    node = ctx.createRoot();
                } else {
                    if (0 < pathToRoot.size()) {
                        node = pathToRoot.getLast().createChild();
                    } else {
                        // looks like there are multiple roots
                        INode oldRoot = ctx.getRoot();
                        INode newRoot = ctx.createRoot("Top");
                        newRoot.addChild(oldRoot);
                        node = newRoot.createChild();
                    }
                }
                node.nodeData().setId(atts.getValue("id"));
                node.nodeData().setIsPreprocessed(-1 < atts.getIndex("", "preprocessed"));
                pathToRoot.addLast(node);
                break;
            case "token":
                acol = pathToRoot.getLast().nodeData().createConcept();
                acol.setId(Integer.parseInt(atts.getValue("id")));
                break;
            case "sense":
                if (null != oracle) {
                    try {
                        if (-1 == atts.getIndex("pos")) {
                                acol.getSenses().add(oracle.createSense(atts.getValue("id")));
                        } else {
                                acol.getSenses().add(oracle.createSense(atts.getValue("pos") + "#" + atts.getValue("id")));
                        }
                    } catch (LinguisticOracleException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
                break;
            default:
                content = new StringBuilder();
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        switch (localName) {
            case "name":
                pathToRoot.getLast().nodeData().setName(makeUnique(content.toString()));
                break;
            case "label-formula":
                pathToRoot.getLast().nodeData().setLabelFormula(content.toString());
                break;
            case "node-formula":
                pathToRoot.getLast().nodeData().setNodeFormula(content.toString());
                break;
            case "provenance":
                pathToRoot.getLast().nodeData().setProvenance(content.toString());
                break;
            case "text":
                acol.setToken(makeUnique(content.toString()));
                break;
            case "lemma":
                acol.setLemma(makeUnique(content.toString()));
                break;
            case "token":
                pathToRoot.getLast().nodeData().getConcepts().add(acol);
                break;
            case "node":
                pathToRoot.removeLast();

                progress();
                break;
        }
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        pathToRoot.clear();
    }
}
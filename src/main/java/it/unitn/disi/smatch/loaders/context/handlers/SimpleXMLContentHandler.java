package it.unitn.disi.smatch.loaders.context.handlers;

import it.unitn.disi.smatch.data.ling.IAtomicConceptOfLabel;
import it.unitn.disi.smatch.data.trees.Context;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Content handler class for SimpleXML loader.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLContentHandler extends BaseXMLContentHandler<IContext> {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLContentHandler.class);

    private final ILinguisticOracle oracle;

    // path to the root node
    protected final Deque<INode> pathToRoot = new ArrayDeque<>();

    // atomic concept being read
    private IAtomicConceptOfLabel acol;

    public SimpleXMLContentHandler(ILinguisticOracle oracle) {
        super();
        this.oracle = oracle;
    }

    public SimpleXMLContentHandler(ILinguisticOracle oracle, boolean uniqueStrings) {
        super(uniqueStrings);
        this.oracle = oracle;
    }

    @Override
    public void startDocument() {
        super.startDocument();
        ctx = new Context();
        pathToRoot.clear();
    }

    @Override
    public void startElement(String namespace, String localName, String qName, Attributes atts) {
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
                node.getNodeData().setId(atts.getValue("id"));
                node.getNodeData().setIsPreprocessed(-1 < atts.getIndex("", "preprocessed"));
                pathToRoot.addLast(node);
                break;
            case "token":
                acol = pathToRoot.getLast().getNodeData().createACoL();
                acol.setId(Integer.parseInt(atts.getValue("id")));
                break;
            case "sense":
                if (null != oracle) {
                    if (-1 == atts.getIndex("pos")) {
                        try {
                            acol.addSense(oracle.createSense(atts.getValue("id")));
                        } catch (LinguisticOracleException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    } else {
                        try {
                            acol.addSense(oracle.createSense(atts.getValue("pos") + "#" + atts.getValue("id")));
                        } catch (LinguisticOracleException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                }
                break;
            default:
                content = new StringBuilder();
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (localName) {
            case "name":
                pathToRoot.getLast().getNodeData().setName(makeUnique(content.toString()));
                break;
            case "label-formula":
                pathToRoot.getLast().getNodeData().setcLabFormula(content.toString());
                break;
            case "node-formula":
                pathToRoot.getLast().getNodeData().setcNodeFormula(content.toString());
                break;
            case "provenance":
                pathToRoot.getLast().getNodeData().setProvenance(content.toString());
                break;
            case "text":
                acol.setToken(makeUnique(content.toString()));
                break;
            case "lemma":
                acol.setLemma(makeUnique(content.toString()));
                break;
            case "token":
                pathToRoot.getLast().getNodeData().addACoL(acol);
                break;
            case "node":
                pathToRoot.removeLast();

                nodesParsed++;
                if (0 == (nodesParsed % 1000)) {
                    log.info("nodes parsed: " + nodesParsed);
                }
                break;
        }
    }

    @Override
    public void endDocument() {
        super.endDocument();
        pathToRoot.clear();
    }
}
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

import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Content handler class for CTXML loader.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class CTXMLContentHandler extends BaseXMLContentHandler<IContext> {

    private static final Logger log = LoggerFactory.getLogger(CTXMLContentHandler.class);

    //default name of the base node
    private static final String BASE_NODE = "ctxBaseNode$c0";

    private final ILinguisticOracle oracle;

    private INode node;
    private IAtomicConceptOfLabel sense;

    // node unique name -> node
    private HashMap<String, INode> nodes;

    public CTXMLContentHandler(ILinguisticOracle oracle) {
        super();
        this.oracle = oracle;
    }

    public CTXMLContentHandler(boolean uniqueStrings, ILinguisticOracle oracle) {
        super(uniqueStrings);
        this.oracle = oracle;
    }

    //org.xml.sax.ContentHandler begin

    public void startDocument() {
        ctx = new Context();
        nodes = new HashMap<>();
        nodesParsed = 0;
    }

    public void startElement(String namespace, String localName, String qName, Attributes atts) {
        content = new StringBuilder();

        switch (localName) {
            case "complexType-Concept":
                String nodeName = atts.getValue("name");
                if (!nodeName.equals(BASE_NODE)) {
                    INode n = nodes.get(nodeName);
                    if (null == n) {
                        node = ctx.createNode();
                        setNodeUniqueName(node, nodeName);
                        nodes.put(nodeName, node);

                        nodesParsed++;
                        if (nodesParsed % 10000 == 0) {
                            log.info("elements parsed: " + nodesParsed);
                        }
                    }
                }
                break;
            case "sense":
                sense = node.getNodeData().createACoL();
                break;
            case "extension":
                String parentName = atts.getValue("base");
                INode parentNode = nodes.get(parentName);
                if (null == parentNode) {
                    parentNode = ctx.createNode();
                    setNodeUniqueName(parentNode, parentName);
                    nodes.put(parentName, parentNode);
                }
                parentNode.addChild(node);
                break;
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (0 < content.length()) {
            switch (localName) {
                case "logicalFormulaRepresentation":
                    node.getNodeData().setcNodeFormula(content.toString().trim());
                    break;
                case "cLabFormula":
                    node.getNodeData().setcLabFormula(content.toString().trim());
                    break;
                case "idToken":
                    sense.setId(Integer.parseInt(content.toString()));
                    break;
                case "token":
                    sense.setToken(content.toString());
                    break;
                case "lemma":
                    sense.setLemma(content.toString());
                    break;
                case "wSenses":
                    if (-1 < content.indexOf("#") && null != oracle) {
                        String[] senses = content.toString().trim().split(" ");
                        for (String s : senses) {
                            try {
                                sense.addSense(oracle.createSense(s));
                            } catch (LinguisticOracleException e) {
                                throw new RuntimeException(e.getMessage(), e);
                            }
                        }
                    }
                    break;
            }
            content = new StringBuilder();
        }
        if ("sense".equals(localName)) {
            node.getNodeData().addACoL(sense);
        }
    }

    public void endDocument() {
        super.endDocument();
        log.debug("Finding root...");
        INode root = findRoot();
        ctx.setRoot(root);
        nodes.clear();
    }

    //org.xml.sax.ContentHandler end

    private INode findRoot() {
        for (INode node : nodes.values()) {
            if (!node.hasParent() && !BASE_NODE.equals(node.getNodeData().getName())) {
                return node;
            }
        }
        return null;
    }

    private void setNodeUniqueName(INode node, String nodeName) {
        StringTokenizer idName = new StringTokenizer(nodeName, "$");
        node.getNodeData().setName(idName.nextToken());
        node.getNodeData().setId(idName.nextToken());
    }
}

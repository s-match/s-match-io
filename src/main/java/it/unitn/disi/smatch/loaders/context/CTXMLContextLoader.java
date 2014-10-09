package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.async.AsyncTask;
import it.unitn.disi.smatch.data.ling.IAtomicConceptOfLabel;
import it.unitn.disi.smatch.data.trees.Context;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * Loader for old CTXML format, remains for compatibility.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 * @author Mikalai Yatskevich mikalai.yatskevich@comlab.ox.ac.uk
 */
public class CTXMLContextLoader extends BaseXMLContextLoader<IContext, INode> implements IContextLoader, IAsyncContextLoader {

    private static final Logger log = LoggerFactory.getLogger(CTXMLContextLoader.class);

    private final ILinguisticOracle oracle;

    //content handler variables
    //default name of the base node
    private static final String BASE_NODE = "ctxBaseNode$c0";

    private INode node;
    private IAtomicConceptOfLabel sense;

    // node unique name -> node
    private HashMap<String, INode> nodes;

    public CTXMLContextLoader(ILinguisticOracle linguisticOracle) {
        super();
        this.oracle = linguisticOracle;
    }

    public CTXMLContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle) {
        super(uniqueStrings);
        this.oracle = linguisticOracle;
    }

    public CTXMLContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle, String location) {
        super(uniqueStrings, location);
        this.oracle = linguisticOracle;
    }

    @Override
    public AsyncTask<IContext, INode> asyncLoad(String location) {
        return new CTXMLContextLoader(isUniqueStrings(), oracle, location);
    }

    // content handler methods
    public void startDocument() throws SAXException {
        super.startDocument();
        ctx = new Context();
        nodes = new HashMap<>();
    }

    public void startElement(String namespace, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(namespace, localName, qName, atts);
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

                        progress();
                    }
                }
                break;
            case "sense":
                sense = node.nodeData().createConcept();
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

    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (0 < content.length()) {
            switch (localName) {
                case "logicalFormulaRepresentation":
                    node.nodeData().setNodeFormula(content.toString().trim());
                    break;
                case "cLabFormula":
                    node.nodeData().setLabelFormula(content.toString().trim());
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
                        try {
                            for (String s : senses) {
                                    sense.getSenses().add(oracle.createSense(s));
                            }
                        } catch (LinguisticOracleException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    }
                    break;
            }
            content = new StringBuilder();
        }
        if ("sense".equals(localName)) {
            node.nodeData().getConcepts().add(sense);
        }
    }

    public void endDocument() throws SAXException {
        super.endDocument();
        log.debug("Finding root...");
        INode root = findRoot();
        ctx.setRoot(root);
        nodes.clear();
    }

    //content handler end

    private INode findRoot() {
        for (INode node : nodes.values()) {
            if (!node.hasParent() && !BASE_NODE.equals(node.nodeData().getName())) {
                return node;
            }
        }
        return null;
    }

    private void setNodeUniqueName(INode node, String nodeName) {
        StringTokenizer idName = new StringTokenizer(nodeName, "$");
        node.nodeData().setName(idName.nextToken());
        node.nodeData().setId(idName.nextToken());
    }
}

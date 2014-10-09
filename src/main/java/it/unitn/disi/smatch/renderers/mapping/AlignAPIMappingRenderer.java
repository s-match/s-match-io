package it.unitn.disi.smatch.renderers.mapping;

import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.INode;
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

/**
 * Renders the mapping in the AlignAPI mapping format.
 * <p/>
 * Needs parameters:
 * <p/>
 * onto1URI, onto2URI - URIs of ontologies
 * onto1Location, onto2Location - locations of ontologies
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class AlignAPIMappingRenderer extends BaseFileMappingRenderer implements IMappingRenderer {

    private final String onto1URI;
    private final String onto2URI;

    private final String onto1Location;
    private final String onto2Location;

    private final static String MEASURE = "1.0";

    public AlignAPIMappingRenderer(String onto1URI, String onto2URI, String onto1Location, String onto2Location) {
        super(null, null);
        this.onto1URI = onto1URI;
        this.onto2URI = onto2URI;
        this.onto1Location = onto1Location;
        this.onto2Location = onto2Location;
    }

    public AlignAPIMappingRenderer(String location, IContextMapping<INode> mapping, String onto1URI, String onto2URI, String onto1Location, String onto2Location) {
        super(location, mapping);
        this.onto1URI = onto1URI;
        this.onto2URI = onto2URI;
        this.onto1Location = onto1Location;
        this.onto2Location = onto2Location;
    }

    @Override
    protected void process(IContextMapping<INode> mapping, BufferedWriter out) throws IOException, MappingRendererException {
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
            final String base = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment#";
            hd.startPrefixMapping("", base);
            hd.endPrefixMapping("");
            hd.startPrefixMapping("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            hd.endPrefixMapping("rdf");
            hd.startPrefixMapping("xsd", "http://www.w3.org/2001/XMLSchema#");
            hd.endPrefixMapping("xsd");
            hd.startPrefixMapping("align", base);
            hd.endPrefixMapping("align");

            AttributesImpl atts = new AttributesImpl();
            hd.startElement("", "", "rdf:RDF", atts);

            hd.startElement("", "", "Alignment", new AttributesImpl());
            renderString(hd, new AttributesImpl(), "xml", "yes");
            renderString(hd, new AttributesImpl(), "level", "0");
            renderString(hd, new AttributesImpl(), "type", "**");

            renderOntology(hd, "1", onto1URI, onto1Location);
            renderOntology(hd, "2", onto2URI, onto2Location);

            for (IMappingElement<INode> mappingElement : mapping) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                hd.startElement("", "", "map", new AttributesImpl());
                hd.startElement("", "", "Cell", new AttributesImpl());

                atts = new AttributesImpl();
                atts.addAttribute("", "", "rdf:resource", "CDATA", onto1URI + "#" + mappingElement.getSource().nodeData().getId());
                hd.startElement("", "", "entity1", atts);
                hd.endElement("", "", "entity1");
                atts = new AttributesImpl();

                atts.addAttribute("", "", "rdf:resource", "CDATA", onto2URI + "#" + mappingElement.getTarget().nodeData().getId());
                hd.startElement("", "", "entity2", atts);
                hd.endElement("", "", "entity2");
                char relation = mappingElement.getRelation();

                hd.startElement("", "", "relation", new AttributesImpl());
                hd.characters(new char[]{relation}, 0, 1);
                hd.endElement("", "", "relation");

                atts = new AttributesImpl();
                atts.addAttribute("", "", "rdf:datatype", "CDATA", "http://www.w3.org/2001/XMLSchema#float");
                hd.startElement("", "", "measure", atts);
                hd.characters(MEASURE.toCharArray(), 0, MEASURE.length());
                hd.endElement("", "", "measure");

                hd.endElement("", "", "Cell");
                hd.endElement("", "", "map");

                progress();
            }//for

            hd.endElement("", "", "Alignment");
            hd.endElement("", "", "rdf:RDF");
            hd.endDocument();
        } catch (SAXException | TransformerConfigurationException e) {
            throw new MappingRendererException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public String getDescription() {
        return ILoader.RDF_FILES;
    }

    public ILoader.LoaderType getType() {
        return ILoader.LoaderType.FILE;
    }

    private static void renderOntology(TransformerHandler hd, String index, String URI, String location) throws SAXException {
        hd.startElement("", "", "onto" + index, new AttributesImpl());
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "", "rdf:about", "CDATA", URI);
        hd.startElement("", "", "Ontology", atts);
        renderString(hd, new AttributesImpl(), "location", location);
        hd.startElement("", "", "formalism", new AttributesImpl());
        atts = new AttributesImpl();
        atts.addAttribute("", "", "align:name", "CDATA", "OWL2.0");
        atts.addAttribute("", "", "align:uri", "CDATA", "http://www.w3.org/2002/07/owl#");
        hd.startElement("", "", "Formalism", atts);
        hd.endElement("", "", "Formalism");
        hd.endElement("", "", "formalism");
        hd.endElement("", "", "Ontology");
        hd.endElement("", "", "onto" + index);

    }

    private static void renderString(TransformerHandler hd, AttributesImpl atts, final String tagName, final String tagValue) throws SAXException {
        if (null != tagValue && 0 < tagValue.length()) {
            hd.startElement("", "", tagName, atts);
            hd.characters(tagValue.toCharArray(), 0, tagValue.length());
            hd.endElement("", "", tagName);
        }
    }
}
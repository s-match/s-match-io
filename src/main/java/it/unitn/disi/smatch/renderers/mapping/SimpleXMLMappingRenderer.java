package it.unitn.disi.smatch.renderers.mapping;

import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.loaders.ILoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Renders the mapping in a Simple XML file.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLMappingRenderer extends BaseFileMappingRenderer {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLMappingRenderer.class);

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
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "", "similarity", "CDATA", Double.toString(mapping.getSimilarity()));
            hd.startElement("", "", "mapping", atts);

            for (IMappingElement<INode> mappingElement : mapping) {
                String sourceConceptId = mappingElement.getSource().getNodeData().getId();
                String targetConceptId = mappingElement.getTarget().getNodeData().getId();
                if (null != sourceConceptId && 0 < sourceConceptId.length() && null != targetConceptId && 0 < targetConceptId.length()) {

                    char relation = mappingElement.getRelation();

                    atts = new AttributesImpl();
                    atts.addAttribute("", "", "source-id", "CDATA", sourceConceptId);
                    atts.addAttribute("", "", "target-id", "CDATA", targetConceptId);
                    atts.addAttribute("", "", "relation", "CDATA", Character.toString(relation));
                    hd.startElement("", "", "link", atts);
                    hd.endElement("", "", "link");

                    countRelation(relation);
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Source or Target node ID absent for mapping element: " + mappingElement);
                    }
                }
                reportProgress();
            }

            hd.endElement("", "", "mapping");
            hd.endDocument();
        } catch (SAXException e) {
            throw new MappingRendererException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (TransformerConfigurationException e) {
            throw new MappingRendererException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public String getDescription() {
        return ILoader.XML_FILES;
    }
}
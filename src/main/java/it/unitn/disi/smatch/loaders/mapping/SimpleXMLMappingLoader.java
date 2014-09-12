package it.unitn.disi.smatch.loaders.mapping;

import it.unitn.disi.smatch.data.mappings.IContextMapping;
import it.unitn.disi.smatch.data.mappings.IMappingFactory;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.data.util.MappingProgressContainer;
import it.unitn.disi.smatch.loaders.ILoader;
import it.unitn.disi.smatch.loaders.mapping.handlers.SimpleXMLMappingContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Loads mappings in SimpleXML format as rendered by SimpleXMLMappingRenderer.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLMappingLoader extends BaseFileMappingLoader {

    private final SAXParser parser;

    public SimpleXMLMappingLoader(IMappingFactory mappingFactory) throws MappingLoaderException {
        super(mappingFactory);

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
        } catch (FactoryConfigurationError | ParserConfigurationException | SAXException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    protected void process(IContextMapping<INode> mapping, IContext source, IContext target, BufferedReader reader, MappingProgressContainer progressContainer) throws IOException, MappingLoaderException {
        SimpleXMLMappingContentHandler contentHandler = new SimpleXMLMappingContentHandler(mapping, source, target, progressContainer);
        try {
            InputSource is = new InputSource(reader);
            parser.parse(is, contentHandler);
        } catch (SAXException | FileNotFoundException | UnsupportedEncodingException e) {
            throw new MappingLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public String getDescription() {
        return ILoader.XML_FILES;
    }
}
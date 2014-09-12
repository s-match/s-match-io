package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.data.trees.IBaseContext;
import it.unitn.disi.smatch.data.trees.IBaseNode;
import it.unitn.disi.smatch.loaders.ILoader;
import it.unitn.disi.smatch.loaders.context.handlers.BaseXMLContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Base class for XML loaders.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public abstract class BaseXMLContextLoader<E extends IBaseContext<? extends IBaseNode>> extends BaseFileContextLoader<E> implements IBaseContextLoader<E> {

    private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";

    protected final XMLReader parser;
    protected final boolean uniqueStrings;

    public BaseXMLContextLoader() throws ContextLoaderException {
        this(false);
    }

    /**
     * @param uniqueStrings whether to make node names unique.
     * @throws ContextLoaderException
     */
    public BaseXMLContextLoader(boolean uniqueStrings) throws ContextLoaderException {
        this.uniqueStrings = uniqueStrings;
        try {
            parser = XMLReaderFactory.createXMLReader(DEFAULT_PARSER_NAME);
            parser.setProperty("http://apache.org/xml/properties/input-buffer-size", 8196);
        } catch (SAXException e) {
            throw new ContextLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Whether to make node names unique.
     * Saves memory in case of contexts with highly repetitive node names.
     *
     * @return true if to make node names unique.
     */
    public boolean isUniqueStrings() {
        return uniqueStrings;
    }

    @Override
    protected void createIds(E result) {
        //ids should be already in XML
    }

    @Override
    protected E process(BufferedReader input) throws IOException, ContextLoaderException {
        BaseXMLContentHandler<E> contentHandler = getContentHandler();
        try {
            InputSource is = new InputSource(input);
            parser.setContentHandler(contentHandler);
            parser.parse(is);
        } catch (SAXException | FileNotFoundException | UnsupportedEncodingException e) {
            throw new ContextLoaderException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
        return contentHandler.getContext();
    }

    protected abstract BaseXMLContentHandler<E> getContentHandler();

    public String getDescription() {
        return ILoader.XML_FILES;
    }
}
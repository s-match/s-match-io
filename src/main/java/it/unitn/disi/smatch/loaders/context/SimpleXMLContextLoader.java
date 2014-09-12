package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.loaders.context.handlers.BaseXMLContentHandler;
import it.unitn.disi.smatch.loaders.context.handlers.SimpleXMLContentHandler;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;

/**
 * Loader for SimpleXML format.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLContextLoader extends BaseXMLContextLoader<IContext> implements IContextLoader {

    protected final ILinguisticOracle oracle;

    public SimpleXMLContextLoader(ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super();
        this.oracle = linguisticOracle;
    }

    public SimpleXMLContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super(uniqueStrings);
        this.oracle = linguisticOracle;
    }

    @Override
    protected BaseXMLContentHandler<IContext> getContentHandler() {
        return new SimpleXMLContentHandler(oracle, isUniqueStrings());
    }
}
package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.loaders.ILoader;
import it.unitn.disi.smatch.loaders.context.handlers.BaseXMLContentHandler;
import it.unitn.disi.smatch.loaders.context.handlers.CTXMLContentHandler;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;

/**
 * Loader for old CTXML format, remains for compatibility.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 * @author Mikalai Yatskevich mikalai.yatskevich@comlab.ox.ac.uk
 */
public class CTXMLContextLoader extends BaseXMLContextLoader<IContext> implements IContextLoader {

    private final ILinguisticOracle oracle;

    public CTXMLContextLoader(ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super();
        this.oracle = linguisticOracle;
    }

    public CTXMLContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super(uniqueStrings);
        this.oracle = linguisticOracle;
    }

    @Override
    protected BaseXMLContentHandler<IContext> getContentHandler() {
        return new CTXMLContentHandler(isUniqueStrings(), oracle);
    }

    public String getDescription() {
        return ILoader.XML_FILES;
    }
}

package it.unitn.disi.smatch.loaders.context;

import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.smatch.data.trees.Node;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loader for XML format. Check whether there are duplicates among siblings and leaves only ones of them
 * consolidating children.
 *
 * @author <a rel="author" href="http://autayeu.com/">Aliaksandr Autayeu</a>
 */
public class SimpleXMLDeDupContextLoader extends SimpleXMLContextLoader {

    private static final Logger log = LoggerFactory.getLogger(SimpleXMLDeDupContextLoader.class);

    public SimpleXMLDeDupContextLoader(ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super(linguisticOracle);
    }

    public SimpleXMLDeDupContextLoader(boolean uniqueStrings, ILinguisticOracle linguisticOracle) throws ContextLoaderException {
        super(uniqueStrings, linguisticOracle);
    }

    @Override
    protected IContext process(BufferedReader input) throws IOException, ContextLoaderException {
        IContext result = super.process(input);

        if (null != result) {
            log.info("Checking sibling duplicates...");
            //checking for duplicates among siblings
            int duplicatesRemoved = 0;

            ArrayList<INode> nodeQ = new ArrayList<>();
            nodeQ.add(result.getRoot());
            INode curNode;
            while (!nodeQ.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) {
                    return null;
                }
                curNode = nodeQ.remove(0);
                if (null == curNode) {
                    //go up
                } else {
                    List<INode> children = new ArrayList<>(curNode.getChildren());
                    Collections.sort(children, Node.NODE_NAME_COMPARATOR);
                    int idx = 1;
                    while (idx < children.size()) {
                        if (children.get(idx - 1).nodeData().getName().equals(children.get(idx).nodeData().getName())) {
                            log.info("Found duplicate: " + children.get(idx).nodeData().getName());
                            moveChildren(children.get(idx), children.get(idx - 1));
                            curNode.removeChild(children.get(idx));
                            children.remove(idx);
                            duplicatesRemoved++;
                        } else {
                            idx++;
                        }
                    }

                    progress();

                    if (curNode.getChildCount() > 0) {
                        //go down
                        nodeQ.add(0, null);
                        //adding to the top of the queue
                        List<INode> childList = curNode.getChildren();
                        for (int i = childList.size() - 1; i >= 0; i--) {
                            nodeQ.add(0, childList.get(i));
                        }
                    }
                }
            }

            log.info("Duplicates removed: " + duplicatesRemoved);
        }

        return result;
    }

    /**
     * Move children from <var>source</var> to <var>target</var>.
     *
     * @param source source node
     * @param target target node
     */
    private void moveChildren(INode source, INode target) {
        List<INode> children = new ArrayList<>(source.getChildren());
        while (0 < children.size()) {
            INode child = children.remove(0);
            int idx = target.getChildIndex(child);
            if (-1 == idx) {
                target.addChild(child);
            } else {
                moveChildren(child, target.getChildAt(idx));
            }
        }
    }
}
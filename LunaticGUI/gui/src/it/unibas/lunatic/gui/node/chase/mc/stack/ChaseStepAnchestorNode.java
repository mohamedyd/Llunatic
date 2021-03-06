package it.unibas.lunatic.gui.node.chase.mc.stack;

import it.unibas.lunatic.Scenario;
import it.unibas.lunatic.gui.node.chase.mc.ChaseStepNode;
import it.unibas.lunatic.model.chase.chasemc.DeltaChaseStep;
import org.openide.nodes.Children;

public class ChaseStepAnchestorNode extends ChaseStepNode {

    public ChaseStepAnchestorNode(DeltaChaseStep key, Scenario s) {
        super(key, s, Children.create(new ChaseStepAnchestorChildFactory(key, s), false));
        setDisplayName(key.getId());
    }
}

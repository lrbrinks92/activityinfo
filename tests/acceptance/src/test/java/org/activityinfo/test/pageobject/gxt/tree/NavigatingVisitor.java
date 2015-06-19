package org.activityinfo.test.pageobject.gxt.tree;

import com.google.common.base.Joiner;
import org.activityinfo.test.pageobject.gxt.GxtTree;

import java.util.Iterator;
import java.util.List;


public class NavigatingVisitor implements GxtTreeVisitor {

    private final List<String> steps;
    private final Iterator<String> path;
    private String current;
    private GxtTree.GxtNode match;

    public NavigatingVisitor(List<String> steps) {
        this.steps = steps;
        this.path = steps.iterator();
        this.current = path.next();
    }

    @Override
    public Action visit(GxtTree.GxtNode node) {
        System.out.println(node.getLabel());
        if(current.equals(node.getLabel())) {
            if(!path.hasNext()) {
                match = node;
                return Action.ABORT;
            } 
            current = path.next();
            node.ensureExpanded();
        }
        return Action.CONTINUE;
    }
    
    public GxtTree.GxtNode get() {
        if(match == null) {
            throw new AssertionError("Could not find node " + Joiner.on(" / ").join(steps));
        }
        return match;
    }
}

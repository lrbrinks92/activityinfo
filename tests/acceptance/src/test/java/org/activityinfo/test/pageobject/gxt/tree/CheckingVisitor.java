package org.activityinfo.test.pageobject.gxt.tree;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.activityinfo.test.pageobject.gxt.GxtTree;

import java.util.Set;

public class CheckingVisitor implements GxtTreeVisitor {
    
    private Set<String> toCheck = Sets.newHashSet();

    public CheckingVisitor(Iterable<String> toCheck) {
        this.toCheck = Sets.newHashSet(toCheck);
    }

    @Override
    public Action visit(GxtTree.GxtNode node) {
        if(node.isLeaf()) {
            String label = node.getLabel();
            node.setChecked(toCheck.contains(label));
            toCheck.remove(label);

        } else {
            node.ensureExpanded();
        }
        return Action.CONTINUE;
    }

    public void validate() {
        if(!toCheck.isEmpty()) {
            throw new AssertionError("Not all nodes to check could be found. Missing: " +
                    Joiner.on(", ").join(toCheck));
        }
    }
}

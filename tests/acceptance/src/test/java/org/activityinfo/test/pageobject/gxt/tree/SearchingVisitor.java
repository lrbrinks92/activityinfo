package org.activityinfo.test.pageobject.gxt.tree;


import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.activityinfo.test.pageobject.gxt.GxtTree;

public class SearchingVisitor implements GxtTreeVisitor {

    private Predicate<GxtTree.GxtNode> predicate;
    private Optional<GxtTree.GxtNode> match = Optional.absent();

    public SearchingVisitor(Predicate<GxtTree.GxtNode> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Action visit(GxtTree.GxtNode node) {
        if(predicate.apply(node)) {
            this.match = Optional.of(node);
            return Action.ABORT;
        } else {
            if(!node.isLeaf()) {
                node.ensureExpanded();
                System.out.println(node + " is expanded");
            } else {
                System.out.println(node + " is a leaf, not expanding");
            }
            return Action.CONTINUE;
        }
    }

    public Optional<GxtTree.GxtNode> getMatch() {
        return match;
    }
    
    public static SearchingVisitor byLabel(final String label) {
        return new SearchingVisitor(new Predicate<GxtTree.GxtNode>() {
            @Override
            public boolean apply(GxtTree.GxtNode input) {
                return input.getLabel().equals(label);
            }
        });
    }
}

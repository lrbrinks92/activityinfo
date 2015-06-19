package org.activityinfo.test.pageobject.gxt.tree;

import org.activityinfo.test.pageobject.gxt.GxtTree;

public interface GxtTreeVisitor {
   
    public enum Action {
        CONTINUE,
        ABORT
    }
    
    Action visit(GxtTree.GxtNode node);
}

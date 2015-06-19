package org.activityinfo.coverage

/**
 * SourceFile Coverage
 */
class FileCoverage {
    
    String path
    private Map<Integer, Boolean> coverage = new HashMap<>()

    FileCoverage(String path) {
        this.path = path
    }

    void setCovered(int lineNumber, boolean covered) {
        boolean currentValue = coverage[lineNumber]
        coverage[lineNumber] = currentValue || covered
    }

    Map<Integer, Boolean> getLineMap() {
        return coverage
    }
}

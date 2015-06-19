package org.activityinfo.coverage

import groovy.json.JsonSlurper

class GwtCoverageParser {

    private JsonSlurper jsonParser = new JsonSlurper()
    private ProjectCoverage coverage

    /**
     *
     * @param pathMapFile file containing the mapping between the relative paths reported by the GWT instrumentation
     *                      and the results 
     * @param coverage
     */
    GwtCoverageParser(ProjectCoverage coverage) {
        this.coverage = coverage
    }

    public void readGwtOutput(File reportFile) {
        /**
         * {"org/activityinfo/ui/client/page/config/ConfigLoader.java":{"44":1,"62":1,"63":1,"65":1,"66":1,"67":1, ...},
         *  "org/activityinfo/ui/client/component/report/editor/map/MapEditor.java":{"64":0,"69":0,"70":0, },
         * }
         */

        if(reportFile.size() > 0) {

            def report = jsonParser.parse(reportFile)
            report.each { sourceFile, lines ->
                def fileCoverage = coverage.getSource(sourceFile)
                lines.each { lineNumber, covered ->
                    fileCoverage.setCovered(Integer.parseInt(lineNumber), covered > 0)
                }
            }
        }
    }
}

package org.activityinfo.coverage

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class MergeCoverageTask extends DefaultTask {
    
    @Input
    File gwtCoverageReportsDir
    
    @Input
    File jacocoReportFile
    
    @OutputFile
    File outputFile
    
    @TaskAction
    public void mergeReports() {
        
        ProjectCoverage coverage = new ProjectCoverage()
        
        // Parse the GWT Coverage Reports
        GwtCoverageParser gwtCoverage = new GwtCoverageParser(coverage)
        gwtCoverageReportsDir.eachFile { it ->
            try {
                gwtCoverage.readGwtOutput(it)
            } catch (Exception e) {
                logger.error("Failed to parse GWT coverage file ${it.absolutePath}", e)
            }
        }
        
        // And the Jacoco reports..
        JacocoParser jacoco = new JacocoParser(coverage)
        jacoco.parse(jacocoReportFile)
        
        // Now write out a coverage report for each project
        project.rootProject.subprojects.each { project ->
            coverage.writeReport(project)
        }
    }
}

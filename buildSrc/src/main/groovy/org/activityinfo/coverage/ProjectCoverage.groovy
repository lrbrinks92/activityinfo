package org.activityinfo.coverage

import groovy.xml.MarkupBuilder
import org.gradle.api.Project

/**
 * Model classes for combining GWT and jacoco test coverage results
 */
class ProjectCoverage {

    Map<String, FileCoverage> files = new HashMap<>()


    public FileCoverage getSource(String sourceFile) {
        FileCoverage fileCoverage = files.get(sourceFile)
        if(!fileCoverage) {
            fileCoverage = new FileCoverage(sourceFile)
            files.put(sourceFile, fileCoverage)
        }
        return fileCoverage
    }


    public void writeReport(Project project) {

        def paths = pathMap(project)
        if(!paths.isEmpty()) {

            // Write out the XML Summary for files that belong
            // to this module
            project.file("${project.buildDir}/coverage-at.xml").withWriter { writer ->
                def xml = new MarkupBuilder(writer)
                xml.coverage(version: 1) {
                    files.each { path, coverage ->
                        if (paths.containsKey(path)) {
                            xml.file(path: paths.get(path)) {
                                coverage.lineMap.each { lineNumber, covered ->
                                    lineToCover(lineNumber: lineNumber, covered: covered)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    def pathMap(Project project) {

        // Create a set of java sources in this project
        // relative to the source directory, for example:
        //
        // org/activityinfo/ui/client/ActivityInfoEntryPoint
        //
        // which is the form we get from the coverage data

        def pathMap = new HashMap()
        project.sourceSets.main.allJava.visit { f ->
            if(f.name.endsWith(".java")) {
                def classPath = f.relativePath.toString()
                def projectPath = project.relativePath(f.file.absolutePath).toString()
                pathMap.put(classPath, projectPath)
            }
        }
        return pathMap
    }
}

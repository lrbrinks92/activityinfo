package org.activityinfo.gwt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Measures the total download size of each permutation and
 * appcache size
 */
class CheckDownloadSizeTask extends DefaultTask {
    
    int initialJsDownloadSizeLimit = Integer.MAX_VALUE
    int totalJsDownloadSizeLimit = Integer.MAX_VALUE
    int appCacheDownloadSizeLimit = Integer.MAX_VALUE
    
    public initialJsDownloadSizeLimit(String size) {
        initialJsDownloadSizeLimit = parseSize(size)
    }
    
    public totalJsDownloadSizeLimit(String size) {
        totalJsDownloadSizeLimit = parseSize(size)
    }
    
    public appCacheDownloadSizeLimit(String size) {
        appCacheDownloadSizeLimit = parseSize(size)
    }
    
    @Input
    File getModuleOutput() {
        return project.file("${project.buildDir}/gwt/out")
    }
    
    @TaskAction
    public void measure() {
        def modules = new ArrayList<Module>()
        moduleOutput.eachDir { moduleDir -> 
            def permutationsFile = new File(moduleDir, "permutations")
            if(permutationsFile.exists()) {
                modules.add(new Module(permutationsFile))               
            }
        }
        def violations = false
        modules.each { m ->
            m.permutations.sort { p -> p.name }.each { p ->

                def violation =
                    (p.initialJsDownloadSize > initialJsDownloadSizeLimit) ||
                    (p.totalJsDownloadSize > totalJsDownloadSizeLimit) ||
                    (p.totalCacheDownloadSize > appCacheDownloadSizeLimit)
                
                if(violation) {
                    violations = true
                }
                
                if(violation || logger.isEnabled(LogLevel.INFO)) {
                    p.printSummary(violation)
                }
            }
        }
        
        if(violations) {
            throw new GradleException('There were GWT modules that exceeded download size limits.')
        }
    }
   
    int parseSize(String size) {
        def units = [ kb: 1024,
                      mb: 1024*1024 ]
        
        for(def unit : units.entrySet()) {
            int start = size.indexOf(unit.key)
            if(start != -1) {
                def number = size.substring(0, start)
                return Integer.parseInt(number) * unit.value
            }
        }
        throw new InvalidUserDataException("Invalid size specification '${size}'")
    }
}

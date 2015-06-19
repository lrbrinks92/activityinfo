package org.activityinfo.gwt

import com.google.common.base.Strings
import com.google.common.io.ByteStreams
import com.google.common.io.CountingOutputStream
import com.google.common.io.Files
import groovy.transform.Memoized

import java.util.zip.GZIPOutputStream

/**
 * An individual GWT permutation along with the 
 * Rebar-generated appcache manifest
 */
class Permutation {
    File moduleDir
    String strongName
    Map<String, String> properties
    List<File> files
    
    Permutation(File moduleDir, String strongName, Map<String, String> properties) {
        this.moduleDir = moduleDir
        this.strongName = strongName
        this.properties = properties
    }
    
    String getName() {
        return properties['user.agent'] + "-" + properties['locale']
    }
    
    File getManifestFile() {
        return new File(moduleDir, "${strongName}.appcache")
    }
    
    File getInitialJsFile() {
        new File(moduleDir, "${strongName}.cache.html")
    }
    
    File getDeferredJsDir() {
        return new File("$moduleDir/deferredjs/$strongName")
    }
    
    String getBootstrapScriptName() {
        return "${moduleDir.name}.nocache.js"
    }
    
    Collection<File> getJsFiles() {
        return [ initialJsFile ] + deferredJsDir.listFiles().toList()
    }

    long getInitialJsSize() {
        initialJsFile.length()
    }
    
    long getInitialJsDownloadSize() {
        gzippedSize(initialJsFile)
    }
    
    long getTotalJsSize() {
        jsFiles.collect { file -> file.length() }.sum()
    }

    long getTotalJsDownloadSize() {
        jsFiles.collect { file -> gzippedSize(file) }.sum()   
    }
    
    @Memoized
    List<File> getCacheEntries() {  
        
        // Read the lines from CACHE: up through the first blank line
        // to see which files have to be downloaded as part of the app cache manifest
        
        manifestFile
        .readLines().iterator()
        .dropWhile { line -> !line.equals("CACHE:")}
        .drop(1)
        .takeWhile { line -> !line.isEmpty() }
        .findAll { entry -> !entry.equals(bootstrapScriptName) }
        .collect { entry -> new File(moduleDir, entry) }
    }
    
    long getTotalCacheSize() {
        cacheEntries.collect { file -> file.length() }.sum()
    }
    
    long getTotalCacheDownloadSize() {
        cacheEntries.collect { file -> downloadSize(file) }.sum()
    }
    
    long downloadSize(File file) {
        if(file.name =~ /\.(html|js|css)$/) {
            return gzippedSize(file)    
        } else {
            return file.length()
        }
    }
    

    private String formatSize(long bytes) {
        return String.format("%.0fkb", bytes / 1024d)
    }
    
    void printSummary(boolean violation) {
        def header = moduleDir.name + " " + Strings.padEnd(name, 11, ' '.charAt(0))
        def initial = formatSize(initialJsDownloadSize)
        def total = formatSize(totalJsDownloadSize)
        def cache = formatSize(totalCacheDownloadSize)
        def symbol = violation ? " [!]" : "";

        println("${header}: Initial: ${initial} Total: ${total}  AppCache: ${cache} ${symbol}")
    }

    @Memoized
    long gzippedSize(File file) {
        def cout = new CountingOutputStream(ByteStreams.nullOutputStream())
        def gzout = new GZIPOutputStream(cout)
        ByteStreams.copy(Files.asByteSource(file), gzout)
        
        return cout.count
    }
}

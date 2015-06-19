package org.activityinfo.gcloud

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Downloads the archived war from previous steps and
 * explodes it
 */
class DownloadArtifactTask extends DefaultTask {

    @Input
    String archiveUrl
    
    @OutputDirectory
    File explodedDir
    
    def from(String url) {
        archiveUrl = url
    }
    
    def into(String dir) {
        explodedDir = new File(dir)
    }
    
    @TaskAction
    def download() {

        def archiveName = archiveUrl.split("/").last()
        def downloadedFile = "${project.buildDir}/${archiveName}"

        project.exec {
            commandLine 'gsutil', 'cp', archiveUrl, downloadedFile
        }

        ant.unzip(src: downloadedFile, dest: explodedDir)
    }
}

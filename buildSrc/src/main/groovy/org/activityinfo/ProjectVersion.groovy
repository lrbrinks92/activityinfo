package org.activityinfo

class ProjectVersion {
    Integer major
    Integer minor
    String build

    ProjectVersion(Integer major, Integer minor, String build) {
        this.major = major
        this.minor = minor
        this.build = build
    }

    String getAppEngineVersion() {
        if(!build) {
            throw new RuntimeException("Only artifacts from the pipeline can be deployed.")
        }
        return "b${build}"
    }
    
    @Override
    String toString() {
        String fullVersion = "$major.$minor"

        if(build) {
            fullVersion += ".$build"
        } else{
            fullVersion += "-dev"
        }

        fullVersion
    }
    
}
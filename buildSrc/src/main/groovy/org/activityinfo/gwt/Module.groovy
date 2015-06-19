package org.activityinfo.gwt

import groovy.json.JsonSlurper

/**
 * GWT Module download sizes
 */
class Module {
    
    File permutationsFile
    Collection<Permutation> permutations

    Module(File permutationsFile) {
        this.permutationsFile = permutationsFile

        def permutationMap = new HashMap<String, Permutation>()
        new JsonSlurper().parse(permutationsFile).each {
            if(!permutationMap.containsKey(it.permutation)) {
                permutationMap.put(it.permutation, new Permutation(getDirectory(), it.permutation, it.properties))
            }
        }
        this.permutations = permutationMap.values()
    }
    
    String getName() {
        return permutationsFile.parentFile.name
    }
    
    File getDirectory() {
        return permutationsFile.parentFile
    }
    
    Permutation getLargestPermutation() {
        return permutations.max { p -> p.totalCacheSize }
    }
}

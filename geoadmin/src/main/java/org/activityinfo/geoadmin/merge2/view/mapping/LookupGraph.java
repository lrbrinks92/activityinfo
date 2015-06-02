package org.activityinfo.geoadmin.merge2.view.mapping;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.activityinfo.geoadmin.merge2.view.profile.FormProfile;
import org.activityinfo.geoadmin.merge2.view.swing.merge.MatchLevel;

import java.util.Collection;

/**
 * Graph that models "closeness" between a set of source keys and 
 * instances of a target reference field.
 * 
 */
public class LookupGraph {

    private final SourceKeySet sourceKeySet;
    private final LookupDistanceMatrix matrix;
    private final Multimap<Integer, Integer> candidates = HashMultimap.create();


    public LookupGraph(SourceKeySet sourceKeySet, FormProfile targetForm) {
        this.sourceKeySet = sourceKeySet;
        this.matrix = new LookupDistanceMatrix(sourceKeySet, targetForm);
        
        for(int i=0;i!=sourceKeySet.distinct().size();++i) {
            SourceLookupKey key = sourceKeySet.distinct().get(i);
            
            for(int j=0;j<targetForm.getRowCount();++j) {
                if(matrix.matches(i, j)) {
                    candidates.put(i, j);
                }
            }
        }
    }

    public int[] matchBest() {
        int matches[] = new int[sourceKeySet.size()];
        for(int sourceKeyIndex=0;sourceKeyIndex!=sourceKeySet.distinct().size();++sourceKeyIndex) {
            Collection<Integer> targetRows = candidates.get(sourceKeyIndex);
            matches[sourceKeyIndex] = -1;
            if(!targetRows.isEmpty()) {
                int bestMatch = bestMatch(sourceKeyIndex, targetRows);
                if(isParetoOptimal(sourceKeyIndex, bestMatch, targetRows)) {
                    matches[sourceKeyIndex] = bestMatch;                                
                }
            }
        }
        return matches;
    }
    

    /**
     * Finds the "best" match among potential candidates, using the sum of the scores across
     * the dimensions as a criteria.
     *
     * @return the index of the target instance that best matches the given source key
     */
    private int bestMatch(int sourceKeyIndex, Collection<Integer> targetRowIndexes) {
        
        double bestScore = 0;
        int bestMatch = -1;
        for(Integer targetRowIndex : targetRowIndexes) {
            double score = matrix.sumScores(sourceKeyIndex, targetRowIndex);
            if(score > bestScore) {
                bestScore = score;
                bestMatch = targetRowIndex;
            }
        }
        return bestMatch;
    }

    private boolean isParetoOptimal(int sourceKeyIndex, int bestTargetIndex, Collection<Integer> targetCandidates) {

        double scores[] = matrix.score(sourceKeyIndex, bestTargetIndex);

        for (Integer targetIndex : targetCandidates) {
            if( targetIndex != bestTargetIndex &&
                    !dominates(scores, matrix.score(sourceKeyIndex, targetIndex))) {

                return false;
            }
        }

        return true;
    }


    /**

     * @return true if {@code x[i] > y[i]} for all {@code i}
     */
    private boolean dominates(double[] x, double[] y) {
        assert x.length == y.length;
        for (int i = 0; i < x.length; i++) {
            if(y[i] > x[i]) {
                return false;
            }
        }
        return true;
    }


    public MatchLevel getLookupConfidence(int keyIndex, int targetIndex) {
        if(targetIndex == -1) {
            return MatchLevel.POOR;
        }
        double minScore = matrix.getMinScore(keyIndex, targetIndex);
        return MatchLevel.of(minScore);
    }

    public Collection<Integer> getCandidates(int sourceKeyIndex) {
        return candidates.get(sourceKeyIndex);
    }

    public double getScore(int sourceKeyIndex, Integer targetRow) {
        return matrix.sumScores(sourceKeyIndex, targetRow);
    }
}

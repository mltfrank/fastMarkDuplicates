package markduplicates;

import htsjdk.samtools.util.SortingLongCollection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Remove duplicate in a chunk.
 * Try to sort in this chunk and build multi small chunk, then do mark duplicate in small chunk in what picard does..
 * Author: Wang Bingchen
 * Date: 2016/2/26.
 */
public class ChunkDuplicateMarker {
    private ReadEndsMDComparator comparator = new ReadEndsMDComparator();

    private void markDuplicateInSmallChunk(List<ReadEndsForMarkDuplicates> chunk,
                                           boolean hasPairPositive, boolean hasPairNegative,
                                           SortingLongCollection duplicateIndexes){
        // find max score for fragment and pair-end.
        double maxFragScore = -1;
        ReadEndsForMarkDuplicates maxFrag = null;
        double maxPairScore = -1;
        ReadEndsForMarkDuplicates maxPair = null;
        for(ReadEndsForMarkDuplicates readEnds : chunk){
            double score = readEnds.score;
            if(readEnds.isPair && score > maxPairScore){
                maxPair = readEnds;
                maxPairScore = score;
            }
            else if(!readEnds.isPair && score > maxFragScore){
                maxFrag = readEnds;
                maxFragScore = score;
            }
        }
        // Mark duplicate for pair and fragment.
        for(ReadEndsForMarkDuplicates readEnds : chunk){
            if(readEnds.isPair && readEnds != maxPair){
                duplicateIndexes.add(readEnds.read1IndexInFile);
                duplicateIndexes.add(readEnds.read2IndexInFile);
            }
            else if(!readEnds.isPair){
                if(hasPairNegative && readEnds.read1NegativeStrand)
                    duplicateIndexes.add(readEnds.read1IndexInFile);
                else if(hasPairPositive && !readEnds.read1NegativeStrand)
                    duplicateIndexes.add(readEnds.read1IndexInFile);
                else if(readEnds != maxFrag)
                    duplicateIndexes.add(readEnds.read1IndexInFile);
            }
        }
    }

    /**
     * Differ to the chunk in picard MarkDuplicates, this chunk may contains pairEnds with different read1 position(Read2 position must be the same)
     * and different orientation. So we do a in-chunk sort with comparator, and do in-chunk judgement.
     * @param chunk chunk to remove duplicate
     * @param duplicateIndexes data struct to add duplicate index in.
     */
    public void markDuplicateInChunk(List<ReadEndsForMarkDuplicates> chunk,
                                     boolean hasPairPositive, boolean hasPairNegative,
                                     SortingLongCollection duplicateIndexes){
        Collections.sort(chunk, this.comparator);
        List<ReadEndsForMarkDuplicates> smallChunk = new ArrayList<ReadEndsForMarkDuplicates>();
        ReadEndsForMarkDuplicates firstInSmallChunk = null;
        for(ReadEndsForMarkDuplicates readEnds : chunk){
            if(firstInSmallChunk == null){
                firstInSmallChunk = readEnds;
            }
            if (areComparableInChunk(firstInSmallChunk, readEnds)){
                smallChunk.add(readEnds);
            }
            else{ // need to mark duplicate in small chunk
                if(smallChunk.size() > 1)
                    markDuplicateInSmallChunk(smallChunk, hasPairPositive, hasPairNegative, duplicateIndexes);
                smallChunk.clear();
                firstInSmallChunk = null;
            }
        }
        if(smallChunk.size() > 1)
            markDuplicateInSmallChunk(smallChunk, hasPairPositive, hasPairNegative, duplicateIndexes);
    }

    /**
     * Judge if two readEnd are comparable for duplicate.
     * Note: We only need to judge the first read and orientation of two reads.
     * Because in our algorithm, pairEnd in chunk are certain to have same read2(they are in same chunk and the latter read is sure to be read2)
     * @param lhs left value
     * @param rhs right value
     * @return if they are comparable
     */
    private boolean areComparableInChunk(final ReadEndsForMarkDuplicates lhs, final ReadEndsForMarkDuplicates rhs) {
        return lhs.read1ReferenceIndex == rhs.read1ReferenceIndex &&
                lhs.read1Coordinate == rhs.read1Coordinate &&
                lhs.orientation == rhs.orientation;
    }

    /**
     * Comparator for ReadEndsForMarkDuplicates that
     * orders by read1 position
     * then pair orientation
     * then read2 position.
     * */
    static class ReadEndsMDComparator implements Comparator<ReadEndsForMarkDuplicates> {
        public int compare(final ReadEndsForMarkDuplicates lhs, final ReadEndsForMarkDuplicates rhs) {
            int compareDifference = lhs.read1ReferenceIndex - rhs.read1ReferenceIndex;
            if (compareDifference == 0) compareDifference = lhs.read1Coordinate - rhs.read1Coordinate;
            if (compareDifference == 0) compareDifference = lhs.orientation - rhs.orientation;
            if (compareDifference == 0) compareDifference = lhs.read2ReferenceIndex - rhs.read2ReferenceIndex;
            if (compareDifference == 0) compareDifference = lhs.read2Coordinate - rhs.read2Coordinate;
            if (compareDifference == 0) compareDifference = (int) (lhs.read1IndexInFile - rhs.read1IndexInFile);
            if (compareDifference == 0) compareDifference = (int) (lhs.read2IndexInFile - rhs.read2IndexInFile);

            return compareDifference;
        }
    }
}

package fastdedup;

import fastdedup.io.BamLocusReader;
import htsjdk.samtools.SAMRecord;

import java.util.*;

/**
 * Created by Wang Bingchen on 2016/3/16.
 */
public class FindDuplicateWorker extends Thread{
    BamLocusReader bamReader = null;
    int intervalSize = 0;
    public FindDuplicateWorker(int intervalSize, String inputFile){
        this.intervalSize = intervalSize;
        this.bamReader = BamLocusReader.getLocusReader(inputFile);
    }

    /** map from readName to readEnds, local, for pairEnds searching */
    Map<String, ReadEnds> readNameToReadEndsMap = new HashMap<String, ReadEnds>();


    /**
     * Find duplicate indexes from chunk.
     * Traverse the readEnds collection and mark duplicate.
     * @param readEndsCollection source read ends.
     */
    public void findDuplicateIndexes(List<ReadEnds> readEndsCollection, List<Long> duplicateIndexes){
        if(readEndsCollection.size() == 0)
            return;

        // This sort operation makes all single readEnds separate by coordinate and orient.
        Collections.sort(readEndsCollection, new ReadEndsTool.SingleReadEndsComparator());

        List<ReadEnds> chunk = new ArrayList<ReadEnds>();
        ReadEnds firstElements = readEndsCollection.get(0); // must have one elements
        boolean hasPair = firstElements.isPair;
        for(ReadEnds readEnds : readEndsCollection){
            if (firstElements.read1Coordinate == readEnds.read1Coordinate
                    && firstElements.orientation == readEnds.orientation
                    && firstElements.read1ReferenceIndex == readEnds.read1ReferenceIndex){ // coordinate and orient is the same
                hasPair |= readEnds.isPair;
                addReadEndsIntoChunk(readEnds, readNameToReadEndsMap, chunk);
            }
            else { // not a same position, do mark duplicate in chunk
                if(chunk.size() > 0) {
                    markDuplicateInChunk(chunk, hasPair, duplicateIndexes);
                }
                chunk.clear();
                firstElements = readEnds;
                addReadEndsIntoChunk(readEnds, readNameToReadEndsMap, chunk);
                hasPair = readEnds.isPair;
            }
        }
    }

    /**
     * Add readEnds into a chunk.
     * If a record is not pair, add into chunk directly.
     * If a record is pair-end, look up the map to find if another record in a pair is already in.
     * If found one, build a new readEnds for a pair, remove old readEnds from old chunk by using the reference stored in readEnds class.
     * @param readEnds new readEnds.
     * @param readNameToReadEndsMap map from readName to readEnds.
     * @param chunk chunk need to be added in.
     */
    private void addReadEndsIntoChunk(ReadEnds readEnds, Map<String, ReadEnds> readNameToReadEndsMap, List<ReadEnds> chunk){
        if (readEnds.isPair) { // pairEnds
            String key = readEnds.readName;
            if(readNameToReadEndsMap.containsKey(key)){ // if found another readEnds in chunks
                // remove from old chunks.
                ReadEnds anotherReadEnds = readNameToReadEndsMap.get(key);
                readNameToReadEndsMap.remove(key);
                ReadEnds pairEnds = ReadEndsTool.buildPairEnds(readEnds, anotherReadEnds);
                chunk.add(pairEnds);
            }
            else{   // if not found, just put it into map. We mark if has pair flag outside, so in chunk duplicate will be ok
                readNameToReadEndsMap.put(key, readEnds);
            }
        }else{  // fragEnds, add into chunk directly
            chunk.add(readEnds);
        }
    }


    /**
     * Mark duplicate in small chunk
     * ReadEnds in this chunk has following feature:
     * 1. must be all positive or all negative, if has pairEnds, the read2 must be in same orient.
     * 2. must have all fragment with same coordinate, and all pairEnds with read2 in same coordinate.
     * Algorithm:
     *      calculate max score for fragment and pairEnds.
     *      if this coordinate has pairEnds reads(can be read2 of pairEnds in chunk and
     *          read1 of pairEnds not in chunk. The latter one is still waiting for its friend in map),
     *          mark all fragments as duplicates, and keep the pairEnds with highest score.
     *      if no pairEnds read on this coordinate, keep the fragments with highest score.
     * @param chunk contains readEnds.
     * @param hasPair if has pair-end reads on this coordinate
     * @param duplicateIndexes target index list
     */
    private void markDuplicateInChunk(List<ReadEnds> chunk, boolean hasPair, List<Long> duplicateIndexes){
        // find max score for fragment and pair-end.
        double maxFragScore = -1;
        ReadEnds maxFrag = null;
        double maxPairScore = -1;
        ReadEnds maxPair = null;
        for(ReadEnds readEnds : chunk){
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
        for(ReadEnds readEnds : chunk){
            if(readEnds.isPair && readEnds != maxPair){
                duplicateIndexes.add(readEnds.read1IndexInFile);
                duplicateIndexes.add(readEnds.read2IndexInFile);
            }
            else if(!readEnds.isPair){
                if(hasPair || readEnds != maxFrag) {
                    duplicateIndexes.add(readEnds.read1IndexInFile);
                }
            }
        }
    }

    /**
     * Mark duplicate in all pairEnds list
     * Sort and scan the list.
     * @param pairEndsList list to hold all pairEnds
     * @param duplicateIndexes list to put duplicate index
     */
    public void markDuplicateInPairEndsList(List<ReadEnds> pairEndsList, List<Long> duplicateIndexes){
        if(pairEndsList.size() == 0)
            return;
        // sort makes all duplicate pairEnds next to each other.
        // just keep pairEnds with highest score when scan the list.
        Collections.sort(pairEndsList, new ReadEndsTool.PairReadEndsComparator());

        ReadEnds maxPair = pairEndsList.get(0);// must have one element
        double maxPairScore = maxPair.score;
        for(ReadEnds pairEnds: pairEndsList){
            if(pairEnds.read2Coordinate == maxPair.read2Coordinate
                    && pairEnds.read1Coordinate == maxPair.read1Coordinate
                    && pairEnds.orientation == maxPair.orientation
                    && pairEnds.read1ReferenceIndex == maxPair.read1ReferenceIndex
                    && pairEnds.read2ReferenceIndex == maxPair.read2ReferenceIndex){    // markDuplicate
                if(pairEnds.score > maxPairScore){
                    duplicateIndexes.add(maxPair.read1IndexInFile);
                    duplicateIndexes.add(maxPair.read2IndexInFile);
                    maxPairScore = pairEnds.score;
                    maxPair = pairEnds;
                }
                else if(maxPair != pairEnds) {
                    duplicateIndexes.add(pairEnds.read1IndexInFile);
                    duplicateIndexes.add(pairEnds.read2IndexInFile);
                }
            }
            else{
                maxPairScore = pairEnds.score;
                maxPair = pairEnds;
            }
        }
    }

    @Override
    public void run(){
        List<SAMRecord> recordsCollection = new ArrayList<SAMRecord>();
        List<ReadEnds> readEndsCollection = new ArrayList<ReadEnds>();
        long readCount = 0;
        List<ReadEnds> pickFromGlobalPool = new ArrayList<ReadEnds>();
        List<Long> duplicateIndexes = new ArrayList<Long>();

        while(true){ // when readCount comes to -1, means have no reads in file and buffer.
            // step 1. Read a collection of reads from bam file.
            //         This step is synchronized.
            readCount = bamReader.readRecords(intervalSize, recordsCollection);
            if(readCount == -1) break;
            readEndsCollection.clear();
            duplicateIndexes.clear();
            for(SAMRecord record: recordsCollection){
                readEndsCollection.add(new ReadEnds(record));
            }
            // step 2. Find duplicate index from collection of readEnds.
            //         It can't find all duplicate because some pairEnds are in different collection.
            findDuplicateIndexes(readEndsCollection, duplicateIndexes);
            // step 3. Using global readEnds pool to match all pairEnds.
            //         Do mark duplicate again to find remain duplicate indexes.
            //         It is thread safe because PublicReadEndsPool provides synchronized method to pick and put.
            if(readNameToReadEndsMap.size() > 0) {  // if not clean, need to visit global readEnds pool
                pickFromGlobalPool.clear();
                GlobalReadEndsPool.INSTANCE.pickAndPut(readNameToReadEndsMap, pickFromGlobalPool);
                markDuplicateInPairEndsList(pickFromGlobalPool, duplicateIndexes);
            }

            // After mark all duplicate indexs, sort the index and push it into global indexPool.
            Collections.sort(duplicateIndexes);
            GlobalDuplicateIndexPool.INSTANCE.addAll(duplicateIndexes);
        }

    }
}

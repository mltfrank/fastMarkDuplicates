package markduplicates;

import htsjdk.samtools.*;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.SortingLongCollection;

import java.io.File;
import java.util.*;

/**
 * MarkDuplicates tools for sorted bam file.
 * I try to write a new tool which has same usage with the broad institute picard MarkDuplicates.
 * Besides, I simplify the algorithm of picard MarkDuplicates, by decrease the times of scan to 2 from 4.
 * A multi thread option is provided, which can make it run faster on multi node in a single node.
 *
 * @Author Wang Bingchen
 * @Date 2016/2/25.
 */
public class MarkDuplicates {
    private final Log log = Log.getInstance(MarkDuplicates.class);
    /** @argument input sam file */
    private String inputFile = null;
    /** @argument output bam file */
    private String outputFile = null;
    /** @argument remove duplicate flag */
    private boolean REMOVE_DUPLICATES = false;

    public List<File> TMP_DIR = new ArrayList();

    /**
     * TODO may be it is no use!!!
     *  global map for unpaired pairEnds
     *  Warning: this chunks must all in memory for fast-random access.
     *  so if there are a lot of pair-end but only one of the pair is in bam file,
     *  it will cost a lot of memory, even cause oom error.
     */
    public Map<String, ReadEndsForMarkDuplicates> chunks = new HashMap<String, ReadEndsForMarkDuplicates>();

    private void parseArguments(String[] args){
        for(int i = 0; i < args.length; i ++){
            String arg = args[i];
            if(arg.startsWith("INPUT=")){
                this.inputFile = arg.substring(6);
            }
            else if(arg.startsWith("OUTPUT=")){
                this.outputFile = arg.substring(7);
            }
            else if(arg.equals("-D")){
                this.REMOVE_DUPLICATES = true;
            }
            else{
                throw new PicardException("Can't parse argument '"+arg+"'");
            }
        }
    }

    public SortingLongCollection findDuplicateIndex(ChunkDuplicateMarker chunkDuplicateMarker, String inputFile){
        //TODO we need to assert the memory we reserve for duplicate index.
        final int maxInMemory = (int) Math.min((Runtime.getRuntime().maxMemory() * 0.25) / SortingLongCollection.SIZEOF,
                (double) (Integer.MAX_VALUE - 5));

        IOHelper.SamHeaderAndIterator headerAndIterator = IOHelper.openInput(inputFile);
        final CloseableIterator<SAMRecord> iterator = headerAndIterator.iterator;

        SortingLongCollection duplicateIndexes = new SortingLongCollection(maxInMemory, TMP_DIR.toArray(new File[TMP_DIR.size()]));

        Map<String, ReadEndsForMarkDuplicates> readNameToReadEndsMap = new HashMap<String, ReadEndsForMarkDuplicates>();
        long readIndexInBamFile = 0;

        LinkedList<ReadEndsForMarkDuplicates> chunk = new LinkedList<ReadEndsForMarkDuplicates>();
        ReadEndsForMarkDuplicates firstElementInChunk = null;
        boolean hasPairPositive = false;
        boolean hasPairNegative = false;
        while(iterator.hasNext()){
            final SAMRecord rec = iterator.next();

            if(rec.getReadUnmappedFlag()){
                if(rec.getReferenceIndex() == -1) { // When we hit the unmapped reads with no coordinate, no reason to continue.
                    break;
                }else{ // If this read is unmapped but sorted with the mapped reads, just skip it.
                    readIndexInBamFile ++;
                    continue;
                }
            } else if(!rec.isSecondaryOrSupplementary()){   // Bam from bwa may has more than two reads with same name.
                ReadEndsForMarkDuplicates readEnds = buildReadEnds(readIndexInBamFile, rec);
                if(firstElementInChunk == null){
                    firstElementInChunk = readEnds;
                }
                if(inSamePositionOnRead1(readEnds, firstElementInChunk)){ // if has same position in read 1, add into chunk.
                    // fill if has pair in this chunk
                    if(rec.getReadPairedFlag() && !rec.getMateUnmappedFlag()){ // is a pair
                        if(rec.getReadNegativeStrandFlag())
                            hasPairNegative = true;
                        else
                            hasPairPositive = true;
                    }
                    addReadEndsIntoChunk(readEnds, rec, readNameToReadEndsMap, chunk);
                }
                else{ // not a same position, do mark duplicate in chunk
                    if(chunk.size() > 0) {
                        chunkDuplicateMarker.markDuplicateInChunk(chunk, hasPairPositive, hasPairNegative, duplicateIndexes);
                    }
                    chunk.clear();
                    firstElementInChunk = readEnds;
                    addReadEndsIntoChunk(readEnds, rec, readNameToReadEndsMap, chunk);

                    hasPairPositive = false;
                    hasPairNegative = false;
                    if(rec.getReadPairedFlag() && !rec.getMateUnmappedFlag()){ // is a pair
                        if(rec.getReadNegativeStrandFlag())
                            hasPairNegative = true;
                        else
                            hasPairPositive = true;
                    }
                }
            }
            readIndexInBamFile ++;
        }
        duplicateIndexes.doneAddingStartIteration();
        iterator.close();
        return duplicateIndexes;
    }

    /**
     * Add read ends into a chunk.
     * If a record is not pair, add into chunk directly.
     * If a record is pair-end, look up the map to find if another record in a pair is already in.
     * If found one, build a new readEnds for a pair, remove old readEnds from old chunk by using the reference stored in readEnds class.
     * @param readEnds new readEnds.
     * @param rec record for the readEnds
     * @param readNameToReadEndsMap map from readName to readEnds.
     * @param chunk chunk need to be added in.
     */
    private void addReadEndsIntoChunk(ReadEndsForMarkDuplicates readEnds, SAMRecord rec, Map<String, ReadEndsForMarkDuplicates> readNameToReadEndsMap, LinkedList<ReadEndsForMarkDuplicates> chunk){
        if (readEnds.isPair) { // pairEnds
            String key = rec.getReadName();
            if(readNameToReadEndsMap.containsKey(key)){ // if found another readEnds in chunks
                // remove from old chunks.
                ReadEndsForMarkDuplicates anotherReadEnds = readNameToReadEndsMap.get(key);
                readNameToReadEndsMap.remove(key);
                ReadEndsForMarkDuplicates pairEnds = buildPairEnds(readEnds, anotherReadEnds);
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
     * Compair if two readEnds' first read has same position(same read1ReferenceIndex and read1Coordinate)
     * @param readEnds1
     * @param readEnds2
     * @return True if one of argument is null. If both are not null, return true if read1 has same position in both readEnds.
     */
    private boolean inSamePositionOnRead1(final ReadEndsForMarkDuplicates readEnds1, final ReadEndsForMarkDuplicates readEnds2){
        return (readEnds1.read1ReferenceIndex == readEnds2.read1ReferenceIndex && readEnds1.read1Coordinate == readEnds2.read1Coordinate);
    }

    /**
     * Mix two pair-end readEnds into one.
     * Re-generate orientation for new pair end readEnds. keep read has small position in read1.
     * @param readEnds1 read ends 1
     * @param readEnds2 read ends 2
     * @return mixed
     */
    private ReadEndsForMarkDuplicates buildPairEnds(ReadEndsForMarkDuplicates readEnds1, ReadEndsForMarkDuplicates readEnds2){
        int sequenceId1 = readEnds1.read1ReferenceIndex;
        int sequenceId2 = readEnds2.read1ReferenceIndex;
        int coordinate1 = readEnds1.read1Coordinate;
        int coordinate2 = readEnds2.read1Coordinate;

        if (sequenceId1 < sequenceId2 || (sequenceId1 == sequenceId2 && coordinate1 <= coordinate2)) {
            readEnds1.isPair = true;
            readEnds1.read2ReferenceIndex = sequenceId2;
            readEnds1.read2Coordinate = coordinate2;
            readEnds1.read2IndexInFile = readEnds2.read1IndexInFile;
            readEnds1.orientation = ReadEndsForMarkDuplicates.getOrientationByte(readEnds1.orientation == ReadEndsForMarkDuplicates.R,
                    readEnds2.read1NegativeStrand);
            readEnds1.score += readEnds2.score;
            return readEnds1;
        } else {
            readEnds2.isPair = true;
            readEnds2.read2ReferenceIndex = sequenceId1;
            readEnds2.read2Coordinate = coordinate1;
            readEnds2.read2IndexInFile = readEnds1.read1IndexInFile;
            readEnds2.orientation = ReadEndsForMarkDuplicates.getOrientationByte(readEnds2.orientation == ReadEndsForMarkDuplicates.R,
                    readEnds1.read1NegativeStrand);
            readEnds2.score += readEnds1.score;
            return readEnds2;
        }
    }

    /**
     * Builds a read ends object that represents a single read.
     * @param index index in bam file of given read
     * @param rec samRecord, a read.
     * @return read end.
     */
    private ReadEndsForMarkDuplicates buildReadEnds(final long index, final SAMRecord rec) {
        final ReadEndsForMarkDuplicates ends = new ReadEndsForMarkDuplicates();
        if(rec.getReadPairedFlag() && !rec.getMateUnmappedFlag())
            ends.isPair = true;
        ends.read1ReferenceIndex = rec.getReferenceIndex();
        ends.read1Coordinate = rec.getReadNegativeStrandFlag() ? rec.getUnclippedEnd() : rec.getUnclippedStart();
        ends.orientation = rec.getReadNegativeStrandFlag() ? ReadEndsForMarkDuplicates.R : ReadEndsForMarkDuplicates.F;
        ends.read1IndexInFile = index;
        ends.read1NegativeStrand = rec.getReadNegativeStrandFlag();
        ends.score = DuplicateScoringStrategy.computeDuplicateScore(rec, DuplicateScoringStrategy.ScoringStrategy.SUM_OF_BASE_QUALITIES);

        //TODO debug
        ends.readName = rec.getReadName();


        // Doing this lets the ends object know that it is part of a pair
        if (rec.getReadPairedFlag() && !rec.getMateUnmappedFlag()) {
            ends.read2ReferenceIndex = rec.getMateReferenceIndex();
        }
        return ends;
    }

    /**
     * Write file into file, and marking duplicate for each record.
     * @param duplicateIndexes record all duplicate indexes.
     * @param inputFile undeduped file
     * @param outputFile output file to write
     */
    public void writeNoneDuplicateIntoFile(SortingLongCollection duplicateIndexes, String inputFile, String outputFile){
        final IOHelper.SamHeaderAndIterator headerAndIterator = IOHelper.openInput(inputFile);
        final SAMFileHeader header = headerAndIterator.header;
        final SAMFileHeader outputHeader = header.clone();
        outputHeader.setSortOrder(SAMFileHeader.SortOrder.coordinate);

        final SAMFileWriter out = IOHelper.getOutput(outputHeader, outputFile);
        final CloseableIterator<SAMRecord> iterator = headerAndIterator.iterator;

        long recordInFileIndex = 0;
        long nextDuplicateIndex = (duplicateIndexes.hasNext() ? duplicateIndexes.next() : -1);

        while(iterator.hasNext()){
            final SAMRecord rec = iterator.next();
            if (!rec.isSecondaryOrSupplementary()) {
                if (recordInFileIndex == nextDuplicateIndex) {
                    rec.setDuplicateReadFlag(true);
                    // Now try and figure out the next duplicate index
                    if (duplicateIndexes.hasNext()) {
                        nextDuplicateIndex = duplicateIndexes.next();
                    } else {
                        // Only happens once we've marked all the duplicates
                        nextDuplicateIndex = -1;
                    }
                } else {
                    rec.setDuplicateReadFlag(false);
                }
            }
            if (!rec.getDuplicateReadFlag() || !this.REMOVE_DUPLICATES) {
                out.addAlignment(rec);
            }
            recordInFileIndex++;
        }
        duplicateIndexes.cleanup();
        iterator.close();
        out.close();
    }

    private void run(String[] args){
        this.TMP_DIR.add(IOUtil.getDefaultTmpDir());

        parseArguments(args);
        IOUtil.assertFileIsReadable(new File(inputFile));
        IOUtil.assertFileIsWritable(new File(outputFile));

        log.info("Start to scan bam file to get duplicate index");
        ChunkDuplicateMarker chunkDuplicateMarker = new ChunkDuplicateMarker();
        SortingLongCollection duplicateIndexes = findDuplicateIndex(chunkDuplicateMarker, inputFile);
        writeNoneDuplicateIntoFile(duplicateIndexes, inputFile, outputFile);
    }

    public static void main(String[] args){
        new MarkDuplicates().run(args);
    }
}

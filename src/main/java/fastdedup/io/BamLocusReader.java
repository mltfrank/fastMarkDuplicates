package fastdedup.io;

import fastdedup.ReadEnds;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.CloseableIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Wang Bingchen on 2016/3/16.
 */
public class BamLocusReader {
    /** singleton */
    private BamLocusReader(){
        System.out.println("A new bamReader instance");
    }
    private static Map<String, BamLocusReader> instances = new ConcurrentHashMap<String, BamLocusReader>();

    private SamHeaderAndIterator samHeaderAndIterator = null;
    private long readFileIndex = -1;
    private int curLocus = 0;
    private int curContigIdx = 0;
    private ArrayList<SAMRecord> readBuffer = new ArrayList<SAMRecord>(); // store the records which are loaded in but not put into resultList yet.

    /**
     * static factory method
     * @param filepath file to read.
     * @return a reader instance. a file has only one instance at one time.
     */
    public static BamLocusReader getLocusReader(String filepath){
        if(!instances.containsKey(filepath)){
            synchronized (BamLocusReader.class){
                if(!instances.containsKey(filepath)){
                    SamHeaderAndIterator headerAndIterator = IOHelper.openInput(filepath);
                    BamLocusReader reader = new BamLocusReader();
                    reader.samHeaderAndIterator = headerAndIterator;
                    instances.put(filepath, reader);
                    return reader;
                }
            }

        }
        return instances.get(filepath);

    }

    /**
     * Read reads from current position to locusLength into targetList.
     * We alloc a readEnds object for each read. The indexInFile is marked.
     * Unmapped and secondary reads are not loaded in.
     * If meet a end of reference contig, the method is returned. On next call, this method will read from start of next contig.
     * If not meet a end of contig, it will count the length of locus from start coordinate to current coordinate. The range must less than locusLength
     * Return contains reads with coordinate equal to (curLocus + locusLength)
     * @param locusLength read locus length
     * @param targetList list to store readEnds. ReadEnds are not init yet, we left this step in threads.
     * @return count in targetList. if no read left in reader and buffer, return -1;
     */
    public synchronized long readRecords(int locusLength, List<SAMRecord> targetList){
        targetList.clear();
        this.curLocus += locusLength;  // read with coordinate less than this will be put into targetList
        int curLocus = this.curLocus;

        // put reads in buffer whose coordinate is before curLocus into target
        // reads not in range keep in readBuffer.
        ArrayList<SAMRecord> swapBuffer = new ArrayList<SAMRecord>();
        for(SAMRecord record : readBuffer){
            int coordinate = record.getReadNegativeStrandFlag() ? record.getUnclippedEnd() : record.getUnclippedStart();
            if(coordinate <= curLocus){
                targetList.add(record);
            }
            else{
                swapBuffer.add(record);
            }
        }
        readBuffer.clear();
        readBuffer = swapBuffer;

        // scan the file to see new reads
        try {
            final CloseableIterator<SAMRecord> iterator = samHeaderAndIterator.iterator;
            while (iterator.hasNext()) {
                SAMRecord record = iterator.next();
                readFileIndex++;
                if (record.getReadUnmappedFlag() || record.isSecondaryOrSupplementary())
                    continue;
                int thisCoordinate = record.getReadNegativeStrandFlag() ? record.getUnclippedEnd() : record.getUnclippedStart();
                record.setAttribute(ReadEnds.FILE_INDEX_TAG, Long.toString(readFileIndex));

                int contigIdx = record.getReferenceIndex();
                if (contigIdx != curContigIdx) { // Finish this method and jump to next contig.
                    curContigIdx = contigIdx;
                    this.curLocus = record.getAlignmentStart();  // mark as the start as it always larger than alignmentEnd
                    readBuffer.add(record);
                    return targetList.size();
                }
                if (thisCoordinate <= curLocus) {  // if in the region, just add into the target
                    targetList.add(record);
                } else {   // if not in the region, see if is negative read.
                    if (record.getReadNegativeStrandFlag()) { // if so, there may still have positive read following, so store it in buffer.
                        readBuffer.add(record);
                    } else {
                        readBuffer.add(record);   // if not, there's no reads in this position, just cut off.
                        return targetList.size();
                    }
                }
            }
            iterator.close(); // may close many times
        }catch (AssertionError e){  // iterator has been closed
            return -1;
        }
        if(readBuffer.size() == 0) {    // if no reads left, return -1 to represent.
            return -1;
        }
        return targetList.size();
    }

}

package markduplicates;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.SortingLongCollection;
import junit.framework.TestCase;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2016/3/11.
 */
public class TestBuildArray extends TestCase {

    public String getEndString(ReadEndsForMarkDuplicates readEnds){
        if(readEnds.isPair)
            return readEnds.read2ReferenceIndex + ":" + readEnds.read2Coordinate;
        else
            return readEnds.read1ReferenceIndex + ":" + readEnds.read1Coordinate;
    }

    public void testBuildArray(){
        // use a numb chunk maker to test build array
        ChunkDuplicateMarker chunkDuplicateMarker = new ChunkDuplicateMarker();
        MarkDuplicates markDuplicates = new MarkDuplicates();

        String inputfile = "sorted.bam";
        ClassLoader classLoader = getClass().getClassLoader();
        inputfile = classLoader.getResource(inputfile).getFile();
        //SortingLongCollection dup_idx = markDuplicates.findDuplicateIndex(chunkDuplicateMarker, inputfile);
        //while(dup_idx.hasNext()){
        //    System.out.println(dup_idx.next());
        //}

        /*List<LinkedList<ReadEndsForMarkDuplicates>> chunks = markDuplicates.chunks;
        for(LinkedList<ReadEndsForMarkDuplicates> chunk : chunks){
            String position = null;
            for(ReadEndsForMarkDuplicates readEnds : chunk){
                if(position == null)
                    position = getEndString(readEnds);
                else{
                    assertEquals(position, getEndString(readEnds));
                }
            }
        }*/
    }
}

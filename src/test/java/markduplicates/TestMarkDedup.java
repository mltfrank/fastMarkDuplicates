package markduplicates;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.SortingLongCollection;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/3/15.
 */
public class TestMarkDedup extends TestCase {

    public List<Long> getStandardAnswer(String inputfile){
        ClassLoader classLoader = getClass().getClassLoader();
        inputfile = classLoader.getResource(inputfile).getFile();
        IOHelper.SamHeaderAndIterator headerAndIterator = IOHelper.openInput(inputfile);

        System.out.println("Duplicate index in test file");
        List<Long> answer = new ArrayList<Long>();
        long idx = 0;
        while(headerAndIterator.iterator.hasNext()){
            SAMRecord record = headerAndIterator.iterator.next();
            if(record.getDuplicateReadFlag()) {
                answer.add(idx);
            }
            idx ++;
        }
        headerAndIterator.iterator.close();
        return answer;
    }

    public void testWriteDedupedFile(){
        List<Long> answer = getStandardAnswer("dedup.bam");

        ChunkDuplicateMarker chunkDuplicateMarker = new ChunkDuplicateMarker();
        MarkDuplicates markDuplicates = new MarkDuplicates();

        String inputfile = "sorted.bam";
        ClassLoader classLoader = getClass().getClassLoader();
        inputfile = classLoader.getResource(inputfile).getFile();
        SortingLongCollection dup_idx = markDuplicates.findDuplicateIndex(chunkDuplicateMarker, inputfile);

        int idx = 0;
        while(dup_idx.hasNext()){
            Long next_idx = dup_idx.next();
            Long answer_idx = answer.get(idx);
            assertEquals(next_idx, answer_idx);
            idx ++;
        }
        assertEquals(idx, answer.size());

        String outputfile = "output.dedup.bam";
        markDuplicates.writeNoneDuplicateIntoFile(dup_idx, inputfile, outputfile);
        // We can't ensure like this because the reader will skip duplicate read automatically
        // so, verify output file by yourself!

        /*IOHelper.SamHeaderAndIterator headerAndIterator = IOHelper.openInput(outputfile);
        List<String> myAnswer = new ArrayList<String>();
        if(headerAndIterator.iterator.hasNext()){
            SAMRecord record = headerAndIterator.iterator.next();
            if(record.getDuplicateReadFlag()) {
                myAnswer.add(record.getReadName());
            }
        }

        System.out.println("Answer");
        for(String name : answer){
            System.out.println(name);
        }
        System.out.println();
        System.out.println("My Answer");
        for(String name : myAnswer){
            System.out.println(name);
        }*/
    }
}

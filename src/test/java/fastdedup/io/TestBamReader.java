package fastdedup.io;

import fastdedup.ReadEnds;
import htsjdk.samtools.SAMRecord;
import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * Created by Wang Bingchen on 2016/3/16.
 */
public class TestBamReader extends TestCase {

    ClassLoader classLoader = getClass().getClassLoader();
    String sortedfile = classLoader.getResource("sorted.bam").getFile();

    public void testLocusReader(){
        assertEquals(BamLocusReader.getLocusReader(sortedfile), BamLocusReader.getLocusReader(sortedfile));
        BamLocusReader reader = BamLocusReader.getLocusReader(sortedfile);
        ArrayList<SAMRecord> target = new ArrayList<SAMRecord>();
        reader.readRecords(10000, target);
        assertEquals(0, target.size());
        reader.readRecords(20000, target);
        assertEquals(2, target.size());
        reader.readRecords(219290, target);
        assertEquals(9, target.size());
        reader.readRecords(1000000000, target);
        assertEquals("SRR1575247.5000", target.get(target.size() - 1).getReadName());
        while(reader.readRecords(10000000, target) != -1){;}

        assertEquals(-1, reader.readRecords(10000000, target));
    }




}

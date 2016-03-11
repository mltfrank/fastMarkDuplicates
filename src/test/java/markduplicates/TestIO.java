package markduplicates;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import junit.framework.TestCase;

/**
 * Created by Wang Bingchen on 2016/3/11.
 */
public class TestIO extends TestCase {
    public void testInput(){
        String inputfile = "sorted.bam";
        ClassLoader classLoader = getClass().getClassLoader();
        inputfile = classLoader.getResource(inputfile).getFile();
        IOHelper.SamHeaderAndIterator headerAndIterator = IOHelper.openInput(inputfile);
        assertEquals(headerAndIterator.header.getSortOrder(), SAMFileHeader.SortOrder.coordinate);
        if(headerAndIterator.iterator.hasNext()){
            SAMRecord record = headerAndIterator.iterator.next();
            assertEquals(record.getReadName(), "SRR1575247.1655");
            assertEquals(record.getAlignmentStart(), 151240);
        }
        headerAndIterator.iterator.close();


        inputfile = "sorted.sam";
        inputfile = classLoader.getResource(inputfile).getFile();
        headerAndIterator = IOHelper.openInput(inputfile);
        assertEquals(headerAndIterator.header.getSortOrder(), SAMFileHeader.SortOrder.coordinate);
        if(headerAndIterator.iterator.hasNext()){
            SAMRecord record = headerAndIterator.iterator.next();
            assertEquals(record.getReadName(), "SRR1575247.1655");
            assertEquals(record.getAlignmentStart(), 151240);
        }
        headerAndIterator.iterator.close();
    }

    public void testOutput(){
        String inputfile = "sorted.bam";
        ClassLoader classLoader = getClass().getClassLoader();
        inputfile = classLoader.getResource(inputfile).getFile();
        IOHelper.SamHeaderAndIterator headerAndIterator = IOHelper.openInput(inputfile);
        SAMFileHeader header = headerAndIterator.header;

        String outputfile = "testIO.bam";
        SAMFileWriter writer = IOHelper.getOutput(header, outputfile);

        if(headerAndIterator.iterator.hasNext()){
            SAMRecord record = headerAndIterator.iterator.next();
            writer.addAlignment(record);
        }
        writer.close();
        headerAndIterator.iterator.close();

        headerAndIterator = IOHelper.openInput(outputfile);
        if(headerAndIterator.iterator.hasNext()){
            SAMRecord record = headerAndIterator.iterator.next();
            assertEquals(record.getReadName(), "SRR1575247.1655");
            assertEquals(record.getAlignmentStart(), 151240);
        }
    }
}

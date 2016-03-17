package fastdedup.io;

import fastdedup.FindDuplicateWorker;
import fastdedup.ReadEnds;
import fastdedup.ReadEndsTool;
import junit.framework.TestCase;

import java.util.*;

/**
 * Created by Wang Bingchen on 2016/3/17.
 */
public class TestReadEndsListMarkDuplicate extends TestCase {

    private ReadEnds buildPairEnds(int read1ReferenceIndex, int read1Coordinate,
                                   int read2ReferenceIndex, int read2Coordinate,
                                   byte orientation,
                                   int read1IndexInFile, int read2IndexInFile,
                                   String readName, int score){
        ReadEnds readEnds = new ReadEnds();
        readEnds.read1ReferenceIndex = read1ReferenceIndex;
        readEnds.read1Coordinate = read1Coordinate;
        readEnds.read2ReferenceIndex = read2ReferenceIndex;
        readEnds.read2Coordinate = read2Coordinate;
        readEnds.orientation = orientation;
        readEnds.read1IndexInFile = read1IndexInFile;
        readEnds.read2IndexInFile = read2IndexInFile;
        readEnds.readName = readName;
        readEnds.score = (short)score;
        return readEnds;
    }

    private ArrayList<ReadEnds> buildTestPairEnds(){

        class comparator implements Comparator<ReadEnds> {
            public int compare(final ReadEnds lhs, final ReadEnds rhs) {
                return lhs.read2Coordinate - lhs.read2ReferenceIndex*12 + rhs.read1Coordinate;
            }
        }

        ArrayList<ReadEnds> result = new ArrayList<ReadEnds>();
        result.add(buildPairEnds(1, 111, 1, 222, ReadEnds.FR, 1, 2, "pair1", 1));
        result.add(buildPairEnds(1, 111, 1, 242, ReadEnds.FR, 3, 4, "pair2", 2));
        result.add(buildPairEnds(1, 223, 1, 242, ReadEnds.FR, 5, 6, "pair3", 2));
        result.add(buildPairEnds(1, 223, 1, 242, ReadEnds.FR, 7, 8, "pair4", 1));  // dup
        result.add(buildPairEnds(1, 234, 1, 256, ReadEnds.FR, 9, 10, "pair5", 1));
        result.add(buildPairEnds(1, 234, 2, 256, ReadEnds.FR, 11, 12, "pair6", 1));     //dup
        result.add(buildPairEnds(1, 234, 2, 256, ReadEnds.FR, 13, 14, "pair7", 2));
        result.add(buildPairEnds(1, 234, 2, 256, ReadEnds.RF, 15, 16, "pair8", 2));
        result.add(buildPairEnds(2, 234, 2, 256, ReadEnds.RF, 17, 18, "pair9", 2));
        result.add(buildPairEnds(2, 333, 2, 3323, ReadEnds.FF, 19, 20, "pair10", 2));
        result.add(buildPairEnds(2, 333, 2, 3323, ReadEnds.FF, 21, 22, "pair11", 2)); // dup

        Collections.sort(result, new comparator());
        return result;
    }

    public void testPairEndsListMarkDuplicate(){

        ClassLoader classLoader = getClass().getClassLoader();
        String sortedfile = classLoader.getResource("sorted.bam").getFile();

        FindDuplicateWorker worker = new FindDuplicateWorker(0, sortedfile);
        List<Long> duplicateIndexes = new ArrayList<Long>();
        worker.markDuplicateInPairEndsList(buildTestPairEnds(), duplicateIndexes);
        assertEquals(6, duplicateIndexes.size());
        assertTrue(duplicateIndexes.contains(new Long(7)));
        assertTrue(duplicateIndexes.contains(new Long(8)));
        assertTrue(duplicateIndexes.contains(new Long(11)));
        assertTrue(duplicateIndexes.contains(new Long(12)));
        assertTrue(duplicateIndexes.contains(new Long(21)));
        assertTrue(duplicateIndexes.contains(new Long(22)));
    }






    private ReadEnds buildSingleEnds(int read1ReferenceIndex, int read1Coordinate,
                                   byte orientation, int read1IndexInFile, boolean isPair, String readName, int score){
        ReadEnds readEnds = new ReadEnds();
        readEnds.isPair = isPair;
        readEnds.read1ReferenceIndex = read1ReferenceIndex;
        readEnds.read1Coordinate = read1Coordinate;
        readEnds.orientation = orientation;
        readEnds.read1IndexInFile = read1IndexInFile;
        readEnds.readName = readName;
        readEnds.score = (short)score;
        return readEnds;
    }


    public ArrayList<ReadEnds> buildSingleReadEnds(){
        ArrayList<ReadEnds> result = new ArrayList<ReadEnds>();
        result.add(buildSingleEnds(1, 111, ReadEnds.F, 1, true, "ReadPair1", 1));   // dup
        result.add(buildSingleEnds(1, 133, ReadEnds.R, 2, true, "ReadPair1", 1));   // dup
        result.add(buildSingleEnds(1, 111, ReadEnds.F, 3, true, "ReadPair2", 4));
        result.add(buildSingleEnds(1, 133, ReadEnds.R, 4, true, "ReadPair2", 1));
        result.add(buildSingleEnds(1, 111, ReadEnds.F, 5, true, "ReadPair3", 2));   // dup
        result.add(buildSingleEnds(1, 133, ReadEnds.R, 6, true, "ReadPair3", 2));   // dup
        result.add(buildSingleEnds(1, 133, ReadEnds.F, 7, false, "ReadPair4", 100));  // dup
        result.add(buildSingleEnds(1, 123, ReadEnds.F, 8, false, "ReadPair5", 1));

        result.add(buildSingleEnds(1, 133, ReadEnds.F, 9, true, "ReadPair6", 1));
        result.add(buildSingleEnds(2, 133, ReadEnds.R, 10, true, "ReadPair6", 1));

        result.add(buildSingleEnds(2, 443, ReadEnds.F, 11, true, "ReadPair7", 1));
        result.add(buildSingleEnds(2, 556, ReadEnds.F, 12, true, "ReadPair7", 1));
        result.add(buildSingleEnds(2, 443, ReadEnds.F, 13, true, "ReadPair8", 1));
        result.add(buildSingleEnds(2, 557, ReadEnds.F, 14, true, "ReadPair8", 1));
        result.add(buildSingleEnds(2, 443, ReadEnds.F, 15, true, "ReadPair9", 1));
        result.add(buildSingleEnds(2, 557, ReadEnds.F, 16, false, "ReadPair9", 1)); // dup, but can't happen


        result.add(buildSingleEnds(2, 999, ReadEnds.F, 17, false, "ReadPair10", 12));
        result.add(buildSingleEnds(2, 999, ReadEnds.F, 18, false, "ReadPair11", 1));    //dup
        result.add(buildSingleEnds(2, 999, ReadEnds.R, 19, false, "ReadPair12", 1));

        result.add(buildSingleEnds(3, 999, ReadEnds.R, 20, false, "ReadPair13", 10));
        result.add(buildSingleEnds(3, 999, ReadEnds.F, 21, false, "ReadPair14", 10));
        return result;
    }

    public void testFindDuplicateIndex(){
        ClassLoader classLoader = getClass().getClassLoader();
        String sortedfile = classLoader.getResource("sorted.bam").getFile();

        FindDuplicateWorker worker = new FindDuplicateWorker(0, sortedfile);
        List<Long> duplicateIndexes = new ArrayList<Long>();
        worker.findDuplicateIndexes(buildSingleReadEnds(), duplicateIndexes);

        assertEquals(7, duplicateIndexes.size());
        assertTrue(duplicateIndexes.contains(new Long(1)));
        assertTrue(duplicateIndexes.contains(new Long(2)));
        assertTrue(duplicateIndexes.contains(new Long(5)));
        assertTrue(duplicateIndexes.contains(new Long(6)));
        assertTrue(duplicateIndexes.contains(new Long(7)));
        assertTrue(duplicateIndexes.contains(new Long(16)));
        assertTrue(duplicateIndexes.contains(new Long(18)));
    }

}

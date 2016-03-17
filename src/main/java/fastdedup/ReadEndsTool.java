package fastdedup;

import java.util.Comparator;

/**
 * Created by Wang Bingchen on 2016/3/17.
 */
public class ReadEndsTool {
    /**
     * Mix two pair-end readEnds into one.
     * Re-generate orientation for new pair end readEnds. keep read has small position in read1.
     * @param readEnds1 read ends 1
     * @param readEnds2 read ends 2
     * @return mixed
     */
    public static ReadEnds buildPairEnds(ReadEnds readEnds1, ReadEnds readEnds2){
        int sequenceId1 = readEnds1.read1ReferenceIndex;
        int sequenceId2 = readEnds2.read1ReferenceIndex;
        int coordinate1 = readEnds1.read1Coordinate;
        int coordinate2 = readEnds2.read1Coordinate;

        if (sequenceId1 < sequenceId2 || (sequenceId1 == sequenceId2 && coordinate1 <= coordinate2)) {
            readEnds1.isPair = true;
            readEnds1.read2ReferenceIndex = sequenceId2;
            readEnds1.read2Coordinate = coordinate2;
            readEnds1.read2IndexInFile = readEnds2.read1IndexInFile;
            readEnds1.orientation = ReadEnds.getOrientationByte(readEnds1.orientation == ReadEnds.R,
                    readEnds2.read1NegativeStrand);
            readEnds1.score += readEnds2.score;
            return readEnds1;
        } else {
            readEnds2.isPair = true;
            readEnds2.read2ReferenceIndex = sequenceId1;
            readEnds2.read2Coordinate = coordinate1;
            readEnds2.read2IndexInFile = readEnds1.read1IndexInFile;
            readEnds2.orientation = ReadEnds.getOrientationByte(readEnds2.orientation == ReadEnds.R,
                    readEnds1.read1NegativeStrand);
            readEnds2.score += readEnds1.score;
            return readEnds2;
        }
    }


    /**
     * Comparator for unpaired ReadEnds
     * orders by read1 position
     * then pair orientation
     * then read1 index in file.
     * */
    public static class SingleReadEndsComparator implements Comparator<ReadEnds> {
        public int compare(final ReadEnds lhs, final ReadEnds rhs) {
            int compareDifference = lhs.read1ReferenceIndex - rhs.read1ReferenceIndex;
            if(compareDifference != 0) return compareDifference;
            compareDifference = lhs.read1Coordinate - rhs.read1Coordinate;
            if(compareDifference != 0) return compareDifference;
            compareDifference = lhs.orientation - rhs.orientation;
            if(compareDifference != 0) return compareDifference;
            compareDifference = (int) (lhs.read1IndexInFile - rhs.read1IndexInFile);
            return compareDifference;
        }
    }


    /**
     * Comparator for paired ReadEnds
     * orders by read1 position
     * then pair orientation
     * then read2 position.
     * at last index in file.
     * */
    public static class PairReadEndsComparator implements Comparator<ReadEnds> {
        public int compare(final ReadEnds lhs, final ReadEnds rhs) {
            int compareDifference = lhs.read1ReferenceIndex - rhs.read1ReferenceIndex;
            if(compareDifference != 0) return compareDifference;
            compareDifference = lhs.read1Coordinate - rhs.read1Coordinate;
            if(compareDifference != 0) return compareDifference;
            compareDifference = lhs.orientation - rhs.orientation;
            if (compareDifference != 0) return compareDifference;
            compareDifference = lhs.read2ReferenceIndex - rhs.read2ReferenceIndex;
            if (compareDifference != 0) return compareDifference;
            compareDifference = lhs.read2Coordinate - rhs.read2Coordinate;
            if(compareDifference != 0) return compareDifference;
            compareDifference = (int) (lhs.read1IndexInFile - rhs.read1IndexInFile);
            if (compareDifference != 0) return compareDifference;
            compareDifference = (int) (lhs.read2IndexInFile - rhs.read2IndexInFile);
            return compareDifference;
        }
    }
}

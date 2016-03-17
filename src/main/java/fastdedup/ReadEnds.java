package fastdedup;

import fastdedup.exceptions.DedupException;
import htsjdk.samtools.DuplicateScoringStrategy;
import htsjdk.samtools.SAMRecord;

/**
 * Created by Wang Bingchen
 * Date: 2016/2/25.
 * Update: 2016/3/16
 */
public class ReadEnds {
    /** used to add tag in record before build readEnds.
     * These tags won't write into file.
     */
    public static final String FILE_INDEX_TAG = "fx";

    public static final byte F = 0, R = 1, FF = 2, FR = 3, RR = 4, RF = 5;

    public boolean isPair = false;

    public short score;
    public long read1IndexInFile = -1;
    public long read2IndexInFile = -1;

    public byte orientation;
    public int read1ReferenceIndex = -1;
    public int read1Coordinate = -1;
    public boolean read1NegativeStrand = false;
    public int read2ReferenceIndex = -1;
    public int read2Coordinate = -1;

    public SAMRecord record = null;
    public String readName = "";

    public ReadEnds(){}
    public ReadEnds(SAMRecord record){
        if(record == null || record.getReadUnmappedFlag() || record.isSecondaryOrSupplementary())
            throw new DedupException("Record in readEnds is null or be unmapped or be secondary/supplementary");
        this.read1Coordinate = record.getReadNegativeStrandFlag() ? record.getUnclippedEnd() : record.getUnclippedStart();
        this.read1IndexInFile = Long.parseLong(record.getStringAttribute(FILE_INDEX_TAG));
        if(record.getReadPairedFlag() && !record.getMateUnmappedFlag())
            isPair = true;
        read1ReferenceIndex = record.getReferenceIndex();
        orientation = record.getReadNegativeStrandFlag() ? ReadEnds.R : ReadEnds.F;
        read1NegativeStrand = record.getReadNegativeStrandFlag();
        score = DuplicateScoringStrategy.computeDuplicateScore(record, DuplicateScoringStrategy.ScoringStrategy.SUM_OF_BASE_QUALITIES);
        readName = record.getReadName();
    }

    /**
     * Returns a single byte that encodes the orientation of the two reads in a pair.
     */
    public static byte getOrientationByte(final boolean read1NegativeStrand, final boolean read2NegativeStrand) {
        if (read1NegativeStrand) {
            if (read2NegativeStrand) return ReadEnds.RR;
            else return ReadEnds.RF;
        } else {
            if (read2NegativeStrand) return ReadEnds.FR;
            else return ReadEnds.FF;
        }
    }
}

package markduplicates;

import java.util.LinkedList;

/**
 * Created by Wang Bingchen
 * Date: 2016/2/25.
 */
public class ReadEndsForMarkDuplicates {
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


    // debug
    public String readName = "";

    /**
     * Returns a single byte that encodes the orientation of the two reads in a pair.
     */
    public static byte getOrientationByte(final boolean read1NegativeStrand, final boolean read2NegativeStrand) {
        if (read1NegativeStrand) {
            if (read2NegativeStrand) return ReadEndsForMarkDuplicates.RR;
            else return ReadEndsForMarkDuplicates.RF;
        } else {
            if (read2NegativeStrand) return ReadEndsForMarkDuplicates.FR;
            else return ReadEndsForMarkDuplicates.FF;
        }
    }
}

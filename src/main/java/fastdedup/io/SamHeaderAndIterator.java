package fastdedup.io;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.CloseableIterator;

/**
 * Created by Administrator on 2016/3/15.
 */
public class SamHeaderAndIterator {
    public final SAMFileHeader header;
    public final CloseableIterator<SAMRecord> iterator;

    public SamHeaderAndIterator(final SAMFileHeader header, final CloseableIterator<SAMRecord> iterator) {
        this.header = header;
        this.iterator = iterator;
    }
}

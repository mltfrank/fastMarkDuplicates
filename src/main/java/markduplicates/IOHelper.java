package markduplicates;

import htsjdk.samtools.*;
import htsjdk.samtools.util.CloseableIterator;

import java.io.File;

/**
 * IO helper for sam/bam file
 * @Author Wang Bingchen
 * @Date 2016/2/25.
 */
public class IOHelper {
    /** Little class used to package up a header and an iterable/iterator. */
    public static final class SamHeaderAndIterator {
        public final SAMFileHeader header;
        public final CloseableIterator<SAMRecord> iterator;

        public SamHeaderAndIterator(final SAMFileHeader header, final CloseableIterator<SAMRecord> iterator) {
            this.header = header;
            this.iterator = iterator;
        }
    }

    /**
     * Get a sam/bam header and read interval.
     * @param inputFilePath sam/bam file
     * @return can access head by obj.header, and read iterator by obj.iterator
     */
    public static SamHeaderAndIterator openInput(String inputFilePath){
        SamReader reader = SamReaderFactory.makeDefault()
                .enable(SamReaderFactory.Option.EAGERLY_DECODE)
                .open(SamInputResource.of(inputFilePath));

        SAMFileHeader header = reader.getFileHeader();
        if (header.getSortOrder() != SAMFileHeader.SortOrder.coordinate) {
            throw new PicardException("Input file " + inputFilePath + " is not coordinate sorted.");
        }
        return new SamHeaderAndIterator(header, reader.iterator());
    }

    /**
     * Open a bam file writer.
     * Unfortunately, it can't write sam file right now.
     * No index is built after write.
     * @param header
     * @param outputFilePath
     * @return
     */
    public static SAMFileWriter getOutput(SAMFileHeader header, String outputFilePath){
        return getOutput(header, new File(outputFilePath));
    }

    public static SAMFileWriter getOutput(SAMFileHeader header, File outputFile){
        final SAMFileWriter out = new SAMFileWriterFactory().makeSAMOrBAMWriter(header,
                true, outputFile);
        return out;
    }



}

package fastdedup.io;

import fastdedup.exceptions.DedupException;
import htsjdk.samtools.*;

import java.io.File;

/**
 * IO helper for sam/bam file
 * @Author Wang Bingchen
 * @Date 2016/2/25.
 */
public class IOHelper {
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
            throw new DedupException("Input file " + inputFilePath + " is not coordinate sorted.");
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

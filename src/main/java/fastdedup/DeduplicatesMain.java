package fastdedup;

import fastdedup.exceptions.DedupException;
import fastdedup.io.IOHelper;
import fastdedup.io.SamHeaderAndIterator;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.IOUtil;
import htsjdk.samtools.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * MarkDuplicates tools for sorted bam file.
 * I try to write a new tool which has same usage with the broad institute picard MarkDuplicates.
 * Besides, I simplify the algorithm of picard MarkDuplicates, by decrease the times of scan to 2 from 4.
 * A multi thread option is provided, which can make it run faster on multi node in a single node.
 * Created by Wang Bingchen on 2016/3/16.
 */
public class DeduplicatesMain {
    private final Log log = Log.getInstance(DeduplicatesMain.class);

    /** @argument input sam file */
    private String inputFile = null;
    /** @argument output bam file */
    private String outputFile = null;
    /** @argument remove duplicate flag */
    private boolean REMOVE_DUPLICATES = false;
    /** @argument chunk size for each thread */
    private int chunkIntervalSize = 10000;
    /** @argument worker count */
    private int threadCount = 1;

    private void parseArguments(String[] args){
        for(int i = 0; i < args.length; i ++){
            String arg = args[i];
            if(arg.startsWith("INPUT=")){
                this.inputFile = arg.substring(6);
            }
            else if(arg.startsWith("OUTPUT=")){
                this.outputFile = arg.substring(7);
            }
            else if(arg.equals("-D")){
                this.REMOVE_DUPLICATES = true;
            }
            else if(arg.startsWith("CHUNK_SIZE=")){
                this.chunkIntervalSize = Integer.parseInt(arg.substring(10));
            }
            else if(arg.startsWith("THREAD=")){
                this.threadCount = Integer.parseInt(arg.substring(7));
            }
            else{
                throw new DedupException("Can't parse argument '"+arg+"'");
            }
        }
    }


    /**
     * Write file into file, and marking duplicate for each record.
     * @param inputFile undeduped file
     * @param outputFile output file to write
     */
    public void writeNotDuplicateIntoFile(String inputFile, String outputFile){
        final SamHeaderAndIterator headerAndIterator = IOHelper.openInput(inputFile);
        final SAMFileHeader header = headerAndIterator.header;
        final SAMFileHeader outputHeader = header.clone();
        outputHeader.setSortOrder(SAMFileHeader.SortOrder.coordinate);

        Iterator<Long> dupIndexIteraor = GlobalDuplicateIndexPool.INSTANCE.getIterator();

        final SAMFileWriter out = IOHelper.getOutput(outputHeader, outputFile);
        final CloseableIterator<SAMRecord> iterator = headerAndIterator.iterator;

        long recordInFileIndex = 0;
        long nextDuplicateIndex = (dupIndexIteraor.hasNext() ? dupIndexIteraor.next() : -1);

        while(iterator.hasNext()){
            final SAMRecord rec = iterator.next();
            if (!rec.isSecondaryOrSupplementary()) {
                if (recordInFileIndex == nextDuplicateIndex) {
                    rec.setDuplicateReadFlag(true);
                    // Now try and figure out the next duplicate index
                    nextDuplicateIndex = (dupIndexIteraor.hasNext() ? dupIndexIteraor.next() : -1);
                } else {
                    rec.setDuplicateReadFlag(false);
                }
            }
            if (!rec.getDuplicateReadFlag() || !this.REMOVE_DUPLICATES) {
                out.addAlignment(rec);
            }
            recordInFileIndex++;
        }
        iterator.close();
        out.close();
    }

    public void run(String[] args){
        parseArguments(args);
        IOUtil.assertFileIsReadable(new File(inputFile));
        IOUtil.assertFileIsWritable(new File(outputFile));
        List<FindDuplicateWorker> workers = new ArrayList<FindDuplicateWorker>(this.threadCount);
        for(int i = 0; i < this.threadCount; i ++){
            FindDuplicateWorker worker = new FindDuplicateWorker(this.chunkIntervalSize, inputFile);
            workers.add(worker);
            worker.start();
        }
        try{
            for (FindDuplicateWorker worker : workers) {
                worker.join();
            }
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        writeNotDuplicateIntoFile(inputFile, outputFile);
    }

    public static void main(String[] args){
        new DeduplicatesMain().run(args);
    }
}

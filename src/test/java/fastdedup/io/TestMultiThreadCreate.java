package fastdedup.io;

import fastdedup.ReadEnds;
import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/3/17.
 */
public class TestMultiThreadCreate {

    public static void main(String[] args){

        final String sortedfile = "C:\\Users\\Administrator\\IdeaProjects\\FastDedup\\src\\test\\resources\\sorted.bam";

        class TestGetInstance extends Thread{
            @Override
            public void run(){
                for(int i = 0; i < 10; i ++) {
                    BamLocusReader reader = BamLocusReader.getLocusReader(sortedfile);
                    ArrayList<SAMRecord> chunk = new ArrayList<SAMRecord>();
                    reader.readRecords(0, chunk);
                }
            }
        }

        for(int i = 0; i < 100; i ++){
            new TestGetInstance().start();
        }
    }
}

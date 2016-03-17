package fastdedup.io;

import fastdedup.GlobalReadEndsPool;
import fastdedup.ReadEnds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2016/3/17.
 */
public class TestMulThreadPublicReadEndsPool {
    final static int THREAD_COUNT = 3;
    final static int PAIR_COUNT = 50;
    final static int UNPAIR_COUNT = 4;

    public static void main(String[] args){
        class TestGetInstance extends Thread{
            Map<String, ReadEnds> readEndsMap = null;
            TestGetInstance(Map<String, ReadEnds> readEndsMap){
                this.readEndsMap = readEndsMap;
            }

            @Override
            public void run(){
                List<ReadEnds> pickedList = new ArrayList<ReadEnds>();
                GlobalReadEndsPool.INSTANCE.pickAndPut(readEndsMap, pickedList);
                System.out.println(this.getName() + "   picked_size:" + pickedList.size() + "     poolSize: " + GlobalReadEndsPool.INSTANCE.getPublicPool().size());
            }
        }

        ArrayList<ReadEnds> readEndsesPair1 = new ArrayList<ReadEnds>();
        ArrayList<ReadEnds> readEndsesPair2 = new ArrayList<ReadEnds>();
        // build pair
        for(int i = 0; i < PAIR_COUNT; i ++){
            String readName = "readName"+i;

            ReadEnds readEnds = new ReadEnds();
            readEnds.readName = readName;
            readEndsesPair1.add(readEnds);

            ReadEnds readEndsClone = new ReadEnds();
            readEndsClone.readName = readName.substring(0);
            readEndsesPair2.add(readEndsClone);
        }
        //build some single(unpaired)
        for(int i = PAIR_COUNT; i < PAIR_COUNT + UNPAIR_COUNT; i ++){
            String readName = "readName"+i;
            ReadEnds readEnds = new ReadEnds();
            readEnds.readName = readName;
            readEndsesPair1.add(readEnds);
        }
        //build some single(unpaired)
        for(int i = PAIR_COUNT+UNPAIR_COUNT; i < PAIR_COUNT+UNPAIR_COUNT*2; i ++){
            String readName = "readName"+i;
            ReadEnds readEnds = new ReadEnds();
            readEnds.readName = readName;
            readEndsesPair2.add(readEnds);
        }
        ArrayList<Map<String, ReadEnds>> readEndsMapList = new ArrayList<Map<String, ReadEnds>>(THREAD_COUNT);
        for(int i = 0; i < THREAD_COUNT; i ++){
            readEndsMapList.add(new HashMap<String, ReadEnds>());
        }
        for(int i = 0; i < readEndsesPair1.size(); i ++){
            int putInThreadNo = i % THREAD_COUNT;
            ReadEnds readEnds1 = readEndsesPair1.get(i);
            String readName1 = readEnds1.readName;
            readEndsMapList.get(putInThreadNo).put(readName1, readEnds1);


            int putInThreadNo2 = (i+1) % THREAD_COUNT;
            ReadEnds readEnds2 = readEndsesPair2.get(i);
            String readName2 = readEnds2.readName;
            readEndsMapList.get(putInThreadNo2).put(readName2, readEnds2);
        }

        for(int i = 0; i < THREAD_COUNT; i ++){
            new TestGetInstance(readEndsMapList.get(i)).start();
        }
        int i = 0;
        int j = 1;
    }
}

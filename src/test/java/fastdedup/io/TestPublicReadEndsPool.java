package fastdedup.io;

import fastdedup.GlobalReadEndsPool;
import fastdedup.ReadEnds;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2016/3/17.
 */
public class TestPublicReadEndsPool extends TestCase {

    private ArrayList<ReadEnds> putTestReadsIn(){
        GlobalReadEndsPool.INSTANCE.clear();
        Map<String, ReadEnds> readEndsMap = new HashMap<String, ReadEnds>();
        ReadEnds readEnds1 = new ReadEnds();
        readEnds1.readName = "readName1";
        readEndsMap.put("readName1", readEnds1);
        ReadEnds readEnds2 = new ReadEnds();
        readEnds2.readName = "readName2";
        readEndsMap.put("readName2", readEnds2);
        ReadEnds readEnds3 = new ReadEnds();
        readEnds3.readName = "readName3";
        readEndsMap.put("readName3", readEnds3);

        ReadEnds readEnds4 = new ReadEnds();    // expect not insert because name "readName3" is already in.
        readEnds4.readName = "readName4";
        readEndsMap.put("readName3", readEnds4);

        ArrayList<ReadEnds> pickedList = new ArrayList<ReadEnds>();
        GlobalReadEndsPool.INSTANCE.pickAndPut(readEndsMap, pickedList);
        return pickedList;
    }

    public void testSingletonPoolPull(){
        ArrayList<ReadEnds> pickedList = putTestReadsIn();
        assertEquals(3, GlobalReadEndsPool.INSTANCE.getPublicPool().size());
        assertEquals(0, pickedList.size());
    }

    public void testSingletonPoolPick(){
        putTestReadsIn();
        ArrayList<ReadEnds> pickedList = new ArrayList<ReadEnds>();

        Map<String, ReadEnds> readEndsMap = new HashMap<String, ReadEnds>();
        ReadEnds readEnds1 = new ReadEnds();
        readEnds1.readName = "readName1";
        readEndsMap.put("readName1", readEnds1);
        ReadEnds readEnds2 = new ReadEnds();
        readEnds2.readName = "readName2";
        readEndsMap.put("readName2", readEnds2);
        GlobalReadEndsPool.INSTANCE.pickAndPut(readEndsMap, pickedList);
        assertEquals(2, pickedList.size());
        assertEquals(1, GlobalReadEndsPool.INSTANCE.getPublicPool().size());
    }

}

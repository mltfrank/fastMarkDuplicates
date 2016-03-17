package fastdedup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A public readEnds pool to store unmatched pairEnds from each thread.
 * After process the bundle of readEnds, each thread operate this pool in a mode.
 * Search the read of each unmatched pairEnds in this pool, if found one, pick it out.
 * If not found, put it in to wait for another read.
 *
 * The pick/put method must be synchronized because multi threads may call this method at a time.
 * Synchronized method make sure that if a pairEnds is picked from the pool, all pairEnds with
 * same coordinate must be picked from pool by one thread. Then this thread will remove duplicate
 * on these pairEnds with same coordinate.
 *
 * Created by Wang Bingchen on 2016/3/17.
 */
public enum GlobalReadEndsPool {
    INSTANCE;

    private Map<String, ReadEnds> publicPool = new ConcurrentHashMap<String, ReadEnds>();

    /**
     * Test Method
     * @return
     */
    public Map<String, ReadEnds> getPublicPool(){
        return publicPool;
    }


    public synchronized void pickAndPut(Map<String, ReadEnds> workerMap, List<ReadEnds> pickReads){
        for(Map.Entry<String, ReadEnds> entry: workerMap.entrySet()){
            String readName = entry.getKey();
            ReadEnds readEnds = entry.getValue();
            ReadEnds readEndsInPool = publicPool.get(readName);
            if(readEndsInPool != null) {
                publicPool.remove(readName);
                pickReads.add(ReadEndsTool.buildPairEnds(readEnds, readEndsInPool));
            }
            else{
                publicPool.put(readName, readEnds);
            }
        }
    }

    /**
     * Clear all elements in public pool
     */
    public void clear(){
        publicPool.clear();
    }
}

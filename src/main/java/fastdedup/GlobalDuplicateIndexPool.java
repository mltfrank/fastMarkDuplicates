package fastdedup;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Created by Wang Bingchen on 2016/3/17.
 */
public enum GlobalDuplicateIndexPool {
    INSTANCE;
    ConcurrentSkipListSet<Long> indexPool = new ConcurrentSkipListSet();

    public void addAll(List<Long> duplicateIndexes){
        indexPool.addAll(duplicateIndexes);
    }

    public Iterator<Long> getIterator(){
        return indexPool.iterator();
    }

}

package utility;

import java.util.HashMap;
import java.util.Map;
/*
 * Reference:http://tutorials.jenkov.com/java-concurrency/read-write-locks.html
 * */

public class MutexLock {
    private Map<Thread, Integer> readingThreads =new HashMap<Thread, Integer>();

    private int writeAccesses = 0;
    private int writeRequests = 0;
    private Thread writingThread = null;

    public synchronized void lockWrite() throws InterruptedException {
        System.out.println(Thread.currentThread().getName()+" is requesting write lock");
        writeRequests++;
        Thread callingThread = Thread.currentThread();
        while(! canGrantWriteAccess(callingThread)){
    	    System.out.println(Thread.currentThread().getName()+" is waiting for write lock");
            wait();
        }
   
        System.out.println(Thread.currentThread().getName()+" got write lock");
        writeRequests--;
        writeAccesses++;
        writingThread = callingThread;
        if (writingThread != null) {
            System.out.println(writingThread.getName() + " is writing");
        }
    }

    public synchronized void unlockWrite() throws InterruptedException{
	    System.out.println(Thread.currentThread().getName()+" is releasing write lock");
        writeAccesses--;
        if(writeAccesses == 0){
            writingThread = null;
        }
        notifyAll();
    }

    private boolean canGrantWriteAccess(Thread callingThread){
        if(hasReaders())
            return false;
        if(writingThread == null)
            return true;
        if(!isWriter(callingThread))
            return false;
        return true;
    }

    private boolean hasReaders(){
        return readingThreads.size() > 0;
    }

    private boolean isWriter(Thread callingThread){
        return writingThread == callingThread;
    }

    public synchronized void lockRead() throws InterruptedException {
        System.out.println(Thread.currentThread().getName()+" is requesting read lock");
        Thread callingThread = Thread.currentThread();

        while(!canGrantReadAccess(callingThread)){
            System.out.println(Thread.currentThread().getName()+" is waiting for read lock");
            wait();
        }
        System.out.println(Thread.currentThread().getName()+" got read lock");
        readingThreads.put(callingThread, (getReadAccessCount(callingThread) + 1));
        System.out.println(Thread.currentThread().getName()+ " is reading");
    }


    public synchronized void unlockRead(){
        System.out.println(Thread.currentThread().getName()+" is releasing read lock");
        Thread callingThread = Thread.currentThread();
        int accessCount = getReadAccessCount(callingThread);
        if(accessCount == 1){ readingThreads.remove(callingThread); }
        else { readingThreads.put(callingThread, (accessCount -1)); }
        notifyAll();
    }

    private boolean canGrantReadAccess(Thread callingThread){
        if(writingThread != null)
            return false;
        if(isReader(callingThread))
            return true;
        if(writeRequests > 0)
            return false;
        return true;
    }

    private int getReadAccessCount(Thread callingThread) {
        Integer accessCount = readingThreads.get(callingThread);
        if(accessCount == null) return 0;
            return accessCount.intValue();
    }

    private boolean isReader(Thread callingThread){
return readingThreads.get(callingThread) != null;
    }
}
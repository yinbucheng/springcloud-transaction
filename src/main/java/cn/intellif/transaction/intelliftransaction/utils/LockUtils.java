package cn.intellif.transaction.intelliftransaction.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockUtils {
    private static Map<String,LockUtils> locks = new ConcurrentHashMap<>();

    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    public static LockUtils getLock(String key){
        return locks.get(key);
    }

    public static void removeLock(String key){
         locks.remove(key);
    }

    public static void initLock(String key){
        LockUtils lockUtils = new LockUtils();
        locks.put(key,lockUtils);
    }

    public  void await(){
        try{
            lock.lock();
            condition.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }


    public void signal(){
        try{
            lock.lock();
            condition.signal();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    public void await(int second){
        try{
            lock.lock();
            condition.await(second, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}

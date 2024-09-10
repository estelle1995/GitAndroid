package com.example.myokdownload.dowload.core.file;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.core.log.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class FileLock {
    private static final String TAG = "FileLock";

    private final Map<String, AtomicInteger> fileLockCountMap = new HashMap<>();
    private final Map<String, Thread> waitThreadForFileLockMap = new HashMap<>();

    FileLock(Map<String, AtomicInteger> fileLockCountMap, Map<String, Thread> waitThreadForFileLockMap) {
        this.fileLockCountMap.putAll(fileLockCountMap);
        this.waitThreadForFileLockMap.putAll(waitThreadForFileLockMap);
    }

    FileLock() {}

    public void increaseLock(@NonNull String path) {
        AtomicInteger lockCount;
        synchronized (fileLockCountMap) {
            lockCount = fileLockCountMap.get(path);
        }
        if (lockCount == null) {
            lockCount = new AtomicInteger(0);
            synchronized (fileLockCountMap) {
                fileLockCountMap.put(path, lockCount);
            }
        }
        LogUtil.d(TAG, "increaseLock increase lock-count to" + lockCount.incrementAndGet() + path);
    }

    public void decreaseLock(@NonNull String path) {
        AtomicInteger lockCount;
        synchronized (fileLockCountMap) {
            lockCount = fileLockCountMap.get(path);
        }
        if (lockCount != null && lockCount.decrementAndGet() == 0) {
            LogUtil.d(TAG, "decreaseLock decrease lock-count to 0 " + path);
            final Thread lockedThread;
            synchronized (waitThreadForFileLockMap) {
                lockedThread = waitThreadForFileLockMap.get(path);
                if (lockedThread != null) {
                    waitThreadForFileLockMap.remove(path);
                }
            }

            if (lockedThread != null) {
                LogUtil.d(TAG, "decreaseLock " + path + " unpark locked thread " + lockCount);
                unpark(lockedThread);
            }

            synchronized (fileLockCountMap) {
                fileLockCountMap.remove(path);
            }
        }
    }

    private static final long WAIT_RELEASE_LOCK_NANO = TimeUnit.MILLISECONDS.toNanos(100);

    public void waitForRelease(@NonNull String filePath) {
        AtomicInteger lockCount;
        synchronized (fileLockCountMap) {
            lockCount = fileLockCountMap.get(filePath);
        }
        if (lockCount == null || lockCount.get() <= 0) return;
        synchronized (waitThreadForFileLockMap) {
            waitThreadForFileLockMap.put(filePath, Thread.currentThread());
        }
        LogUtil.d(TAG, "waitForRelease start " + filePath);
        while (true) {
            if (isNotLocked(lockCount)) break;
            park();
        }
        LogUtil.d(TAG, "waitForRelease finish " + filePath);
    }

    boolean isNotLocked(AtomicInteger lockCount) {
        return lockCount.get() <= 0;
    }

    void park() {
        LockSupport.park(WAIT_RELEASE_LOCK_NANO);
    }

    void unpark(@NonNull Thread lockedThread) {
        LockSupport.unpark(lockedThread);
    }
}

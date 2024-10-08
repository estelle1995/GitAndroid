package com.example.myokdownload.dowload.core.dispatcher;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.StatusUtil;
import com.example.myokdownload.dowload.core.IdentifiedTask;
import com.example.myokdownload.dowload.core.breakpoint.DownloadStore;
import com.example.myokdownload.dowload.core.cause.EndCause;
import com.example.myokdownload.dowload.core.download.DownloadCall;
import com.example.myokdownload.dowload.core.log.LogUtil;
import com.example.myokdownload.dowload.core.thread.ThreadUtil;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadDispatcher {
    private static final String TAG = "DownloadDispatcher";

    int maxParallelRunningCount = 5;

    private final List<DownloadCall> readyAsyncCalls;
    private final List<DownloadCall> runningAsyncCalls;
    private final List<DownloadCall> runningSyncCalls;

    private final List<DownloadCall> finishingCalls;

    private final AtomicInteger flyingCanceledAsyncCallCount = new AtomicInteger();

    private @Nullable volatile ExecutorService executorService;

    private final AtomicInteger skipProceedCallCount = new AtomicInteger();

    private DownloadStore store;

    public DownloadDispatcher() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    DownloadDispatcher(List<DownloadCall> readyAsyncCalls, List<DownloadCall> runningAsyncCalls,
                       List<DownloadCall> runningSyncCalls, List<DownloadCall> finishingCalls) {
        this.readyAsyncCalls = readyAsyncCalls;
        this.runningSyncCalls = runningSyncCalls;
        this.runningAsyncCalls = runningAsyncCalls;
        this.finishingCalls = finishingCalls;
    }

    public void setDownloadStore(@NonNull DownloadStore store) {
        this.store = store;
    }

    synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                    ThreadUtil.threadFactory("OkDownload Download", false));
        }

        return executorService;
    }

    private synchronized void enqueueIgnorePriority(DownloadTask task) {
        final DownloadCall call = DownloadCall.create(task, true, store);
        if (runningAsyncSize() < maxParallelRunningCount) {
            runningAsyncCalls.add(call);
            getExecutorService().execute(call);
        } else {
            readyAsyncCalls.add(call);
        }
    }

    private int runningAsyncSize() {
        return runningAsyncCalls.size() - flyingCanceledAsyncCallCount.get();
    }

    public synchronized void flyingCanceled(DownloadCall call) {
        LogUtil.d(TAG, "flying canceled: " + call.task.getId());
        if (call.asyncExecuted) flyingCanceledAsyncCallCount.incrementAndGet();
    }

    public synchronized boolean isFileConflictAfterRun(@NonNull DownloadTask task) {
        LogUtil.d(TAG, "is file conflict after run: " + task.getId());
        final File file = task.getFile();
        if (file == null) return false;

        for (DownloadCall syncCall: runningSyncCalls) {
            if (syncCall.isCanceled() || syncCall.task == task) continue;
            final File otherFile = syncCall.task.getFile();
            if (otherFile != null && file.equals(otherFile)) {
                return true;
            }
        }

        for (DownloadCall asyncCall : runningAsyncCalls) {
            if (asyncCall.isCanceled() || asyncCall.task == task) continue;

            final File otherFile = asyncCall.task.getFile();
            if (otherFile != null && file.equals(otherFile)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public synchronized DownloadTask findSameTask(DownloadTask task) {
        LogUtil.d(TAG, "findSameTask: " + task.getId());
        for (DownloadCall call : readyAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return call.task;
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return call.task;
        }

        for (DownloadCall call : runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return call.task;
        }

        return null;
    }

    public synchronized void finish(DownloadCall call) {
        final boolean asyncExecuted = call.asyncExecuted;
        final Collection<DownloadCall> calls;
        if (finishingCalls.contains(call)) {
            calls = finishingCalls;
        } else if (asyncExecuted) {
            calls = runningAsyncCalls;
        } else {
            calls = runningSyncCalls;
        }
        if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
        if (asyncExecuted && call.isCanceled()) flyingCanceledAsyncCallCount.decrementAndGet();
        if (asyncExecuted) processCalls();
    }

    private synchronized void processCalls() {
        if (skipProceedCallCount.get() > 0) return;
        if (runningAsyncSize() >= maxParallelRunningCount) return;

        for (Iterator<DownloadCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            DownloadCall call = i.next();

            i.remove();

            final DownloadTask task = call.task;
            if (isFileConflictAfterRun(task)) {
                OKDownload.with().callbackDispatcher().dispatch().taskEnd(task, EndCause.FILE_BUSY,
                        null);
                continue;
            }

            runningAsyncCalls.add(call);
            getExecutorService().execute(call);

            if (runningAsyncSize() >= maxParallelRunningCount) return;
        }
    }

    public synchronized boolean isRunning(DownloadTask task) {
        LogUtil.d(TAG, "isRunning: " + task.getId());
        for (DownloadCall call : runningSyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) {
                return true;
            }
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) {
                return true;
            }
        }

        return false;
    }

    public synchronized boolean isPending(DownloadTask task) {
        LogUtil.d(TAG, "isPending: " + task.getId());
        for (DownloadCall call : readyAsyncCalls) {
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) return true;
        }

        return false;
    }

    boolean inspectCompleted(@NonNull DownloadTask task) {
        return inspectCompleted(task, null);
    }

    boolean inspectCompleted(@NonNull DownloadTask task, @Nullable Collection<DownloadTask> completedCollection) {
        if (task.isPassIfAlreadyCompleted() && StatusUtil.isCompleted(task)) {
            if (task.getFilename() == null && !OKDownload.with().downloadStrategy().validFilenameFromStore(task)) {
                return false;
            }

            OKDownload.with().downloadStrategy().validInfoOnCompleted(task, store);

            if (completedCollection != null) {
                completedCollection.add(task);
            } else {
                OKDownload.with().callbackDispatcher().dispatch().taskEnd(task, EndCause.COMPLETED, null);
            }
        }
        return false;
    }

    public void enqueue(DownloadTask[] tasks) {
        skipProceedCallCount.incrementAndGet();
        enqueueLocked(tasks);
        skipProceedCallCount.decrementAndGet();
    }

    private synchronized void enqueueLocked(DownloadTask[] tasks) {
        final long startTime = SystemClock.uptimeMillis();
        LogUtil.d(TAG, "start enqueueLocked for bunch task: " + tasks.length);

        final List<DownloadTask> taskList = new ArrayList<>();
        Collections.addAll(taskList, tasks);
        if (taskList.size() > 1) Collections.sort(taskList);

        final int originReadyAsyncCallSize = readyAsyncCalls.size();
        try {
            OKDownload.with().downloadStrategy().inspectNetworkAvailable();

            final Collection<DownloadTask> completedTaskList = new ArrayList<>();
            final Collection<DownloadTask> sameTaskConflictList = new ArrayList<>();
            final Collection<DownloadTask> fileBusyList = new ArrayList<>();

            for (DownloadTask task : taskList) {
                if (inspectCompleted(task, completedTaskList)) continue;
                if (inspectForConflict(task, sameTaskConflictList, fileBusyList)) continue;

                enqueueIgnorePriority(task);
            }
            OKDownload.with().callbackDispatcher()
                    .endTasks(completedTaskList, sameTaskConflictList, fileBusyList);
        } catch (UnknownHostException e) {
            final Collection<DownloadTask> errorList = new ArrayList<>(taskList);
            OKDownload.with().callbackDispatcher().endTasksWithError(errorList, e);
        }

        if (originReadyAsyncCallSize != readyAsyncCalls.size()) Collections.sort(readyAsyncCalls);

        LogUtil.d(TAG, "end enqueueLocked for bunch task: " + tasks.length + " consume "
                + (SystemClock.uptimeMillis() - startTime) + "ms");
    }

    public void enqueue(DownloadTask task) {
        skipProceedCallCount.incrementAndGet();
        enqueueLocked(task);
        skipProceedCallCount.decrementAndGet();
    }

    private synchronized void enqueueLocked(DownloadTask task) {
        LogUtil.d(TAG, "enqueueLocked for single task: " + task);
        if (inspectCompleted(task)) return;
        if (inspectForConflict(task)) return;

        final int originReadyAsyncCallSize = readyAsyncCalls.size();
        enqueueIgnorePriority(task);
        if (originReadyAsyncCallSize != readyAsyncCalls.size()) Collections.sort(readyAsyncCalls);
    }

    public void execute(DownloadTask task) {
        LogUtil.d(TAG, "execute: " + task);
        final DownloadCall call;

        synchronized (this) {
            if (inspectCompleted(task)) return;
            if (inspectForConflict(task)) return;

            call = DownloadCall.create(task, false, store);
            runningSyncCalls.add(call);
        }

        syncRunCall(call);
    }

    // this method convenient for unit-test.
    void syncRunCall(DownloadCall call) {
        call.run();
    }

    private boolean inspectForConflict(@NonNull DownloadTask task) {
        return inspectForConflict(task, null, null);
    }

    private boolean inspectForConflict(@NonNull DownloadTask task,
                                       @Nullable Collection<DownloadTask> sameTaskList,
                                       @Nullable Collection<DownloadTask> fileBusyList) {
        return inspectForConflict(task, readyAsyncCalls, sameTaskList, fileBusyList)
                || inspectForConflict(task, runningAsyncCalls, sameTaskList, fileBusyList)
                || inspectForConflict(task, runningSyncCalls, sameTaskList, fileBusyList);
    }

    boolean inspectForConflict(@NonNull DownloadTask task, @NonNull Collection<DownloadCall> calls,
                               @Nullable Collection<DownloadTask> sameTaskList, @Nullable Collection<DownloadTask> fileBusyList) {
        final CallbackDispatcher callbackDispatcher = OKDownload.with().callbackDispatcher();
        final Iterator<DownloadCall> iterator = calls.iterator();
        while (iterator.hasNext()) {
            DownloadCall call = iterator.next();
            if (call.isCanceled()) continue;
            if (call.equalsTask(task)) {
                if (call.isFinishing()) {
                    LogUtil.d(TAG, "task: " + task.getId()
                            + " is finishing, move it to finishing list");
                    finishingCalls.add(call);
                    iterator.remove();
                }

                if (sameTaskList != null) {
                    sameTaskList.add(task);
                } else {
                    callbackDispatcher.dispatch()
                            .taskEnd(task, EndCause.SAME_TASK_BUSY, null);
                }
                return true;
            }

            final File file = call.getFile();
            final File taskFile = task.getFile();
            if (file != null && taskFile != null && file.equals(taskFile)) {
                if (fileBusyList != null) {
                    fileBusyList.add(task);
                } else {
                    callbackDispatcher.dispatch().taskEnd(task, EndCause.FILE_BUSY, null);
                }
                return true;
            }
        }
        return false;
    }

    private synchronized void filterCanceledCalls(@NonNull IdentifiedTask task,
                                                  @NonNull List<DownloadCall> needCallbackCalls,
                                                  @NonNull List<DownloadCall> needCancelCalls) {
        for (Iterator<DownloadCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            DownloadCall call = i.next();
            if (call.task == task || call.task.getId() == task.getId()) {
                if (call.isCanceled() || call.isFinishing()) return;

                i.remove();
                needCallbackCalls.add(call);
                return;
            }
        }

        for (DownloadCall call : runningAsyncCalls) {
            if (call.task == task || call.task.getId() == task.getId()) {
                needCallbackCalls.add(call);
                needCancelCalls.add(call);
                return;
            }
        }

        for (DownloadCall call : runningSyncCalls) {
            if (call.task == task || call.task.getId() == task.getId()) {
                needCallbackCalls.add(call);
                needCancelCalls.add(call);
                return;
            }
        }
    }

    public void cancel(IdentifiedTask[] tasks) {
        skipProceedCallCount.incrementAndGet();
        cancelLocked(tasks);
        skipProceedCallCount.decrementAndGet();
        processCalls();
    }

    public boolean cancel(IdentifiedTask task) {
        skipProceedCallCount.incrementAndGet();
        final boolean result = cancelLocked(task);
        skipProceedCallCount.decrementAndGet();
        processCalls();
        return result;
    }

    public boolean cancel(int id) {
        skipProceedCallCount.incrementAndGet();
        final boolean result = cancelLocked(DownloadTask.mockTaskForCompare(id));
        skipProceedCallCount.decrementAndGet();
        processCalls();
        return result;
    }

    public void cancelAll() {
        skipProceedCallCount.incrementAndGet();
        // assemble tasks
        List<DownloadTask> taskList = new ArrayList<>();
        for (DownloadCall call : readyAsyncCalls) taskList.add(call.task);
        for (DownloadCall call : runningAsyncCalls) taskList.add(call.task);
        for (DownloadCall call : runningSyncCalls) taskList.add(call.task);

        if (!taskList.isEmpty()) {
            DownloadTask[] tasks = new DownloadTask[taskList.size()];
            cancelLocked(taskList.toArray(tasks));
        }

        skipProceedCallCount.decrementAndGet();
    }

    private synchronized void cancelLocked(IdentifiedTask[] tasks) {
        final long startCancelTime = SystemClock.uptimeMillis();
        LogUtil.d(TAG, "start cancel bunch task manually: " + tasks.length);

        final List<DownloadCall> needCallbackCalls = new ArrayList<>();
        final List<DownloadCall> needCancelCalls = new ArrayList<>();
        try {
            for (IdentifiedTask task : tasks) {
                filterCanceledCalls(task, needCallbackCalls, needCancelCalls);
            }
        } finally {
            handleCanceledCalls(needCallbackCalls, needCancelCalls);
            LogUtil.d(TAG,
                    "finish cancel bunch task manually: " + tasks.length + " consume "
                            + (SystemClock.uptimeMillis() - startCancelTime) + "ms");
        }
    }

    synchronized boolean cancelLocked(IdentifiedTask task) {
        LogUtil.d(TAG, "cancel manually: " + task.getId());
        final List<DownloadCall> needCallbackCalls = new ArrayList<>();
        final List<DownloadCall> needCancelCalls = new ArrayList<>();

        try {
            filterCanceledCalls(task, needCallbackCalls, needCancelCalls);
        } finally {
            handleCanceledCalls(needCallbackCalls, needCancelCalls);
        }

        return needCallbackCalls.size() > 0 || needCancelCalls.size() > 0;
    }

    private synchronized void handleCanceledCalls(@NonNull List<DownloadCall> needCallbackCalls,
                                                  @NonNull List<DownloadCall> needCancelCalls) {
        LogUtil.d(TAG, "handle cancel calls, cancel calls: " + needCancelCalls.size());
        if (!needCancelCalls.isEmpty()) {
            for (DownloadCall call : needCancelCalls) {
                if (!call.cancel()) {
                    needCallbackCalls.remove(call);
                }
            }
        }

        LogUtil.d(TAG, "handle cancel calls, callback cancel event: " + needCallbackCalls.size());
        if (!needCallbackCalls.isEmpty()) {
            if (needCallbackCalls.size() <= 1) {
                final DownloadCall call = needCallbackCalls.get(0);
                OKDownload.with().callbackDispatcher().dispatch().taskEnd(call.task,
                        EndCause.CANCELED,
                        null);
            } else {
                List<DownloadTask> callbackCanceledTasks = new ArrayList<>();
                for (DownloadCall call : needCallbackCalls) {
                    callbackCanceledTasks.add(call.task);
                }
                OKDownload.with().callbackDispatcher().endTasksWithCanceled(callbackCanceledTasks);
            }
        }
    }

    public static void setMaxParallelRunningCount(int maxParallelRunningCount) {
        DownloadDispatcher dispatcher = OKDownload.with().downloadDispatcher();
        if (dispatcher.getClass() != DownloadDispatcher.class) {
            throw new IllegalStateException(
                    "The current dispatcher is " + dispatcher + " not DownloadDispatcher exactly!");
        }

        maxParallelRunningCount = Math.max(1, maxParallelRunningCount);
        dispatcher.maxParallelRunningCount = maxParallelRunningCount;
    }
}

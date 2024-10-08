package com.example.myokdownload.dowload.core.dispatcher;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadListener;
import com.example.myokdownload.dowload.DownloadMonitor;
import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.OKDownload;
import com.example.myokdownload.dowload.core.breakpoint.BreakpointInfo;
import com.example.myokdownload.dowload.core.cause.EndCause;
import com.example.myokdownload.dowload.core.cause.ResumeFailedCause;
import com.example.myokdownload.dowload.core.log.LogUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Dispatch callback to listeners
public class CallbackDispatcher {
    private static final String TAG = "CallbackDispatcher";

    // Just transmit to the main looper.
    private final DownloadListener transmit;

    private final Handler uiHandler;

    CallbackDispatcher(@NonNull Handler handler, @NonNull DownloadListener transmit) {
        this.uiHandler = handler;
        this.transmit = transmit;
    }

    public CallbackDispatcher() {
        this.uiHandler = new Handler(Looper.getMainLooper());
        this.transmit = new DefaultTransmitListener(uiHandler);
    }

    public boolean isFetchProcessMoment(DownloadTask task) {
        final long minInterval = task.getMinIntervalMillisCallbackProcess();
        final long now = SystemClock.uptimeMillis();
        return minInterval <= 0 || now - DownloadTask.TaskHideWrapper.getLastCallbackProcessTs(task) >= minInterval;
    }

    public void endTasksWithError(@NonNull final Collection<DownloadTask> errorCollection,
                                  @NonNull final Exception realCause) {
        if (errorCollection.size() <= 0) return;

        LogUtil.d(TAG, "endTasksWithError error[" + errorCollection.size() + "] realCause: "
                + realCause);

        final Iterator<DownloadTask> iterator = errorCollection.iterator();
        while (iterator.hasNext()) {
            final DownloadTask task = iterator.next();
            if (!task.isAutoCallbackToUIThread()) {
                task.getListener().taskEnd(task, EndCause.ERROR, realCause);
                iterator.remove();
            }
        }

        uiHandler.post(new Runnable() {
            @Override public void run() {
                for (DownloadTask task : errorCollection) {
                    task.getListener().taskEnd(task, EndCause.ERROR, realCause);
                }
            }
        });
    }

    public void endTasks(@NonNull final Collection<DownloadTask> completedTaskCollection,
                         @NonNull final Collection<DownloadTask> sameTaskConflictCollection,
                         @NonNull final Collection<DownloadTask> fileBusyCollection) {
        if (completedTaskCollection.isEmpty() && sameTaskConflictCollection.isEmpty()
                && fileBusyCollection.isEmpty()) {
            return;
        }

        LogUtil.d(TAG, "endTasks completed[" + completedTaskCollection.size()
                + "] sameTask[" + sameTaskConflictCollection.size()
                + "] fileBusy[" + fileBusyCollection.size() + "]");

        if (!completedTaskCollection.isEmpty()) {
            final Iterator<DownloadTask> iterator = completedTaskCollection.iterator();
            while (iterator.hasNext()) {
                final DownloadTask task = iterator.next();
                if (!task.isAutoCallbackToUIThread()) {
                    task.getListener().taskEnd(task, EndCause.COMPLETED, null);
                    iterator.remove();
                }
            }
        }


        if (!sameTaskConflictCollection.isEmpty()) {
            final Iterator<DownloadTask> iterator = sameTaskConflictCollection.iterator();
            while (iterator.hasNext()) {
                final DownloadTask task = iterator.next();
                if (!task.isAutoCallbackToUIThread()) {
                    task.getListener().taskEnd(task, EndCause.SAME_TASK_BUSY, null);
                    iterator.remove();
                }
            }
        }

        if (fileBusyCollection.size() > 0) {
            final Iterator<DownloadTask> iterator = fileBusyCollection.iterator();
            while (iterator.hasNext()) {
                final DownloadTask task = iterator.next();
                if (!task.isAutoCallbackToUIThread()) {
                    task.getListener().taskEnd(task, EndCause.FILE_BUSY, null);
                    iterator.remove();
                }
            }
        }

        if (completedTaskCollection.size() == 0 && sameTaskConflictCollection.size() == 0
                && fileBusyCollection.size() == 0) {
            return;
        }

        uiHandler.post(new Runnable() {
            @Override public void run() {
                for (DownloadTask task : completedTaskCollection) {
                    task.getListener().taskEnd(task, EndCause.COMPLETED, null);
                }
                for (DownloadTask task : sameTaskConflictCollection) {
                    task.getListener().taskEnd(task, EndCause.SAME_TASK_BUSY, null);
                }
                for (DownloadTask task : fileBusyCollection) {
                    task.getListener().taskEnd(task, EndCause.FILE_BUSY, null);
                }
            }
        });
    }

    public void endTasksWithCanceled(@NonNull final Collection<DownloadTask> canceledCollection) {
        if (canceledCollection.size() <= 0) return;

        LogUtil.d(TAG, "endTasksWithCanceled canceled[" + canceledCollection.size() + "]");

        final Iterator<DownloadTask> iterator = canceledCollection.iterator();
        while (iterator.hasNext()) {
            final DownloadTask task = iterator.next();
            if (!task.isAutoCallbackToUIThread()) {
                task.getListener().taskEnd(task, EndCause.CANCELED, null);
                iterator.remove();
            }
        }

        uiHandler.post(new Runnable() {
            @Override public void run() {
                for (DownloadTask task : canceledCollection) {
                    task.getListener().taskEnd(task, EndCause.CANCELED, null);
                }
            }
        });
    }

    public DownloadListener dispatch() {
        return transmit;
    }

    static class DefaultTransmitListener implements DownloadListener {
        @NonNull private final Handler uiHandler;

        DefaultTransmitListener(@NonNull Handler uiHandler) {
            this.uiHandler = uiHandler;
        }

        @Override
        public void taskStart(@NonNull DownloadTask task) {
            LogUtil.d(TAG, "taskStart: " + task.getId());
            inspectTaskStart(task);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().taskStart(task);
                    }
                });
            } else {
                task.getListener().taskStart(task);
            }
        }

        @Override
        public void connectTrialStart(@NonNull DownloadTask task, @NonNull Map<String, List<String>> requestHeaderFields) {
            LogUtil.d(TAG, "-----> start trial task(" + task.getId() + ") " + requestHeaderFields);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().connectTrialStart(task, requestHeaderFields);
                    }
                });
            } else {
                task.getListener().connectTrialStart(task, requestHeaderFields);
            }
        }

        @Override
        public void connectTrialEnd(@NonNull DownloadTask task, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {
            LogUtil.d(TAG, "<----- finish trial task(" + task.getId()
                    + ") code[" + responseCode + "]" + responseHeaderFields);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener()
                                .connectTrialEnd(task, responseCode, responseHeaderFields);
                    }
                });
            } else {
                task.getListener()
                        .connectTrialEnd(task, responseCode, responseHeaderFields);
            }
        }

        @Override
        public void downloadFromBeginning(@NonNull DownloadTask task, @NonNull BreakpointInfo info, @NonNull ResumeFailedCause cause) {
            LogUtil.d(TAG, "downloadFromBeginning: " + task.getId());
            inspectDownloadFromBeginning(task, info, cause);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().downloadFromBeginning(task, info, cause);
                    }
                });
            } else {
                task.getListener().downloadFromBeginning(task, info, cause);
            }
        }

        @Override
        public void downloadFromBreakpoint(@NonNull DownloadTask task, @NonNull BreakpointInfo info) {
            LogUtil.d(TAG, "downloadFromBreakpoint: " + task.getId());
            inspectDownloadFromBreakpoint(task, info);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().downloadFromBreakpoint(task, info);
                    }
                });
            } else {
                task.getListener().downloadFromBreakpoint(task, info);
            }
        }

        @Override
        public void connectStart(@NonNull DownloadTask task, int blockIndex, @NonNull Map<String, List<String>> requestHeaderFields) {
            LogUtil.d(TAG, "-----> start connection task(" + task.getId()
                    + ") block(" + blockIndex + ") " + requestHeaderFields);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().connectStart(task, blockIndex, requestHeaderFields);
                    }
                });
            } else {
                task.getListener().connectStart(task, blockIndex, requestHeaderFields);
            }
        }

        @Override
        public void connectEnd(@NonNull DownloadTask task, int blockIndex, int responseCode, @NonNull Map<String, List<String>> responseHeaderFields) {
            LogUtil.d(TAG, "<----- finish connection task(" + task.getId() + ") block("
                    + blockIndex + ") code[" + responseCode + "]" + responseHeaderFields);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().connectEnd(task, blockIndex, responseCode,
                               responseHeaderFields);
                    }
                });
            } else {
                task.getListener().connectEnd(task, blockIndex, responseCode,
                        responseHeaderFields);
            }
        }

        @Override
        public void fetchStart(@NonNull DownloadTask task, int blockIndex, long contentLength) {
            LogUtil.d(TAG, "fetchStart: " + task.getId());
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().fetchStart(task, blockIndex, contentLength);
                    }
                });
            } else {
                task.getListener().fetchStart(task, blockIndex, contentLength);
            }
        }

        @Override
        public void fetchProgress(@NonNull DownloadTask task, int blockIndex, long increaseBytes) {
            if (task.getMinIntervalMillisCallbackProcess() > 0) {
                DownloadTask.TaskHideWrapper
                        .setLastCallbackProcessTs(task, SystemClock.uptimeMillis());
            }

            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().fetchProgress(task, blockIndex, increaseBytes);
                    }
                });
            } else {
                task.getListener().fetchProgress(task, blockIndex, increaseBytes);
            }
        }

        @Override
        public void fetchEnd(@NonNull DownloadTask task, int blockIndex, long contentLength) {
            LogUtil.d(TAG, "fetchEnd: " + task.getId());
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().fetchEnd(task, blockIndex, contentLength);
                    }
                });
            } else {
                task.getListener().fetchEnd(task, blockIndex, contentLength);
            }
        }

        @Override
        public void taskEnd(@NonNull DownloadTask task, @NonNull EndCause cause, @Nullable Exception realCause) {
            if (cause == EndCause.ERROR) {
                // only care about error.
                LogUtil.d(TAG, "taskEnd: " + task.getId() + " " + cause + " " + realCause);
            }
            inspectTaskEnd(task, cause, realCause);
            if (task.isAutoCallbackToUIThread()) {
                uiHandler.post(new Runnable() {
                    @Override public void run() {
                        task.getListener().taskEnd(task, cause, realCause);
                    }
                });
            } else {
                task.getListener().taskEnd(task, cause, realCause);
            }
        }

        void inspectDownloadFromBreakpoint(@NonNull DownloadTask task,
                                           @NonNull BreakpointInfo info) {
            final DownloadMonitor monitor = OKDownload.with().getMonitor();
            if (monitor != null) monitor.taskDownloadFromBreakpoint(task, info);
        }

        void inspectDownloadFromBeginning(@NonNull DownloadTask task,
                                          @NonNull BreakpointInfo info,
                                          @NonNull ResumeFailedCause cause) {
            final DownloadMonitor monitor = OKDownload.with().getMonitor();
            if (monitor != null) monitor.taskDownloadFromBeginning(task, info, cause);
        }

        void inspectTaskStart(DownloadTask task) {
            final DownloadMonitor monitor = OKDownload.with().getMonitor();
            if (monitor != null) monitor.taskStart(task);
        }

        void inspectTaskEnd(final DownloadTask task, final EndCause cause,
                            @Nullable final Exception realCause) {
            final DownloadMonitor monitor = OKDownload.with().getMonitor();
            if (monitor != null) monitor.taskEnd(task, cause, realCause);
        }
    }
}

package com.example.myokdownload.dowload.core.breakpoint;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myokdownload.dowload.DownloadTask;
import com.example.myokdownload.dowload.core.IdentifiedTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BreakpointStoreOnCache implements DownloadStore {
    @NonNull private final KeyToIdMap keyToIdMap;
    private final SparseArray<BreakpointInfo> storedInfos;

    private final HashMap<String, String> responseFilenameMap;

    private final SparseArray<IdentifiedTask> unStoredTasks;

    private final List<Integer> sortedOccupiedIds;

    private final List<Integer> fileDirtyList;

    public BreakpointStoreOnCache() {
        this(new SparseArray<BreakpointInfo>(), new ArrayList<Integer>(),
                new HashMap<String, String>());
    }

    BreakpointStoreOnCache(SparseArray<BreakpointInfo> storedInfos,
                           List<Integer> fileDirtyList,
                           HashMap<String, String> responseFilenameMap,
                           SparseArray<IdentifiedTask> unStoredTasks,
                           List<Integer> sortedOccupiedIds,
                           KeyToIdMap keyToIdMap) {
        this.storedInfos = storedInfos;
        this.keyToIdMap = keyToIdMap;
        this.unStoredTasks = unStoredTasks;
        this.sortedOccupiedIds = sortedOccupiedIds;
        this.fileDirtyList = fileDirtyList;
        this.responseFilenameMap = responseFilenameMap;
    }

    public BreakpointStoreOnCache(SparseArray<BreakpointInfo> storedInfos,
                                  List<Integer> fileDirtyList,
                                  HashMap<String, String> responseFilenameMap) {
        this.unStoredTasks = new SparseArray<>();
        this.storedInfos = storedInfos;
        this.keyToIdMap = new KeyToIdMap();
        this.fileDirtyList = fileDirtyList;
        this.responseFilenameMap = responseFilenameMap;

        final int count = storedInfos.size();

        sortedOccupiedIds = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            sortedOccupiedIds.add(storedInfos.valueAt(i).id);
        }
        Collections.sort(sortedOccupiedIds);
    }

    @Nullable
    @Override
    public BreakpointInfo get(int id) {
        return storedInfos.get(id);
    }

    @Override
    public int findOrCreateId(@NonNull DownloadTask task) {
        final Integer candidate = keyToIdMap.get(task);
        if (candidate != null) return candidate;

        final int size = storedInfos.size();
        for (int i = 0; i < size; i++) {
            final BreakpointInfo info = storedInfos.valueAt(i);
            if (info != null && info.isSameFrom(task)) {
                return info.id;
            }
        }

        final int unStoredSize = unStoredTasks.size();
        for (int i = 0; i < unStoredSize; i++) {
            final IdentifiedTask another = unStoredTasks.valueAt(i);
            if (another == null) continue;
            if (another.compareIgnoreId(task)) return another.getId();
        }

        final int id = allocateId();
        unStoredTasks.put(id, task.mock(id));
        keyToIdMap.add(task, id);
        return id;
    }

    @Override
    public boolean isOnlyMemoryCache() {
        return true;
    }

    @Override
    public boolean isFileDirty(int id) {
        return fileDirtyList.contains(id);
    }

    @Nullable
    @Override
    public String getResponseFilename(String url) {
        return responseFilenameMap.get(url);
    }

    public static final int FIRST_ID = 1;

    synchronized int allocateId() {
        int newId = 0;

        int index = 0;

        int preId = 0;
        int curId;

        for (int i = 0; i < sortedOccupiedIds.size(); i++) {
            final Integer curIdObj = sortedOccupiedIds.get(i);
            if (curIdObj == null) {
                index = i;
                newId = preId + 1;
                break;
            }

            curId = curIdObj;
            if (preId == 0) {
                if (curId != FIRST_ID) {
                    newId = FIRST_ID;
                    index = 0;
                    break;
                }
                preId = curId;
                continue;
            }

            if (curId != preId + 1) {
                newId = preId + 1;
                index = i;
                break;
            }

            preId = curId;
        }

        if (newId == 0) {
            if (sortedOccupiedIds.isEmpty()) {
                newId = FIRST_ID;
            } else {
                newId = sortedOccupiedIds.get(sortedOccupiedIds.size() - 1) + 1;
                index = sortedOccupiedIds.size();
            }
        }

        sortedOccupiedIds.add(index, newId);

        return newId;
    }
}

package com.example.myokdownload.dowload.core;

import androidx.annotation.NonNull;

import com.example.myokdownload.dowload.core.connection.DownloadConnection;

import java.util.List;
import java.util.Map;

public class Util {
    public static final int CHUNKED_CONTENT_LENGTH = -1;

    public static void addRequestHeaderFields(
            @NonNull Map<String, List<String>> headerFields,
            @NonNull DownloadConnection connection) {
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            for (String value : values) {
                connection.addHeader(key, value);
            }
        }
    }
}

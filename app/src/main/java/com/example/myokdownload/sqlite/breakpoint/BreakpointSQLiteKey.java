/*
 * Copyright (c) 2017 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myokdownload.sqlite.breakpoint;

public interface BreakpointSQLiteKey {
    String ID = "id";
    String URL = "url";
    String ETAG = "etag";

    String PARENT_PATH = "parent_path";
    String FILENAME = "filename";
    String TASK_ONLY_PARENT_PATH = "task_only_parent_path";
    String CHUNKED = "chunked";

    String HOST_ID = "breakpoint_id";
    String BLOCK_INDEX = "block_index";
    String START_OFFSET = "start_offset";
    String CONTENT_LENGTH = "content_length";
    String CURRENT_OFFSET = "current_offset";
}

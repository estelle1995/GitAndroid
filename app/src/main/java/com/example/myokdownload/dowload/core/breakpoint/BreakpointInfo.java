package com.example.myokdownload.dowload.core.breakpoint;

import java.util.List;

public class BreakpointInfo {
    private List<BlockInfo> blockInfoList;

    public BlockInfo getBlock(int blockIndex) {
        return blockInfoList.get(blockIndex);
    }
}

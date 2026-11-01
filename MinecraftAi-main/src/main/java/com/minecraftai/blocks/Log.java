package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Log extends Block {
    public Log(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        switch (face) {
            case TOP, BOTTOM:
                return 5;
            default:
                return 4;
        }
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.LOG;
    }
}
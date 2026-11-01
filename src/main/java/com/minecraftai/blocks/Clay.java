package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Clay extends Block {
    public Clay(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 20;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.CLAY_BALL;
    }
}

package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Sand extends Block {
    public Sand(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 23;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.SAND;
    }
}

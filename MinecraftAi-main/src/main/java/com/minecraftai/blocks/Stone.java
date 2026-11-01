package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Stone extends Block {
    public Stone(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 2;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.COBBLESTONE;
    }
}
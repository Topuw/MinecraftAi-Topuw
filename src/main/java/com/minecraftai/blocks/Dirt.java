package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Dirt extends Block {
    public Dirt(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 1;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.DIRT;
    }
}
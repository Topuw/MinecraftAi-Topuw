package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Leaves extends Block {
    public Leaves(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 6;
    }

    @Override
    public ItemType getItemDrop() {
        return null;
    }
}
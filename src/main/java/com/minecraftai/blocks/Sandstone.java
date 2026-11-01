package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Sandstone extends Block {
    public Sandstone(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        if (face == Face.TOP) return 28;
        if (face == Face.BOTTOM) return 29;
        return 27;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.SANDSTONE;
    }
}

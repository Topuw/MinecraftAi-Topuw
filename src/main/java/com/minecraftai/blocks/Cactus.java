package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Cactus extends Block {
    public Cactus(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        if (face == Face.TOP) return 25;
        if (face == Face.BOTTOM) return 26;
        return 24;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.CACTUS;
    }
}

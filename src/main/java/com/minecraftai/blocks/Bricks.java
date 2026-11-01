package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Bricks extends Block {
    public Bricks(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 22;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.BRICKS;
    }
}

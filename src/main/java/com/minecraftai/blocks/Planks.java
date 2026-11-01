package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Planks extends Block {
    public Planks(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 9;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.PLANKS;
    }
}
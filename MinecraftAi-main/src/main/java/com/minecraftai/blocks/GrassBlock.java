package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class GrassBlock extends Block {
    public GrassBlock(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        switch (face) {
            case TOP:
                return 0;
            default:
                return 1;
        }
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.DIRT;
    }
}
package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class KebabBlock extends Block {
    public KebabBlock(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 18;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.KEBAB_BLOCK;
    }
}
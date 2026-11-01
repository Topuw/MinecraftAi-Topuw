package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class KebabOre extends Block {
    public KebabOre(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 17;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.KEBAB_ORE;
    }
}
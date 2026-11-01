package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Terracotta extends Block {
    public Terracotta(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 21;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.TERRACOTTA;
    }
}

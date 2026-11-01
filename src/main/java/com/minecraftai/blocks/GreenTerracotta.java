package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class GreenTerracotta extends Block {
    public GreenTerracotta(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 31;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.GREEN_TERRACOTTA;
    }
}

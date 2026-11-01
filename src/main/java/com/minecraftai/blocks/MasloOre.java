package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class MasloOre extends Block {
    public MasloOre(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        return 19;
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.MASLO_ORE;
    }
}
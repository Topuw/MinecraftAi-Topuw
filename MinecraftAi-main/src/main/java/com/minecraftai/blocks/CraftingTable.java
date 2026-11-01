package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class CraftingTable extends Block {
    public CraftingTable(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public int getTextureIndex(Face face) {
        if (face == Face.TOP) {
            return 10;
        } else if (face == Face.BOTTOM) {
            return 9;
        } else if (face == Face.NORTH) {
            return 12;
        } else {
            return 11;
        }
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.CRAFTING_TABLE;
    }
}
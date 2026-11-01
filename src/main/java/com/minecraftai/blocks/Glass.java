package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Glass extends Block {
    public Glass(int x, int y, int z) {
        super(x, y, z);
        this.isTransparent = true;
    }

    @Override
    public int getTextureIndex(Face face) {
        return 30;
    }

    @Override
    public boolean isSolid() {
        return true;
    }

    @Override
    public ItemType getItemDrop() {
        return null;
    }
}

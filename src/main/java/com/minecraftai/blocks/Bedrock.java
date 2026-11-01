package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Bedrock extends Block {
    public Bedrock(int x, int y, int z) {
        super(x, y, z);
        this.isDestructible = false;
    }

    @Override
    public int getTextureIndex(Face face) {
        return 8;
    }

    @Override
    public ItemType getItemDrop() {
        return null;
    }
}
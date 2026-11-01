package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class Water extends Block {
    public Water(int x, int y, int z) {
        super(x, y, z);
        this.isTransparent = true;
        this.blockHeight = 0.875f;
        this.isDestructible = false;
    }

    public int getLevel() {
        return 8;
    }

    @Override
    public int getTextureIndex(Face face) {
        return 7;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean collidesWithPlayer(float playerX, float playerY, float playerZ) {
        return false;
    }

    @Override
    public ItemType getItemDrop() {
        return null;
    }
}
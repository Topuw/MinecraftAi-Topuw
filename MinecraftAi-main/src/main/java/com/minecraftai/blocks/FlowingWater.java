package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;

public class FlowingWater extends Block {
    private int level = 7;

    public FlowingWater(int x, int y, int z) {
        this(x, y, z, 7);
    }

    public FlowingWater(int x, int y, int z, int level) {
        super(x, y, z);
        this.isTransparent = true;
        this.isDestructible = false;
        setLevel(level);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        if (level < 1) level = 1;
        if (level > 8) level = 8;
        this.level = level;
        this.blockHeight = level / 8.0f;
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
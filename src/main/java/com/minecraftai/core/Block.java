package com.minecraftai.core;

import static org.lwjgl.opengl.GL11.*;

public abstract class Block {
    protected int x, y, z;
    protected float blockHeight = 1.0f;
    protected boolean isTransparent = false;
    protected boolean isDestructible = true;

    public enum Face { TOP, BOTTOM, NORTH, SOUTH, EAST, WEST }

    public Block(int x, int y, int z) {
        this.x = x; this.y = y; this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public abstract int getTextureIndex(Face face);

    public abstract ItemType getItemDrop();

    public boolean collidesWithPlayer(float playerX, float playerY, float playerZ) {
        float playerWidth = 0.3f;
        float playerHeight = 1.7f;

        float px0 = playerX - playerWidth;
        float px1 = playerX + playerWidth;
        float py0 = playerY;
        float py1 = playerY + playerHeight;
        float pz0 = playerZ - playerWidth;
        float pz1 = playerZ + playerWidth;

        float bx0 = x;
        float bx1 = x + 1;
        float by0 = y;
        float by1 = y + this.blockHeight;
        float bz0 = z;
        float bz1 = z + 1;

        boolean collideX = px1 > bx0 && px0 < bx1;
        boolean collideY = py1 > by0 && py0 < by1;
        boolean collideZ = pz1 > bz0 && pz0 < bz1;

        return collideX && collideY && collideZ;
    }

    public boolean isSolid() {
        return !isTransparent;
    }

    public boolean isTransparent() {
        return isTransparent;
    }

    public boolean isDestructible() {
        return isDestructible;
    }

    public float getBlockHeight() {
        return blockHeight;
    }

    public void setTransparent(float alpha) {
        glColor4f(1.0f, 1.0f, 1.0f, alpha);
    }

    public void setOpaque() {
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
package com.minecraftai.core;

import com.minecraftai.renderer.TextureAtlas;
import static org.lwjgl.opengl.GL11.*;

public class DroppedItem {
    private float x, y, z;
    private float vx, vy, vz;
    private ItemType type;
    private float age;

    public DroppedItem(float x, float y, float z, ItemType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
        this.age = (float) (Math.random() * 100.0);

        this.vx = (float) (Math.random() - 0.5f) * 1.5f;
        this.vz = (float) (Math.random() - 0.5f) * 1.5f;
        this.vy = 3.0f;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public ItemType getType() { return type; }

    public void update(float dt, World world, Player player) {
        age += dt;

        vy -= 9.8f * dt;

        float nextX = x + vx * dt;
        float nextY = y + vy * dt;
        float nextZ = z + vz * dt;

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(nextY);
        int blockZ = (int) Math.floor(z);

        Block bBelow = world.getBlockAt(blockX, blockY, blockZ);
        if (bBelow != null && bBelow.isSolid()) {
            y = blockY + 1.0f;
            vy = 0.0f;
            vx = 0.0f;
            vz = 0.0f;
        } else {
            y = nextY;
        }

        int nextBlockX = (int) Math.floor(nextX);
        Block bX = world.getBlockAt(nextBlockX, (int) Math.floor(y), blockZ);
        if (bX != null && bX.isSolid()) {
            vx = 0.0f;
        } else {
            x = nextX;
        }

        int nextBlockZ = (int) Math.floor(nextZ);
        Block bZ = world.getBlockAt(blockX, (int) Math.floor(y), nextBlockZ);
        if (bZ != null && bZ.isSolid()) {
            vz = 0.0f;
        } else {
            z = nextZ;
        }
    }

    public void render() {
        glPushMatrix();

        float hover = (float) Math.sin(age * 3.0f) * 0.1f;
        glTranslatef(x, y + 0.15f + hover, z);

        glOriginalRotate();

        glScalef(0.25f, 0.25f, 0.25f);

        if (type == ItemType.STICK || type == ItemType.WOODEN_PICKAXE || type == ItemType.KEBAB) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, type.getTextureId());
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            float r = 0.5f;
            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f); glVertex3f(-r, -r, 0.0f);
            glTexCoord2f(1.0f, 0.0f); glVertex3f(r, -r, 0.0f);
            glTexCoord2f(1.0f, 1.0f); glVertex3f(r, r, 0.0f);
            glTexCoord2f(0.0f, 1.0f); glVertex3f(-r, r, 0.0f);
            glEnd();

            glDisable(GL_BLEND);
            glDisable(GL_TEXTURE_2D);
            glPopMatrix();
            return;
        }

        float r = 0.5f;

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.TOP)),
                 -r,  r, -r,
                 -r,  r,  r,
                  r,  r,  r,
                  r,  r, -r);

        glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.BOTTOM)),
                 -r, -r, -r,
                  r, -r, -r,
                  r, -r,  r,
                 -r, -r,  r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.EAST)),
                  r, -r, -r,
                  r,  r, -r,
                  r,  r,  r,
                  r, -r,  r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.WEST)),
                 -r, -r, -r,
                 -r, -r,  r,
                 -r,  r,  r,
                 -r,  r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.NORTH)),
                 -r, -r, -r,
                  r, -r, -r,
                  r,  r, -r,
                 -r,  r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.SOUTH)),
                 -r, -r,  r,
                 -r,  r,  r,
                  r,  r,  r,
                  r, -r,  r);

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    private void glOriginalRotate() {
        glRotatef(age * 60.0f, 0.0f, 1.0f, 0.0f);
    }

    private void drawFace(float[] uv, float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3) {
        glBegin(GL_QUADS);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x0, y0, z0);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x1, y1, z1);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y2, z2);
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x3, y3, z3);
        glEnd();
    }

    private int getBlockTextureIndex(ItemType type, Block.Face face) {
        switch (type) {
            case DIRT:
                return 1;
            case STONE:
                return 2;
            case COBBLESTONE:
                return 3;
            case LOG:
                if (face == Block.Face.TOP || face == Block.Face.BOTTOM) {
                    return 5;
                } else {
                    return 4;
                }
            case PLANKS:
                return 9;
            case CRAFTING_TABLE:
                if (face == Block.Face.TOP) {
                    return 10;
                } else if (face == Block.Face.BOTTOM) {
                    return 9;
                } else if (face == Block.Face.NORTH) {
                    return 12;
                } else {
                    return 11;
                }
            case FURNACE:
                if (face == Block.Face.TOP || face == Block.Face.BOTTOM) {
                    return 13;
                } else if (face == Block.Face.NORTH) {
                    return 15;
                } else {
                    return 14;
                }
            case KEBAB_ORE:
                return 17;
            case KEBAB_BLOCK:
                return 18;
            default:
                return 0;
        }
    }
}
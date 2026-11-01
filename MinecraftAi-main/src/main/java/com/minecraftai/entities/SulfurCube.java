package com.minecraftai.entities;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.Player;
import com.minecraftai.core.World;
import com.minecraftai.renderer.TextureAtlas;
import com.minecraftai.renderer.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class SulfurCube {
    private float x, y, z;
    private float vx, vy, vz;
    private float age;
    private float size = 0.6f;
    private ItemType absorbedBlock = null;
    
    private boolean onGround = false;
    private float hopTimer = 1.0f;
    
    private static int innerTextureID = 0;
    private static int outerTextureID = 0;

    public SulfurCube(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = 0.0f;
        this.vy = 0.0f;
        this.vz = 0.0f;
        this.age = (float) (Math.random() * 100.0);
        this.hopTimer = 1.0f + (float) (Math.random() * 2.0);
        
        if (innerTextureID == 0) {
            innerTextureID = TextureLoader.loadTexture("/assets/textures/entity/sulfur_cube/sulfur_cube_inner.png");
            outerTextureID = TextureLoader.loadTexture("/assets/textures/entity/sulfur_cube/sulfur_cube_outer.png");
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getSize() { return size; }
    
    public ItemType getAbsorbedBlock() { return absorbedBlock; }
    public void setAbsorbedBlock(ItemType type) { this.absorbedBlock = type; }
    
    public void setVelocities(float vx, float vy, float vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.onGround = false;
    }

    public void update(float dt, World world, Player player) {
        age += dt;

        vy -= 9.8f * dt;

        if (absorbedBlock == null) {
            if (onGround) {
                vx = 0.0f;
                vz = 0.0f;
                hopTimer -= dt;
                if (hopTimer <= 0.0f) {
                    double angle = Math.random() * Math.PI * 2.0;
                    vx = (float) Math.cos(angle) * 1.2f;
                    vz = (float) Math.sin(angle) * 1.2f;
                    vy = 2.5f;
                    onGround = false;
                    hopTimer = 1.5f + (float) Math.random() * 2.0f;
                }
            }
        }

        float nextX = x + vx * dt;
        float nextY = y + vy * dt;
        float nextZ = z + vz * dt;

        float elasticity = 0.5f;
        if (absorbedBlock == ItemType.COBBLESTONE) {
            elasticity = 0.2f;
        } else if (absorbedBlock == ItemType.LOG || absorbedBlock == ItemType.PLANKS) {
            elasticity = 0.8f;
        }

        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(nextY);
        int blockZ = (int) Math.floor(z);

        Block bBelow = world.getBlockAt(blockX, blockY, blockZ);
        if (bBelow != null && bBelow.isSolid()) {
            if (vy < 0.0f) {
                y = blockY + 1.0f;
                vy = -vy * elasticity;
                if (Math.abs(vy) < 0.2f) {
                    vy = 0.0f;
                    onGround = true;
                }
                vx *= 0.85f;
                vz *= 0.85f;
            } else {
                y = blockY - size;
                vy = -vy * elasticity;
            }
        } else {
            y = nextY;
            if (y < 0) {
                y = 0;
                vy = 0;
                onGround = true;
            }
        }

        int nextBlockX = (int) Math.floor(nextX);
        Block bX = world.getBlockAt(nextBlockX, (int) Math.floor(y), blockZ);
        if (bX != null && bX.isSolid()) {
            vx = -vx * elasticity;
        } else {
            x = nextX;
        }

        int nextBlockZ = (int) Math.floor(nextZ);
        Block bZ = world.getBlockAt(blockX, (int) Math.floor(y), nextBlockZ);
        if (bZ != null && bZ.isSolid()) {
            vz = -vz * elasticity;
        } else {
            z = nextZ;
        }
    }

    public void render(Player player) {
        glPushMatrix();

        float scaleY = size;
        float scaleXZ = size;
        if (absorbedBlock == null && !onGround) {
            scaleY = size * 1.15f;
            scaleXZ = size * 0.9f;
        }
        
        glTranslatef(x, y + scaleY / 2.0f, z);

        glRotatef(age * 15.0f, 0.0f, 1.0f, 0.0f);
        
        glEnable(GL_TEXTURE_2D);

        if (absorbedBlock == null) {
            // Render Inner Core (opaque)
            glBindTexture(GL_TEXTURE_2D, innerTextureID);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            float rInner = scaleXZ * 0.35f;
            float hInner = scaleY * 0.35f;
            drawBoxWithUV(-rInner, -hInner, -rInner, rInner, hInner, rInner, 0, 36, 16, 16, 16);
        } else {
            // Render Absorbed Block inside core if present (when core is not visible)
            glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());
            float rBlock = scaleXZ * 0.22f;
            float hBlock = scaleY * 0.22f;
            drawTexturedBlock(absorbedBlock, -rBlock, -hBlock, -rBlock, rBlock, hBlock, rBlock);
        }

        // Render Outer Shell (semi-transparent)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, outerTextureID);
        glColor4f(1.0f, 1.0f, 1.0f, 0.65f);

        float rOuter = scaleXZ * 0.5f;
        float hOuter = scaleY * 0.5f;
        drawBoxWithUV(-rOuter, -hOuter, -rOuter, rOuter, hOuter, rOuter, 0, 0, 18, 18, 18);

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    private void drawBoxWithUV(float x1, float y1, float z1, float x2, float y2, float z2, int u, int v, int sizeX, int sizeY, int sizeZ) {
        float tw = 128.0f;
        float th = 128.0f;

        // Top face
        float t_u1 = (u + sizeZ) / tw;
        float t_u2 = (u + sizeZ + sizeX) / tw;
        float t_v1 = v / th;
        float t_v2 = (v + sizeZ) / th;

        // Bottom face
        float b_u1 = (u + sizeZ + sizeX) / tw;
        float b_u2 = (u + sizeZ + sizeX + sizeX) / tw;
        float b_v1 = v / th;
        float b_v2 = (v + sizeZ) / th;

        // West face (Left)
        float w_u1 = u / tw;
        float w_u2 = (u + sizeZ) / tw;
        float w_v1 = (v + sizeZ) / th;
        float w_v2 = (v + sizeZ + sizeY) / th;

        // North face (Front)
        float n_u1 = (u + sizeZ) / tw;
        float n_u2 = (u + sizeZ + sizeX) / tw;
        float n_v1 = (v + sizeZ) / th;
        float n_v2 = (v + sizeZ + sizeY) / th;

        // East face (Right)
        float e_u1 = (u + sizeZ + sizeX) / tw;
        float e_u2 = (u + sizeZ + sizeX + sizeZ) / tw;
        float e_v1 = (v + sizeZ) / th;
        float e_v2 = (v + sizeZ + sizeY) / th;

        // South face (Back)
        float s_u1 = (u + sizeZ + sizeX + sizeZ) / tw;
        float s_u2 = (u + sizeZ + sizeX + sizeZ + sizeX) / tw;
        float s_v1 = (v + sizeZ) / th;
        float s_v2 = (v + sizeZ + sizeY) / th;

        glBegin(GL_QUADS);
        // Front (North) - facing z2
        glTexCoord2f(n_u1, n_v2); glVertex3f(x1, y1, z2);
        glTexCoord2f(n_u2, n_v2); glVertex3f(x2, y1, z2);
        glTexCoord2f(n_u2, n_v1); glVertex3f(x2, y2, z2);
        glTexCoord2f(n_u1, n_v1); glVertex3f(x1, y2, z2);

        // Back (South) - facing z1
        glTexCoord2f(s_u2, s_v2); glVertex3f(x1, y1, z1);
        glTexCoord2f(s_u2, s_v1); glVertex3f(x1, y2, z1);
        glTexCoord2f(s_u1, s_v1); glVertex3f(x2, y2, z1);
        glTexCoord2f(s_u1, s_v2); glVertex3f(x2, y1, z1);

        // Top - facing y2
        glTexCoord2f(t_u1, t_v2); glVertex3f(x1, y2, z1);
        glTexCoord2f(t_u1, t_v1); glVertex3f(x1, y2, z2);
        glTexCoord2f(t_u2, t_v1); glVertex3f(x2, y2, z2);
        glTexCoord2f(t_u2, t_v2); glVertex3f(x2, y2, z1);

        // Bottom - facing y1
        glTexCoord2f(b_u2, b_v2); glVertex3f(x1, y1, z1);
        glTexCoord2f(b_u1, b_v2); glVertex3f(x2, y1, z1);
        glTexCoord2f(b_u1, b_v1); glVertex3f(x2, y1, z2);
        glTexCoord2f(b_u2, b_v1); glVertex3f(x1, y1, z2);

        // Right (East) - facing x2
        glTexCoord2f(e_u2, e_v2); glVertex3f(x2, y1, z1);
        glTexCoord2f(e_u2, e_v1); glVertex3f(x2, y2, z1);
        glTexCoord2f(e_u1, e_v1); glVertex3f(x2, y2, z2);
        glTexCoord2f(e_u1, e_v2); glVertex3f(x2, y1, z2);

        // Left (West) - facing x1
        glTexCoord2f(w_u1, w_v2); glVertex3f(x1, y1, z1);
        glTexCoord2f(w_u2, w_v2); glVertex3f(x1, y1, z2);
        glTexCoord2f(w_u2, w_v1); glVertex3f(x1, y2, z2);
        glTexCoord2f(w_u1, w_v1); glVertex3f(x1, y2, z1);
        glEnd();
    }

    private void drawTexturedBlock(ItemType type, float x1, float y1, float z1, float x2, float y2, float z2) {
        glBegin(GL_QUADS);
        float[] uv = TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.TOP));
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x1, y2, z1);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x1, y2, z2);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x2, y2, z2);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y2, z1);
        uv = TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.BOTTOM));
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x1, y1, z1);
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x2, y1, z1);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x2, y1, z2);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x1, y1, z2);
        uv = TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.EAST));
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x2, y1, z1);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y1, z2);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x2, y2, z2);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x2, y2, z1);
        uv = TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.WEST));
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x1, y1, z1);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x1, y1, z2);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x1, y2, z2);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x1, y2, z1);
        uv = TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.NORTH));
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x1, y1, z2);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y1, z2);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x2, y2, z2);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x1, y2, z2);
        uv = TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.SOUTH));
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x1, y1, z1);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y1, z1);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x2, y2, z1);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x1, y2, z1);
        glEnd();
    }

    private int getBlockTextureIndex(ItemType type, Block.Face face) {
        switch (type) {
            case LOG:
                return (face == Block.Face.TOP || face == Block.Face.BOTTOM) ? 5 : 4;
            case PLANKS:
                return 9;
            case COBBLESTONE:
                return 3;
            default:
                return 9;
        }
    }
}
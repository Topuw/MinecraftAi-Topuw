package com.minecraftai.entities;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.Player;
import com.minecraftai.core.World;
import com.minecraftai.renderer.TextureAtlas;
import com.minecraftai.renderer.TextureLoader;
import static org.lwjgl.opengl.GL11.*;

public class CopperGolem {
    private float x, y, z;
    private float vx, vy, vz;
    private float yaw;
    private float age;
    private ItemType heldItem = null;
    
    private boolean onGround = false;
    private float wanderTimer = 0.0f;
    private float lookTimer = 0.0f;
    private float targetYaw = 0.0f;
    
    private static int textureID = 0;

    public static void initTextures() {
        if (textureID == 0) {
            textureID = TextureLoader.loadTexture("/assets/textures/block/stone.png");
        }
    }

    public CopperGolem(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.age = (float)(Math.random() * 100.0);
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public ItemType getHeldItem() { return heldItem; }
    public void setHeldItem(ItemType item) { this.heldItem = item; }

    public void update(float dt, World world, Player player) {
        age += dt;
        vy -= 9.8f * dt;

        wanderTimer -= dt;
        lookTimer -= dt;

        if (lookTimer <= 0) {
            targetYaw = (float)(Math.random() * 360.0);
            lookTimer = 2.0f + (float)Math.random() * 3.0f;
        }

        float yawDiff = targetYaw - yaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        yaw += yawDiff * dt * 2.0f;

        if (wanderTimer <= 0 && onGround) {
            if (Math.random() > 0.5) {
                float rad = (float)Math.toRadians(yaw);
                vx = (float)Math.sin(rad) * 1.5f;
                vz = (float)-Math.cos(rad) * 1.5f;
            } else {
                vx = 0; vz = 0;
            }
            wanderTimer = 1.0f + (float)Math.random() * 2.0f;
        }

        float nextX = x + vx * dt;
        float nextY = y + vy * dt;
        float nextZ = z + vz * dt;

        Block b = world.getBlockAt((int)Math.floor(x), (int)Math.floor(nextY), (int)Math.floor(z));
        if (b != null && b.isSolid()) {
            if (vy < 0) onGround = true;
            vy = 0;
        } else {
            y = nextY;
            onGround = false;
        }

        Block bX = world.getBlockAt((int)Math.floor(nextX), (int)Math.floor(y), (int)Math.floor(z));
        if (bX == null || !bX.isSolid()) {
            x = nextX;
        } else { vx = -vx * 0.5f; }

        Block bZ = world.getBlockAt((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(nextZ));
        if (bZ == null || !bZ.isSolid()) {
            z = nextZ;
        } else { vz = -vz * 0.5f; }
        
        if (y < 0) { y = 0; vy = 0; onGround = true; }
    }

    public void render() {
        glPushMatrix();
        glTranslatef(x, y, z);
        glRotatef(-yaw, 0, 1, 0);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glColor4f(0.8f, 0.4f, 0.3f, 1.0f); // Copper color

        drawBox(-0.3f, 0, -0.2f, 0.3f, 0.6f, 0.2f); // Body
        drawBox(-0.2f, 0.6f, -0.2f, 0.2f, 0.9f, 0.2f); // Head
        drawBox(-0.05f, 0.65f, -0.3f, 0.05f, 0.75f, -0.2f); // Nose

        float armSwing = (float)Math.sin(age * 4.0f) * 0.2f;
        drawBox(-0.45f, 0.2f, -0.1f + armSwing, -0.3f, 0.5f, 0.1f + armSwing); // L Arm
        drawBox(0.3f, 0.2f, -0.1f - armSwing, 0.45f, 0.5f, 0.1f - armSwing); // R Arm

        if (heldItem != null) {
            glPushMatrix();
            glTranslatef(0.4f, 0.3f, 0.2f);
            glScalef(0.3f, 0.3f, 0.3f);
            glBindTexture(GL_TEXTURE_2D, heldItem.getTextureId());
            drawBox(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
            glPopMatrix();
        }

        glPopMatrix();
    }

    private void drawBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        glBegin(GL_QUADS);
        glTexCoord2f(0,0); glVertex3f(x1, y2, z1);
        glTexCoord2f(1,0); glVertex3f(x1, y2, z2);
        glTexCoord2f(1,1); glVertex3f(x2, y2, z2);
        glTexCoord2f(0,1); glVertex3f(x2, y2, z1);
        
        glTexCoord2f(1,1); glVertex3f(x1, y1, z1);
        glTexCoord2f(0,1); glVertex3f(x2, y1, z1);
        glTexCoord2f(0,0); glVertex3f(x2, y1, z2);
        glTexCoord2f(1,0); glVertex3f(x1, y1, z2);

        glTexCoord2f(0,0); glVertex3f(x1, y1, z2); glTexCoord2f(1,0); glVertex3f(x2, y1, z2); glTexCoord2f(1,1); glVertex3f(x2, y2, z2); glTexCoord2f(0,1); glVertex3f(x1, y2, z2);
        glTexCoord2f(0,0); glVertex3f(x1, y1, z1); glTexCoord2f(1,0); glVertex3f(x1, y2, z1); glTexCoord2f(1,1); glVertex3f(x2, y2, z1); glTexCoord2f(0,1); glVertex3f(x2, y1, z1);
        glTexCoord2f(0,0); glVertex3f(x2, y1, z1); glTexCoord2f(1,0); glVertex3f(x2, y2, z1); glTexCoord2f(1,1); glVertex3f(x2, y2, z2); glTexCoord2f(0,1); glVertex3f(x2, y1, z2);
        glTexCoord2f(0,0); glVertex3f(x1, y1, z1); glTexCoord2f(1,0); glVertex3f(x1, y1, z2); glTexCoord2f(1,1); glVertex3f(x1, y2, z2); glTexCoord2f(0,1); glVertex3f(x1, y2, z1);
        glEnd();
    }
}

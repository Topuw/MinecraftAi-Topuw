package com.minecraftai.core;

import com.minecraftai.blocks.*;
import com.minecraftai.entities.SulfurCube;
import com.minecraftai.renderer.TextureLoader;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Player {
    private float x, y, z;
    private float prevX, prevY, prevZ;
    private boolean isSprinting = false;
    private float walkTime = 0.0f;
    private float yaw, pitch;
    private float speed = 0.07f;
    private float velocityY = 0;
    private final float gravity = 0.008f;
    private final float jumpStrength = 0.14f;
    private final float eyeHeight = 1.7f;
    private long lastBlockPlaceTime = 0;
    private final long blockPlaceCooldown = 200_000_000L;
    private World world;
    private int playerTextureID;

    private int selectedSlot = 0;
    private ItemStack[] inventory = new ItemStack[36];

    private int targetX, targetY, targetZ;
    private float breakProgress = 0.0f;
    private Block currentTargetBlock = null;

    private int health = 20;
    private final int maxHealth = 20;
    private float highestYSinceOnGround = 80.0f;
    private boolean wasOnGround = true;
    private float regenTimer = 0.0f;
    private boolean isDead = false;
    private boolean godMode = false;

    public Player(World world) {
        this.world = world;
        float spawnY = 80.0f;
        if (world != null) {
            world.getOrLoadChunk(0, 0, true);
            for (int y = Chunk.CHUNK_SIZE_Y - 1; y >= 0; y--) {
                Block block = world.getBlockAt(0, y, 0, true);
                if (block != null) {
                    spawnY = y + 1.0f;
                    break;
                }
            }
        }
        this.x = 0;
        this.y = spawnY;
        this.z = 0;
        this.prevX = 0;
        this.prevY = spawnY;
        this.prevZ = 0;
        this.yaw = 0;
        this.pitch = 0;
        this.playerTextureID = TextureLoader.loadTexture("/assets/textures/entity/player.png");
        this.highestYSinceOnGround = spawnY;
    }

    public void handleInput(long window) {
        handleInput(window, false);
    }

    public void handleInput(long window, boolean noInput) {
        if (isDead || noInput) return;
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS) {
            SulfurCube targetCube = getTargetCube(world, 4.5f);
            if (targetCube != null) {
                return;
            }
            Block target = getTargetBlock(world, 4.5f);
            if (target instanceof CraftingTable || target instanceof Furnace) {
            } else {
                tryPlaceBlock(world);
            }
        }
    }

    public float getRenderX(float alpha) {
        return prevX + (x - prevX) * alpha;
    }

    public float getRenderY(float alpha) {
        return prevY + (y - prevY) * alpha;
    }

    public float getRenderZ(float alpha) {
        return prevZ + (z - prevZ) * alpha;
    }

    public void applyCameraTransform(float alpha, int cameraMode) {
        float[] cam = getCameraLookAt(alpha, cameraMode);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        lookAt(cam[0], cam[1], cam[2], cam[3], cam[4], cam[5], 0, 1, 0);
    }

    public void renderPlayerModel(float alpha, int cameraMode) {
        if (cameraMode == 0) return;

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, playerTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glPushMatrix();

        float rx = getRenderX(alpha);
        float ry = getRenderY(alpha);
        float rz = getRenderZ(alpha);
        glTranslatef(rx, ry, rz);

        glRotatef(-yaw, 0.0f, 1.0f, 0.0f);

        float swingAngle = (float) Math.sin(walkTime) * 35.0f;

        glPushMatrix();
        glTranslatef(0.0f, 1.5f, 0.0f);
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);

        drawTexturedBox(-0.25f, 0.0f, -0.25f, 0.25f, 0.5f, 0.25f, 0, 0, 8, 8, 8);
        drawTexturedBox(-0.27f, -0.02f, -0.27f, 0.27f, 0.52f, 0.27f, 32, 0, 8, 8, 8);
        glPopMatrix();

        drawTexturedBox(-0.25f, 0.75f, -0.125f, 0.25f, 1.5f, 0.125f, 16, 16, 8, 12, 4);
        drawTexturedBox(-0.27f, 0.73f, -0.145f, 0.27f, 1.52f, 0.145f, 16, 32, 8, 12, 4);

        glPushMatrix();
        glTranslatef(-0.375f, 1.375f, 0.0f);
        glRotatef(swingAngle, 1.0f, 0.0f, 0.0f);
        drawTexturedBox(-0.125f, -0.625f, -0.125f, 0.125f, 0.125f, 0.125f, 40, 16, 4, 12, 4);
        drawTexturedBox(-0.145f, -0.645f, -0.145f, 0.145f, 0.145f, 0.145f, 40, 32, 4, 12, 4);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(0.375f, 1.375f, 0.0f);
        glRotatef(-swingAngle, 1.0f, 0.0f, 0.0f);
        drawTexturedBox(-0.125f, -0.625f, -0.125f, 0.125f, 0.125f, 0.125f, 32, 48, 4, 12, 4);
        drawTexturedBox(-0.145f, -0.645f, -0.145f, 0.145f, 0.145f, 0.145f, 48, 48, 4, 12, 4);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(-0.125f, 0.75f, 0.0f);
        glRotatef(-swingAngle, 1.0f, 0.0f, 0.0f);
        drawTexturedBox(-0.125f, -0.75f, -0.125f, 0.125f, 0.0f, 0.125f, 0, 16, 4, 12, 4);
        drawTexturedBox(-0.145f, -0.77f, -0.145f, 0.145f, 0.02f, 0.145f, 0, 32, 4, 12, 4);
        glPopMatrix();

        glPushMatrix();
        glTranslatef(0.125f, 0.75f, 0.0f);
        glRotatef(swingAngle, 1.0f, 0.0f, 0.0f);
        drawTexturedBox(-0.125f, -0.75f, -0.125f, 0.125f, 0.0f, 0.125f, 16, 48, 4, 12, 4);
        drawTexturedBox(-0.145f, -0.77f, -0.145f, 0.145f, 0.02f, 0.145f, 0, 48, 4, 12, 4);
        glPopMatrix();

        glPopMatrix();
        glDisable(GL_BLEND);
    }


    private void drawTexturedBox(float x1, float y1, float z1, float x2, float y2, float z2, int u, int v, int w, int h, int d) {
        float tw = 64.0f;
        float th = 64.0f;

        glBegin(GL_QUADS);

        glNormal3f(0.0f, 1.0f, 0.0f);
        glTexCoord2f((u + d) / tw, (v + d) / th);             glVertex3f(x1, y2, z1);
        glTexCoord2f((u + d) / tw, v / th);                 glVertex3f(x1, y2, z2);
        glTexCoord2f((u + d + w) / tw, v / th);             glVertex3f(x2, y2, z2);
        glTexCoord2f((u + d + w) / tw, (v + d) / th);         glVertex3f(x2, y2, z1);

        glNormal3f(0.0f, -1.0f, 0.0f);
        glTexCoord2f((u + d + w) / tw, (v + d) / th);         glVertex3f(x1, y1, z1);
        glTexCoord2f((u + d + 2 * w) / tw, (v + d) / th);     glVertex3f(x2, y1, z1);
        glTexCoord2f((u + d + 2 * w) / tw, v / th);         glVertex3f(x2, y1, z2);
        glTexCoord2f((u + d + w) / tw, v / th);             glVertex3f(x1, y1, z2);

        glNormal3f(0.0f, 0.0f, -1.0f);
        glTexCoord2f((u + d) / tw, (v + d + h) / th);         glVertex3f(x1, y1, z1);
        glTexCoord2f((u + d + w) / tw, (v + d + h) / th);     glVertex3f(x2, y1, z1);
        glTexCoord2f((u + d + w) / tw, (v + d) / th);         glVertex3f(x2, y2, z1);
        glTexCoord2f((u + d) / tw, (v + d) / th);             glVertex3f(x1, y2, z1);

        glNormal3f(0.0f, 0.0f, 1.0f);
        glTexCoord2f((u + 2 * d + 2 * w) / tw, (v + d + h) / th); glVertex3f(x1, y1, z2);
        glTexCoord2f((u + 2 * d + w) / tw, (v + d + h) / th);     glVertex3f(x2, y1, z2);
        glTexCoord2f((u + 2 * d + w) / tw, (v + d) / th);         glVertex3f(x2, y2, z2);
        glTexCoord2f((u + 2 * d + 2 * w) / tw, (v + d) / th);     glVertex3f(x1, y2, z2);

        glNormal3f(1.0f, 0.0f, 0.0f);
        glTexCoord2f((u + d + w) / tw, (v + d + h) / th);     glVertex3f(x2, y1, z2);
        glTexCoord2f((u + 2 * d + w) / tw, (v + d + h) / th); glVertex3f(x2, y1, z1);
        glTexCoord2f((u + 2 * d + w) / tw, (v + d) / th);     glVertex3f(x2, y2, z1);
        glTexCoord2f((u + d + w) / tw, (v + d) / th);         glVertex3f(x2, y2, z2);

        glNormal3f(-1.0f, 0.0f, 0.0f);
        glTexCoord2f((u + d) / tw, (v + d + h) / th);         glVertex3f(x1, y1, z1);
        glTexCoord2f(u / tw, (v + d + h) / th);             glVertex3f(x1, y1, z2);
        glTexCoord2f(u / tw, (v + d) / th);                 glVertex3f(x1, y2, z2);
        glTexCoord2f((u + d) / tw, (v + d) / th);             glVertex3f(x1, y2, z1);

        glEnd();
    }

    private void drawSolidBox(float x1, float y1, float z1, float x2, float y2, float z2) {
        glBegin(GL_QUADS);

        glNormal3f(0.0f, 0.0f, 1.0f);
        glVertex3f(x1, y1, z2);
        glVertex3f(x2, y1, z2);
        glVertex3f(x2, y2, z2);
        glVertex3f(x1, y2, z2);

        glNormal3f(0.0f, 0.0f, -1.0f);
        glVertex3f(x1, y1, z1);
        glVertex3f(x1, y2, z1);
        glVertex3f(x2, y2, z1);
        glVertex3f(x2, y1, z1);

        glNormal3f(0.0f, 1.0f, 0.0f);
        glVertex3f(x1, y2, z1);
        glVertex3f(x1, y2, z2);
        glVertex3f(x2, y2, z2);
        glVertex3f(x2, y2, z1);

        glNormal3f(0.0f, -1.0f, 0.0f);
        glVertex3f(x1, y1, z1);
        glVertex3f(x2, y1, z1);
        glVertex3f(x2, y1, z2);
        glVertex3f(x1, y1, z2);

        glNormal3f(1.0f, 0.0f, 0.0f);
        glVertex3f(x2, y1, z1);
        glVertex3f(x2, y2, z1);
        glVertex3f(x2, y2, z2);
        glVertex3f(x2, y1, z2);

        glNormal3f(-1.0f, 0.0f, 0.0f);
        glVertex3f(x1, y1, z1);
        glVertex3f(x1, y1, z2);
        glVertex3f(x1, y2, z2);
        glVertex3f(x1, y2, z1);

        glEnd();
    }

    private void lookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        float[] f = {centerX - eyeX, centerY - eyeY, centerZ - eyeZ};
        float fLen = (float) Math.sqrt(f[0]*f[0] + f[1]*f[1] + f[2]*f[2]);
        f[0] /= fLen; f[1] /= fLen; f[2] /= fLen;
        float[] up = {upX, upY, upZ};
        float upLen = (float) Math.sqrt(up[0]*up[0] + up[1]*up[1] + up[2]*up[2]);
        up[0]/=upLen; up[1]/=upLen; up[2]/=upLen;
        float[] s = {f[1]*up[2]-f[2]*up[1], f[2]*up[0]-f[0]*up[2], f[0]*up[1]-f[1]*up[0]};
        float sLen = (float) Math.sqrt(s[0]*s[0]+s[1]*s[1]+s[2]*s[2]);
        s[0]/=sLen; s[1]/=sLen; s[2]/=sLen;
        float[] u = {s[1]*f[2]-s[2]*f[1], s[2]*f[0]-s[0]*f[2], s[0]*f[1]-s[1]*f[0]};
        float[] m = {s[0], u[0], -f[0],0, s[1], u[1], -f[1],0, s[2], u[2], -f[2],0, 0,0,0,1};
        glLoadMatrixf(m);
        glTranslatef(-eyeX, -eyeY, -eyeZ);
    }

    public void update(long window, double dt) {
        update(window, dt, false);
    }

    public void update(long window, double dt, boolean noInput) {
        if (isDead) {
            velocityY = 0;
            return;
        }

        prevX = x;
        prevY = y;
        prevZ = z;

        boolean isBreaking = !noInput && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        SulfurCube targetCube = noInput ? null : getTargetCube(world, 4.5f);
        if (targetCube != null) {
            isBreaking = false;
        }
        Block target = noInput ? null : getTargetBlock(world, 4.5f);

        if (isBreaking && target != null) {

            if (currentTargetBlock != null &&
                    target.getX() == targetX &&
                    target.getY() == targetY &&
                    target.getZ() == targetZ) {

                float speedVal = getBreakSpeed(target);
                breakProgress += dt * speedVal;

                if (breakProgress >= 1.0f) {
                    world.removeBlock(targetX, targetY, targetZ);
                    
                    boolean dropsItems = true;
                    String blockName = target.getClass().getSimpleName();
                    if (blockName.equals("Stone") || blockName.equals("Cobblestone") || blockName.equals("Furnace") || blockName.equals("KebabOre") || blockName.equals("KebabBlock")) {
                        dropsItems = isPickaxeHeld() || godMode;
                    }

                    if (dropsItems) {
                        ItemType drop = target.getItemDrop();
                        if (drop != null) {
                            world.spawnDroppedItem(targetX + 0.5f, targetY + 0.5f, targetZ + 0.5f, drop);
                        }
                    }

                    if (isPickaxeHeld()) {
                        ItemStack held = inventory[selectedSlot];
                        held.decrementDurability();
                        if (held.isBroken()) {
                            inventory[selectedSlot] = null;
                        }
                    }

                    breakProgress = 0.0f;
                    currentTargetBlock = null;
                }
            } else {
                currentTargetBlock = target;
                targetX = target.getX();
                targetY = target.getY();
                targetZ = target.getZ();
                breakProgress = 0.0f;
            }
        } else {
            breakProgress = 0.0f;
            currentTargetBlock = null;
        }

        float dx = 0, dz = 0;
        float inputX = 0;
        float inputZ = 0;

        if (!noInput) {
            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) inputZ += 1;
            if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) inputZ -= 1;
            if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) inputX -= 1;
            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) inputX += 1;
        }

        isSprinting = false;
        if (!noInput && (inputX != 0 || inputZ != 0)) {
            isSprinting = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS && inputZ > 0;

            float speedMultiplier = 1.0f;
            if (inputZ < 0) {
                speedMultiplier = 0.6f;
            } else if (inputZ == 0 && inputX != 0) {
                speedMultiplier = 0.8f;
            } else if (inputZ > 0 && inputX != 0) {
                speedMultiplier = 0.9f;
            }

            if (isSprinting) {
                speedMultiplier *= 1.3f;
            }

            double radYaw = Math.toRadians(yaw);
            double sin = Math.sin(radYaw);
            double cos = Math.cos(radYaw);

            float length = (float) Math.sqrt(inputX * inputX + inputZ * inputZ);
            float nx = inputX / length;
            float nz = inputZ / length;

            dx = (float) (nx * cos + nz * sin) * speed * speedMultiplier;
            dz = (float) (nx * sin - nz * cos) * speed * speedMultiplier;
        }

        Block playerBlock = world.getBlockAt((int) Math.floor(x), (int) Math.floor(y + 0.1f), (int) Math.floor(z));
        if (playerBlock instanceof Water || playerBlock instanceof FlowingWater) {
            int level = (playerBlock instanceof Water) ? 8 : ((FlowingWater) playerBlock).getLevel();
            if (level < 8) {
                int wx = playerBlock.getX();
                int wy = playerBlock.getY();
                int wz = playerBlock.getZ();

                int levelL = getWaterLevelAt(wx - 1, wy, wz);
                int levelR = getWaterLevelAt(wx + 1, wy, wz);
                int levelB = getWaterLevelAt(wx, wy, wz - 1);
                int levelF = getWaterLevelAt(wx, wy, wz + 1);

                float pushX = levelL - levelR;
                float pushZ = levelB - levelF;

                float length = (float) Math.sqrt(pushX * pushX + pushZ * pushZ);
                if (length > 0) {
                    pushX /= length;
                    pushZ /= length;
                    float pushSpeed = 0.012f;
                    dx += pushX * pushSpeed;
                    dz += pushZ * pushSpeed;
                }
            }
        }


        if (!collides(x + dx, y, z)) {
            x += dx;
        }
        if (!collides(x, y, z + dz)) {
            z += dz;
        }

        velocityY -= gravity;
        float nextY = y + velocityY;

        if (velocityY > 0 && collides(x, nextY, z)) {
            velocityY = 0;
        } else if (velocityY < 0 && collides(x, nextY, z)) {
            velocityY = 0;
            y = (float) Math.floor(nextY) + 1.0f;
        } else {
            y = nextY;
        }

        if (!noInput && glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround()) {
            velocityY = jumpStrength;
        }

        if (dx != 0 || dz != 0) {
            walkTime += dt * (isSprinting ? 15.0f : 10.0f);
        } else {
            float walkTimeMod = walkTime % (float)(Math.PI * 2);
            if (walkTimeMod > 0.1f) {
                walkTime -= dt * 5.0f;
            } else if (walkTimeMod < -0.1f) {
                walkTime += dt * 5.0f;
            } else {
                walkTime = 0.0f;
            }
        }

        boolean isOnGround = onGround();
        if (isOnGround) {
            if (!wasOnGround) {
                float fallDistance = highestYSinceOnGround - y;
                if (fallDistance >= 4.0f) {
                    takeDamage(Math.round(fallDistance - 3.0f));
                }
            }
            highestYSinceOnGround = y;
        } else {
            if (y > highestYSinceOnGround) {
                highestYSinceOnGround = y;
            }
        }
        wasOnGround = isOnGround;

        if (!isDead && health < maxHealth) {
            regenTimer += dt;
            if (regenTimer >= 1.5f) {
                health = Math.min(maxHealth, health + 1);
                regenTimer = 0.0f;
            }
        } else {
            regenTimer = 0.0f;
        }
    }

    private boolean isPickaxeHeld() {
        ItemStack held = inventory[selectedSlot];
        return held != null && held.getType() == ItemType.WOODEN_PICKAXE;
    }

    private float getBreakSpeed(Block target) {
        if (!target.isDestructible()) return 0.0f;
        if (godMode) return 1000.0f;
        String name = target.getClass().getSimpleName();
        boolean hasPickaxe = isPickaxeHeld();
        switch (name) {
            case "Leaves":
                return 5.0f;
            case "GrassBlock":
            case "Dirt":
                return 2.0f;
            case "Log":
                return 1.25f;
            case "Stone":
            case "Cobblestone":
            case "KebabOre":
            case "KebabBlock":
                return hasPickaxe ? 1.8f : 0.7f;
            case "Furnace":
                return hasPickaxe ? 2.0f : 1.0f;
            default:
                return 1.0f;
        }
    }


    private boolean onGround() {
        return collides(x, y - 0.05f, z);
    }

    private boolean collides(float nextX, float nextY, float nextZ) {
        float playerWidth = 0.3f;
        float playerHeight = 1.7f;

        int minX = (int) Math.floor(nextX - playerWidth);
        int maxX = (int) Math.ceil(nextX + playerWidth);
        int minY = (int) Math.floor(nextY);
        int maxY = (int) Math.ceil(nextY + playerHeight);
        int minZ = (int) Math.floor(nextZ - playerWidth);
        int maxZ = (int) Math.ceil(nextZ + playerWidth);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {

                    Block b = world.getBlockAt(x, y, z, true);

                    if (b != null) {
                        if (b.collidesWithPlayer(nextX, nextY, nextZ)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void addRotation(float dx, float dy) {
        float sensitivity = 0.1f;
        yaw += dx * sensitivity;
        pitch -= dy * sensitivity;
        final float MAX_PITCH = 89.9f;
        if (pitch > MAX_PITCH) pitch = MAX_PITCH;
        if (pitch < -MAX_PITCH) pitch = -MAX_PITCH;
    }

    public float[] getCameraLookAt(float alpha, int cameraMode) {
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));

        float headX = getRenderX(alpha);
        float headY = getRenderY(alpha) + eyeHeight;
        float headZ = getRenderZ(alpha);

        float eyeX, eyeY, eyeZ;
        float lookX, lookY, lookZ;

        if (cameraMode == 1) {
            float distance = 4.0f;
            float step = 0.2f;
            for (float t = 0; t <= 4.0f; t += step) {
                float cx = headX - dirX * t;
                float cy = headY - dirY * t;
                float cz = headZ - dirZ * t;
                Block b = world.getBlockAt((int)Math.floor(cx), (int)Math.floor(cy), (int)Math.floor(cz));
                if (b != null && b.isSolid()) {
                    distance = Math.max(0.0f, t - 0.3f);
                    break;
                }
            }
            eyeX = headX - dirX * distance;
            eyeY = headY - dirY * distance;
            eyeZ = headZ - dirZ * distance;
            lookX = headX;
            lookY = headY;
            lookZ = headZ;
        } else if (cameraMode == 2) {
            float distance = 4.0f;
            float step = 0.2f;
            for (float t = 0; t <= 4.0f; t += step) {
                float cx = headX + dirX * t;
                float cy = headY + dirY * t;
                float cz = headZ + dirZ * t;
                Block b = world.getBlockAt((int)Math.floor(cx), (int)Math.floor(cy), (int)Math.floor(cz));
                if (b != null && b.isSolid()) {
                    distance = Math.max(0.0f, t - 0.3f);
                    break;
                }
            }
            eyeX = headX + dirX * distance;
            eyeY = headY + dirY * distance;
            eyeZ = headZ + dirZ * distance;
            lookX = headX;
            lookY = headY;
            lookZ = headZ;
        } else {
            eyeX = headX;
            eyeY = headY;
            eyeZ = headZ;
            lookX = eyeX + dirX;
            lookY = eyeY + dirY;
            lookZ = eyeZ + dirZ;
        }
        return new float[]{eyeX, eyeY, eyeZ, lookX, lookY, lookZ};
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public float getBreakProgress() {
        return breakProgress;
    }

    public boolean isGodMode() {
        return godMode;
    }

    public void setGodMode(boolean godMode) {
        this.godMode = godMode;
    }

    public Block getCurrentTargetBlock() {
        return currentTargetBlock;
    }

    public void addItem(ItemType itemType) {
        if (itemType == null) return;

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.getType() == itemType && !stack.isFull()) {
                stack.addAmount(1);
                return;
            }
        }

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) {
                inventory[i] = new ItemStack(itemType, 1);
                return;
            }
        }
    }

    public void tryPlaceBlock(World world) {
        long now = System.nanoTime();
        if (now - lastBlockPlaceTime < blockPlaceCooldown) return;

        ItemStack heldStack = inventory[selectedSlot];
        if (heldStack == null) {
            return;
        }

        float maxDistance = 4f;

        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));

        float eyeX = x;
        float eyeY = y + eyeHeight;
        float eyeZ = z;

        float step = 0.05f;

        int prevX = (int)Math.floor(eyeX);
        int prevY = (int)Math.floor(eyeY);
        int prevZ = (int)Math.floor(eyeZ);

        for (float t = 0; t <= maxDistance; t += step) {
            float currentRayX = eyeX + dirX * t;
            float currentRayY = eyeY + dirY * t;
            float currentRayZ = eyeZ + dirZ * t;

            int currentBlockX = (int)Math.floor(currentRayX);
            int currentBlockY = (int)Math.floor(currentRayY);
            int currentBlockZ = (int)Math.floor(currentRayZ);

            if (currentBlockX != prevX || currentBlockY != prevY || currentBlockZ != prevZ) {
                Block b = world.getBlockAt(currentBlockX, currentBlockY, currentBlockZ);

                if (b != null && !(b instanceof Water || b instanceof FlowingWater)) {
                    Block newBlock = null;
                    switch (heldStack.getType()) {
                        case DIRT:
                            newBlock = new Dirt(prevX, prevY, prevZ);
                            break;
                        case COBBLESTONE:
                            newBlock = new Cobblestone(prevX, prevY, prevZ);
                            break;
                        case STONE:
                            newBlock = new Stone(prevX, prevY, prevZ);
                            break;
                        case LOG:
                            newBlock = new Log(prevX, prevY, prevZ);
                            break;
                        case PLANKS:
                            newBlock = new Planks(prevX, prevY, prevZ);
                            break;
                        case CRAFTING_TABLE:
                            newBlock = new CraftingTable(prevX, prevY, prevZ);
                            break;
                        case FURNACE:
                            newBlock = new Furnace(prevX, prevY, prevZ, getFacingDirection());
                            break;
                        case KEBAB_ORE:
                            newBlock = new KebabOre(prevX, prevY, prevZ);
                            break;
                        case KEBAB_BLOCK:
                            newBlock = new KebabBlock(prevX, prevY, prevZ);
                            break;
                    }

                    if (newBlock == null || newBlock.collidesWithPlayer(this.x, this.y, this.z)) {
                        return;
                    }

                    world.addBlock(prevX, prevY, prevZ, newBlock);
                    lastBlockPlaceTime = now;

                    heldStack.setCount(heldStack.getCount() - 1);
                    if (heldStack.getCount() <= 0) {
                        inventory[selectedSlot] = null;
                    }
                    return;
                }

                prevX = currentBlockX;
                prevY = currentBlockY;
                prevZ = currentBlockZ;
            }
        }
    }

    private Block.Face getFacingDirection() {
        float normalizedYaw = (yaw % 360 + 360) % 360;
        if (normalizedYaw >= 315 || normalizedYaw < 45) {
            return Block.Face.SOUTH;
        } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
            return Block.Face.WEST;
        } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
            return Block.Face.NORTH;
        } else {
            return Block.Face.EAST;
        }
    }

    public Block getTargetBlock(World world, float maxDistance) {
        float eyeX = x;
        float eyeY = y + 1.7f;
        float eyeZ = z;
        float radYaw = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);
        float dirX = (float)(Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float)(Math.sin(radPitch));
        float dirZ = (float)(-Math.cos(radYaw) * Math.cos(radPitch));
        float step = 0.1f;
        for (float t = 0; t <= maxDistance; t += step) {
            float checkX = eyeX + dirX * t;
            float checkY = eyeY + dirY * t;
            float checkZ = eyeZ + dirZ * t;
            Block b = world.getBlockAt((int)Math.floor(checkX), (int)Math.floor(checkY), (int)Math.floor(checkZ));
            if (b != null && !(b instanceof Water || b instanceof FlowingWater)) {
                return b;
            }
        }
        return null;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setZ(float z) { this.z = z; }
    public void setVelocityY(float vy) { this.velocityY = vy; }
    public void resetPrevPosition() {
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
    }
    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }
    public World getWorld() { return world; }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < 9) {
            this.selectedSlot = slot;
        }
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public boolean addItemStack(ItemStack stack) {
        if (stack == null) return true;
        ItemType itemType = stack.getType();

        for (int i = 0; i < inventory.length; i++) {
            ItemStack s = inventory[i];
            if (s != null && s.getType() == itemType && !s.isFull()) {
                int left = s.addAmount(stack.getCount());
                stack.setCount(left);
                if (left == 0) return true;
            }
        }

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null) {
                inventory[i] = stack;
                return true;
            }
        }

        return stack.getCount() == 0;
    }

    public int getHealth() { return health; }
    public void setHealth(int health) {
        this.health = health;
        this.isDead = (health <= 0);
    }
    public int getMaxHealth() { return maxHealth; }
    public boolean isDead() { return isDead; }

    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public void setPosition(float px, float py, float pz) {
        this.x = px;
        this.y = py;
        this.z = pz;
        this.prevX = px;
        this.prevY = py;
        this.prevZ = pz;
        this.highestYSinceOnGround = py;
    }

    public void takeDamage(int amount) {
        if (isDead) return;
        health = Math.max(0, health - amount);
        if (health <= 0) {
            isDead = true;
        }
    }

    public void respawn() {
        float spawnY = 80.0f;
        if (world != null) {
            world.getOrLoadChunk(0, 0, true);
            for (int y = Chunk.CHUNK_SIZE_Y - 1; y >= 0; y--) {
                Block block = world.getBlockAt(0, y, 0, true);
                if (block != null) {
                    spawnY = y + 1.0f;
                    break;
                }
            }
        }
        this.x = 0;
        this.y = spawnY;
        this.z = 0;
        this.prevX = 0;
        this.prevY = spawnY;
        this.prevZ = 0;
        this.velocityY = 0;
        this.health = maxHealth;
        this.isDead = false;
        this.highestYSinceOnGround = spawnY;
        this.wasOnGround = true;
        this.regenTimer = 0.0f;
    }

    public void renderPlayerModelGUI(float centerX, float centerY, float modelScale, double mouseX, double mouseY, float currentW, float currentH) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, playerTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -100, 100);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);

        glTranslatef(centerX, centerY, 0.0f);
        glScalef(modelScale, -modelScale, modelScale);

        float dx = (float) (mouseX - centerX);
        float dy = (float) (mouseY - centerY);
        float angleYaw = (float) Math.toDegrees(Math.atan2(dx, 40.0f));
        float anglePitch = (float) Math.toDegrees(Math.atan2(dy, 40.0f));

        angleYaw = Math.max(-45.0f, Math.min(45.0f, angleYaw));
        anglePitch = Math.max(-30.0f, Math.min(30.0f, anglePitch));

        glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        glRotatef(-20.0f, 0.0f, 1.0f, 0.0f);

        drawTexturedBox(-0.25f, 0.75f, -0.125f, 0.25f, 1.5f, 0.125f, 16, 16, 8, 12, 4);
        drawTexturedBox(-0.27f, 0.73f, -0.145f, 0.27f, 1.52f, 0.145f, 16, 32, 8, 12, 4);

        drawTexturedBox(-0.5f, 0.75f, -0.125f, -0.25f, 1.5f, 0.125f, 40, 16, 4, 12, 4);
        drawTexturedBox(-0.52f, 0.73f, -0.145f, -0.23f, 1.52f, 0.145f, 40, 32, 4, 12, 4);

        drawTexturedBox(0.25f, 0.75f, -0.125f, 0.5f, 1.5f, 0.125f, 32, 48, 4, 12, 4);
        drawTexturedBox(0.23f, 0.73f, -0.145f, 0.52f, 1.52f, 0.145f, 48, 48, 4, 12, 4);

        drawTexturedBox(-0.25f, 0.0f, -0.125f, 0.0f, 0.75f, 0.125f, 0, 16, 4, 12, 4);
        drawTexturedBox(-0.27f, -0.02f, -0.145f, 0.02f, 0.77f, 0.145f, 0, 32, 4, 12, 4);

        drawTexturedBox(0.0f, 0.0f, -0.125f, 0.25f, 0.75f, 0.125f, 16, 48, 4, 12, 4);
        drawTexturedBox(-0.02f, -0.02f, -0.145f, 0.27f, 0.77f, 0.145f, 0, 48, 4, 12, 4);

        glPushMatrix();
        glTranslatef(0.0f, 1.5f, 0.0f);
        glRotatef(angleYaw, 0.0f, 1.0f, 0.0f);
        glRotatef(-anglePitch, 1.0f, 0.0f, 0.0f);

        drawTexturedBox(-0.25f, 0.0f, -0.25f, 0.25f, 0.5f, 0.25f, 0, 0, 8, 8, 8);
        drawTexturedBox(-0.27f, -0.02f, -0.27f, 0.27f, 0.52f, 0.27f, 32, 0, 8, 8, 8);
        glPopMatrix();

        glDisable(GL_DEPTH_TEST);
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    public SulfurCube getTargetCube(World world, float maxDistance) {
        if (world == null) return null;
        float eyeX = x;
        float eyeY = y + eyeHeight;
        float eyeZ = z;
        
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float dirX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float dirY = (float) (Math.sin(radPitch));
        float dirZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));
        
        SulfurCube bestCube = null;
        float bestDist = maxDistance;
        
        for (SulfurCube cube : world.getSulfurCubes()) {
            float cx = cube.getX();
            float cy = cube.getY() + cube.getSize() / 2.0f;
            float cz = cube.getZ();
            
            float dx = cx - eyeX;
            float dy = cy - eyeY;
            float dz = cz - eyeZ;
            float dist = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < bestDist) {
                float dot = (dx*dirX + dy*dirY + dz*dirZ) / dist;
                if (dot > 0.95f) {
                    bestCube = cube;
                    bestDist = dist;
                }
            }
        }
        return bestCube;
    }

    private int getWaterLevelAt(int x, int y, int z) {
        Block b = world.getBlockAt(x, y, z);
        if (b instanceof Water) {
            return 8;
        }
        if (b instanceof FlowingWater) {
            return ((FlowingWater) b).getLevel();
        }
        return 0;
    }
}
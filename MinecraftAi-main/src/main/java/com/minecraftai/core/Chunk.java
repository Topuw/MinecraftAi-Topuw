package com.minecraftai.core;

import com.minecraftai.blocks.*;
import com.minecraftai.generator.Tree;
import com.minecraftai.renderer.TextureAtlas;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

public class Chunk {

    public static final int CHUNK_SIZE_X = 8;
    public static final int CHUNK_SIZE_Y = 128;
    public static final int CHUNK_SIZE_Z = 8;

    private Block[][][] blocks = new Block[CHUNK_SIZE_X][CHUNK_SIZE_Y][CHUNK_SIZE_Z];
    private int worldX, worldZ;
    private static final Random random = new Random();

    private int displayListIdOpaque = -1;
    private int displayListIdTransparent = -1;

    private boolean needsRebuild = true;
    private volatile boolean isGenerated = false;
    private volatile boolean isGenerating = false;
    private boolean isModified = false;

    public boolean isGenerating() {
        return isGenerating;
    }

    public Chunk(int chunkX, int chunkZ) {
        this.worldX = chunkX;
        this.worldZ = chunkZ;
    }

    public void generate(World world) {
        if (isGenerated || isGenerating) {
            return;
        }

        isGenerating = true;
        try {
            int startX = worldX * CHUNK_SIZE_X;
            int startZ = worldZ * CHUNK_SIZE_Z;

            int[][] surfaceHeights = new int[CHUNK_SIZE_X][CHUNK_SIZE_Z];
            boolean[][] isGrass = new boolean[CHUNK_SIZE_X][CHUNK_SIZE_Z];

            for (int x = 0; x < CHUNK_SIZE_X; x++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    int globalX = startX + x;
                    int globalZ = startZ + z;

                    double terrainNoise = world.getTerrainNoise(globalX, globalZ);
                    int surfaceHeight;
                    if (terrainNoise < 0) {
                        surfaceHeight = World.BASE_Y + (int) (terrainNoise * 14.0);
                    } else {
                        double hillNoise = Math.pow(terrainNoise, 1.5) * 44.0;
                        surfaceHeight = World.BASE_Y + (int) hillNoise;
                    }

                    surfaceHeights[x][z] = surfaceHeight;

                    for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                        if (y > surfaceHeight) {
                            if (y <= World.WATER_LEVEL) {
                                setBlock(x, y, z, new Water(globalX, y, globalZ), false);
                            } else {
                                blocks[x][y][z] = null;
                            }
                            continue;
                        }

                        if (y == 0) {
                            setBlock(x, y, z, new Bedrock(globalX, y, globalZ), false);
                            continue;
                        }
                        if (y < 4 && random.nextInt(y + 1) == 0) {
                            setBlock(x, y, z, new Bedrock(globalX, y, globalZ), false);
                            continue;
                        }

                        if (world.isCave(globalX, y, globalZ, surfaceHeight)) {
                            blocks[x][y][z] = null;
                            continue;
                        }

                        if (y == surfaceHeight) {
                            if (y >= World.WATER_LEVEL) {
                                setBlock(x, y, z, new GrassBlock(globalX, y, globalZ), false);
                                isGrass[x][z] = true;
                            } else {
                                setBlock(x, y, z, new Dirt(globalX, y, globalZ), false);
                            }
                        } else if (y > surfaceHeight - 4) {
                            setBlock(x, y, z, new Dirt(globalX, y, globalZ), false);
                        } else {
                            if (y < surfaceHeight - 5 && random.nextInt(70) == 0) {
                                setBlock(x, y, z, new KebabOre(globalX, y, globalZ), false);
                            } else {
                                setBlock(x, y, z, new Stone(globalX, y, globalZ), false);
                            }
                        }
                    }
                }
            }

            for (int x = 0; x < CHUNK_SIZE_X; x++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    if (isGrass[x][z] && random.nextInt(100) == 0) {
                        int globalX = startX + x;
                        int globalZ = startZ + z;
                        int y = surfaceHeights[x][z] + 1;

                        Tree.generateTree(world, globalX, y, globalZ);
                    }
                }
            }

            if (random.nextInt(30) == 0) {
                for (int attempt = 0; attempt < 50; attempt++) {
                    int rx = random.nextInt(CHUNK_SIZE_X);
                    int rz = random.nextInt(CHUNK_SIZE_Z);
                    int ry = random.nextInt(CHUNK_SIZE_Y - 5) + 2;

                    if (blocks[rx][ry][rz] == null && ry < surfaceHeights[rx][rz]) {
                        Block below = blocks[rx][ry - 1][rz];
                        if (below != null && below.isSolid()) {
                            Block above = (ry + 1 < CHUNK_SIZE_Y) ? blocks[rx][ry + 1][rz] : null;
                            if (above == null) {
                                float spawnX = startX + rx + 0.5f;
                                float spawnY = ry;
                                float spawnZ = startZ + rz + 0.5f;
                                world.spawnSulfurCube(spawnX, spawnY, spawnZ);
                                break;
                            }
                        }
                    }
                }
            }

            this.needsRebuild = true;
        } finally {
            this.isGenerating = false;
            this.isGenerated = true;
        }
    }

    public boolean isGenerated() {
        return isGenerated;
    }

    public double getDistanceToPlayer(Player player) {
        double chunkCenterX = (worldX * CHUNK_SIZE_X) + (CHUNK_SIZE_X / 2.0);
        double chunkCenterZ = (worldZ * CHUNK_SIZE_Z) + (CHUNK_SIZE_Z / 2.0);

        double dx = chunkCenterX - player.getX();
        double dz = chunkCenterZ - player.getZ();

        return dx * dx + dz * dz;
    }

    private void checkAndRebuild(World world) {
        if (!isGenerated) {
            return;
        }
        if (needsRebuild) {
            rebuildMeshes(world);
            needsRebuild = false;
        }
    }

    public void renderOpaque(World world) {
        checkAndRebuild(world);
        if (displayListIdOpaque > -1) {
            glCallList(displayListIdOpaque);
        }
    }

    public void renderTransparent(World world) {
        checkAndRebuild(world);
        if (displayListIdTransparent > -1) {
            glCallList(displayListIdTransparent);
        }
    }

    private void rebuildMeshes(World world) {
        if (displayListIdOpaque > -1) {
            glDeleteLists(displayListIdOpaque, 1);
        }
        if (displayListIdTransparent > -1) {
            glDeleteLists(displayListIdTransparent, 1);
        }

        displayListIdOpaque = glGenLists(1);
        glNewList(displayListIdOpaque, GL_COMPILE);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());
        glPushMatrix();
        glTranslatef(worldX * CHUNK_SIZE_X, 0, worldZ * CHUNK_SIZE_Z);

        glBegin(GL_QUADS);
        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    Block currentBlock = blocks[x][y][z];
                    if (currentBlock != null && !currentBlock.isTransparent()) {
                        renderBlockFacesBatched(currentBlock, x, y, z, world);
                    }
                }
            }
        }
        glEnd();

        glPopMatrix();
        glDisable(GL_TEXTURE_2D);
        glEndList();

        displayListIdTransparent = glGenLists(1);
        glNewList(displayListIdTransparent, GL_COMPILE);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());

        glPushMatrix();
        glTranslatef(worldX * CHUNK_SIZE_X, 0, worldZ * CHUNK_SIZE_Z);

        glColor4f(1.0f, 1.0f, 1.0f, 0.7f);
        glBegin(GL_QUADS);
        for (int x = 0; x < CHUNK_SIZE_X; x++) {
            for (int y = 0; y < CHUNK_SIZE_Y; y++) {
                for (int z = 0; z < CHUNK_SIZE_Z; z++) {
                    Block currentBlock = blocks[x][y][z];
                    if (currentBlock != null && currentBlock.isTransparent()) {
                        renderBlockFacesBatched(currentBlock, x, y, z, world);
                    }
                }
            }
        }
        glEnd();
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glPopMatrix();

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glEndList();
    }

    private float getVertexLight(World world, int vx, int vy, int vz) {
        if (com.minecraftai.Game.GAMMA_FULL_BRIGHT) {
            return 1.0f;
        }
        float l0 = world.getColumnLightFactor(vx - 1, vy, vz - 1);
        float l1 = world.getColumnLightFactor(vx, vy, vz - 1);
        float l2 = world.getColumnLightFactor(vx - 1, vy, vz);
        float l3 = world.getColumnLightFactor(vx, vy, vz);
        return (l0 + l1 + l2 + l3) / 4.0f;
    }

    private void renderBlockFacesBatched(Block current, int x, int y, int z, World world) {
        int globalX = worldX * CHUNK_SIZE_X + x;
        int globalZ = worldZ * CHUNK_SIZE_Z + z;
        float h = current.blockHeight;
        float alpha = current.isTransparent() ? 0.7f : 1.0f;

        Block neighbor = (y + 1 >= CHUNK_SIZE_Y) ? null : blocks[x][y + 1][z];
        if (shouldRenderFace(current, neighbor, false)) {
            float[] uv = TextureAtlas.getUV(current.getTextureIndex(Block.Face.TOP));
            glNormal3f(0, 1, 0);
            float l0 = getVertexLight(world, globalX, y + 1, globalZ);
            float l1 = getVertexLight(world, globalX + 1, y + 1, globalZ);
            float l2 = getVertexLight(world, globalX + 1, y + 1, globalZ + 1);
            float l3 = getVertexLight(world, globalX, y + 1, globalZ + 1);
            
            glColor4f(l0, l0, l0, alpha); glTexCoord2f(uv[0], uv[1]); glVertex3f(x, y + h, z);
            glColor4f(l1, l1, l1, alpha); glTexCoord2f(uv[2], uv[1]); glVertex3f(x + 1, y + h, z);
            glColor4f(l2, l2, l2, alpha); glTexCoord2f(uv[2], uv[3]); glVertex3f(x + 1, y + h, z + 1);
            glColor4f(l3, l3, l3, alpha); glTexCoord2f(uv[0], uv[3]); glVertex3f(x, y + h, z + 1);
        }

        neighbor = (y - 1 < 0) ? null : blocks[x][y - 1][z];
        if (shouldRenderFace(current, neighbor, false)) {
            float[] uv = TextureAtlas.getUV(current.getTextureIndex(Block.Face.BOTTOM));
            glNormal3f(0, -1, 0);
            float l0 = getVertexLight(world, globalX, y, globalZ);
            float l1 = getVertexLight(world, globalX + 1, y, globalZ);
            float l2 = getVertexLight(world, globalX + 1, y, globalZ + 1);
            float l3 = getVertexLight(world, globalX, y, globalZ + 1);
            
            glColor4f(l0, l0, l0, alpha); glTexCoord2f(uv[0], uv[1]); glVertex3f(x, y, z);
            glColor4f(l1, l1, l1, alpha); glTexCoord2f(uv[2], uv[1]); glVertex3f(x + 1, y, z);
            glColor4f(l2, l2, l2, alpha); glTexCoord2f(uv[2], uv[3]); glVertex3f(x + 1, y, z + 1);
            glColor4f(l3, l3, l3, alpha); glTexCoord2f(uv[0], uv[3]); glVertex3f(x, y, z + 1);
        }

        neighbor = (x + 1 >= CHUNK_SIZE_X) ? world.getBlockAt(globalX + 1, y, globalZ) : blocks[x + 1][y][z];
        if (shouldRenderFace(current, neighbor, true)) {
            float[] uv = TextureAtlas.getUV(current.getTextureIndex(Block.Face.EAST));
            glNormal3f(1, 0, 0);
            float l0 = getVertexLight(world, globalX + 1, y, globalZ);
            float l1 = getVertexLight(world, globalX + 1, y, globalZ + 1);
            float l2 = getVertexLight(world, globalX + 1, y + 1, globalZ + 1);
            float l3 = getVertexLight(world, globalX + 1, y + 1, globalZ);
            
            glColor4f(l0, l0, l0, alpha); glTexCoord2f(uv[0], uv[3]); glVertex3f(x + 1, y, z);
            glColor4f(l1, l1, l1, alpha); glTexCoord2f(uv[2], uv[3]); glVertex3f(x + 1, y, z + 1);
            glColor4f(l2, l2, l2, alpha); glTexCoord2f(uv[2], uv[1]); glVertex3f(x + 1, y + h, z + 1);
            glColor4f(l3, l3, l3, alpha); glTexCoord2f(uv[0], uv[1]); glVertex3f(x + 1, y + h, z);
        }

        neighbor = (x - 1 < 0) ? world.getBlockAt(globalX - 1, y, globalZ) : blocks[x - 1][y][z];
        if (shouldRenderFace(current, neighbor, true)) {
            float[] uv = TextureAtlas.getUV(current.getTextureIndex(Block.Face.WEST));
            glNormal3f(-1, 0, 0);
            float l0 = getVertexLight(world, globalX - 1, y, globalZ);
            float l1 = getVertexLight(world, globalX - 1, y, globalZ + 1);
            float l2 = getVertexLight(world, globalX - 1, y + 1, globalZ + 1);
            float l3 = getVertexLight(world, globalX - 1, y + 1, globalZ);
            
            glColor4f(l0, l0, l0, alpha); glTexCoord2f(uv[0], uv[3]); glVertex3f(x, y, z);
            glColor4f(l1, l1, l1, alpha); glTexCoord2f(uv[2], uv[3]); glVertex3f(x, y, z + 1);
            glColor4f(l2, l2, l2, alpha); glTexCoord2f(uv[2], uv[1]); glVertex3f(x, y + h, z + 1);
            glColor4f(l3, l3, l3, alpha); glTexCoord2f(uv[0], uv[1]); glVertex3f(x, y + h, z);
        }

        neighbor = (z + 1 >= CHUNK_SIZE_Z) ? world.getBlockAt(globalX, y, globalZ + 1) : blocks[x][y][z + 1];
        if (shouldRenderFace(current, neighbor, true)) {
            float[] uv = TextureAtlas.getUV(current.getTextureIndex(Block.Face.NORTH));
            glNormal3f(0, 0, 1);
            float l0 = getVertexLight(world, globalX, y, globalZ + 1);
            float l1 = getVertexLight(world, globalX + 1, y, globalZ + 1);
            float l2 = getVertexLight(world, globalX + 1, y + 1, globalZ + 1);
            float l3 = getVertexLight(world, globalX, y + 1, globalZ + 1);
            
            glColor4f(l0, l0, l0, alpha); glTexCoord2f(uv[0], uv[3]); glVertex3f(x, y, z + 1);
            glColor4f(l1, l1, l1, alpha); glTexCoord2f(uv[2], uv[3]); glVertex3f(x + 1, y, z + 1);
            glColor4f(l2, l2, l2, alpha); glTexCoord2f(uv[2], uv[1]); glVertex3f(x + 1, y + h, z + 1);
            glColor4f(l3, l3, l3, alpha); glTexCoord2f(uv[0], uv[1]); glVertex3f(x, y + h, z + 1);
        }

        neighbor = (z - 1 < 0) ? world.getBlockAt(globalX, y, globalZ - 1) : blocks[x][y][z - 1];
        if (shouldRenderFace(current, neighbor, true)) {
            float[] uv = TextureAtlas.getUV(current.getTextureIndex(Block.Face.SOUTH));
            glNormal3f(0, 0, -1);
            float l0 = getVertexLight(world, globalX, y, globalZ - 1);
            float l1 = getVertexLight(world, globalX + 1, y, globalZ - 1);
            float l2 = getVertexLight(world, globalX + 1, y + 1, globalZ - 1);
            float l3 = getVertexLight(world, globalX, y + 1, globalZ - 1);
            
            glColor4f(l0, l0, l0, alpha); glTexCoord2f(uv[0], uv[3]); glVertex3f(x, y, z);
            glColor4f(l1, l1, l1, alpha); glTexCoord2f(uv[2], uv[3]); glVertex3f(x + 1, y, z);
            glColor4f(l2, l2, l2, alpha); glTexCoord2f(uv[2], uv[1]); glVertex3f(x + 1, y + h, z);
            glColor4f(l3, l3, l3, alpha); glTexCoord2f(uv[0], uv[1]); glVertex3f(x, y + h, z);
        }
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private boolean shouldRenderFace(Block current, Block neighbor, boolean isSideFace) {
        if (neighbor == null) {
            return true;
        }
        if (neighbor.isTransparent() && !current.isTransparent()) {
            return true;
        }
        if (current.isTransparent() && neighbor.isTransparent()) {
            boolean isCurrentWater = current instanceof Water || current instanceof FlowingWater;
            boolean isNeighborWater = neighbor instanceof Water || neighbor instanceof FlowingWater;
            if (isCurrentWater && isNeighborWater) {
                if (isSideFace) {
                    return current.getBlockHeight() > neighbor.getBlockHeight();
                }
                return false;
            }
            return current.getClass() != neighbor.getClass();
        }
        return !neighbor.isSolid();
    }


    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return null;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, Block block, boolean markDirty) {
        if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
            return;
        }
        blocks[x][y][z] = block;
        if (markDirty) {
            this.needsRebuild = true;
            this.isModified = true;
        }
    }

    public void markDirty() {
        this.needsRebuild = true;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        this.isModified = modified;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldZ() {
        return worldZ;
    }

    public void setGenerated(boolean generated) {
        this.isGenerated = generated;
    }
}
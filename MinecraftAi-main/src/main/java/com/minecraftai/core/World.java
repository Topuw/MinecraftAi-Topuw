package com.minecraftai.core;

import com.minecraftai.blocks.Water;
import com.minecraftai.blocks.FlowingWater;
import com.minecraftai.blocks.Furnace;
import com.minecraftai.entities.SulfurCube;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.opengl.GL11.*;

public class World {
    private Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private List<DroppedItem> droppedItems = new CopyOnWriteArrayList<>();
    private final java.util.List<SulfurCube> sulfurCubes = new java.util.concurrent.CopyOnWriteArrayList<>();
    private PerlinNoise noiseGen;
    private double waterTickTimer = 0.0;
    private int chunksGeneratedThisFrame = 0;
    private final java.util.Set<BlockPos> activeWaterPos = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static final int BASE_Y = 64;
    public static final int WATER_LEVEL = 64;
    public static final double TERRAIN_SCALE = 0.015;
    public static final double CAVE_SCALE = 0.04;

    public static int RENDER_DISTANCE = 12;
    private long seed;

    public World() {
        setSeed(new Random().nextLong());
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        this.noiseGen = new PerlinNoise(seed);
    }

    public Map<String, Chunk> getChunks() {
        return chunks;
    }

    private String worldName = null;

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String name) {
        this.worldName = name;
    }

    public Chunk getOrLoadChunk(int chunkX, int chunkZ) {
        return getOrLoadChunk(chunkX, chunkZ, false);
    }

    public Chunk getOrLoadChunk(int chunkX, int chunkZ, boolean force) {
        String key = chunkX + "_" + chunkZ;

        Chunk chunk = chunks.computeIfAbsent(key, k -> {
            return new Chunk(chunkX, chunkZ);
        });

        if (!chunk.isGenerated() && !chunk.isGenerating()) {
            if (force || chunksGeneratedThisFrame < 8) {
                boolean loaded = false;
                if (worldName != null) {
                    byte[] data = WorldSaveManager.loadChunkData(worldName, chunkX, chunkZ);
                    if (data != null) {
                        chunksGeneratedThisFrame++;
                        int startX = chunkX * Chunk.CHUNK_SIZE_X;
                        int startZ = chunkZ * Chunk.CHUNK_SIZE_Z;
                        int idx = 0;
                        for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                            for (int y = 0; y < Chunk.CHUNK_SIZE_Y; y++) {
                                for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                                    int id = data[idx++] & 0xFF;
                                    Block b = WorldSaveManager.createBlockById(id, startX + x, y, startZ + z);
                                    chunk.setBlock(x, y, z, b, false);
                                    if (b instanceof Water || b instanceof FlowingWater) {
                                        queueWaterUpdate(startX + x, y, startZ + z);
                                    }
                                    if (b instanceof Furnace) {
                                        queueFurnaceUpdate(startX + x, y, startZ + z);
                                    }
                                }
                            }
                        }
                        chunk.setGenerated(true);
                        chunk.setModified(true);
                        chunk.markDirty();
                        markNeighborsDirty(chunkX, chunkZ);
                        loaded = true;
                    }
                }
                if (!loaded) {
                    chunksGeneratedThisFrame++;
                    chunk.generate(this);
                    markNeighborsDirty(chunkX, chunkZ);
                }
            }
        }

        return chunk;
    }

    public void markNeighborsDirty(int chunkX, int chunkZ) {
        int[] dx = {-1, 1, 0, 0};
        int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            Chunk neighbor = chunks.get((chunkX + dx[i]) + "_" + (chunkZ + dz[i]));
            if (neighbor != null) {
                neighbor.markDirty();
            }
        }
    }

    public void render(Player player) {
        chunksGeneratedThisFrame = 0;
        int playerChunkX = (int) Math.floor(player.getX() / Chunk.CHUNK_SIZE_X);
        int playerChunkZ = (int) Math.floor(player.getZ() / Chunk.CHUNK_SIZE_Z);

        List<int[]> chunkCoords = new ArrayList<>();
        for (int x = playerChunkX - RENDER_DISTANCE; x <= playerChunkX + RENDER_DISTANCE; x++) {
            for (int z = playerChunkZ - RENDER_DISTANCE; z <= playerChunkZ + RENDER_DISTANCE; z++) {
                chunkCoords.add(new int[]{x, z});
            }
        }

        chunkCoords.sort((a, b) -> {
            int distA = (a[0] - playerChunkX) * (a[0] - playerChunkX) + (a[1] - playerChunkZ) * (a[1] - playerChunkZ);
            int distB = (b[0] - playerChunkX) * (b[0] - playerChunkX) + (b[1] - playerChunkZ) * (b[1] - playerChunkZ);
            return Integer.compare(distA, distB);
        });

        List<Chunk> visibleChunks = new ArrayList<>();
        for (int[] coord : chunkCoords) {
            visibleChunks.add(getOrLoadChunk(coord[0], coord[1]));
        }

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);

        for (Chunk chunk : visibleChunks) {
            chunk.renderOpaque(this);
        }

        visibleChunks.sort((a, b) -> Double.compare(
                b.getDistanceToPlayer(player),
                a.getDistanceToPlayer(player)
            ));

        glEnable(GL_BLEND);
        glDepthMask(false);

        for (Chunk chunk : visibleChunks) {
            chunk.renderTransparent(this);
        }

        glDepthMask(true);
        glDisable(GL_BLEND);

        for (SulfurCube cube : sulfurCubes) {
            cube.render(player);
        }
        renderDroppedItems();
    }

    public void spawnDroppedItem(float x, float y, float z, ItemType type) {
        droppedItems.add(new DroppedItem(x, y, z, type));
    }

    public void updateDroppedItems(float dt, Player player) {
        for (DroppedItem item : droppedItems) {
            item.update(dt, this, player);

            float dx = item.getX() - player.getX();
            float dy = item.getY() - player.getY();
            float dz = item.getZ() - player.getZ();
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance < 1.5f) {
                player.addItem(item.getType());
                droppedItems.remove(item);
            }
        }
    }

    public void renderDroppedItems() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        for (DroppedItem item : droppedItems) {
            item.render();
        }
        glDisable(GL_TEXTURE_2D);
    }

    public Block getBlockAt(int globalX, int globalY, int globalZ) {
        return getBlockAt(globalX, globalY, globalZ, false);
    }

    public Block getBlockAt(int globalX, int globalY, int globalZ, boolean force) {
        if (globalY < 0 || globalY >= Chunk.CHUNK_SIZE_Y) {
            return null;
        }

        int chunkX = (int) Math.floor((double) globalX / Chunk.CHUNK_SIZE_X);
        int chunkZ = (int) Math.floor((double) globalZ / Chunk.CHUNK_SIZE_Z);

        Chunk chunk = getOrLoadChunk(chunkX, chunkZ, force);

        int localX = globalX % Chunk.CHUNK_SIZE_X;
        if (localX < 0) localX += Chunk.CHUNK_SIZE_X;

        int localZ = globalZ % Chunk.CHUNK_SIZE_Z;
        if (localZ < 0) localZ += Chunk.CHUNK_SIZE_Z;

        return chunk.getBlock(localX, globalY, localZ);
    }

    public void setBlockAt(int globalX, int globalY, int globalZ, Block block) {
        if (globalY < 0 || globalY >= Chunk.CHUNK_SIZE_Y) return;

        int chunkX = (int) Math.floor((double) globalX / Chunk.CHUNK_SIZE_X);
        int chunkZ = (int) Math.floor((double) globalZ / Chunk.CHUNK_SIZE_Z);

        Chunk chunk = getOrLoadChunk(chunkX, chunkZ);
        if (chunk == null) return;

        int localX = globalX % Chunk.CHUNK_SIZE_X;
        if (localX < 0) localX += Chunk.CHUNK_SIZE_X;

        int localZ = globalZ % Chunk.CHUNK_SIZE_Z;
        if (localZ < 0) localZ += Chunk.CHUNK_SIZE_Z;

        Block oldBlock = chunk.getBlock(localX, globalY, localZ);
        if (oldBlock instanceof Furnace) {
            removeFurnaceUpdate(globalX, globalY, globalZ);
            dropFurnaceContents(globalX, globalY, globalZ, (Furnace) oldBlock);
        }

        chunk.setBlock(localX, globalY, localZ, block, true);

        if (block instanceof Furnace) {
            queueFurnaceUpdate(globalX, globalY, globalZ);
        }

        queueWaterUpdate(globalX, globalY, globalZ);
        queueWaterUpdate(globalX - 1, globalY, globalZ);
        queueWaterUpdate(globalX + 1, globalY, globalZ);
        queueWaterUpdate(globalX, globalY - 1, globalZ);
        queueWaterUpdate(globalX, globalY + 1, globalZ);
        queueWaterUpdate(globalX, globalY, globalZ - 1);
        queueWaterUpdate(globalX, globalY, globalZ + 1);

        Chunk neighbor;
        if (localX == 0) {
            neighbor = chunks.get((chunkX - 1) + "_" + chunkZ);
            if (neighbor != null) neighbor.markDirty();
        } else if (localX == Chunk.CHUNK_SIZE_X - 1) {
            neighbor = chunks.get((chunkX + 1) + "_" + chunkZ);
            if (neighbor != null) neighbor.markDirty();
        }

        if (localZ == 0) {
            neighbor = chunks.get(chunkX + "_" + (chunkZ - 1));
            if (neighbor != null) neighbor.markDirty();
        } else if (localZ == Chunk.CHUNK_SIZE_Z - 1) {
            neighbor = chunks.get(chunkX + "_" + (chunkZ + 1));
            if (neighbor != null) neighbor.markDirty();
        }
    }

    public void addBlock(int x, int y, int z, Block block) {
        setBlockAt(x, y, z, block);
    }

    public void removeBlock(int x, int y, int z) {
        setBlockAt(x, y, z, null);
    }

    public double getTerrainNoise(double x, double z) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 0.005;
        double maxValue = 0.0;

        for (int i = 0; i < 4; i++) {
            total += noiseGen.noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= 0.55;
            frequency *= 2.0;
        }

        return total / maxValue;
    }

    public double getCaveNoise(double x, double y, double z) {
        return noiseGen.noise(x * CAVE_SCALE, y * CAVE_SCALE * 2.0, z * CAVE_SCALE);
    }

    public boolean isCave(double x, double y, double z, int surfaceHeight) {
        if (y <= 4) {
            return false;
        }
        if (y > surfaceHeight) {
            return false;
        }

        // Global low-frequency noise mask to reduce cave frequency
        double caveMask = noiseGen.noise(x * 0.01, y * 0.015, z * 0.01);
        if (caveMask < 0.15) {
            return false;
        }

        double scale = 0.025;
        double n1 = noiseGen.noise(x * scale, y * (scale * 1.2), z * scale);
        double n2 = noiseGen.noise(x * scale + 2000.0, y * (scale * 1.2) + 5000.0, z * scale - 3000.0);

        double val = n1 * n1 + n2 * n2;
        double threshold = 0.008; // Much thinner caves (was 0.024)

        if (y >= surfaceHeight - 4) {
            double maskNoise = noiseGen.noise(x * 0.005, z * 0.005); // Lower frequency mask
            if (maskNoise < 0.6) { // Rarer surface entrances (was 0.5)
                return false;
            }
            threshold = 0.004; // Thinner surface entrance (was 0.014)
        }

        return val < threshold;
    }

    public float getColumnLightFactor(int x, int y, int z) {
        if (y < 0) return 0.2f;
        if (y >= Chunk.CHUNK_SIZE_Y) return 1.0f;

        int chunkX = (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X);
        int chunkZ = (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z);

        String key = chunkX + "_" + chunkZ;
        Chunk chunk = chunks.get(key);
        if (chunk == null) return 1.0f;

        int localX = x % Chunk.CHUNK_SIZE_X;
        if (localX < 0) localX += Chunk.CHUNK_SIZE_X;
        int localZ = z % Chunk.CHUNK_SIZE_Z;
        if (localZ < 0) localZ += Chunk.CHUNK_SIZE_Z;

        float totalBlockage = 0.0f;
        for (int cy = y + 1; cy < Chunk.CHUNK_SIZE_Y; cy++) {
            Block b = chunk.getBlock(localX, cy, localZ);
            if (b != null) {
                if (b.getClass().getSimpleName().equals("Leaves")) {
                    totalBlockage += 0.15f;
                } else if (b.isSolid()) {
                    totalBlockage += 1.0f;
                }
            }
            if (totalBlockage >= 0.8f) {
                break;
            }
        }
        float skyLight = Math.max(0.2f, 1.0f - totalBlockage);

        float furnaceLight = 0.0f;
        for (BlockPos pos : activeFurnaces) {
            int dx = Math.abs(pos.x - x);
            int dy = Math.abs(pos.y - y);
            int dz = Math.abs(pos.z - z);
            int dist = dx + dy + dz;
            if (dist <= 3) {
                Block b = getBlockAt(pos.x, pos.y, pos.z);
                if (b instanceof Furnace && ((Furnace) b).isLit()) {
                    float val = 1.0f - dist * 0.25f;
                    if (val > furnaceLight) {
                        furnaceLight = val;
                    }
                }
            }
        }
        return Math.max(skyLight, furnaceLight);
    }

    private final java.util.Set<BlockPos> activeFurnaces = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public void queueFurnaceUpdate(int x, int y, int z) {
        activeFurnaces.add(new BlockPos(x, y, z));
    }

    public void removeFurnaceUpdate(int x, int y, int z) {
        activeFurnaces.remove(new BlockPos(x, y, z));
    }

    public void updateFurnaces(double dt) {
        java.util.List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos pos : activeFurnaces) {
            Block b = getBlockAt(pos.x, pos.y, pos.z);
            if (b instanceof Furnace) {
                ((Furnace) b).update(this, dt);
            } else {
                toRemove.add(pos);
            }
        }
        activeFurnaces.removeAll(toRemove);
    }

    private void dropFurnaceContents(int x, int y, int z, Furnace furnace) {
        if (furnace.getInput() != null) {
            for (int i = 0; i < furnace.getInput().getCount(); i++) {
                spawnDroppedItem(x + 0.5f, y + 0.5f, z + 0.5f, furnace.getInput().getType());
            }
        }
        if (furnace.getFuel() != null) {
            for (int i = 0; i < furnace.getFuel().getCount(); i++) {
                spawnDroppedItem(x + 0.5f, y + 0.5f, z + 0.5f, furnace.getFuel().getType());
            }
        }
        if (furnace.getOutput() != null) {
            for (int i = 0; i < furnace.getOutput().getCount(); i++) {
                spawnDroppedItem(x + 0.5f, y + 0.5f, z + 0.5f, furnace.getOutput().getType());
            }
        }
    }

    public void queueWaterUpdate(int x, int y, int z) {
        activeWaterPos.add(new BlockPos(x, y, z));
    }

    public void updateWater(double dt) {
        waterTickTimer += dt;
        if (waterTickTimer >= 0.15) {
            waterTickTimer = 0.0;
            tickWater();
        }
    }

    private void tickWater() {
        if (activeWaterPos.isEmpty()) return;

        List<BlockPos> toProcess = new ArrayList<>(activeWaterPos);
        activeWaterPos.clear();

        for (BlockPos pos : toProcess) {
            Block current = getBlockAt(pos.x, pos.y, pos.z);
            if (current instanceof Water) {
                Block above = getBlockAt(pos.x, pos.y + 1, pos.z);
                boolean isSource = (pos.y >= WATER_LEVEL) || !(above instanceof Water || above instanceof FlowingWater);
                if (isSource) {
                    spreadWater(pos.x, pos.y, pos.z, 8);
                } else {
                    int targetLevel = calculateTargetWaterLevel(pos.x, pos.y, pos.z);
                    if (targetLevel == 0) {
                        removeBlock(pos.x, pos.y, pos.z);
                    } else if (targetLevel < 8) {
                        FlowingWater flowing = new FlowingWater(pos.x, pos.y, pos.z, targetLevel);
                        setBlockAt(pos.x, pos.y, pos.z, flowing);
                        spreadWater(pos.x, pos.y, pos.z, targetLevel);
                    } else {
                        spreadWater(pos.x, pos.y, pos.z, 8);
                    }
                }
            } else if (current instanceof FlowingWater) {
                FlowingWater flowing = (FlowingWater) current;
                int targetLevel = calculateTargetWaterLevel(pos.x, pos.y, pos.z);
                if (targetLevel == 0) {
                    removeBlock(pos.x, pos.y, pos.z);
                } else if (targetLevel == 8) {
                    Water water = new Water(pos.x, pos.y, pos.z);
                    setBlockAt(pos.x, pos.y, pos.z, water);
                    spreadWater(pos.x, pos.y, pos.z, 8);
                } else if (targetLevel != flowing.getLevel()) {
                    flowing.setLevel(targetLevel);
                    setBlockAt(pos.x, pos.y, pos.z, flowing);
                    spreadWater(pos.x, pos.y, pos.z, targetLevel);
                } else {
                    spreadWater(pos.x, pos.y, pos.z, targetLevel);
                }
            } else if (current == null) {
                int targetLevel = calculateTargetWaterLevel(pos.x, pos.y, pos.z);
                if (targetLevel > 0) {
                    Block newWater;
                    if (targetLevel == 8) {
                        newWater = new Water(pos.x, pos.y, pos.z);
                    } else {
                        newWater = new FlowingWater(pos.x, pos.y, pos.z, targetLevel);
                    }
                    setBlockAt(pos.x, pos.y, pos.z, newWater);
                    spreadWater(pos.x, pos.y, pos.z, targetLevel);
                }
            }
        }
    }

    private int getWaterLevel(Block b) {
        if (b instanceof Water) return 8;
        if (b instanceof FlowingWater) return ((FlowingWater) b).getLevel();
        return 0;
    }

    private int calculateTargetWaterLevel(int x, int y, int z) {
        Block above = getBlockAt(x, y + 1, z);
        if (above instanceof Water || above instanceof FlowingWater) {
            return 8;
        }

        int maxNeighborLevel = 0;
        int[] dx = {-1, 1, 0, 0};
        int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            Block n = getBlockAt(x + dx[i], y, z + dz[i]);
            if (n instanceof Water) {
                maxNeighborLevel = Math.max(maxNeighborLevel, 8);
            } else if (n instanceof FlowingWater) {
                maxNeighborLevel = Math.max(maxNeighborLevel, ((FlowingWater) n).getLevel());
            }
        }

        return Math.max(0, maxNeighborLevel - 1);
    }

    private void spreadWater(int x, int y, int z, int level) {
        if (y > 0) {
            Block below = getBlockAt(x, y - 1, z);
            if (below == null || ((below instanceof Water || below instanceof FlowingWater) && getWaterLevel(below) < 8)) {
                queueWaterUpdate(x, y - 1, z);
            }
        }

        boolean canSpreadHorizontally = false;
        if (y > 0) {
            Block below = getBlockAt(x, y - 1, z);
            if (below != null && below.isSolid()) {
                canSpreadHorizontally = true;
            }
        } else {
            canSpreadHorizontally = true;
        }

        if (canSpreadHorizontally && level > 1) {
            int[] dx = {-1, 1, 0, 0};
            int[] dz = {0, 0, -1, 1};
            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int nz = z + dz[i];
                Block neighbor = getBlockAt(nx, y, nz);
                if (neighbor == null || ((neighbor instanceof Water || neighbor instanceof FlowingWater) && getWaterLevel(neighbor) < level - 1)) {
                    queueWaterUpdate(nx, y, nz);
                }
            }
        }
    }

    public java.util.List<SulfurCube> getSulfurCubes() {
        return sulfurCubes;
    }

    public void spawnSulfurCube(float x, float y, float z) {
        sulfurCubes.add(new SulfurCube(x, y, z));
    }

    public void updateSulfurCubes(float dt, Player player) {
        for (SulfurCube cube : sulfurCubes) {
            cube.update(dt, this, player);
        }
    }
}
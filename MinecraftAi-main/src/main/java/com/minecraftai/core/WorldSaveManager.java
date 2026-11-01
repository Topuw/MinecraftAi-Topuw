package com.minecraftai.core;

import com.minecraftai.Game;
import com.minecraftai.blocks.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class WorldSaveManager {
    private static final String APPDATA_ROOT = System.getenv("APPDATA") != null 
        ? System.getenv("APPDATA") 
        : System.getProperty("user.home") + "/AppData/Roaming";
        
    private static final String SAVES_DIR = APPDATA_ROOT + "/MinecraftAI/saves";
    private static final String OPTIONS_FILE = APPDATA_ROOT + "/MinecraftAI/options.json";

    public static File getWorldDir(String worldName) {
        return new File(SAVES_DIR, worldName);
    }

    public static boolean worldExists(String worldName) {
        return new File(getWorldDir(worldName), "level.dat").exists();
    }

    public static double getWorldSizeMB(String worldName) {
        File dir = getWorldDir(worldName);
        if (!dir.exists()) return 0.0;
        long bytes = getFolderSize(dir);
        return (double) bytes / (1024.0 * 1024.0);
    }

    private static long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += getFolderSize(file);
                }
            }
        }
        return length;
    }

    public static void deleteWorld(String worldName) {
        File dir = getWorldDir(worldName);
        if (dir.exists()) {
            deleteFolder(dir);
        }
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }

    public static void saveWorld(World world, Player player, float timeOfDay, String worldName) {
        try {
            File worldDir = getWorldDir(worldName);
            File chunksDir = new File(worldDir, "chunks");
            if (!chunksDir.exists()) {
                chunksDir.mkdirs();
            }

            File levelFile = new File(worldDir, "level.dat");
            try (PrintWriter pw = new PrintWriter(new FileWriter(levelFile))) {
                pw.println("seed=" + world.getSeed());
                pw.println("timeOfDay=" + timeOfDay);
                pw.println("playerX=" + player.getX());
                pw.println("playerY=" + player.getY());
                pw.println("playerZ=" + player.getZ());
                pw.println("playerYaw=" + player.getYaw());
                pw.println("playerPitch=" + player.getPitch());
                pw.println("playerHealth=" + player.getHealth());
                pw.println("playerSelectedSlot=" + player.getSelectedSlot());

                StringBuilder invSb = new StringBuilder();
                ItemStack[] inv = player.getInventory();
                for (int i = 0; i < inv.length; i++) {
                    ItemStack stack = inv[i];
                    if (stack != null) {
                        if (invSb.length() > 0) invSb.append(";");
                        invSb.append(i).append(":").append(stack.getType().name()).append(":").append(stack.getCount()).append(":").append(stack.getDurability());
                    }
                }
                pw.println("inventory=" + invSb.toString());
            }

            for (Chunk chunk : world.getChunks().values()) {
                if (chunk.isModified()) {
                    File chunkFile = new File(chunksDir, "chunk_" + chunk.getWorldX() + "_" + chunk.getWorldZ() + ".dat");
                    byte[] data = new byte[Chunk.CHUNK_SIZE_X * Chunk.CHUNK_SIZE_Y * Chunk.CHUNK_SIZE_Z];
                    int idx = 0;
                    for (int x = 0; x < Chunk.CHUNK_SIZE_X; x++) {
                        for (int y = 0; y < Chunk.CHUNK_SIZE_Y; y++) {
                            for (int z = 0; z < Chunk.CHUNK_SIZE_Z; z++) {
                                Block b = chunk.getBlock(x, y, z);
                                data[idx++] = (byte) getBlockId(b);
                            }
                        }
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                        gzos.write(data);
                    }
                    Files.write(chunkFile.toPath(), baos.toByteArray());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float loadWorld(World world, Player player, String worldName) {
        float timeOfDay = 6000.0f;
        try {
            File worldDir = getWorldDir(worldName);
            File levelFile = new File(worldDir, "level.dat");
            if (!levelFile.exists()) return timeOfDay;

            BufferedReader br = new BufferedReader(new FileReader(levelFile));
            String line;
            long seed = 0;
            float px = 0, py = 80, pz = 0;
            float yaw = 0, pitch = 0;
            int health = 20;
            int selectedSlot = 0;
            String inventoryStr = "";

            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length < 2) continue;
                String key = parts[0];
                String val = parts[1];
                switch (key) {
                    case "seed": seed = Long.parseLong(val); break;
                    case "timeOfDay": timeOfDay = Float.parseFloat(val); break;
                    case "playerX": px = Float.parseFloat(val); break;
                    case "playerY": py = Float.parseFloat(val); break;
                    case "playerZ": pz = Float.parseFloat(val); break;
                    case "playerYaw": yaw = Float.parseFloat(val); break;
                    case "playerPitch": pitch = Float.parseFloat(val); break;
                    case "playerHealth": health = Integer.parseInt(val); break;
                    case "playerSelectedSlot": selectedSlot = Integer.parseInt(val); break;
                    case "inventory": inventoryStr = val; break;
                }
            }
            br.close();

            world.setSeed(seed);

            player.setPosition(px, py, pz);
            player.setYaw(yaw);
            player.setPitch(pitch);
            player.setHealth(health);
            player.setSelectedSlot(selectedSlot);

            ItemStack[] inv = player.getInventory();
            Arrays.fill(inv, null);

            if (!inventoryStr.isEmpty()) {
                String[] slots = inventoryStr.split(";");
                for (String slot : slots) {
                    if (slot.isEmpty()) continue;
                    String[] slotParts = slot.split(":");
                    int idx = Integer.parseInt(slotParts[0]);
                    ItemType type = ItemType.valueOf(slotParts[1]);
                    int count = Integer.parseInt(slotParts[2]);
                    int durability = slotParts.length > 3 ? Integer.parseInt(slotParts[3]) : -1;
                    inv[idx] = new ItemStack(type, count);
                    if (durability != -1) {
                        inv[idx].setDurability(durability);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timeOfDay;
    }

    public static byte[] loadChunkData(String worldName, int chunkX, int chunkZ) {
        File chunkFile = new File(new File(getWorldDir(worldName), "chunks"), "chunk_" + chunkX + "_" + chunkZ + ".dat");
        if (chunkFile.exists()) {
            try {
                byte[] compressedData = Files.readAllBytes(chunkFile.toPath());
                byte[] decompressedData = new byte[Chunk.CHUNK_SIZE_X * Chunk.CHUNK_SIZE_Y * Chunk.CHUNK_SIZE_Z];

                try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
                    int bytesRead = 0;
                    while (bytesRead < decompressedData.length) {
                        int n = gzis.read(decompressedData, bytesRead, decompressedData.length - bytesRead);
                        if (n == -1) break;
                        bytesRead += n;
                    }
                }
                return decompressedData;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static int getBlockId(Block block) {
        if (block == null) return 0;
        String name = block.getClass().getSimpleName();
        switch (name) {
            case "Dirt": return 1;
            case "Stone": return 2;
            case "Cobblestone": return 3;
            case "GrassBlock": return 4;
            case "Water": return 5;
            case "Leaves": return 6;
            case "Log": return 7;
            case "Bedrock": return 8;
            case "FlowingWater": return 9;
            case "Planks": return 10;
            case "CraftingTable": return 11;
            case "Furnace": return 12;
            case "KebabOre": return 13;
            case "KebabBlock": return 14;
            default: return 0;
        }
    }

    public static Block createBlockById(int id, int x, int y, int z) {
        switch (id) {
            case 1: return new Dirt(x, y, z);
            case 2: return new Stone(x, y, z);
            case 3: return new Cobblestone(x, y, z);
            case 4: return new GrassBlock(x, y, z);
            case 5: return new Water(x, y, z);
            case 6: return new Leaves(x, y, z);
            case 7: return new Log(x, y, z);
            case 8: return new Bedrock(x, y, z);
            case 9: return new FlowingWater(x, y, z, 7);
            case 10: return new Planks(x, y, z);
            case 11: return new CraftingTable(x, y, z);
            case 12: return new Furnace(x, y, z);
            case 13: return new KebabOre(x, y, z);
            case 14: return new KebabBlock(x, y, z);
            default: return null;
        }
    }

    public static void loadOptions() {
        File file = new File(OPTIONS_FILE);
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                int idx = content.indexOf("renderDistance");
                if (idx != -1) {
                    int colonIdx = content.indexOf(":", idx);
                    if (colonIdx != -1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = colonIdx + 1; i < content.length(); i++) {
                            char c = content.charAt(i);
                            if (Character.isDigit(c)) {
                                sb.append(c);
                            } else if (sb.length() > 0) {
                                break;
                            }
                        }
                        if (sb.length() > 0) {
                            World.RENDER_DISTANCE = Integer.parseInt(sb.toString());
                        }
                    }
                }
                int cmdIdx = content.indexOf("commandsEnabled");
                if (cmdIdx != -1) {
                    int colonIdx = content.indexOf(":", cmdIdx);
                    if (colonIdx != -1) {
                        int endIdx = content.indexOf("\n", colonIdx);
                        if (endIdx == -1) endIdx = content.indexOf("}", colonIdx);
                        if (endIdx != -1) {
                            String valStr = content.substring(colonIdx + 1, endIdx).trim();
                            if (valStr.endsWith(",")) {
                                valStr = valStr.substring(0, valStr.length() - 1).trim();
                            }
                            Game.COMMANDS_ENABLED = Boolean.parseBoolean(valStr);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveOptions() {
        File file = new File(OPTIONS_FILE);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) parentDir.mkdirs();
        try {
            String json = "{\n  \"renderDistance\": " + World.RENDER_DISTANCE + ",\n  \"commandsEnabled\": " + Game.COMMANDS_ENABLED + "\n}";
            Files.writeString(file.toPath(), json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
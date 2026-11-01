package com.minecraftai.generator;

import com.minecraftai.blocks.Leaves;
import com.minecraftai.blocks.Log;
import com.minecraftai.core.Chunk;
import com.minecraftai.core.World;

import java.util.Random;

public class Tree {
    private static final Random random = new Random();

    public static void generateTree(World world, int globalX, int globalY, int globalZ) {
        int treeHeight = 4 + random.nextInt(3);

        if (globalY + treeHeight + 1 >= Chunk.CHUNK_SIZE_Y) {
            return;
        }

        for (int i = 0; i < treeHeight; i++) {
            int currentY = globalY + i;
            world.setBlockAt(globalX, currentY, globalZ, new Log(globalX, currentY, globalZ));
        }

        int leafStartY = globalY + treeHeight - 2;
        int leafTopY = globalY + treeHeight + 1;

        for (int y = leafStartY; y < leafTopY; y++) {
            if (y < 0 || y >= Chunk.CHUNK_SIZE_Y) {
                continue;
            }

            int radius = (y == leafTopY - 1) ? 1 : 2;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {

                    if (Math.abs(x) == radius && Math.abs(z) == radius && radius > 1) {
                        continue;
                    }

                    int currentGlobalX = globalX + x;
                    int currentGlobalZ = globalZ + z;

                    if (world.getBlockAt(currentGlobalX, y, currentGlobalZ) == null) {
                        world.setBlockAt(currentGlobalX, y, currentGlobalZ, new Leaves(currentGlobalX, y, currentGlobalZ));
                    }
                }
            }
        }
    }
}
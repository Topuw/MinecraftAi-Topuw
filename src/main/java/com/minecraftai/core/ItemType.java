package com.minecraftai.core;

import com.minecraftai.renderer.TextureLoader;

public enum ItemType {
    DIRT,
    GRASS_BLOCK,
    LEAVES,
    BEDROCK,
    WATER,
    FLOWING_WATER,
    COBBLESTONE,
    LOG,
    PLANKS,
    CRAFTING_TABLE,
    STICK,
    FURNACE,
    STONE,
    WOODEN_PICKAXE,
    KEBAB_ORE,
    KEBAB_BLOCK,
    KEBAB,
    MASLO_ORE,
    MASLO_INGOT,
    MASLO_PICKAXE,
    CLAY,
    CLAY_BALL,
    TERRACOTTA,
    BRICK,
    BRICKS,
    SAND,
    CACTUS,
    SANDSTONE,
    GLASS,
    GREEN_DYE,
    GREEN_TERRACOTTA,
    STONE_PICKAXE;


    private int textureId = 0;

    public int getTextureId() {
        if (textureId == 0) {
            switch (this) {
                case DIRT:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/dirt.png");
                    break;
                case GRASS_BLOCK:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/grass.png");
                    break;
                case LEAVES:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/leaves.png");
                    break;
                case BEDROCK:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/bedrock.png");
                    break;
                case WATER:
                case FLOWING_WATER:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/water.png");
                    break;
                case COBBLESTONE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/cobblestone.png");
                    break;
                case LOG:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/log.png");
                    break;
                case PLANKS:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/planks.png");
                    break;
                case CRAFTING_TABLE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/crafting_table_front.png");
                    break;
                case STICK:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/stick.png");
                    break;
                case FURNACE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/furnace_front.png");
                    break;
                case STONE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/stone.png");
                    break;
                case WOODEN_PICKAXE:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/wooden_pickaxe.png");
                    break;
                case KEBAB_ORE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/kebab_ore.png");
                    break;
                case KEBAB_BLOCK:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/kebab_block.png");
                    break;
                case KEBAB:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/kebab.png");
                    break;
                case MASLO_ORE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/maslo_ore.png");
                    break;
                case MASLO_INGOT:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/maslo.png");
                    break;
                case MASLO_PICKAXE:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/maslo_pickaxe.png");
                    break;
                case CLAY:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/clay.png");
                    break;
                case CLAY_BALL:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/clay_ball.png");
                    break;
                case TERRACOTTA:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/terracotta.png");
                    break;
                case BRICK:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/brick.png");
                    break;
                case BRICKS:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/bricks.png");
                    break;
                case SAND:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/sand.png");
                    break;
                case CACTUS:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/cactus_side.png");
                    break;
                case SANDSTONE:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/sandstone.png");
                    break;
                case GLASS:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/glass.png");
                    break;
                case GREEN_DYE:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/green_dye.png");
                    break;
                case GREEN_TERRACOTTA:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/green_terracotta.png");
                    break;
                case STONE_PICKAXE:
                    textureId = TextureLoader.loadTexture("/assets/textures/item/stone_pickaxe.png");
                    break;
            }
        }
        return textureId;
    }

}
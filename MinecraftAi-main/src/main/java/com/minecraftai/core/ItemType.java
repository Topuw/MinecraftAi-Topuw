package com.minecraftai.core;

import com.minecraftai.renderer.TextureLoader;

public enum ItemType {
    DIRT,
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
    KEBAB;

    private int textureId = 0;

    public int getTextureId() {
        if (textureId == 0) {
            switch (this) {
                case DIRT:
                    textureId = TextureLoader.loadTexture("/assets/textures/block/dirt.png");
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
            }
        }
        return textureId;
    }
}
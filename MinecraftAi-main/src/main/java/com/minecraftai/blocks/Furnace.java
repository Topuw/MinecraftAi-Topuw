package com.minecraftai.blocks;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemStack;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.World;
import com.minecraftai.core.Chunk;

public class Furnace extends Block {
    private Face facing;
    private boolean isLit;

    private ItemStack input = null;
    private ItemStack fuel = null;
    private ItemStack output = null;

    private float burnTime = 0.0f;
    private float maxBurnTime = 0.0f;
    private float cookTime = 0.0f;
    private final float maxCookTime = 10.0f;

    public Furnace(int x, int y, int z) {
        this(x, y, z, Face.NORTH);
    }

    public Furnace(int x, int y, int z, Face facing) {
        super(x, y, z);
        this.facing = facing;
        this.isLit = false;
    }

    public Face getFacing() {
        return facing;
    }

    public void setFacing(Face facing) {
        this.facing = facing;
    }

    public boolean isLit() {
        return isLit;
    }

    public void setLit(boolean lit) {
        this.isLit = lit;
    }

    public ItemStack getInput() { return input; }
    public void setInput(ItemStack stack) { this.input = stack; }

    public ItemStack getFuel() { return fuel; }
    public void setFuel(ItemStack stack) { this.fuel = stack; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack stack) { this.output = stack; }

    public float getBurnProgress() {
        if (maxBurnTime <= 0.0f) return 0.0f;
        return burnTime / maxBurnTime;
    }

    public float getCookProgress() {
        return cookTime / maxCookTime;
    }

    @Override
    public int getTextureIndex(Face face) {
        if (face == Face.TOP || face == Face.BOTTOM) {
            return 13;
        } else if (face == facing) {
            return isLit ? 16 : 15;
        } else {
            return 14;
        }
    }

    @Override
    public ItemType getItemDrop() {
        return ItemType.FURNACE;
    }

    public void update(World world, double dt) {
        boolean litBefore = isLit;

        if (burnTime > 0.0f) {
            burnTime -= (float) dt;
            if (burnTime < 0.0f) {
                burnTime = 0.0f;
            }
        }

        ItemType resultType = getSmeltingResult(input);
        boolean canSmelt = resultType != null && (output == null || (output.getType() == resultType && output.getCount() < ItemStack.MAX_STACK_SIZE));

        if (burnTime <= 0.0f && canSmelt && fuel != null && fuel.getCount() > 0) {
            float duration = getBurnDuration(fuel.getType());
            if (duration > 0.0f) {
                burnTime = duration;
                maxBurnTime = duration;
                fuel.setCount(fuel.getCount() - 1);
                if (fuel.getCount() <= 0) {
                    fuel = null;
                }
            }
        }

        isLit = (burnTime > 0.0f);

        if (isLit && canSmelt) {
            cookTime += (float) dt;
            if (cookTime >= maxCookTime) {
                cookTime = 0.0f;
                if (output == null) {
                    output = new ItemStack(resultType, 1);
                } else {
                    output.addAmount(1);
                }
                input.setCount(input.getCount() - 1);
                if (input.getCount() <= 0) {
                    input = null;
                }
            }
        } else {
            cookTime = Math.max(0.0f, cookTime - (float)dt * 2.0f);
        }

        if (isLit != litBefore) {
            int chunkX = (int) Math.floor((double) x / Chunk.CHUNK_SIZE_X);
            int chunkZ = (int) Math.floor((double) z / Chunk.CHUNK_SIZE_Z);
            Chunk chunk = world.getChunks().get(chunkX + "_" + chunkZ);
            if (chunk != null) {
                chunk.markDirty();
            }
        }
    }

    private ItemType getSmeltingResult(ItemStack inputStack) {
        if (inputStack == null || inputStack.getCount() <= 0) return null;
        if (inputStack.getType() == ItemType.COBBLESTONE) {
            return ItemType.STONE;
        }
        if (inputStack.getType() == ItemType.KEBAB_ORE) {
            return ItemType.KEBAB;
        }
        return null;
    }

    private float getBurnDuration(ItemType type) {
        switch (type) {
            case LOG:
            case PLANKS:
            case CRAFTING_TABLE:
                return 15.0f;
            case STICK:
                return 5.0f;
            default:
                return 0.0f;
        }
    }
}
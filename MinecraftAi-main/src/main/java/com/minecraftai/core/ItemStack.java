package com.minecraftai.core;

public class ItemStack {
    private ItemType type;
    private int count;
    private int durability;

    public static final int MAX_STACK_SIZE = 64;

    public ItemStack(ItemType type, int count) {
        this.type = type;
        this.count = count;
        if (type == ItemType.WOODEN_PICKAXE) {
            this.durability = 50;
        } else {
            this.durability = -1;
        }
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = durability;
    }

    public void decrementDurability() {
        if (this.durability > 0) {
            this.durability--;
        }
    }

    public boolean isBroken() {
        return this.durability == 0;
    }

    public ItemType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int addAmount(int amount) {
        int canAdd = MAX_STACK_SIZE - this.count;
        int toAdd = Math.min(amount, canAdd);
        this.count += toAdd;
        return amount - toAdd;
    }

    public boolean isFull() {
        return this.count >= MAX_STACK_SIZE;
    }
}
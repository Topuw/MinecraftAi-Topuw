package com.minecraftai.core;

public abstract class Item {
    protected ItemType type;

    public Item(ItemType type) {
        this.type = type;
    }

    public ItemType getType() {
        return type;
    }
}
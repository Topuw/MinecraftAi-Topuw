package com.minecraftai.gui;

import com.minecraftai.core.Player;
import com.minecraftai.core.ItemStack;
import com.minecraftai.core.ItemType;
import com.minecraftai.blocks.Furnace;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.renderer.FontRenderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class FurnaceMenu {
    private Player player;
    private Furnace block;
    private int guiTextureID;
    private ItemStack cursorStack = null;
    private double mouseX, mouseY;

    public FurnaceMenu(Player player) {
        this.player = player;
    }

    public void setBlock(Furnace block) {
        this.block = block;
    }

    public void init() {
        guiTextureID = TextureLoader.loadTexture("/assets/textures/gui/furnace.png");
    }

    public void handleMouseMove(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public ItemStack getCursorStack() {
        return cursorStack;
    }

    public void setCursorStack(ItemStack stack) {
        this.cursorStack = stack;
    }

    public void handleMouseClick(double mouseX, double mouseY, int button, int action) {
        if (block == null) return;
        if (action != GLFW_PRESS) return;

        int clickedSlot = getClickedSlot(mouseX, mouseY);
        if (clickedSlot == -1) return;

        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            if (clickedSlot == 38) { // Output slot (take only)
                ItemStack outputStack = block.getOutput();
                if (outputStack != null) {
                    if (cursorStack == null) {
                        cursorStack = outputStack;
                        block.setOutput(null);
                    } else if (cursorStack.getType() == outputStack.getType() &&
                               cursorStack.getCount() + outputStack.getCount() <= ItemStack.MAX_STACK_SIZE) {
                        cursorStack.addAmount(outputStack.getCount());
                        block.setOutput(null);
                    }
                }
            } else if (clickedSlot == 36) { // Input slot
                ItemStack inputStack = block.getInput();
                if (cursorStack == null) {
                    block.setInput(null);
                    cursorStack = inputStack;
                } else {
                    if (inputStack == null) {
                        block.setInput(cursorStack);
                        cursorStack = null;
                    } else if (inputStack.getType() == cursorStack.getType()) {
                        int remaining = inputStack.addAmount(cursorStack.getCount());
                        if (remaining == 0) {
                            cursorStack = null;
                        } else {
                            cursorStack.setCount(remaining);
                        }
                    } else {
                        block.setInput(cursorStack);
                        cursorStack = inputStack;
                    }
                }
            } else if (clickedSlot == 37) { // Fuel slot
                ItemStack fuelStack = block.getFuel();
                if (cursorStack == null) {
                    block.setFuel(null);
                    cursorStack = fuelStack;
                } else {
                    // Only allow placing fuel
                    if (getBurnDuration(cursorStack.getType()) > 0) {
                        if (fuelStack == null) {
                            block.setFuel(cursorStack);
                            cursorStack = null;
                        } else if (fuelStack.getType() == cursorStack.getType()) {
                            int remaining = fuelStack.addAmount(cursorStack.getCount());
                            if (remaining == 0) {
                                cursorStack = null;
                            } else {
                                cursorStack.setCount(remaining);
                            }
                        } else {
                            block.setFuel(cursorStack);
                            cursorStack = fuelStack;
                        }
                    }
                }
            } else { // Player inventory (0-35)
                ItemStack[] inventory = player.getInventory();
                ItemStack slotStack = inventory[clickedSlot];

                if (cursorStack == null) {
                    cursorStack = slotStack;
                    inventory[clickedSlot] = null;
                } else {
                    if (slotStack == null) {
                        inventory[clickedSlot] = cursorStack;
                        cursorStack = null;
                    } else if (slotStack.getType() == cursorStack.getType()) {
                        int remaining = slotStack.addAmount(cursorStack.getCount());
                        if (remaining == 0) {
                            cursorStack = null;
                        } else {
                            cursorStack.setCount(remaining);
                        }
                    } else {
                        inventory[clickedSlot] = cursorStack;
                        cursorStack = slotStack;
                    }
                }
            }
        } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
            if (clickedSlot == 38) { // Output slot (take only - behaves like left click)
                ItemStack outputStack = block.getOutput();
                if (outputStack != null) {
                    if (cursorStack == null) {
                        cursorStack = outputStack;
                        block.setOutput(null);
                    } else if (cursorStack.getType() == outputStack.getType() &&
                               cursorStack.getCount() + outputStack.getCount() <= ItemStack.MAX_STACK_SIZE) {
                        cursorStack.addAmount(outputStack.getCount());
                        block.setOutput(null);
                    }
                }
            } else if (clickedSlot == 36) { // Input slot
                ItemStack inputStack = block.getInput();
                if (cursorStack == null) {
                    if (inputStack != null) {
                        int take = (inputStack.getCount() + 1) / 2;
                        cursorStack = new ItemStack(inputStack.getType(), take);
                        inputStack.setCount(inputStack.getCount() - take);
                        if (inputStack.getCount() <= 0) {
                            block.setInput(null);
                        }
                    }
                } else {
                    if (inputStack == null) {
                        block.setInput(new ItemStack(cursorStack.getType(), 1));
                        cursorStack.setCount(cursorStack.getCount() - 1);
                        if (cursorStack.getCount() <= 0) {
                            cursorStack = null;
                        }
                    } else if (inputStack.getType() == cursorStack.getType()) {
                        if (inputStack.getCount() < ItemStack.MAX_STACK_SIZE) {
                            inputStack.addAmount(1);
                            cursorStack.setCount(cursorStack.getCount() - 1);
                            if (cursorStack.getCount() <= 0) {
                                cursorStack = null;
                            }
                        }
                    } else {
                        // Swap
                        block.setInput(cursorStack);
                        cursorStack = inputStack;
                    }
                }
            } else if (clickedSlot == 37) { // Fuel slot
                ItemStack fuelStack = block.getFuel();
                if (cursorStack == null) {
                    if (fuelStack != null) {
                        int take = (fuelStack.getCount() + 1) / 2;
                        cursorStack = new ItemStack(fuelStack.getType(), take);
                        fuelStack.setCount(fuelStack.getCount() - take);
                        if (fuelStack.getCount() <= 0) {
                            block.setFuel(null);
                        }
                    }
                } else {
                    // Only allow placing fuel
                    if (getBurnDuration(cursorStack.getType()) > 0) {
                        if (fuelStack == null) {
                            block.setFuel(new ItemStack(cursorStack.getType(), 1));
                            cursorStack.setCount(cursorStack.getCount() - 1);
                            if (cursorStack.getCount() <= 0) {
                                cursorStack = null;
                            }
                        } else if (fuelStack.getType() == cursorStack.getType()) {
                            if (fuelStack.getCount() < ItemStack.MAX_STACK_SIZE) {
                                fuelStack.addAmount(1);
                                cursorStack.setCount(cursorStack.getCount() - 1);
                                if (cursorStack.getCount() <= 0) {
                                    cursorStack = null;
                                }
                            }
                        } else {
                            // Swap
                            block.setFuel(cursorStack);
                            cursorStack = fuelStack;
                        }
                    }
                }
            } else { // Player inventory (0-35)
                ItemStack[] inventory = player.getInventory();
                ItemStack slotStack = inventory[clickedSlot];

                if (cursorStack == null) {
                    if (slotStack != null) {
                        int take = (slotStack.getCount() + 1) / 2;
                        cursorStack = new ItemStack(slotStack.getType(), take);
                        slotStack.setCount(slotStack.getCount() - take);
                        if (slotStack.getCount() <= 0) {
                            inventory[clickedSlot] = null;
                        }
                    }
                } else {
                    if (slotStack == null) {
                        inventory[clickedSlot] = new ItemStack(cursorStack.getType(), 1);
                        cursorStack.setCount(cursorStack.getCount() - 1);
                        if (cursorStack.getCount() <= 0) {
                            cursorStack = null;
                        }
                    } else if (slotStack.getType() == cursorStack.getType()) {
                        if (slotStack.getCount() < ItemStack.MAX_STACK_SIZE) {
                            slotStack.addAmount(1);
                            cursorStack.setCount(cursorStack.getCount() - 1);
                            if (cursorStack.getCount() <= 0) {
                                cursorStack = null;
                            }
                        }
                    } else {
                        // Swap
                        inventory[clickedSlot] = cursorStack;
                        cursorStack = slotStack;
                    }
                }
            }
        }
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

    public void onClose() {
        if (cursorStack != null) {
            boolean fit = player.addItemStack(cursorStack);
            if (!fit) {
                player.getWorld().spawnDroppedItem(player.getX(), player.getY() + 0.5f, player.getZ(), cursorStack.getType());
            }
            cursorStack = null;
        }
    }

    public void render(long windowHandle) {
        if (block == null) return;

        setup2DRendering(windowHandle);

        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        float currentW = w[0];
        float currentH = h[0];

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.0f, 0.0f, 0.0f, 0.4f);
        drawSolidQuad(0, 0, currentW, currentH);

        float guiScale = 3.0f;
        float invWidth = 176.0f * guiScale;
        float invHeight = 166.0f * guiScale;
        float invX = (currentW - invWidth) / 2.0f;
        float invY = (currentH - invHeight) / 2.0f;

        glEnable(GL_TEXTURE_2D);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, guiTextureID);

        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f); glVertex2f(invX, invY);
        glTexCoord2f(176.0f / 256.0f, 0.0f); glVertex2f(invX + invWidth, invY);
        glTexCoord2f(176.0f / 256.0f, 166.0f / 256.0f); glVertex2f(invX + invWidth, invY + invHeight);
        glTexCoord2f(0.0f, 166.0f / 256.0f); glVertex2f(invX, invY + invHeight);
        glEnd();

        // Draw progress indicators from texture
        // 1. Flame progress (burn progress)
        // Texture location of flame: x=176, y=0, w=14, h=14
        if (block.isLit()) {
            float burnRatio = block.getBurnProgress();
            float flameH = 14.0f * burnRatio;
            float flameYOffset = 14.0f - flameH;

            glBegin(GL_QUADS);
            glTexCoord2f(176.0f / 256.0f, flameYOffset / 256.0f); 
            glVertex2f(invX + 56.0f * guiScale, invY + (36.0f + flameYOffset) * guiScale);
            
            glTexCoord2f((176.0f + 14.0f) / 256.0f, flameYOffset / 256.0f); 
            glVertex2f(invX + (56.0f + 14.0f) * guiScale, invY + (36.0f + flameYOffset) * guiScale);
            
            glTexCoord2f((176.0f + 14.0f) / 256.0f, 14.0f / 256.0f); 
            glVertex2f(invX + (56.0f + 14.0f) * guiScale, invY + (36.0f + 14.0f) * guiScale);
            
            glTexCoord2f(176.0f / 256.0f, 14.0f / 256.0f); 
            glVertex2f(invX + 56.0f * guiScale, invY + (36.0f + 14.0f) * guiScale);
            glEnd();
        }

        // 2. Cook progress arrow
        // Texture location of arrow: x=176, y=14, w=24, h=17
        float cookRatio = block.getCookProgress();
        if (cookRatio > 0) {
            float arrowW = 24.0f * cookRatio;
            
            glBegin(GL_QUADS);
            glTexCoord2f(176.0f / 256.0f, 14.0f / 256.0f); 
            glVertex2f(invX + 79.0f * guiScale, invY + 34.0f * guiScale);
            
            glTexCoord2f((176.0f + arrowW) / 256.0f, 14.0f / 256.0f); 
            glVertex2f(invX + (79.0f + arrowW) * guiScale, invY + 34.0f * guiScale);
            
            glTexCoord2f((176.0f + arrowW) / 256.0f, (14.0f + 17.0f) / 256.0f); 
            glVertex2f(invX + (79.0f + arrowW) * guiScale, invY + (34.0f + 17.0f) * guiScale);
            
            glTexCoord2f(176.0f / 256.0f, (14.0f + 17.0f) / 256.0f); 
            glVertex2f(invX + 79.0f * guiScale, invY + (34.0f + 17.0f) * guiScale);
            glEnd();
        }

        glDisable(GL_TEXTURE_2D);

        // Draw title
        glColor4f(0.3f, 0.3f, 0.3f, 1.0f);
        FontRenderer.drawStringRegular("Furnace", invX + 60.0f * guiScale, invY + 10.0f * guiScale);
        FontRenderer.drawStringRegular("Inventory", invX + 8.0f * guiScale, invY + 76.0f * guiScale);

        // Draw inventory slots
        ItemStack[] inventory = player.getInventory();
        float innerSize = 16.0f * guiScale;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory[i];
            if (stack == null) continue;

            float slotX = 0;
            float slotY = 0;

            if (i < 9) {
                slotX = invX + (8.0f + i * 18.0f) * guiScale;
                slotY = invY + 142.0f * guiScale;
            } else {
                int col = (i - 9) % 9;
                int row = (i - 9) / 9;
                slotX = invX + (8.0f + col * 18.0f) * guiScale;
                slotY = invY + (84.0f + row * 18.0f) * guiScale;
            }

            drawItem(stack, slotX + 1.0f * guiScale, slotY + 1.0f * guiScale, innerSize, currentW, currentH);
        }

        // Draw Furnace slots
        if (block.getInput() != null) {
            float slotX = invX + 56.0f * guiScale;
            float slotY = invY + 17.0f * guiScale;
            drawItem(block.getInput(), slotX + 1.0f * guiScale, slotY + 1.0f * guiScale, innerSize, currentW, currentH);
        }

        if (block.getFuel() != null) {
            float slotX = invX + 56.0f * guiScale;
            float slotY = invY + 53.0f * guiScale;
            drawItem(block.getFuel(), slotX + 1.0f * guiScale, slotY + 1.0f * guiScale, innerSize, currentW, currentH);
        }

        if (block.getOutput() != null) {
            float slotX = invX + 116.0f * guiScale;
            float slotY = invY + 35.0f * guiScale;
            drawItem(block.getOutput(), slotX + 1.0f * guiScale, slotY + 1.0f * guiScale, innerSize, currentW, currentH);
        }

        // Draw floating cursor stack
        if (cursorStack != null) {
            float floatSize = 16.0f * guiScale;
            drawItem(cursorStack, (float) mouseX - floatSize / 2.0f, (float) mouseY - floatSize / 2.0f, floatSize, currentW, currentH);
        }

        glDisable(GL_BLEND);
        restore3DRendering();
    }

    private void drawItem(ItemStack stack, float itemX, float itemY, float size, float currentW, float currentH) {
        float centerX = itemX + size / 2.0f;
        float centerY = itemY + size / 2.0f;
        GuiRenderer.draw3DBlock(centerX, centerY, size, stack.getType(), currentW, currentH);

        int count = stack.getCount();
        if (count > 1) {
            String countStr = String.valueOf(count);
            float textWidth = FontRenderer.getStringWidthRegular(countStr);
            float textX = (itemX + size) - textWidth - 2;
            float textY = (itemY + size) - 2;

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
            FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            FontRenderer.drawStringRegular(countStr, textX, textY);
        }
    }

    private int getClickedSlot(double mouseX, double mouseY) {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(glfwGetCurrentContext(), w, h);
        float currentW = w[0];
        float currentH = h[0];

        float guiScale = 3.0f;
        float invWidth = 176.0f * guiScale;
        float invHeight = 166.0f * guiScale;
        float invX = (currentW - invWidth) / 2.0f;
        float invY = (currentH - invHeight) / 2.0f;

        float slotSize = 18.0f * guiScale;

        // Player hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            float slotX = invX + (8.0f + i * 18.0f) * guiScale;
            float slotY = invY + 142.0f * guiScale;
            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                return i;
            }
        }

        // Player inventory (9-35)
        for (int i = 9; i < 36; i++) {
            int col = (i - 9) % 9;
            int row = (i - 9) / 9;
            float slotX = invX + (8.0f + col * 18.0f) * guiScale;
            float slotY = invY + (84.0f + row * 18.0f) * guiScale;
            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                return i;
            }
        }

        // Furnace Input (36)
        float inX = invX + 56.0f * guiScale;
        float inY = invY + 17.0f * guiScale;
        if (mouseX >= inX && mouseX < inX + slotSize && mouseY >= inY && mouseY < inY + slotSize) {
            return 36;
        }

        // Furnace Fuel (37)
        float fuelX = invX + 56.0f * guiScale;
        float fuelY = invY + 53.0f * guiScale;
        if (mouseX >= fuelX && mouseX < fuelX + slotSize && mouseY >= fuelY && mouseY < fuelY + slotSize) {
            return 37;
        }

        // Furnace Output (38)
        float outX = invX + 116.0f * guiScale;
        float outY = invY + 35.0f * guiScale;
        if (mouseX >= outX && mouseX < outX + slotSize && mouseY >= outY && mouseY < outY + slotSize) {
            return 38;
        }

        return -1;
    }

    private void setup2DRendering(long windowHandle) {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(windowHandle, width, height);
        glViewport(0, 0, width[0], height[0]);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width[0], height[0], 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
    }

    private void restore3DRendering() {
        glEnable(GL_LIGHTING);
        glEnable(GL_DEPTH_TEST);
    }

    private void drawSolidQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
    }
}

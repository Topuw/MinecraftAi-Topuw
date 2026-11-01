package com.minecraftai.gui;

import com.minecraftai.core.Player;
import com.minecraftai.core.ItemStack;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.RecipeManager;
import com.minecraftai.blocks.CraftingTable;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.renderer.FontRenderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class CraftingTableMenu {
    private Player player;
    private CraftingTable block;
    private int guiTextureID;
    private ItemStack cursorStack = null;
    private double mouseX, mouseY;

    private ItemStack[] craftingGrid = new ItemStack[9];
    private ItemStack craftingOutput = null;

    public CraftingTableMenu(Player player) {
        this.player = player;
    }

    public void setBlock(CraftingTable block) {
        this.block = block;
    }

    public void init() {
        guiTextureID = TextureLoader.loadTexture("/assets/textures/gui/crafting_table.png");
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
        if (action != GLFW_PRESS) return;
        int clickedSlot = getClickedSlot(mouseX, mouseY);
        if (clickedSlot == -1) return;

        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            if (clickedSlot == 45) { // Output slot
                if (craftingOutput != null) {
                    if (cursorStack == null) {
                        cursorStack = craftingOutput;
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 3, 3);
                    } else if (cursorStack.getType() == craftingOutput.getType() &&
                               cursorStack.getCount() + craftingOutput.getCount() <= ItemStack.MAX_STACK_SIZE) {
                        cursorStack.addAmount(craftingOutput.getCount());
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 3, 3);
                    }
                }
            } else if (clickedSlot >= 36 && clickedSlot < 45) { // Crafting grid
                int gridIdx = clickedSlot - 36;
                ItemStack gridStack = craftingGrid[gridIdx];

                if (cursorStack == null) {
                    craftingGrid[gridIdx] = null;
                    cursorStack = gridStack;
                } else {
                    if (gridStack == null) {
                        craftingGrid[gridIdx] = cursorStack;
                        cursorStack = null;
                    } else if (gridStack.getType() == cursorStack.getType()) {
                        int remaining = gridStack.addAmount(cursorStack.getCount());
                        if (remaining == 0) {
                            cursorStack = null;
                        } else {
                            cursorStack.setCount(remaining);
                        }
                    } else {
                        craftingGrid[gridIdx] = cursorStack;
                        cursorStack = gridStack;
                    }
                }
                craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 3, 3);
            } else { // Player inventory slots 0-35
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
            if (clickedSlot == 45) { // Output slot behaves like left click
                if (craftingOutput != null) {
                    if (cursorStack == null) {
                        cursorStack = craftingOutput;
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 3, 3);
                    } else if (cursorStack.getType() == craftingOutput.getType() &&
                               cursorStack.getCount() + craftingOutput.getCount() <= ItemStack.MAX_STACK_SIZE) {
                        cursorStack.addAmount(craftingOutput.getCount());
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 3, 3);
                    }
                }
            } else if (clickedSlot >= 36 && clickedSlot < 45) { // Crafting grid
                int gridIdx = clickedSlot - 36;
                ItemStack gridStack = craftingGrid[gridIdx];

                if (cursorStack == null) {
                    if (gridStack != null) {
                        int take = (gridStack.getCount() + 1) / 2;
                        cursorStack = new ItemStack(gridStack.getType(), take);
                        gridStack.setCount(gridStack.getCount() - take);
                        if (gridStack.getCount() <= 0) {
                            craftingGrid[gridIdx] = null;
                        }
                    }
                } else {
                    if (gridStack == null) {
                        craftingGrid[gridIdx] = new ItemStack(cursorStack.getType(), 1);
                        cursorStack.setCount(cursorStack.getCount() - 1);
                        if (cursorStack.getCount() <= 0) {
                            cursorStack = null;
                        }
                    } else if (gridStack.getType() == cursorStack.getType()) {
                        if (gridStack.getCount() < ItemStack.MAX_STACK_SIZE) {
                            gridStack.addAmount(1);
                            cursorStack.setCount(cursorStack.getCount() - 1);
                            if (cursorStack.getCount() <= 0) {
                                cursorStack = null;
                            }
                        }
                    } else {
                        // Swap
                        craftingGrid[gridIdx] = cursorStack;
                        cursorStack = gridStack;
                    }
                }
                craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 3, 3);
            } else { // Player inventory slots 0-35
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

    private void consumeIngredients() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftingGrid[i];
            if (stack != null) {
                stack.setCount(stack.getCount() - 1);
                if (stack.getCount() <= 0) {
                    craftingGrid[i] = null;
                }
            }
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
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftingGrid[i];
            if (stack != null) {
                boolean fit = player.addItemStack(stack);
                if (!fit) {
                    player.getWorld().spawnDroppedItem(player.getX(), player.getY() + 0.5f, player.getZ(), stack.getType());
                }
                craftingGrid[i] = null;
            }
        }
        craftingOutput = null;
    }

    public void render(long windowHandle) {
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

        glDisable(GL_TEXTURE_2D);

        // Draw title
        glColor4f(0.3f, 0.3f, 0.3f, 1.0f);
        FontRenderer.drawStringRegular("Crafting", invX + 28.0f * guiScale, invY + 10.0f * guiScale);
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

        // Draw 3x3 crafting grid
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                ItemStack stack = craftingGrid[r * 3 + c];
                if (stack == null) continue;

                float slotX = invX + (30.0f + c * 18.0f) * guiScale;
                float slotY = invY + (17.0f + r * 18.0f) * guiScale;

                drawItem(stack, slotX + 1.0f * guiScale, slotY + 1.0f * guiScale, innerSize, currentW, currentH);
            }
        }

        // Draw crafting output
        if (craftingOutput != null) {
            float slotX = invX + 124.0f * guiScale;
            float slotY = invY + 35.0f * guiScale;
            drawItem(craftingOutput, slotX + 1.0f * guiScale, slotY + 1.0f * guiScale, innerSize, currentW, currentH);
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

        // 3x3 Crafting grid (36-44)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                float slotX = invX + (30.0f + c * 18.0f) * guiScale;
                float slotY = invY + (17.0f + r * 18.0f) * guiScale;
                if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                    return 36 + r * 3 + c;
                }
            }
        }

        // Output slot (45)
        float outX = invX + 124.0f * guiScale;
        float outY = invY + 35.0f * guiScale;
        if (mouseX >= outX && mouseX < outX + slotSize && mouseY >= outY && mouseY < outY + slotSize) {
            return 45;
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

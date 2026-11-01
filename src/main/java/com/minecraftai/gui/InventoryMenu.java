package com.minecraftai.gui;

import com.minecraftai.core.Player;
import com.minecraftai.core.ItemStack;
import com.minecraftai.core.ItemType;
import com.minecraftai.core.RecipeManager;
import com.minecraftai.core.LanguageManager;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.renderer.FontRenderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class InventoryMenu {
    private Player player;
    private int inventoryTextureID;
    private ItemStack cursorStack = null;
    private double mouseX, mouseY;

    private ItemStack[] craftingGrid = new ItemStack[4];
    private ItemStack craftingOutput = null;

    public InventoryMenu(Player player) {
        this.player = player;
    }

    public void init() {
        inventoryTextureID = TextureLoader.loadTexture("/assets/textures/gui/inventory.png");
    }

    public void handleMouseMove(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void handleMouseClick(double mouseX, double mouseY, int button, int action) {
        if (action != GLFW_PRESS) return;
        int clickedSlot = getClickedSlot(mouseX, mouseY);
        if (clickedSlot == -1) return;

        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            if (clickedSlot == 40) { // Crafting output
                if (craftingOutput != null) {
                    if (cursorStack == null) {
                        cursorStack = craftingOutput;
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 2, 2);
                    } else if (cursorStack.getType() == craftingOutput.getType() &&
                               cursorStack.getCount() + craftingOutput.getCount() <= ItemStack.MAX_STACK_SIZE) {
                        cursorStack.addAmount(craftingOutput.getCount());
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 2, 2);
                    }
                }
            } else if (clickedSlot >= 36 && clickedSlot < 40) { // Crafting grid
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
                craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 2, 2);
            } else { // Regular inventory
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
            if (clickedSlot == 40) { // Crafting output behaves like left click
                if (craftingOutput != null) {
                    if (cursorStack == null) {
                        cursorStack = craftingOutput;
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 2, 2);
                    } else if (cursorStack.getType() == craftingOutput.getType() &&
                               cursorStack.getCount() + craftingOutput.getCount() <= ItemStack.MAX_STACK_SIZE) {
                        cursorStack.addAmount(craftingOutput.getCount());
                        craftingOutput = null;
                        consumeIngredients();
                        craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 2, 2);
                    }
                }
            } else if (clickedSlot >= 36 && clickedSlot < 40) { // Crafting grid
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
                craftingOutput = RecipeManager.findMatchingRecipe(craftingGrid, 2, 2);
            } else { // Regular inventory
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
        for (int i = 0; i < 4; i++) {
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
        for (int i = 0; i < 4; i++) {
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
        glBindTexture(GL_TEXTURE_2D, inventoryTextureID);

        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f); glVertex2f(invX, invY);
        glTexCoord2f(176.0f / 256.0f, 0.0f); glVertex2f(invX + invWidth, invY);
        glTexCoord2f(176.0f / 256.0f, 166.0f / 256.0f); glVertex2f(invX + invWidth, invY + invHeight);
        glTexCoord2f(0.0f, 166.0f / 256.0f); glVertex2f(invX, invY + invHeight);
        glEnd();

        glDisable(GL_TEXTURE_2D);

        float previewCenterX = invX + 51.0f * guiScale;
        float previewCenterY = invY + 54.0f * guiScale;
        player.renderPlayerModelGUI(previewCenterX, previewCenterY, 20.0f * guiScale, mouseX, mouseY, currentW, currentH);

        setup2DRendering(windowHandle);

        ItemStack[] inventory = player.getInventory();
        float innerSize = 16.0f * guiScale;

        for (int i = 0; i < inventory.length; i++) {
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

            float itemX = slotX + 1.0f * guiScale;
            float itemY = slotY + 1.0f * guiScale;

            float centerX = itemX + innerSize / 2.0f;
            float centerY = itemY + innerSize / 2.0f;

            GuiRenderer.draw3DBlock(centerX, centerY, innerSize, stack.getType(), currentW, currentH);

            int count = stack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);

                float textX = (itemX + innerSize) - textWidth - 2;
                float textY = (itemY + innerSize) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        // Draw 2x2 crafting grid
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                ItemStack stack = craftingGrid[r * 2 + c];
                if (stack == null) continue;

                float slotX = invX + (98.0f + c * 18.0f) * guiScale;
                float slotY = invY + (18.0f + r * 18.0f) * guiScale;

                float itemX = slotX + 1.0f * guiScale;
                float itemY = slotY + 1.0f * guiScale;
                float centerX = itemX + innerSize / 2.0f;
                float centerY = itemY + innerSize / 2.0f;

                GuiRenderer.draw3DBlock(centerX, centerY, innerSize, stack.getType(), currentW, currentH);

                int count = stack.getCount();
                if (count > 1) {
                    String countStr = String.valueOf(count);
                    float textWidth = FontRenderer.getStringWidthRegular(countStr);
                    float textX = (itemX + innerSize) - textWidth - 2;
                    float textY = (itemY + innerSize) - 2;

                    glEnable(GL_BLEND);
                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                    FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                    FontRenderer.drawStringRegular(countStr, textX, textY);
                }
            }
        }

        // Draw crafting output
        if (craftingOutput != null) {
            float slotX = invX + 154.0f * guiScale;
            float slotY = invY + 28.0f * guiScale;

            float itemX = slotX + 1.0f * guiScale;
            float itemY = slotY + 1.0f * guiScale;
            float centerX = itemX + innerSize / 2.0f;
            float centerY = itemY + innerSize / 2.0f;

            GuiRenderer.draw3DBlock(centerX, centerY, innerSize, craftingOutput.getType(), currentW, currentH);

            int count = craftingOutput.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);
                float textX = (itemX + innerSize) - textWidth - 2;
                float textY = (itemY + innerSize) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        // Tooltip rendering
        int hoveredSlot = getClickedSlot(mouseX, mouseY);
        if (hoveredSlot != -1 && cursorStack == null) {
            ItemStack hoveredStack = null;
            if (hoveredSlot < 36) {
                hoveredStack = inventory[hoveredSlot];
            } else if (hoveredSlot >= 36 && hoveredSlot < 40) {
                hoveredStack = craftingGrid[hoveredSlot - 36];
            } else if (hoveredSlot == 40) {
                hoveredStack = craftingOutput;
            }

            if (hoveredStack != null) {
                renderTooltip(hoveredStack, (float) mouseX, (float) mouseY);
            }
        }

        if (cursorStack != null) {
            float floatSize = 16.0f * guiScale;
            float centerX = (float) mouseX;
            float centerY = (float) mouseY;

            GuiRenderer.draw3DBlock(centerX, centerY, floatSize, cursorStack.getType(), currentW, currentH);

            int count = cursorStack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);

                float textX = centerX + (floatSize / 2.0f) - textWidth - 2;
                float textY = centerY + (floatSize / 2.0f) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        glDisable(GL_BLEND);
        restore3DRendering();
    }

    private void renderTooltip(ItemStack stack, float x, float y) {
        String name = LanguageManager.getItemName(stack.getType());
        String durabilityInfo = "";
        if (stack.getDurability() != -1) {
            durabilityInfo = "Durability: " + stack.getDurability() + "/" + ItemStack.getMaxDurability(stack.getType());
        }

        float textWidth = FontRenderer.getStringWidth(name);
        if (!durabilityInfo.isEmpty()) {
            textWidth = Math.max(textWidth, FontRenderer.getStringWidth(durabilityInfo));
        }
        float textHeight = 12;

        float rectX = x + 12;
        float rectY = y - 12;
        float rectW = textWidth + 8;
        float rectH = durabilityInfo.isEmpty() ? (textHeight + 6) : (textHeight * 2 + 10);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Border and background
        glColor4f(0.1f, 0.0f, 0.1f, 0.9f);
        drawSolidQuad(rectX - 1, rectY - 1, rectW + 2, rectH + 2);
        glColor4f(0.1f, 0.1f, 0.1f, 0.95f);
        drawSolidQuad(rectX, rectY, rectW, rectH);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        FontRenderer.drawString(name, rectX + 4, rectY + textHeight);
        
        if (!durabilityInfo.isEmpty()) {
            glColor4f(0.7f, 0.7f, 0.7f, 1.0f);
            FontRenderer.drawString(durabilityInfo, rectX + 4, rectY + textHeight * 2 + 4);
        }

        glEnable(GL_DEPTH_TEST);
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

        for (int i = 0; i < 9; i++) {
            float slotX = invX + (8.0f + i * 18.0f) * guiScale;
            float slotY = invY + 142.0f * guiScale;

            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                return i;
            }
        }

        for (int i = 9; i < 36; i++) {
            int col = (i - 9) % 9;
            int row = (i - 9) / 9;
            float slotX = invX + (8.0f + col * 18.0f) * guiScale;
            float slotY = invY + (84.0f + row * 18.0f) * guiScale;

            if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                return i;
            }
        }

        // 2x2 Crafting Grid (36-39)
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                float slotX = invX + (98.0f + c * 18.0f) * guiScale;
                float slotY = invY + (18.0f + r * 18.0f) * guiScale;

                if (mouseX >= slotX && mouseX < slotX + slotSize && mouseY >= slotY && mouseY < slotY + slotSize) {
                    return 36 + r * 2 + c;
                }
            }
        }

        // Crafting Output (40)
        float outX = invX + 154.0f * guiScale;
        float outY = invY + 28.0f * guiScale;
        if (mouseX >= outX && mouseX < outX + slotSize && mouseY >= outY && mouseY < outY + slotSize) {
            return 40;
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
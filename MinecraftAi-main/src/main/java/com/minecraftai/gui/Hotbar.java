package com.minecraftai.gui;

import com.minecraftai.core.Player;
import com.minecraftai.core.ItemStack;
import com.minecraftai.core.ItemType;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.renderer.FontRenderer;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.*;

public class Hotbar {
    private int hotbarTextureID;
    private int selectionTextureID;
    private int containerTextureID;
    private int fullTextureID;
    private int halfTextureID;
    private Player player;

    private static final float ASPECT_RATIO = 182.0f / 22.0f;
    private static final float HOTBAR_WIDTH_RATIO = 0.5f;
    private static final float BOTTOM_MARGIN_PX = 0.0f;

    public Hotbar(Player player) {
        this.player = player;
    }

    public void init() {
        hotbarTextureID = TextureLoader.loadTexture("/assets/textures/gui/hotbar.png");
        selectionTextureID = TextureLoader.loadTexture("/assets/textures/gui/hotbar_selection.png");
        containerTextureID = TextureLoader.loadTexture("/assets/textures/gui/heart/container.png");
        fullTextureID = TextureLoader.loadTexture("/assets/textures/gui/heart/full.png");
        halfTextureID = TextureLoader.loadTexture("/assets/textures/gui/heart/half.png");
    }

    public void render(long windowHandle) {
        setup2DRendering(windowHandle);

        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(windowHandle, w, h);
        float currentW = w[0];
        float currentH = h[0];

        float barWidth = currentW * HOTBAR_WIDTH_RATIO;
        float barHeight = barWidth / ASPECT_RATIO;
        float barX = (currentW - barWidth) / 2;
        float barY = currentH - barHeight - BOTTOM_MARGIN_PX;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glBindTexture(GL_TEXTURE_2D, hotbarTextureID);
        drawTexturedQuad(barX, barY, barWidth, barHeight);

        glDisable(GL_TEXTURE_2D);

        ItemStack[] inventory = player.getInventory();
        float scale = barWidth / 182.0f;
        float slotWidth = 20.0f * scale;
        float itemSize = 16.0f * scale;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory[i];
            if (stack == null) {
                continue;
            }

            ItemType type = stack.getType();

            float slotX = barX + (1.0f + i * 20.0f) * scale;
            float slotY = barY + 1.0f * scale;

            float itemX = slotX + 2.0f * scale;
            float itemY = slotY + 2.0f * scale;

            float centerX = itemX + itemSize / 2.0f;
            float centerY = itemY + itemSize / 2.0f;
            GuiRenderer.draw3DBlock(centerX, centerY, itemSize, type, currentW, currentH);

            int count = stack.getCount();
            if (count > 1) {
                String countStr = String.valueOf(count);
                float textWidth = FontRenderer.getStringWidthRegular(countStr);

                float textX = (itemX + itemSize) - textWidth - 2;
                float textY = (itemY + itemSize) - 2;

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(0.1f, 0.1f, 0.1f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX + 1, textY + 1);

                glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                FontRenderer.drawStringRegular(countStr, textX, textY);
            }
        }

        int health = player.getHealth();
        float heartSize = 9.0f * scale;
        float heartY = barY - 12.0f * scale;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (int i = 0; i < 10; i++) {
            float heartX = barX + (i * 8.0f) * scale;

            glBindTexture(GL_TEXTURE_2D, containerTextureID);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            drawTexturedQuad(heartX, heartY, heartSize, heartSize);

            if (health >= (i * 2 + 2)) {
                glBindTexture(GL_TEXTURE_2D, fullTextureID);
                drawTexturedQuad(heartX, heartY, heartSize, heartSize);
            } else if (health == (i * 2 + 1)) {
                glBindTexture(GL_TEXTURE_2D, halfTextureID);
                drawTexturedQuad(heartX, heartY, heartSize, heartSize);
            }
        }
        glDisable(GL_TEXTURE_2D);

        int selectedSlot = player.getSelectedSlot();
        float selectedSlotX = barX + (1.0f + selectedSlot * 20.0f) * scale;

        float selX = selectedSlotX - 2.0f * scale;
        float selY = barY - 1.0f * scale;
        float selWidth = 24.0f * scale;
        float selHeight = 23.0f * scale;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, selectionTextureID);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedQuad(selX, selY, selWidth, selHeight);
        glDisable(GL_TEXTURE_2D);

        glDisable(GL_BLEND);
        restore3DRendering();
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

    private void drawTexturedQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(x, y);
        glTexCoord2f(1, 0); glVertex2f(x + w, y);
        glTexCoord2f(1, 1); glVertex2f(x + w, y + h);
        glTexCoord2f(0, 1); glVertex2f(x, y + h);
        glEnd();
    }
}
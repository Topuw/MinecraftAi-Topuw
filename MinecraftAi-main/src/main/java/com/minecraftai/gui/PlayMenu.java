package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.FontRenderer;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.core.WorldSaveManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class PlayMenu {
    private static final String BACKGROUND_PATH = "/assets/textures/misc/background.png";
    private static final String BUTTON_PATH = "/assets/textures/gui/button.png";

    private Game game;
    private int backgroundTextureID;
    private int buttonTextureID;

    private final float[][] worldButtonRects = new float[5][4];
    private final float[] doneButtonRect = new float[4];
    private final float[] deleteButtonRect = new float[4];

    private final boolean[] worldSlotExists = new boolean[5];
    private final String[] worldSlotTexts = new String[5];
    private final int[] hoveredSlots = new int[5];
    
    private boolean isDoneHovered = false;
    private boolean isDeleteHovered = false;
    private boolean isDeleteMode = false;

    public PlayMenu(Game game) {
        this.game = game;
        this.backgroundTextureID = TextureLoader.loadTexture(BACKGROUND_PATH);
        this.buttonTextureID = TextureLoader.loadTexture(BUTTON_PATH);
        refreshSlots();
    }

    public void refreshSlots() {
        for (int i = 0; i < 5; i++) {
            String worldName = "World" + (i + 1);
            if (WorldSaveManager.worldExists(worldName)) {
                worldSlotExists[i] = true;
                double sizeMB = WorldSaveManager.getWorldSizeMB(worldName);
                worldSlotTexts[i] = String.format("%s [%.1f MB]", worldName, sizeMB);
            } else {
                worldSlotExists[i] = false;
                worldSlotTexts[i] = "[empty]";
            }
        }
    }

    public void handleMouseMove(double x, double y) {
        isDoneHovered = isMouseOver(x, y, doneButtonRect);
        isDeleteHovered = isMouseOver(x, y, deleteButtonRect);
        for (int i = 0; i < 5; i++) {
            if (isMouseOver(x, y, worldButtonRects[i])) {
                hoveredSlots[i] = 1;
            } else {
                hoveredSlots[i] = 0;
            }
        }
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (isDoneHovered) {
                isDeleteMode = false;
                goBack();
            } else if (isDeleteHovered) {
                isDeleteMode = !isDeleteMode;
            } else {
                for (int i = 0; i < 5; i++) {
                    if (hoveredSlots[i] == 1) {
                        String worldName = "World" + (i + 1);
                        if (isDeleteMode) {
                            if (worldSlotExists[i]) {
                                WorldSaveManager.deleteWorld(worldName);
                                refreshSlots();
                            }
                            isDeleteMode = false;
                        } else {
                            game.startNewOrLoadWorld(worldName);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void goBack() {
        game.setGameState(Game.GameState.MAIN_MENU);
    }

    public void render() {
        setup2DRendering();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), w, h);
        float currentW = w[0];
        float currentH = h[0];

        drawTexturedBackground(backgroundTextureID, currentW, currentH);

        String title = isDeleteMode ? "Select World to Delete" : "Select World";
        float titleX = (currentW - FontRenderer.getStringWidth(title)) / 2;
        float titleY = currentH * 0.08f;

        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        FontRenderer.drawString(title, titleX + 1.0f, titleY + 1.0f);
        if (isDeleteMode) {
            glColor4f(1.0f, 0.3f, 0.3f, 1.0f);
        } else {
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
        FontRenderer.drawString(title, titleX, titleY);

        float buttonWidth = 350;
        float buttonHeight = 40;
        float buttonX = (currentW - buttonWidth) / 2;
        float startY = currentH * 0.18f;
        float gap = 12;

        for (int i = 0; i < 5; i++) {
            float y = startY + i * (buttonHeight + gap);
            worldButtonRects[i][0] = buttonX;
            worldButtonRects[i][1] = y;
            worldButtonRects[i][2] = buttonWidth;
            worldButtonRects[i][3] = buttonHeight;

            boolean isSlotHovered = (hoveredSlots[i] == 1);
            GuiRenderer.drawButton(buttonX, y, buttonWidth, buttonHeight, worldSlotTexts[i], isSlotHovered, buttonTextureID);
        }

        float deleteButtonWidth = 200;
        float deleteButtonHeight = 40;
        float deleteX = (currentW - deleteButtonWidth) / 2;
        float deleteY = startY + 5 * (buttonHeight + gap) + 15;

        deleteButtonRect[0] = deleteX;
        deleteButtonRect[1] = deleteY;
        deleteButtonRect[2] = deleteButtonWidth;
        deleteButtonRect[3] = deleteButtonHeight;

        String deleteText = isDeleteMode ? "Delete Mode: ON" : "Delete World";
        GuiRenderer.drawButton(deleteX, deleteY, deleteButtonWidth, deleteButtonHeight, deleteText, isDeleteHovered || isDeleteMode, buttonTextureID);

        float doneButtonWidth = 200;
        float doneButtonHeight = 40;
        float doneX = (currentW - doneButtonWidth) / 2;
        float doneY = deleteY + deleteButtonHeight + 10;

        doneButtonRect[0] = doneX;
        doneButtonRect[1] = doneY;
        doneButtonRect[2] = doneButtonWidth;
        doneButtonRect[3] = doneButtonHeight;

        GuiRenderer.drawButton(doneX, doneY, doneButtonWidth, doneButtonHeight, "Done", isDoneHovered, buttonTextureID);

        restore3DRendering();
    }

    private void drawTexturedBackground(int textureID, float w, float h) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f); glVertex2f(0, 0);
        glTexCoord2f(1.0f, 0.0f); glVertex2f(w, 0);
        glTexCoord2f(1.0f, 1.0f); glVertex2f(w, h);
        glTexCoord2f(0.0f, 1.0f); glVertex2f(0, h);
        glEnd();
        
        glDisable(GL_TEXTURE_2D);
    }

    private void setup2DRendering() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), width, height);
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

    private boolean isMouseOver(double mouseX, double mouseY, float[] rect) {
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] &&
                mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}
package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.TextureLoader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class PauseMenu {
    private static final String BUTTON_PATH = "/assets/textures/gui/button.png";

    private Game game;
    private int buttonTextureID;

    private final float[] backToGameRect = new float[4];
    private final float[] optionsRect = new float[4];
    private final float[] saveAndQuitRect = new float[4];

    private boolean isBackHovered = false;
    private boolean isOptionsHovered = false;
    private boolean isQuitHovered = false;

    public PauseMenu(Game game) {
        this.game = game;
        this.buttonTextureID = TextureLoader.loadTexture(BUTTON_PATH);
    }

    public void handleMouseMove(double x, double y) {
        isBackHovered = isMouseOver(x, y, backToGameRect);
        isOptionsHovered = isMouseOver(x, y, optionsRect);
        isQuitHovered = isMouseOver(x, y, saveAndQuitRect);
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (isBackHovered) {
                game.resumeGame();
            } else if (isOptionsHovered) {
                game.enterOptionsMenu(Game.GameState.PAUSE_MENU);
            } else if (isQuitHovered) {
                game.saveAndQuit();
            }
        }
    }

    public void render() {
        setup2DRendering();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), w, h);
        float currentW = w[0];
        float currentH = h[0];

        drawDarkenedBackground(currentW, currentH);

        float buttonWidth = 300;
        float buttonHeight = 40;
        float buttonX = (currentW - buttonWidth) / 2;
        float backY = currentH * 0.35f;
        float optionsY = backY + buttonHeight + 15;
        float quitY = optionsY + buttonHeight + 15;

        backToGameRect[0] = buttonX; backToGameRect[1] = backY; backToGameRect[2] = buttonWidth; backToGameRect[3] = buttonHeight;
        optionsRect[0] = buttonX; optionsRect[1] = optionsY; optionsRect[2] = buttonWidth; optionsRect[3] = buttonHeight;
        saveAndQuitRect[0] = buttonX; saveAndQuitRect[1] = quitY; saveAndQuitRect[2] = buttonWidth; saveAndQuitRect[3] = buttonHeight;

        GuiRenderer.drawButton(buttonX, backY, buttonWidth, buttonHeight, "Back to game", isBackHovered, buttonTextureID);
        GuiRenderer.drawButton(buttonX, optionsY, buttonWidth, buttonHeight, "Options", isOptionsHovered, buttonTextureID);
        GuiRenderer.drawButton(buttonX, quitY, buttonWidth, buttonHeight, "Save and quit", isQuitHovered, buttonTextureID);

        restore3DRendering();
    }

    private void drawDarkenedBackground(float w, float h) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.0f, 0.0f, 0.0f, 0.6f);
        
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(w, 0);
        glVertex2f(w, h);
        glVertex2f(0, h);
        glEnd();
        
        glDisable(GL_BLEND);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
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
package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.TextureLoader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class MainMenu {
    private static final String BACKGROUND_PATH = "/assets/textures/misc/background.png";
    private static final String LOGO_PATH = "/assets/textures/misc/logo.png";
    private static final String BUTTON_PATH = "/assets/textures/gui/button.png";

    private Game game;
    private int backgroundTextureID;
    private int logoTextureID;
    private int buttonTextureID;

    private final float[] playButtonRect = new float[4];
    private final float[] optionsButtonRect = new float[4];
    private final float[] quitButtonRect = new float[4];

    private boolean isPlayHovered = false;
    private boolean isOptionsHovered = false;
    private boolean isQuitHovered = false;

    public MainMenu(Game game) {
        this.game = game;
        this.backgroundTextureID = TextureLoader.loadTexture(BACKGROUND_PATH);
        this.logoTextureID = TextureLoader.loadTexture(LOGO_PATH);
        this.buttonTextureID = TextureLoader.loadTexture(BUTTON_PATH);
    }

    public void handleMouseMove(double x, double y) {
        isPlayHovered = isMouseOver(x, y, playButtonRect);
        isOptionsHovered = isMouseOver(x, y, optionsButtonRect);
        isQuitHovered = isMouseOver(x, y, quitButtonRect);
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (isPlayHovered) {
                game.enterPlayMenu();
            } else if (isOptionsHovered) {
                game.enterOptionsMenu(Game.GameState.MAIN_MENU);
            } else if (isQuitHovered) {
                game.quitGame();
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

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, backgroundTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedQuad(0, 0, currentW, currentH);
        glDisable(GL_TEXTURE_2D);

        float logoWidth = 512;
        float logoHeight = 128;
        float logoX = (currentW - logoWidth) / 2;
        float logoY = currentH * 0.15f;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, logoTextureID);
        drawTexturedQuad(logoX, logoY, logoWidth, logoHeight);
        glDisable(GL_TEXTURE_2D);

        float buttonWidth = 300;
        float buttonHeight = 40;
        float buttonX = (currentW - buttonWidth) / 2;
        float playY = currentH * 0.45f;
        float optionsY = playY + buttonHeight + 15;
        float quitY = optionsY + buttonHeight + 15;

        playButtonRect[0] = buttonX; playButtonRect[1] = playY; playButtonRect[2] = buttonWidth; playButtonRect[3] = buttonHeight;
        optionsButtonRect[0] = buttonX; optionsButtonRect[1] = optionsY; optionsButtonRect[2] = buttonWidth; optionsButtonRect[3] = buttonHeight;
        quitButtonRect[0] = buttonX; quitButtonRect[1] = quitY; quitButtonRect[2] = buttonWidth; quitButtonRect[3] = buttonHeight;

        GuiRenderer.drawButton(buttonX, playY, buttonWidth, buttonHeight, "PLAY", isPlayHovered, buttonTextureID);
        GuiRenderer.drawButton(buttonX, optionsY, buttonWidth, buttonHeight, "OPTIONS", isOptionsHovered, buttonTextureID);
        GuiRenderer.drawButton(buttonX, quitY, buttonWidth, buttonHeight, "QUIT", isQuitHovered, buttonTextureID);

        glDisable(GL_BLEND);
        restore3DRendering();
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

    private void drawTexturedQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(x, y);
        glTexCoord2f(1, 0); glVertex2f(x + w, y);
        glTexCoord2f(1, 1); glVertex2f(x + w, y + h);
        glTexCoord2f(0, 1); glVertex2f(x, y + h);
        glEnd();
    }

    private boolean isMouseOver(double mouseX, double mouseY, float[] rect) {
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] &&
                mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}
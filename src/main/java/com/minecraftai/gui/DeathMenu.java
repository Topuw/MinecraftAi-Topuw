package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.core.Player;
import com.minecraftai.renderer.FontRenderer;
import com.minecraftai.renderer.TextureLoader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class DeathMenu {
    private Game game;
    private Player player;
    private int buttonTextureID;

    private final float[] respawnButtonRect = new float[4];
    private final float[] quitButtonRect = new float[4];

    private boolean isRespawnHovered = false;
    private boolean isQuitHovered = false;

    public DeathMenu(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public void init() {
        buttonTextureID = TextureLoader.loadTexture("/assets/textures/gui/button.png");
    }

    public void handleMouseMove(double x, double y) {
        isRespawnHovered = isMouseOver(x, y, respawnButtonRect);
        isQuitHovered = isMouseOver(x, y, quitButtonRect);
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (isRespawnHovered) {
                player.respawn();
                game.resumeGame();
            } else if (isQuitHovered) {
                game.quitGame();
            }
        }
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
        glColor4f(0.4f, 0.0f, 0.0f, 0.5f);
        drawSolidQuad(0, 0, currentW, currentH);

        String title = "YOU DIED";
        float titleScale = 2.0f;
        float titleWidth = FontRenderer.getStringWidth(title) * titleScale;
        float titleX = (currentW - titleWidth) / 2.0f;
        float titleY = currentH * 0.3f;

        glColor4f(0.1f, 0.0f, 0.0f, 1.0f);
        FontRenderer.drawString(title, titleX + 4, titleY + 4, titleScale);
        glColor4f(1.0f, 0.2f, 0.2f, 1.0f);
        FontRenderer.drawString(title, titleX, titleY, titleScale);

        float buttonWidth = 300;
        float buttonHeight = 60;
        float buttonX = (currentW - buttonWidth) / 2.0f;
        float respawnY = currentH * 0.50f;
        float quitY = respawnY + buttonHeight + 20;

        respawnButtonRect[0] = buttonX; respawnButtonRect[1] = respawnY;
        respawnButtonRect[2] = buttonWidth; respawnButtonRect[3] = buttonHeight;

        quitButtonRect[0] = buttonX; quitButtonRect[1] = quitY;
        quitButtonRect[2] = buttonWidth; quitButtonRect[3] = buttonHeight;

        GuiRenderer.drawButton(buttonX, respawnY, buttonWidth, buttonHeight, "RESPAWN", isRespawnHovered, buttonTextureID);
        GuiRenderer.drawButton(buttonX, quitY, buttonWidth, buttonHeight, "QUIT", isQuitHovered, buttonTextureID);

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

    private void drawSolidQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
    }

    private boolean isMouseOver(double mouseX, double mouseY, float[] rect) {
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] &&
                mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}
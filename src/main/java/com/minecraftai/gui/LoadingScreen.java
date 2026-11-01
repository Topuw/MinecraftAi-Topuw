package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.FontRenderer;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.core.LanguageManager;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.*;

public class LoadingScreen {
    private final Game game;
    private int backgroundTextureID;
    private volatile float progress = 0;
    private volatile String status = "";

    public LoadingScreen(Game game) {
        this.game = game;
        // Tekstura ładowana w głównym wątku (Game.init)
        this.backgroundTextureID = TextureLoader.loadTexture("/assets/textures/misc/background.png");
    }

    public void setProgress(float progress, String status) {
        this.progress = progress;
        this.status = (status != null) ? status : "";
    }

    public void render() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), w, h);
        float width = w[0];
        float height = h[0];

        glViewport(0, 0, (int)width, (int)height);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);

        // Tło
        if (backgroundTextureID != 0) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, backgroundTextureID);
            glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex2f(0, 0);
            glTexCoord2f(width / 32f, 0); glVertex2f(width, 0);
            glTexCoord2f(width / 32f, height / 32f); glVertex2f(width, height);
            glTexCoord2f(0, height / 32f); glVertex2f(0, height);
            glEnd();
            glDisable(GL_TEXTURE_2D);
        } else {
            glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
        }

        // Pasek postępu
        float barW = 400;
        float barH = 20;
        float barX = (width - barW) / 2;
        float barY = (height - barH) / 2 + 40;

        // Tło paska
        glColor4f(0.2f, 0.2f, 0.2f, 1.0f);
        glBegin(GL_QUADS);
        glVertex2f(barX, barY);
        glVertex2f(barX + barW, barY);
        glVertex2f(barX + barW, barY + barH);
        glVertex2f(barX, barY + barH);
        glEnd();

        // Wypełnienie paska
        glColor4f(0.5f, 1.0f, 0.5f, 1.0f);
        glBegin(GL_QUADS);
        glVertex2f(barX, barY);
        glVertex2f(barX + barW * progress, barY);
        glVertex2f(barX + barW * progress, barY + barH);
        glVertex2f(barX, barY + barH);
        glEnd();

        // Ramka
        glColor4f(0, 0, 0, 1);
        glLineWidth(2);
        glBegin(GL_LINE_LOOP);
        glVertex2f(barX, barY);
        glVertex2f(barX + barW, barY);
        glVertex2f(barX + barW, barY + barH);
        glVertex2f(barX, barY + barH);
        glEnd();

        // Teksty
        String loadingText = LanguageManager.get("gui.loading") + "... " + (int)(progress * 100) + "%";
        float textX = (width - FontRenderer.getStringWidth(loadingText)) / 2;
        FontRenderer.drawString(loadingText, textX, barY - 20);

        String currentStatus = this.status;
        float statusX = (width - FontRenderer.getStringWidth(currentStatus)) / 2;
        FontRenderer.drawString(currentStatus, statusX, barY + barH + 20);

        glEnable(GL_DEPTH_TEST);
    }
}

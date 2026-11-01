package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.FontRenderer;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.core.LanguageManager;
import com.minecraftai.core.WorldSaveManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class KeybindsMenu {
    private Game game;
    private OptionsMenu optionsMenu;
    private int backgroundTextureID;
    private int buttonTextureID;

    private final float[][] keyButtonsRects = new float[7][4];
    private final float[] backButtonRect = new float[4];
    private int listeningKeyIndex = -1;
    private boolean isBackHovered = false;
    private int hoveredButton = -1;

    private static final String[] KEY_NAMES = {
        "key.forward", "key.back", "key.left", "key.right", "key.jump", "key.inventory", "key.drop"
    };

    public KeybindsMenu(Game game, OptionsMenu optionsMenu) {
        this.game = game;
        this.optionsMenu = optionsMenu;
        this.backgroundTextureID = TextureLoader.loadTexture("/assets/textures/misc/background.png");
        this.buttonTextureID = TextureLoader.loadTexture("/assets/textures/gui/button.png");
    }

    public void handleMouseMove(double x, double y) {
        isBackHovered = isMouseOver(x, y, backButtonRect);
        hoveredButton = -1;
        for (int i = 0; i < keyButtonsRects.length; i++) {
            if (isMouseOver(x, y, keyButtonsRects[i])) {
                hoveredButton = i;
                break;
            }
        }
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            if (isBackHovered) {
                game.setGameState(Game.GameState.OPTIONS_MENU);
            } else if (hoveredButton != -1) {
                listeningKeyIndex = hoveredButton;
            } else {
                listeningKeyIndex = -1;
            }
        }
    }

    public void handleKey(int key, int action) {
        if (listeningKeyIndex != -1 && action == GLFW_PRESS) {
            if (key != GLFW_KEY_ESCAPE) {
                updateKey(listeningKeyIndex, key);
            }
            listeningKeyIndex = -1;
            WorldSaveManager.saveOptions();
        }
    }

    private void updateKey(int index, int key) {
        switch (index) {
            case 0: Game.KEY_FORWARD = key; break;
            case 1: Game.KEY_BACK = key; break;
            case 2: Game.KEY_LEFT = key; break;
            case 3: Game.KEY_RIGHT = key; break;
            case 4: Game.KEY_JUMP = key; break;
            case 5: Game.KEY_INVENTORY = key; break;
            case 6: Game.KEY_DROP = key; break;
        }
    }

    private String getKeyName(int index) {
        int code = 0;
        switch (index) {
            case 0: code = Game.KEY_FORWARD; break;
            case 1: code = Game.KEY_BACK; break;
            case 2: code = Game.KEY_LEFT; break;
            case 3: code = Game.KEY_RIGHT; break;
            case 4: code = Game.KEY_JUMP; break;
            case 5: code = Game.KEY_INVENTORY; break;
            case 6: code = Game.KEY_DROP; break;
        }
        String name = glfwGetKeyName(code, 0);
        if (name == null) {
            if (code == GLFW_KEY_SPACE) return "SPACE";
            if (code == GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
            if (code == GLFW_KEY_LEFT_CONTROL) return "LCTRL";
            return "KEY " + code;
        }
        return name.toUpperCase();
    }

    public void render() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), w, h);
        float currentW = w[0];
        float currentH = h[0];

        glViewport(0, 0, (int)currentW, (int)currentH);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);

        // Background
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, backgroundTextureID);
        glColor4f(0.25f, 0.25f, 0.25f, 1.0f);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(0, 0);
        glTexCoord2f(currentW / 32f, 0); glVertex2f(currentW, 0);
        glTexCoord2f(currentW / 32f, currentH / 32f); glVertex2f(currentW, currentH);
        glTexCoord2f(0, currentH / 32f); glVertex2f(0, currentH);
        glEnd();

        String title = LanguageManager.get("options.keybinds");
        FontRenderer.drawString(title, (currentW - FontRenderer.getStringWidth(title)) / 2, 30);

        float startY = 80;
        float spacing = 35;
        float btnW = 150;
        float btnH = 20;
        float leftX = currentW / 2 - 160;
        float rightX = currentW / 2 + 10;

        for (int i = 0; i < KEY_NAMES.length; i++) {
            float y = startY + i * spacing;
            String label = LanguageManager.get(KEY_NAMES[i]);
            FontRenderer.drawString(label, leftX, y + 15);

            keyButtonsRects[i][0] = rightX;
            keyButtonsRects[i][1] = y;
            keyButtonsRects[i][2] = btnW;
            keyButtonsRects[i][3] = btnH;

            String keyDisp = listeningKeyIndex == i ? "> ??? <" : getKeyName(i);
            GuiRenderer.drawButton(rightX, y, btnW, btnH, keyDisp, hoveredButton == i, buttonTextureID);
        }

        backButtonRect[0] = (currentW - 200) / 2;
        backButtonRect[1] = currentH - 50;
        backButtonRect[2] = 200;
        backButtonRect[3] = 30;
        GuiRenderer.drawButton(backButtonRect[0], backButtonRect[1], 200, 30, LanguageManager.get("gui.done"), isBackHovered, buttonTextureID);

        glEnable(GL_DEPTH_TEST);
    }

    private boolean isMouseOver(double mouseX, double mouseY, float[] rect) {
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] &&
                mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}

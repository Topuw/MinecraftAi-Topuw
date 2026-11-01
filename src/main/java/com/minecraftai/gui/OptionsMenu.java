package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.FontRenderer;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.core.World;
import com.minecraftai.core.WorldSaveManager;
import com.minecraftai.core.LanguageManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class OptionsMenu {
    private static final String BACKGROUND_PATH = "/assets/textures/misc/background.png";
    private static final String BUTTON_PATH = "/assets/textures/gui/button.png";

    private final Game game;
    private final int backgroundTextureID;
    private final int buttonTextureID;

    private final float[] rdSliderRect = new float[4];
    private final float[] hbSliderRect = new float[4];
    private final float[] chSliderRect = new float[4];
    private final float[] doneButtonRect = new float[4];
    private final float[] commandsButtonRect = new float[4];
    private final float[] langButtonRect = new float[4];
    private final float[] keybindsButtonRect = new float[4];
    private final float[] swapMouseButtonsRect = new float[4];

    private int activeSlider = -1;
    private boolean isDoneHovered = false;
    private boolean isCommandsHovered = false;
    private boolean isLangHovered = false;
    private boolean isKeybindsHovered = false;
    private boolean isSwapMouseButtonsHovered = false;
    private Game.GameState previousState = Game.GameState.MAIN_MENU;

    public OptionsMenu(Game game) {
        this.game = game;
        this.backgroundTextureID = TextureLoader.loadTexture(BACKGROUND_PATH);
        this.buttonTextureID = TextureLoader.loadTexture(BUTTON_PATH);
    }

    public void setPreviousState(Game.GameState state) {
        this.previousState = state;
    }

    public Game.GameState getPreviousState() {
        return previousState;
    }

    public void handleMouseMove(double x, double y) {
        isDoneHovered = isMouseOver(x, y, doneButtonRect);
        isCommandsHovered = isMouseOver(x, y, commandsButtonRect);
        isLangHovered = isMouseOver(x, y, langButtonRect);
        isKeybindsHovered = isMouseOver(x, y, keybindsButtonRect);
        isSwapMouseButtonsHovered = isMouseOver(x, y, swapMouseButtonsRect);

        if (activeSlider != -1) {
            updateSliderValue(x);
        }
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button != GLFW_MOUSE_BUTTON_LEFT) return;

        if (action == GLFW_PRESS) {
            if (isDoneHovered) {
                goBack();
            } else if (isCommandsHovered) {
                Game.COMMANDS_ENABLED = !Game.COMMANDS_ENABLED;
                WorldSaveManager.saveOptions();
            } else if (isLangHovered) {
                String current = LanguageManager.getCurrentLang();
                LanguageManager.setCurrentLang(current.equals("pl_pl") ? "en_us" : "pl_pl");
                WorldSaveManager.saveOptions();
            } else if (isKeybindsHovered) {
                game.setGameState(Game.GameState.KEYBINDS_MENU);
            } else if (isSwapMouseButtonsHovered) {
                Game.SWAP_MOUSE_BUTTONS = !Game.SWAP_MOUSE_BUTTONS;
                WorldSaveManager.saveOptions();
            } else if (isMouseOver(x, y, rdSliderRect)) {
                activeSlider = 0;
                updateSliderValue(x);
            } else if (isMouseOver(x, y, hbSliderRect)) {
                activeSlider = 1;
                updateSliderValue(x);
            } else if (isMouseOver(x, y, chSliderRect)) {
                activeSlider = 2;
                updateSliderValue(x);
            }
        } else if (action == GLFW_RELEASE && activeSlider != -1) {
            activeSlider = -1;
            WorldSaveManager.saveOptions();
        }
    }

    public void goBack() {
        WorldSaveManager.saveOptions();
        game.setGameState(previousState);
    }

    private void updateSliderValue(double mouseX) {
        float[] rect = activeSlider == 0 ? rdSliderRect : (activeSlider == 1 ? hbSliderRect : chSliderRect);
        float relativeX = Math.max(0, Math.min((float) mouseX - rect[0], rect[2]));
        float percentage = relativeX / rect[2];

        if (activeSlider == 0) {
            World.RENDER_DISTANCE = 2 + Math.round(percentage * 14);
        } else if (activeSlider == 1) {
            Game.HOTBAR_SCALE = 0.2f + percentage * 0.6f;
        } else if (activeSlider == 2) {
            Game.CROSSHAIR_SIZE = 2.0f + percentage * 28.0f;
        }
    }

    public void render() {
        setup2DRendering();
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(game.getWindowHandle(), w, h);
        float currentW = w[0];
        float currentH = h[0];

        if (previousState == Game.GameState.MAIN_MENU) {
            drawTexturedBackground(backgroundTextureID, currentW, currentH);
        } else {
            drawDarkenedBackground(currentW, currentH);
        }

        String title = LanguageManager.get("options.title");
        glColor4f(1f, 1f, 1f, 1f);
        FontRenderer.drawString(title, (currentW - FontRenderer.getStringWidth(title)) / 2f, currentH * 0.09f);

        float margin = Math.max(24f, currentW * 0.06f);
        float gap = Math.max(18f, currentW * 0.025f);
        float panelY = currentH * 0.20f;
        float panelH = currentH * 0.62f;
        float panelW = Math.min(570f, (currentW - margin * 2f - gap) / 2f);
        float totalW = panelW * 2f + gap;
        float leftX = (currentW - totalW) / 2f;
        float rightX = leftX + panelW + gap;

        drawPanel(leftX, panelY, panelW, panelH, LanguageManager.get("options.category.visual"));
        drawPanel(rightX, panelY, panelW, panelH, LanguageManager.get("options.category.gameplay"));

        float contentX = leftX + 22f;
        float sliderW = panelW - 44f;
        float firstSliderY = panelY + 88f;
        float sliderSpacing = Math.max(76f, (panelH - 130f) / 3f);

        drawSlider(contentX, firstSliderY, sliderW,
                LanguageManager.get("options.render_distance") + ": " + World.RENDER_DISTANCE,
                (World.RENDER_DISTANCE - 2) / 14.0f, rdSliderRect);
        int hbPercent = (int) ((Game.HOTBAR_SCALE - 0.2f) / 0.6f * 100);
        drawSlider(contentX, firstSliderY + sliderSpacing, sliderW,
                LanguageManager.get("options.hotbar_size") + ": " + hbPercent + "%",
                (Game.HOTBAR_SCALE - 0.2f) / 0.6f, hbSliderRect);
        drawSlider(contentX, firstSliderY + sliderSpacing * 2f, sliderW,
                LanguageManager.get("options.crosshair_size") + ": " + (int) Game.CROSSHAIR_SIZE,
                (Game.CROSSHAIR_SIZE - 2.0f) / 28.0f, chSliderRect);

        float buttonX = rightX + 22f;
        float buttonW = panelW - 44f;
        float buttonH = Math.min(34f, Math.max(25f, panelH * 0.11f));
        float buttonGap = Math.max(14f, (panelH - 88f - buttonH * 4f) / 3f);
        float firstButtonY = panelY + 76f;

        String langName = LanguageManager.getCurrentLang().equals("pl_pl") ? "Polski" : "English";
        drawOptionButton(buttonX, firstButtonY, buttonW, buttonH,
                LanguageManager.get("options.language") + ": " + langName, isLangHovered, langButtonRect);
        String commandsStatus = Game.COMMANDS_ENABLED ? LanguageManager.get("gui.on") : LanguageManager.get("gui.off");
        drawOptionButton(buttonX, firstButtonY + buttonH + buttonGap, buttonW, buttonH,
                LanguageManager.get("options.commands") + ": " + commandsStatus, isCommandsHovered, commandsButtonRect);
        String swapStatus = Game.SWAP_MOUSE_BUTTONS ? LanguageManager.get("gui.on") : LanguageManager.get("gui.off");
        drawOptionButton(buttonX, firstButtonY + (buttonH + buttonGap) * 2f, buttonW, buttonH,
                LanguageManager.get("options.swap_mouse_buttons") + ": " + swapStatus,
                isSwapMouseButtonsHovered, swapMouseButtonsRect);
        drawOptionButton(buttonX, firstButtonY + (buttonH + buttonGap) * 3f, buttonW, buttonH,
                LanguageManager.get("options.keybinds"), isKeybindsHovered, keybindsButtonRect);

        float doneW = Math.min(240f, currentW * 0.34f);
        float doneH = 32f;
        float doneX = (currentW - doneW) / 2f;
        float doneY = currentH * 0.88f;
        setRect(doneButtonRect, doneX, doneY, doneW, doneH);
        GuiRenderer.drawButton(doneX, doneY, doneW, doneH,
                LanguageManager.get("gui.done"), isDoneHovered, buttonTextureID);

        restore3DRendering();
    }

    private void drawPanel(float x, float y, float w, float h, String title) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.035f, 0.035f, 0.035f, 0.78f);
        drawSolidQuad(x, y, w, h);
        glColor4f(0.72f, 0.72f, 0.72f, 0.9f);
        drawOutlinedQuad(x, y, w, h);
        glColor4f(0.22f, 0.22f, 0.22f, 0.95f);
        drawSolidQuad(x + 1f, y + 1f, w - 2f, 38f);
        glDisable(GL_BLEND);

        glColor4f(1f, 1f, 1f, 1f);
        FontRenderer.drawString(title, x + 16f, y + 25f);
    }

    private void drawOptionButton(float x, float y, float w, float h, String label,
                                  boolean hovered, float[] rect) {
        setRect(rect, x, y, w, h);
        GuiRenderer.drawButton(x, y, w, h, label, hovered, buttonTextureID);
    }

    private void drawSlider(float x, float y, float w, String label, float percentage, float[] hitBox) {
        float h = 24f;
        setRect(hitBox, x, y, w, h);

        float labelX = x + (w - FontRenderer.getStringWidth(label)) / 2f;
        glColor4f(1f, 1f, 1f, 1f);
        FontRenderer.drawString(label, labelX, y - 15f);

        float trackY = y + 9f;
        glColor4f(0.12f, 0.12f, 0.12f, 1f);
        drawSolidQuad(x, trackY + 4f, w, 10f);
        glColor4f(0f, 0f, 0f, 1f);
        drawOutlinedQuad(x, trackY + 4f, w, 10f);

        float clampedPercentage = Math.max(0f, Math.min(1f, percentage));
        float knobW = 14f;
        float knobX = x + clampedPercentage * (w - knobW);
        glColor4f(0.72f, 0.72f, 0.72f, 1f);
        drawSolidQuad(knobX, trackY, knobW, 18f);
        glColor4f(0f, 0f, 0f, 1f);
        drawOutlinedQuad(knobX, trackY, knobW, 18f);
    }

    private void setRect(float[] rect, float x, float y, float w, float h) {
        rect[0] = x;
        rect[1] = y;
        rect[2] = w;
        rect[3] = h;
    }

    private void drawTexturedBackground(int textureID, float w, float h) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 1f);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2f(0, 0);
        glTexCoord2f(1, 0); glVertex2f(w, 0);
        glTexCoord2f(1, 1); glVertex2f(w, h);
        glTexCoord2f(0, 1); glVertex2f(0, h);
        glEnd();
        glDisable(GL_TEXTURE_2D);
    }

    private void drawDarkenedBackground(float w, float h) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0f, 0f, 0.6f);
        glBegin(GL_QUADS);
        glVertex2f(0, 0); glVertex2f(w, 0); glVertex2f(w, h); glVertex2f(0, h);
        glEnd();
        glDisable(GL_BLEND);
    }

    private void drawSolidQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y); glVertex2f(x + w, y); glVertex2f(x + w, y + h); glVertex2f(x, y + h);
        glEnd();
    }

    private void drawOutlinedQuad(float x, float y, float w, float h) {
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y); glVertex2f(x + w, y); glVertex2f(x + w, y + h); glVertex2f(x, y + h);
        glEnd();
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
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2]
                && mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}

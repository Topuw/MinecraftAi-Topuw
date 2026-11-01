package com.minecraftai.gui;

import com.minecraftai.Game;
import com.minecraftai.renderer.FontRenderer;
import com.minecraftai.renderer.TextureLoader;
import com.minecraftai.core.World;
import com.minecraftai.core.WorldSaveManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class OptionsMenu {
    private static final String BACKGROUND_PATH = "/assets/textures/misc/background.png";
    private static final String BUTTON_PATH = "/assets/textures/gui/button.png";

    private Game game;
    private int backgroundTextureID;
    private int buttonTextureID;

    private final float[] sliderRect = new float[4];
    private final float[] sliderKnobRect = new float[4];
    private final float[] doneButtonRect = new float[4];
    private final float[] commandsButtonRect = new float[4];

    private boolean isDraggingSlider = false;
    private boolean isDoneHovered = false;
    private boolean isCommandsHovered = false;
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

        if (isDraggingSlider) {
            updateSliderValue(x);
        }
    }

    public void handleMouseClick(double x, double y, int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW_PRESS) {
                if (isDoneHovered) {
                    goBack();
                } else if (isCommandsHovered) {
                    Game.COMMANDS_ENABLED = !Game.COMMANDS_ENABLED;
                    WorldSaveManager.saveOptions();
                } else if (isMouseOver(x, y, sliderRect)) {
                    isDraggingSlider = true;
                    updateSliderValue(x);
                }
            } else if (action == GLFW_RELEASE) {
                if (isDraggingSlider) {
                    isDraggingSlider = false;
                    WorldSaveManager.saveOptions();
                }
            }
        }
    }

    public void goBack() {
        WorldSaveManager.saveOptions();
        game.setGameState(previousState);
    }

    private void updateSliderValue(double mouseX) {
        float sliderX = sliderRect[0];
        float sliderWidth = sliderRect[2];

        float relativeX = Math.max(0, Math.min((float)mouseX - sliderX, sliderWidth));
        float percentage = relativeX / sliderWidth;

        int newValue = 2 + Math.round(percentage * 30);
        World.RENDER_DISTANCE = newValue;
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

        String title = "OPTIONS";
        float titleX = (currentW - FontRenderer.getStringWidth(title)) / 2;
        float titleY = currentH * 0.25f;

        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        FontRenderer.drawString(title, titleX + 1.0f, titleY + 1.0f);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        FontRenderer.drawString(title, titleX, titleY);

        float sliderWidth = 310;
        float sliderHeight = 40;
        float sliderX = (currentW - sliderWidth) / 2;
        float sliderY = currentH * 0.45f;
        float sliderBarHeight = 12;

        sliderRect[0] = sliderX;
        sliderRect[1] = sliderY + (sliderHeight - sliderBarHeight) / 2;
        sliderRect[2] = sliderWidth;
        sliderRect[3] = sliderBarHeight;

        glColor4f(0.4f, 0.4f, 0.4f, 1.0f);
        drawSolidQuad(sliderX, sliderY + (sliderHeight - sliderBarHeight) / 2, sliderWidth, sliderBarHeight);

        glColor4f(0.2f, 0.2f, 0.2f, 1.0f);
        drawOutlinedQuad(sliderX, sliderY + (sliderHeight - sliderBarHeight) / 2, sliderWidth, sliderBarHeight);

        float knobWidth = 14;
        float knobHeight = 24;
        float currentPercentage = (World.RENDER_DISTANCE - 2) / 30.0f;
        float knobX = sliderX + (currentPercentage * (sliderWidth - knobWidth));
        float knobY = sliderY + (sliderHeight / 2) - (knobHeight / 2);

        sliderKnobRect[0] = knobX;
        sliderKnobRect[1] = knobY;
        sliderKnobRect[2] = knobWidth;
        sliderKnobRect[3] = knobHeight;

        glColor4f(0.08f, 0.08f, 0.08f, 1.0f);
        drawSolidQuad(knobX, knobY, knobWidth, knobHeight);

        glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
        drawOutlinedQuad(knobX, knobY, knobWidth, knobHeight);

        String rdText = "Render Distance: " + World.RENDER_DISTANCE;
        float textX = sliderX + (sliderWidth - FontRenderer.getStringWidth(rdText)) / 2;
        float textY = sliderY - FontRenderer.FONT_HEIGHT - 6;

        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        FontRenderer.drawString(rdText, textX + 1, textY + 1);

        glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
        FontRenderer.drawString(rdText, textX, textY);

        float buttonWidth = 200;
        float buttonHeight = 40;
        float buttonX = (currentW - buttonWidth) / 2;
        float cmdY = currentH * 0.60f;
        float doneY = currentH * 0.75f;

        commandsButtonRect[0] = buttonX; commandsButtonRect[1] = cmdY; commandsButtonRect[2] = buttonWidth; commandsButtonRect[3] = buttonHeight;
        doneButtonRect[0] = buttonX; doneButtonRect[1] = doneY; doneButtonRect[2] = buttonWidth; doneButtonRect[3] = buttonHeight;

        String cmdText = "Commands: " + (Game.COMMANDS_ENABLED ? "ON" : "OFF");
        GuiRenderer.drawButton(buttonX, cmdY, buttonWidth, buttonHeight, cmdText, isCommandsHovered, buttonTextureID);
        GuiRenderer.drawButton(buttonX, doneY, buttonWidth, buttonHeight, "Done", isDoneHovered, buttonTextureID);

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

    private void drawSolidQuad(float x, float y, float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
    }

    private void drawOutlinedQuad(float x, float y, float w, float h) {
        glBegin(GL_LINE_LOOP);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
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
        return mouseX >= rect[0] && mouseX <= rect[0] + rect[2] &&
                mouseY >= rect[1] && mouseY <= rect[1] + rect[3];
    }
}
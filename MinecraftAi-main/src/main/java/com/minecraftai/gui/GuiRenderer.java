package com.minecraftai.gui;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;
import com.minecraftai.renderer.TextureAtlas;
import com.minecraftai.renderer.FontRenderer;

import static org.lwjgl.opengl.GL11.*;

public class GuiRenderer {

    public static void draw3DBlock(float centerX, float centerY, float size, ItemType type, float currentW, float currentH) {
        if (type == ItemType.STICK || type == ItemType.WOODEN_PICKAXE || type == ItemType.KEBAB) {
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, currentW, currentH, 0, -100.0f, 100.0f);

            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();

            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, type.getTextureId());
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            float r = size * 0.4f;
            glBegin(GL_QUADS);
            glTexCoord2f(0.0f, 0.0f); glVertex2f(centerX - r, centerY - r);
            glTexCoord2f(1.0f, 0.0f); glVertex2f(centerX + r, centerY - r);
            glTexCoord2f(1.0f, 1.0f); glVertex2f(centerX + r, centerY + r);
            glTexCoord2f(0.0f, 1.0f); glVertex2f(centerX - r, centerY + r);
            glEnd();

            glDisable(GL_BLEND);
            glDisable(GL_TEXTURE_2D);

            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();

            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            return;
        }

        float r = size * 0.23f;

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, currentW, currentH, 0, -100.0f, 100.0f);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glEnable(GL_DEPTH_TEST);
        glClear(GL_DEPTH_BUFFER_BIT);

        glTranslatef(centerX, centerY, 0.0f);
        glRotatef(-30.0f, 1.0f, 0.0f, 0.0f);
        glRotatef(45.0f, 0.0f, 1.0f, 0.0f);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.TOP)),
                 -r, -r, -r,
                  r, -r, -r,
                  r, -r,  r,
                 -r, -r,  r);

        glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.BOTTOM)),
                 -r,  r, -r,
                  r,  r, -r,
                  r,  r,  r,
                 -r,  r,  r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.EAST)),
                  r, -r, -r,
                  r, -r,  r,
                  r,  r,  r,
                  r,  r, -r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.WEST)),
                 -r, -r, -r,
                 -r, -r,  r,
                 -r,  r,  r,
                 -r,  r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.NORTH)),
                 -r, -r, -r,
                  r, -r, -r,
                  r,  r, -r,
                 -r,  r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.SOUTH)),
                 -r, -r,  r,
                  r, -r,  r,
                  r,  r,  r,
                 -r,  r,  r);

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    private static void drawFace(float[] uv, float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3) {
        glBegin(GL_QUADS);
        glTexCoord2f(uv[0], uv[1]); glVertex3f(x0, y0, z0);
        glTexCoord2f(uv[2], uv[1]); glVertex3f(x1, y1, z1);
        glTexCoord2f(uv[2], uv[3]); glVertex3f(x2, y2, z2);
        glTexCoord2f(uv[0], uv[3]); glVertex3f(x3, y3, z3);
        glEnd();
    }

    private static int getBlockTextureIndex(ItemType type, Block.Face face) {
        switch (type) {
            case DIRT:
                return 1;
            case STONE:
                return 2;
            case COBBLESTONE:
                return 3;
            case LOG:
                if (face == Block.Face.TOP || face == Block.Face.BOTTOM) {
                    return 5;
                } else {
                    return 4;
                }
            case PLANKS:
                return 9;
            case CRAFTING_TABLE:
                if (face == Block.Face.TOP) {
                    return 10;
                } else if (face == Block.Face.BOTTOM) {
                    return 9;
                } else if (face == Block.Face.NORTH) { // front representation
                    return 12;
                } else {
                    return 11;
                }
            case FURNACE:
                if (face == Block.Face.TOP || face == Block.Face.BOTTOM) {
                    return 13;
                } else if (face == Block.Face.NORTH) { // front representation
                    return 15;
                } else {
                    return 14;
                }
            case KEBAB_ORE:
                return 17;
            case KEBAB_BLOCK:
                return 18;
            default:
                return 0;
        }
    }

    public static void drawButton(float x, float y, float w, float h, String text, boolean hovered, int buttonTextureID) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, buttonTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (hovered) {
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        }

        // Draw button using 3-slice cropping/stretching from the middle
        float leftU = 4.0f / 200.0f;
        float rightU = 196.0f / 200.0f;
        
        // Calculate U coordinates for middle section: crop if w < 200, stretch if w >= 200
        float midUStart, midUEnd;
        if (w < 200.0f) {
            float midTextureWidth = w - 8.0f;
            midUStart = (100.0f - midTextureWidth / 2.0f) / 200.0f;
            midUEnd = (100.0f + midTextureWidth / 2.0f) / 200.0f;
        } else {
            midUStart = 4.0f / 200.0f;
            midUEnd = 196.0f / 200.0f;
        }

        glBegin(GL_QUADS);
        // Left part (width 4)
        glTexCoord2f(0.0f, 0.0f); glVertex2f(x, y);
        glTexCoord2f(leftU, 0.0f); glVertex2f(x + 4, y);
        glTexCoord2f(leftU, 1.0f); glVertex2f(x + 4, y + h);
        glTexCoord2f(0.0f, 1.0f); glVertex2f(x, y + h);

        // Middle part (width w - 8)
        glTexCoord2f(midUStart, 0.0f); glVertex2f(x + 4, y);
        glTexCoord2f(midUEnd, 0.0f); glVertex2f(x + w - 4, y);
        glTexCoord2f(midUEnd, 1.0f); glVertex2f(x + w - 4, y + h);
        glTexCoord2f(midUStart, 1.0f); glVertex2f(x + 4, y + h);

        // Right part (width 4)
        glTexCoord2f(rightU, 0.0f); glVertex2f(x + w - 4, y);
        glTexCoord2f(1.0f, 0.0f); glVertex2f(x + w, y);
        glTexCoord2f(1.0f, 1.0f); glVertex2f(x + w, y + h);
        glTexCoord2f(rightU, 1.0f); glVertex2f(x + w - 4, y + h);
        glEnd();

        glDisable(GL_TEXTURE_2D);

        // Draw text with shadow
        float textWidth = FontRenderer.getStringWidth(text);
        float textX = x + (w - textWidth) / 2.0f;
        float textY = y + (h + FontRenderer.FONT_HEIGHT) / 2.0f - 4.0f;

        // Shadow
        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        FontRenderer.drawString(text, textX + 1.0f, textY + 1.0f);

        // Text
        if (hovered) {
            glColor4f(1.0f, 1.0f, 0.6f, 1.0f);
        } else {
            glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
        }
        FontRenderer.drawString(text, textX, textY);
    }
}
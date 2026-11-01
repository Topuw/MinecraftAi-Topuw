package com.minecraftai.gui;

import com.minecraftai.core.Block;
import com.minecraftai.core.ItemType;
import com.minecraftai.renderer.TextureAtlas;
import com.minecraftai.renderer.FontRenderer;

import static org.lwjgl.opengl.GL11.*;

public class GuiRenderer {

    public static void draw3DBlock(float centerX, float centerY, float size, ItemType type, float currentW, float currentH) {
        if (isSprite(type)) {
            renderSprite(centerX, centerY, size * 0.4f, type, currentW, currentH);
            return;
        }

        float r = size * 0.23f;
        setup3DGUI(centerX, centerY, currentW, currentH);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());

        renderBlock(r, type, null);

        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
    }

    public static void draw3DBlockInHand(ItemType type) {
        if (isSprite(type)) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, type.getTextureId());
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            float r = 0.5f;
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0); glVertex3f(-r, -r, 0);
            glTexCoord2f(1, 0); glVertex3f(r, -r, 0);
            glTexCoord2f(1, 1); glVertex3f(r, r, 0);
            glTexCoord2f(0, 1); glVertex3f(-r, r, 0);
            glEnd();
            return;
        }

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, TextureAtlas.getAtlasTextureID());
        glEnable(GL_DEPTH_TEST);
        renderBlock(0.5f, type, null);
    }

    private static boolean isSprite(ItemType type) {
        return type == ItemType.STICK || type == ItemType.WOODEN_PICKAXE ||
               type == ItemType.STONE_PICKAXE || type == ItemType.GREEN_DYE || type == ItemType.KEBAB || 
               type == ItemType.MASLO_INGOT || type == ItemType.MASLO_PICKAXE ||
               type == ItemType.CLAY_BALL || type == ItemType.BRICK;
    }

    private static void renderSprite(float centerX, float centerY, float r, ItemType type, float currentW, float currentH) {
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
    }

    private static void setup3DGUI(float centerX, float centerY, float currentW, float currentH) {
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
    }

    private static void renderBlock(float r, ItemType type, Block.Face facing) {
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.TOP)),
                 -r, -r, -r, r, -r, -r, r, -r, r, -r, -r, r);

        glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.BOTTOM)),
                 -r, r, -r, r, r, -r, r, r, r, -r, r, r);

        glColor4f(0.6f, 0.6f, 0.6f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.EAST)),
                 r, -r, -r, r, -r, r, r, r, r, r, r, -r);

        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.WEST)),
                 -r, -r, -r, -r, -r, r, -r, r, r, -r, r, -r);

        glColor4f(0.8f, 0.8f, 0.8f, 1.0f);
        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.NORTH)),
                 -r, -r, -r, r, -r, -r, r, r, -r, -r, r, -r);

        drawFace(TextureAtlas.getUV(getBlockTextureIndex(type, Block.Face.SOUTH)),
                 -r, -r, r, r, -r, r, r, r, r, -r, r, r);
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
            case DIRT: return 1;
            case SAND: return 23;
            case CACTUS: return face == Block.Face.TOP ? 25 : face == Block.Face.BOTTOM ? 26 : 24;
            case SANDSTONE: return face == Block.Face.TOP ? 28 : face == Block.Face.BOTTOM ? 29 : 27;
            case GLASS: return 30;
            case GREEN_TERRACOTTA: return 31;
            case GRASS_BLOCK: return 0;
            case LEAVES: return 6;
            case BEDROCK: return 8;
            case WATER: return 7;
            case FLOWING_WATER: return 7;
            case STONE: return 2;
            case COBBLESTONE: return 3;
            case LOG: return (face == Block.Face.TOP || face == Block.Face.BOTTOM) ? 5 : 4;
            case PLANKS: return 9;
            case CRAFTING_TABLE:
                if (face == Block.Face.TOP) return 10;
                if (face == Block.Face.BOTTOM) return 9;
                if (face == Block.Face.NORTH) return 12;
                return 11;
            case FURNACE:
                if (face == Block.Face.TOP || face == Block.Face.BOTTOM) return 13;
                if (face == Block.Face.NORTH) return 15;
                return 14;
            case KEBAB_ORE: return 17;
            case KEBAB_BLOCK: return 18;
            case MASLO_ORE: return 19;
            case CLAY: return 20;
            case TERRACOTTA: return 21;
            case BRICKS: return 22;
            default: return 0;
        }
    }

    public static void drawButton(float x, float y, float w, float h, String text, boolean hovered, int buttonTextureID) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, buttonTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        if (hovered) glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        else glColor4f(0.8f, 0.8f, 0.8f, 1.0f);

        float leftU = 4.0f / 200.0f;
        float rightU = 196.0f / 200.0f;
        
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f); glVertex2f(x, y);
        glTexCoord2f(leftU, 0.0f); glVertex2f(x + 4, y);
        glTexCoord2f(leftU, 1.0f); glVertex2f(x + 4, y + h);
        glTexCoord2f(0.0f, 1.0f); glVertex2f(x, y + h);

        glTexCoord2f(leftU, 0.0f); glVertex2f(x + 4, y);
        glTexCoord2f(rightU, 0.0f); glVertex2f(x + w - 4, y);
        glTexCoord2f(rightU, 1.0f); glVertex2f(x + w - 4, y + h);
        glTexCoord2f(leftU, 1.0f); glVertex2f(x + 4, y + h);

        glTexCoord2f(rightU, 0.0f); glVertex2f(x + w - 4, y);
        glTexCoord2f(1.0f, 0.0f); glVertex2f(x + w, y);
        glTexCoord2f(1.0f, 1.0f); glVertex2f(x + w, y + h);
        glTexCoord2f(rightU, 1.0f); glVertex2f(x + w - 4, y + h);
        glEnd();

        glDisable(GL_TEXTURE_2D);

        float textWidth = FontRenderer.getStringWidth(text);
        float textX = x + (w - textWidth) / 2.0f;
        float textY = y + (h + FontRenderer.FONT_HEIGHT) / 2.0f - 4.0f;

        glColor4f(0.0f, 0.0f, 0.0f, 0.5f);
        FontRenderer.drawString(text, textX + 1.0f, textY + 1.0f);

        if (hovered) glColor4f(1.0f, 1.0f, 0.6f, 1.0f);
        else glColor4f(0.9f, 0.9f, 0.9f, 1.0f);
        FontRenderer.drawString(text, textX, textY);
    }
}

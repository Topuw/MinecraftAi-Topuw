package com.minecraftai.renderer;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

public class FontRenderer {
    private static final String REGULAR_FONT_PATH = "/assets/misc/regular.ttf";

    private static final int BITMAP_W = 1024;
    private static final int BITMAP_H = 1024;
    public static final float FONT_HEIGHT = 18.0f;
    public static final float REGULAR_FONT_HEIGHT = 18.0f;

    private static int fontTextureID;
    private static STBTTBakedChar.Buffer charData;
    private static STBTTAlignedQuad alignedQuad;

    public static void initFont() {
        // Load regular.ttf as the sole font
        try (InputStream is = FontRenderer.class.getResourceAsStream(REGULAR_FONT_PATH)) {
            if (is == null) throw new IOException("Nie znaleziono pliku czcionki: " + REGULAR_FONT_PATH);

            byte[] fontBytes = is.readAllBytes();
            ByteBuffer ttf = BufferUtils.createByteBuffer(fontBytes.length);
            ttf.put(fontBytes).flip();

            charData = STBTTBakedChar.malloc(96);
            ByteBuffer bitmap = BufferUtils.createByteBuffer(BITMAP_W * BITMAP_H);

            STBTruetype.stbtt_BakeFontBitmap(ttf, FONT_HEIGHT, bitmap, BITMAP_W, BITMAP_H, 32, charData);

            fontTextureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, fontTextureID);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, BITMAP_W, BITMAP_H, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

            alignedQuad = STBTTAlignedQuad.malloc();

        } catch (IOException e) {
            throw new RuntimeException("Nie udało się załadować lub zainicjować czcionki: regular.ttf", e);
        }
    }

    public static void drawString(String text, float x, float y) {
        if (charData == null) return;

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, fontTextureID);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xPos = stack.floats(x);
            FloatBuffer yPos = stack.floats(y);

            glBegin(GL_QUADS);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= 32 && c < 32 + 96) {
                    STBTruetype.stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - 32, xPos, yPos, alignedQuad, true);

                    float x0 = alignedQuad.x0(); float y0 = alignedQuad.y0();
                    float x1 = alignedQuad.x1(); float y1 = alignedQuad.y1();
                    float s0 = alignedQuad.s0(); float t0 = alignedQuad.t0();
                    float s1 = alignedQuad.s1(); float t1 = alignedQuad.t1();

                    glTexCoord2f(s0, t0); glVertex2f(x0, y0);
                    glTexCoord2f(s1, t0); glVertex2f(x1, y0);
                    glTexCoord2f(s1, t1); glVertex2f(x1, y1);
                    glTexCoord2f(s0, t1); glVertex2f(x0, y1);
                }
            }

            glEnd();
        }

        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_TEXTURE_2D);
    }

    public static void drawString(String text, float x, float y, float scale) {
        if (scale == 1.0f) {
            drawString(text, x, y);
            return;
        }
        glPushMatrix();
        glTranslatef(x, y, 0);
        glScalef(scale, scale, 1.0f);
        drawString(text, 0, 0);
        glPopMatrix();
    }

    public static void drawStringRegular(String text, float x, float y) {
        drawString(text, x, y);
    }

    public static float getStringWidth(String text) {
        if (charData == null) return 0;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xPos = stack.floats(0);
            FloatBuffer yPos = stack.floats(0);

            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= 32 && c < 32 + 96) {
                    STBTruetype.stbtt_GetBakedQuad(charData, BITMAP_W, BITMAP_H, c - 32, xPos, yPos, q, false);
                }
            }

            return xPos.get(0);
        }
    }

    public static float getStringWidthRegular(String text) {
        return getStringWidth(text);
    }
}